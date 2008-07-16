package net.guha.apps.cdkdesc.ui;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;

/**
 * Created by IntelliJ IDEA.
 * User: rguha
 * Date: Jul 16, 2008
 * Time: 3:17:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class TreeHandler extends MouseAdapter implements TreeSelectionListener {
    private JTree mTree;
    private TreePath mPath;
    private String mPathComponent;
    private int mCount;

    public TreeHandler(JTree tree) {
        mTree = tree;
    }

    public void mouseClicked(java.awt.event.MouseEvent event) {
        Object object = event.getSource();
        if (object == mTree)
            mTree_mouseClicked(event);
    }

    void mTree_mouseClicked(java.awt.event.MouseEvent event) {
        TreePath path = mTree.getPathForLocation(event.getX(), event.getY());
        if (path == null) return;
        if (event.getModifiers() == InputEvent.BUTTON3_MASK) {  // left click implies selection
            if (path.getPathCount() == 1) {
                // select/deselect everything
            } else if (path.getPathCount() == 2) {
                // select/deselect all descs of this class
            } else if (path.getPathCount() == 3) {
                // select/deselect this descriptor
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
//                System.out.println("Hello from valueChanged");
        }

    }
}
