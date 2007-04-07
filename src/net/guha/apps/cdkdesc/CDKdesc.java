package net.guha.apps.cdkdesc;

import net.guha.apps.cdkdesc.ui.*;
import net.guha.ui.checkboxtree.CheckBoxTreeUtils;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.qsar.IDescriptor;

import javax.swing.*;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author Rajarshi Guha
 */
public class CDKdesc extends JFrame implements DropTargetListener {

    private ApplicationUI ui;
    private DescriptorTree descriptorTree;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton goButton;

    private DescriptorSwingWorker task;
    private Timer timer;

    private File tempFile;

    boolean wasCancelled = false;

    private DropTarget dropTarget;


    public CDKdesc() {
        super("CDK Descriptor Calculator");

        Calendar cal = new GregorianCalendar();
        long time = cal.getTimeInMillis();

        try {
            tempFile = File.createTempFile("cdkdesc", String.valueOf(time));
        } catch (IOException e) {
            e.printStackTrace();
        }
        tempFile.deleteOnExit();

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }


        getContentPane().setLayout(new BorderLayout());
        ApplicationMenu appMenu = new ApplicationMenu();
        setJMenuBar(appMenu.createMenu());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });


        goButton = new JButton("Go");
        goButton.setName("go");
        goButton.  addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                goApp(e);
            }
        });


        progressBar = new JProgressBar(0, 1);
        //progressBar.setBorder(new EmptyBorder(4, 0, 0, 0));

        statusLabel = new JLabel("Ready               ");

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.EAST);

        Border emptyBorder = new EmptyBorder(4, 2, 4, 2);
        Border lowerEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(lowerEtched, emptyBorder));
        progressBar.setVisible(false);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(2, 4, 2, 4));
        bottomPanel.add(goButton, BorderLayout.NORTH);
        //bottomPanel.add(progressBar, BorderLayout.SOUTH);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        ui = new ApplicationUI();

        try {
            descriptorTree = new DescriptorTree(true);
        } catch (CDKException e) {
            System.out.println("e = " + e);
        }
        JScrollPane sp = new JScrollPane(descriptorTree.getTree(),
                VERTICAL_SCROLLBAR_AS_NEEDED,
                HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ui.getSubpanel().add(sp, BorderLayout.CENTER);

        getContentPane().add(ui.getPanel(), BorderLayout.CENTER);

        dropTarget = new DropTarget(ui.getSdfFileTextField(), this);

    }


    private void doSave() {
        if (AppOptions.getInstance().getOutputMethod().equals(CDKDescConstants.OUTPUT_CSV) ||
                AppOptions.getInstance().getOutputMethod().equals(CDKDescConstants.OUTPUT_SPC) ||
                AppOptions.getInstance().getOutputMethod().equals(CDKDescConstants.OUTPUT_TAB) ||
                AppOptions.getInstance().getOutputMethod().equals(CDKDescConstants.OUTPUT_SDF)
                ) {
            try {
                FileChannel srcChannel = new FileInputStream(tempFile).getChannel();
                FileChannel dstChannel = new FileOutputStream(ui.getOutFileTextField().getText()).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private double round(double num, int decimalPlaces) {
        int intPart = (int) num;
        double decPart = num - intPart;

        double tmp1 = decPart * Math.pow(10, decimalPlaces);
        int tmp2 = Math.round((float) tmp1);

        return intPart + (double) tmp2 / Math.pow(10, decimalPlaces);
    }

    private void goApp(ActionEvent e) {
        if (ui.getSdfFileTextField().getText().equals("")) return;
        if (ui.getOutFileTextField().getText().equals("")) return;

        progressBar.setVisible(true);
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(true);

        if (((JButton) e.getSource()).getName().equals("cancel")) {
            task.stop();
            return;
        }

        if (((JButton) e.getSource()).getName().equals("go")) {
            goButton.setName("cancel");
            goButton.setText("Cancel");
            wasCancelled = false;
        }


        List checkedDescriptors = CheckBoxTreeUtils.getCheckedLeaves(
                descriptorTree.getCheckTreeManager(), descriptorTree.getTree());
        List<IDescriptor> selectedDescriptors = new ArrayList<IDescriptor>();
        for (Object obj : checkedDescriptors) {
            TreePath procPath = (TreePath) obj;
            DescriptorTreeLeaf aLeaf = (DescriptorTreeLeaf) ((DefaultMutableTreeNode) procPath.getLastPathComponent()).getUserObject();
            selectedDescriptors.add(aLeaf.getInstance());
        }

        if (selectedDescriptors.size() == 0) {
            JOptionPane.showMessageDialog(null,
                    "You need to select one or more descriptors!",
                    "CDKDescUI Error",
                    JOptionPane.ERROR_MESSAGE);
            goButton.setName("go");
            goButton.setText("Go");
            progressBar.setValue(0);
            progressBar.setString("");
            return;
        }


        task = new DescriptorSwingWorker(selectedDescriptors, ui, progressBar, tempFile);
        if (task.getInputFormat().equals("invalid")) {
            goButton.setName("go");
            goButton.setText("Go");
            progressBar.setValue(0);
            progressBar.setString("");
            return;
        }

        final int totaLength = task.getLengthOfTask();
        //progressBar.setMaximum(totaLength);
        //progressBar.setValue(0);
        //progressBar.setString("");


        timer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int current = task.getCurrent();
                //progressBar.setValue(current);
                //progressBar.setString((int) round(100.0 * current / (double) totaLength, 0) + "%");
                //goButton.setText("Molecule "+current);

                statusLabel.setText("Mol. " + current);

                if (task.isDone()) {
                    timer.stop();
                    goButton.setName("go");
                    goButton.setText("Go");
                    progressBar.setIndeterminate(false);
                    progressBar.setString("Completed");
                    progressBar.setVisible(false);

                    doSave();


                    if (task.getExceptionList().size() > 0) {
                        ExceptionListDialog eld = new ExceptionListDialog(task.getExceptionList());
                        eld.setVisible(true);
                    }

                } else if (task.isCancelled()) {
                    timer.stop();
                    goButton.setName("go");
                    goButton.setText("Go");
                    //progressBar.setValue(0);
                    progressBar.setIndeterminate(false);
                    progressBar.setString("");
                    progressBar.setVisible(false);
                    wasCancelled = true;
                }
            }
        });

        if (wasCancelled) {
            wasCancelled = false;
            return;
        }

        task.go();
        timer.start();
    }


    private void shutdown() {
        System.exit(0);
    }


    public static void main(String[] args) {
        CDKdesc app = new CDKdesc();
        app.pack();
        app.setVisible(true);
    }

    public void dragEnter(DropTargetDragEvent dtde) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void dragOver(DropTargetDragEvent dtde) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void dragExit(DropTargetEvent dte) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void drop(DropTargetDropEvent dtde) {
        System.out.println("drop");
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        Transferable trans = dtde.getTransferable();
        boolean gotData = false;
        try {
            if (trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String s = (String) trans.getTransferData(DataFlavor.stringFlavor);
                this.ui.getSdfFileTextField().setText(s.substring(7));
                gotData = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dtde.dropComplete(gotData);
        }


    }

}
