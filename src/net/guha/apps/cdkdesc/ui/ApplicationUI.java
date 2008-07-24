package net.guha.apps.cdkdesc.ui;

import net.guha.apps.cdkdesc.AppOptions;
import net.guha.apps.cdkdesc.CDKDescUtils;
import net.guha.ui.checkboxtree.CheckTreeManager;

import javax.swing.*;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author Rajarshi Guha
 */
public class ApplicationUI {

    private JTextField sdfFileTextField;
    private JTextField outFileTextField;

    private JPanel panel;
    private JButton sdfBrowseButton;
    private JButton outBrowseButton;

    private File sdFile;
    private File outFile;

    private JPanel subpanel;
    private JTabbedPane tabbedPane;

    private DescriptorTree descriptorTree;

    public JPanel getSubpanel() {
        return subpanel;
    }


    public JTextField getSdfFileTextField() {
        return sdfFileTextField;
    }

    public JTextField getOutFileTextField() {
        return outFileTextField;
    }

    public JPanel getPanel() {
        return panel;
    }

    public JButton getSdfBrowseButton() {
        return sdfBrowseButton;
    }

    public JButton getOutBrowseButton() {
        return outBrowseButton;
    }

    public DescriptorTree getDescriptorTree() {
        return descriptorTree;
    }

    public boolean descriptorPaneIsSelected() {
        return true;
    }

    public ApplicationUI(DescriptorTree descriptorTree) {

        this.descriptorTree = descriptorTree;

        subpanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(descriptorTree.getTree(), VERTICAL_SCROLLBAR_AS_NEEDED,
                HORIZONTAL_SCROLLBAR_AS_NEEDED);

        tabbedPane = new JTabbedPane();
        tabbedPane.add("Descriptors", scrollPane);
        tabbedPane.add("Fingerprints", new FingerprintPanel());
        subpanel.add(tabbedPane, BorderLayout.CENTER);

        sdfBrowseButton = new JButton("Browse");
        sdfBrowseButton.setName("sdfButton");
        sdfBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onBrowse(e);
            }
        });

        outBrowseButton = new JButton("Browse");
        outBrowseButton.setName("outButton");
        outBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onBrowse(e);
            }
        });


        panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();


        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 4, 2, 4);

        JLabel label = new JLabel("Input File");
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setVerticalAlignment(JLabel.CENTER);
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.0;
        panel.add(label, c);

        sdfFileTextField = new JTextField(10);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 0.0;
        panel.add(sdfFileTextField, c);

        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        panel.add(sdfBrowseButton, c);

        label = new JLabel("Output File");
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setVerticalAlignment(JLabel.CENTER);
        c.gridx = 0;
        c.gridy = 1;
        panel.add(label, c);

        outFileTextField = new JTextField(10);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        panel.add(outFileTextField, c);

        c.gridx = 2;
        c.gridy = 1;
        c.weightx = 0.0;
        panel.add(outBrowseButton, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.ipady = 10;
        c.fill = GridBagConstraints.BOTH;
        panel.add(subpanel, c);

    }

    public void checkSelectedDescriptors() {
        Map<String, Boolean> selDescMap = AppOptions.getInstance().getSelectedDescriptors();
        TreeNode rootNode = descriptorTree.getRootNode();
        CheckTreeManager checks = descriptorTree.getCheckTreeManager();

        checks.getSelectionModel().removeSelectionPath(new TreePath(rootNode));
        java.util.List<TreePath> selectedPaths = new ArrayList<TreePath>();
        for (int i = 0; i < rootNode.getChildCount(); i++) {  // the class nodes
            TreeNode childNode = rootNode.getChildAt(i);
            for (int j = 0; j < childNode.getChildCount(); j++) { // individual descriptors
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) childNode.getChildAt(j);
                TreePath path = new TreePath(node.getPath());
                DescriptorTreeLeaf leaf = (DescriptorTreeLeaf) node.getUserObject();
                String specRef = leaf.getSpec().getSpecificationReference();
                boolean isSelected = selDescMap.get(specRef);
                if (isSelected) selectedPaths.add(path);
            }
        }
        checks.getSelectionModel().setSelectionPaths(selectedPaths.toArray(new TreePath[]{}));
    }

    private void onBrowse(ActionEvent e) {
        String buttonName = ((JButton) e.getSource()).getName();
        JFileChooser fileDialog = new JFileChooser();
        int status = fileDialog.showOpenDialog(this.getPanel());
        if (status == JFileChooser.APPROVE_OPTION) {
            if (buttonName.equals("sdfButton")) {
                sdFile = fileDialog.getSelectedFile();
                sdfFileTextField.setText(sdFile.getAbsolutePath());

                // check to see if it's a SMILES file. If so,
                // disable parts of the descriptor tree that
                // cannot be evaluated for SMILES
                if (CDKDescUtils.isSMILESFormat(sdFile.getAbsolutePath())) {
                    DefaultMutableTreeNode root = descriptorTree.getRootNode();
                    CheckTreeManager checks = descriptorTree.getCheckTreeManager();
                    checks.getSelectionModel().removeSelectionPath(new TreePath(root));
                    for (int i = 0; i < root.getChildCount(); i++) {
                        TreeNode child = root.getChildAt(i);
                        if (child.toString().equals("topological")) {
                            TreePath path = new TreePath(new Object[]{root, child});
                            checks.getSelectionModel().setSelectionPath(path);
                            break;
                        }
                    }
                } else { // lets just select everything, since we might have had a SMI file
                    DefaultMutableTreeNode root = descriptorTree.getRootNode();
                    CheckTreeManager checks = descriptorTree.getCheckTreeManager();
                    checks.getSelectionModel().setSelectionPath(new TreePath(root));
                }
            } else {
                outFile = fileDialog.getSelectedFile();
                outFileTextField.setText(outFile.getAbsolutePath());
            }
        }
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        JFrame frame = new JFrame("Test Frame");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        ApplicationUI ui = new ApplicationUI(null);
        frame.add(ui.getPanel());
        frame.pack();
        frame.setVisible(true);
    }

}



