package net.guha.apps.cdkdesc.ui;

import net.guha.apps.cdkdesc.AppOptions;
import net.guha.ui.checkboxtree.CheckTreeManager;
import net.guha.ui.checkboxtree.CheckTreeSelectionModel;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.IDescriptor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import static java.util.Collections.sort;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rajarshi Guha
 */
public class DescriptorTree {
    private JTree tree;
    private CheckTreeManager checkTreeManager;
    private DefaultMutableTreeNode rootNode;

    public DescriptorTree(boolean expandRoot) throws CDKException {
        rootNode = new DefaultMutableTreeNode("All Descriptors");
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel) {
            public String getToolTipText(MouseEvent e) {
                DefaultMutableTreeNode node = null;
                DescriptorTreeLeaf leafObject = null;
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if (path != null && path.getPathCount() == 3) {
                    node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    leafObject = (DescriptorTreeLeaf) node.getUserObject();
                }
                return node == null ? null : leafObject.getDefinition();
            }
        };
        ToolTipManager.sharedInstance().registerComponent(tree);


        String[] availableClasses = AppOptions.getInstance().getEngine().getAvailableDictionaryClasses();

        // make a HashMap for the available classes
        HashMap<String, DefaultMutableTreeNode> level1Map = new HashMap<String, DefaultMutableTreeNode>();
        for (String dictClass : availableClasses) {
            String[] tmp = dictClass.split("Descriptor");
            level1Map.put(dictClass, addObject(null, tmp[0], rootNode, treeModel));
        }

        List descInst = AppOptions.getEngine().getDescriptorInstances();
        List descSpec = AppOptions.getInstance().getEngine().getDescriptorSpecifications();
        int ndesc = descInst.size();

        List leaves = new ArrayList();
        for (int i = 0; i < ndesc; i++) {
            DescriptorSpecification spec = (DescriptorSpecification) descSpec.get(i);
            String definition = AppOptions.getInstance().getEngine().getDictionaryDefinition(spec);
            DescriptorTreeLeaf leaf = new DescriptorTreeLeaf((IDescriptor) descInst.get(i), definition);
            if (leaf.getName() == null || definition == null) {
                throw new CDKException("Seems that " + leaf.getInstance() + " is missing an entry in the OWL dictionary");
            } else
                leaves.add(leaf);
        }
        sort(leaves);

        for (Object object : leaves) {
            DescriptorTreeLeaf aLeaf = (DescriptorTreeLeaf) object;
            DescriptorSpecification spec = aLeaf.getSpec();
            String[] dictClass = AppOptions.getInstance().getEngine().getDictionaryClass(spec);
            if (dictClass == null || dictClass.length == 0) {
                System.err.println(spec.getImplementationIdentifier() + " : " + "Had no class entries in the dictionary!");
                continue;
            }
            DefaultMutableTreeNode parent = level1Map.get(dictClass[0]);
            addObject(parent, aLeaf, rootNode, treeModel);
        }


        checkTreeManager = new CheckTreeManager(tree);

        // expand the root node on first display
        TreePath path = new TreePath(treeModel.getPathToRoot(rootNode));

        if (expandRoot)
            tree.expandPath(new TreePath(rootNode));

        // select all the entries
        CheckTreeSelectionModel selModel = checkTreeManager.getSelectionModel();
        selModel.setSelectionPaths(new TreePath[]{path});


    }

    public JTree getTree() {
        return tree;
    }

    public CheckTreeManager getCheckTreeManager() {
        return checkTreeManager;
    }

    public DefaultMutableTreeNode getRootNode() {
        return rootNode;
    }

    private DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
                                             Object child,
                                             DefaultMutableTreeNode rootNode,
                                             DefaultTreeModel treeModel) {
        return addObject(parent, child, false, rootNode, treeModel);
    }

    private DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
                                             Object child,
                                             boolean shouldBeVisible,
                                             DefaultMutableTreeNode rootNode,
                                             DefaultTreeModel treeModel) {
        DefaultMutableTreeNode childNode =
                new DefaultMutableTreeNode(child);

        if (parent == null) {
            parent = rootNode;
        }

        treeModel.insertNodeInto(childNode, parent,
                parent.getChildCount());

        //Make sure the user can see the lovely new node.
        if (shouldBeVisible) {
//            tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        }
        return childNode;
    }
}
