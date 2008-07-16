package net.guha.apps.cdkdesc.ui;

import net.guha.apps.cdkdesc.AppOptions;
import net.guha.ui.checkboxtree.CheckTreeManager;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: rguha
 * Date: Jul 16, 2008
 * Time: 3:17:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class TreeHandler extends MouseAdapter implements TreeSelectionListener {
    private JTree mTree;
    private CheckTreeManager checkTreeManager;
    private TreePath mPath;
    private String mPathComponent;
    private int mCount;

    public TreeHandler(JTree tree, CheckTreeManager checkTreeManager) {
        mTree = tree;
        this.checkTreeManager = checkTreeManager;
    }

    public void mouseClicked(java.awt.event.MouseEvent event) {
        Object object = event.getSource();
        if (object == mTree)
            mTree_mouseClicked(event);
    }

    void mTree_mouseClicked(java.awt.event.MouseEvent event) {
        TreePath path = mTree.getPathForLocation(event.getX(), event.getY());
        if (path == null) return;
        if (event.getModifiers() == InputEvent.BUTTON1_MASK) {  // left click implies selection
            Map<String, Boolean> selDescMap = AppOptions.getInstance().getSelectedDescriptors();

            if (path.getPathCount() == 1) {
                TreeNode rootNode = (TreeNode) path.getLastPathComponent();
                int childCount = rootNode.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    TreeNode node = rootNode.getChildAt(i);
                    int grandChildCount = node.getChildCount();
                    for (int j = 0; j < grandChildCount; j++) {
                        DefaultMutableTreeNode grandChildNode = (DefaultMutableTreeNode) node.getChildAt(j);
                        TreePath childPath = new TreePath(grandChildNode.getPath());
                        boolean pathIsSelected = checkTreeManager.getSelectionModel().isPathSelected(childPath, true);
                        DescriptorTreeLeaf leaf = (DescriptorTreeLeaf) grandChildNode.getUserObject();
                        String specRef = leaf.getSpec().getSpecificationReference();
                        selDescMap.put(specRef, pathIsSelected);
                    }
                }
            } else if (path.getPathCount() == 2) {
                TreeNode node = (TreeNode) path.getLastPathComponent();
                int childCount = node.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                    TreePath childPath = new TreePath(childNode.getPath());
                    boolean pathIsSelected = checkTreeManager.getSelectionModel().isPathSelected(childPath, true);
                    DescriptorTreeLeaf leaf = (DescriptorTreeLeaf) childNode.getUserObject();
                    String specRef = leaf.getSpec().getSpecificationReference();
                    selDescMap.put(specRef, pathIsSelected);
                }
            } else if (path.getPathCount() == 3) {
                boolean pathIsSelected = checkTreeManager.getSelectionModel().isPathSelected(path, true);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                DescriptorTreeLeaf leaf = (DescriptorTreeLeaf) node.getUserObject();
                String specRef = leaf.getSpec().getSpecificationReference();
                selDescMap.put(specRef, pathIsSelected);
            }
        }
//        if (event.getModifiers() == InputEvent.BUTTON3_MASK) {//right button click on mouse
//            if ((mCount == 3) && (mPathComponent.equals("UR_NODE_NAME1"))) {
//                   NODE_NAME1_PopupMenu.show(mTree, event.getX(), event.getY());
//                System.out.println("mPathComponent = " + mPathComponent);
//            }
//        }
    }

    public void valueChanged(TreeSelectionEvent aTreeEvent) {
        mPath = aTreeEvent.getPath();
        mCount = mPath.getPathCount();
        mPathComponent = mPath.getPathComponent(mCount - 1).toString();
        if (mCount == 3) {

        }

    }
}
