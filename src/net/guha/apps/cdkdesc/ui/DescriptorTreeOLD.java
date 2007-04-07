package net.guha.apps.cdkdesc.ui;

import net.guha.ui.checkboxtree.CheckTreeManager;
import net.guha.ui.checkboxtree.CheckTreeSelectionModel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.Random;

/**
 * @author Rajarshi Guha
 */
public class DescriptorTreeOLD {
    private JTree tree;
    private CheckTreeManager checkTreeManager;
    private DefaultMutableTreeNode rootNode;

    public DescriptorTreeOLD(boolean expandRoot) {
        rootNode = new DefaultMutableTreeNode("All Descriptors");
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);

        Random rng = new Random();
        DefaultMutableTreeNode parentNode = addObject(null, "Topological", rootNode, treeModel);
        for (int j = 0; j < rng.nextInt(30); j++)
            addObject(parentNode, "Descriptor" + j, rootNode, treeModel);

        parentNode = addObject(null, "Geometric", rootNode, treeModel);
        for (int j = 0; j < rng.nextInt(30); j++)
            addObject(parentNode, "Descriptor" + j, rootNode, treeModel);

        parentNode = addObject(null, "Electronic", rootNode, treeModel);
        for (int j = 0; j < rng.nextInt(30); j++)
            addObject(parentNode, "Descriptor" + j, rootNode, treeModel);

        parentNode = addObject(null, "Miscellaneous", rootNode, treeModel);
        for (int j = 0; j < rng.nextInt(30); j++)
            addObject(parentNode, "Descriptor" + j, rootNode, treeModel);

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
