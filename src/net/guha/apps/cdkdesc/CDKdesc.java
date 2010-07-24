package net.guha.apps.cdkdesc;

import net.guha.apps.cdkdesc.interfaces.ISwingWorker;
import net.guha.apps.cdkdesc.ui.ApplicationMenu;
import net.guha.apps.cdkdesc.ui.ApplicationUI;
import net.guha.apps.cdkdesc.ui.DescriptorTree;
import net.guha.apps.cdkdesc.ui.DescriptorTreeLeaf;
import net.guha.apps.cdkdesc.ui.ExceptionListDialog;
import net.guha.apps.cdkdesc.workers.DescriptorSwingWorker;
import net.guha.apps.cdkdesc.workers.FingerprintSwingWorker;
import net.guha.ui.checkboxtree.CheckBoxTreeUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.qsar.IDescriptor;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * @author Rajarshi Guha
 */
public class CDKdesc extends JFrame {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private ApplicationUI ui;

    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton goButton;

    private ISwingWorker task;


    private Timer timer;

    private File tempFile;

    boolean wasCancelled = false;

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
        if (CDKDescUtils.isMacOs()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "CDKDescUI");
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        UIManager.put("ProgressBar.foreground", new java.awt.Color(156, 154, 206));
        UIManager.put("ProgressBar.background", java.awt.Color.lightGray);
        UIManager.put("Label.foreground", java.awt.Color.black);


        getContentPane().setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });


        goButton = new JButton("Go");
        goButton.setName("go");
        goButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ui.descriptorPaneIsSelected()) goApp(e);
                else goFingerprintApp(e);
            }
        });


        progressBar = new JProgressBar(0, 1);
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
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);


        DescriptorTree descriptorTree = null;
        try {
            descriptorTree = new DescriptorTree(true);
        } catch (CDKException e) {
            System.out.println("e = " + e);
        }
        ui = new ApplicationUI(descriptorTree);

//        JScrollPane scrollPane = new JScrollPane(descriptorTree.getTree(), VERTICAL_SCROLLBAR_AS_NEEDED,
//                HORIZONTAL_SCROLLBAR_AS_NEEDED);
//
//        JTabbedPane tabbedPane = new JTabbedPane();
//        tabbedPane.add("Descriptors", scrollPane);
//        tabbedPane.add("Fingerprints", new FingerprintPanel());

//        ui.getSubpanel().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(ui.getPanel(), BorderLayout.CENTER);

        ApplicationMenu appMenu = new ApplicationMenu(ui);
        setJMenuBar(appMenu.createMenu());
    }


    private void doSave() {
        if (AppOptions.getInstance().getOutputMethod().equals(CDKDescConstants.OUTPUT_CSV) ||
                AppOptions.getInstance().getOutputMethod().equals(CDKDescConstants.OUTPUT_SPC) ||
                AppOptions.getInstance().getOutputMethod().equals(CDKDescConstants.OUTPUT_TAB) ||
                AppOptions.getInstance().getOutputMethod().equals(CDKDescConstants.OUTPUT_ARFF) ||
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


    private void goApp(ActionEvent e) {

        if (ui.getSdfFileTextField().getText().equals("") ||
                ui.getOutFileTextField().getText().equals("")) {
            JOptionPane.showMessageDialog(null, "Must provide an input file and an output file",
                    "CDKDescUI Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        progressBar.setVisible(true);
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(true);

        if (((JButton) e.getSource()).getName().equals("cancel")) {
            task.stop();
            doSave();
            return;
        }

        if (((JButton) e.getSource()).getName().equals("go")) {
            goButton.setName("cancel");
            goButton.setText("Cancel");
            wasCancelled = false;
        }


        DescriptorTree descriptorTree = ui.getDescriptorTree();
        List checkedDescriptors = CheckBoxTreeUtils.getCheckedLeaves(
                descriptorTree.getCheckTreeManager(), descriptorTree.getTree());
        List<IDescriptor> selectedDescriptors = new ArrayList<IDescriptor>();
        for (Object obj : checkedDescriptors) {
            TreePath procPath = (TreePath) obj;
            DescriptorTreeLeaf aLeaf = (DescriptorTreeLeaf) ((DefaultMutableTreeNode) procPath.getLastPathComponent()).getUserObject();
            selectedDescriptors.add(aLeaf.getInstance());
        }
        Collections.sort(selectedDescriptors, CDKDescUtils.getDescriptorComparator());

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
                    statusLabel.setText("Completed (" + current + ")");

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
                    statusLabel.setText("Cancelled");
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

    private void goFingerprintApp(ActionEvent e) {

        if (ui.getSdfFileTextField().getText().equals("") ||
                ui.getOutFileTextField().getText().equals("")) {
            JOptionPane.showMessageDialog(null, "Must provide an input file and an output file",
                    "CDKDescUI Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        progressBar.setVisible(true);
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(true);

        if (((JButton) e.getSource()).getName().equals("cancel")) {
            task.stop();
            doSave();
            return;
        }

        if (((JButton) e.getSource()).getName().equals("go")) {
            goButton.setName("cancel");
            goButton.setText("Cancel");
            wasCancelled = false;
        }

        if (AppOptions.getSelectedFingerprintType() == null) {
            JOptionPane.showMessageDialog(null,
                    "You need to select a fingerprint type!",
                    "CDKDescUI Error",
                    JOptionPane.ERROR_MESSAGE);
            goButton.setName("go");
            goButton.setText("Go");
            progressBar.setValue(0);
            progressBar.setString("");
            return;
        }


        task = new FingerprintSwingWorker(ui, progressBar, tempFile);
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
                    statusLabel.setText("Completed (" + current + " in " + task.getElapsedTime() + "s)");

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
                    statusLabel.setText("Cancelled");
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


    private static void usage(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("cdkdescui [OPTIONS] inputfile\n", options);
        System.out.println("\nCDKDescUI v" + CDKDescConstants.VERSION + " Rajarshi Guha <rajarshi.guha@gmail.com>\n");
        System.exit(-1);
    }

    public static void main(String[] args) throws CDKException {
        String outputFile = "output.txt";
        String inputFile = null;
        String descriptorType = null;
        boolean descTypeSpecified = true;
        String fpType = null;
        boolean batchMode = false;
        boolean verbose = false;

        Options options = new Options();
        options.addOption("b", false, "Batch mode");
        options.addOption("h", false, "Help");
        options.addOption("v", false, "Verbose output");
        options.addOption("o", true, "Output file");
        options.addOption("t", true, "Descriptor type: all, topological, geometric, constitutional, electronic, hybrid");
        options.addOption("f", true, "Fingerprint type: estate, extended, graph, standard, pubchem, substructure");
        options.addOption("s", true, "A descriptor selection file. Overrides the descriptor type option");

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) usage(options);
            if (cmd.hasOption("b")) {
                batchMode = true;
                String[] tmp = cmd.getArgs();
                if (tmp.length != 1) {
                    System.out.println("ERROR: Must specify a single input file");
                    usage(options);
                } else inputFile = tmp[0];
            }
            if (cmd.hasOption("v")) verbose = true;
            if (cmd.hasOption("o")) outputFile = cmd.getOptionValue("o");
            if (cmd.hasOption("f")) {
                fpType = cmd.getOptionValue("f");
            }
            if (cmd.hasOption("t")) {
                descriptorType = cmd.getOptionValue("t");
                String[] validTypes = new String[]{"all", "topological", "geometric", "protein", "constitutional", "electronic", "hybrid"};
                boolean typeOk = false;
                for (String s : validTypes) {
                    if (s.equals(descriptorType)) {
                        typeOk = true;
                        break;
                    }
                }
                if (!typeOk) usage(options);
            }
            if (cmd.hasOption("s")) {
                descTypeSpecified = false;
                descriptorType = cmd.getOptionValue("s");
            }
        } catch (ParseException e) {
            System.out.println("ERROR: Error parsing command line");
            System.exit(-1);
        }


        CDKdesc app = new CDKdesc();
        if (!batchMode) {
            app.pack();
            app.setVisible(true);
        } else {
            if (fpType == null && descriptorType == null) throw new CDKException("One of -t or -f must be specified");
            else if (fpType != null && descriptorType != null)
                throw new CDKException("ERROR: One of -t or -f must be specified");
            if (descriptorType != null)
                CDKdescBatch.batchDescriptor(inputFile, outputFile, descriptorType, descTypeSpecified, verbose);
            else CDKdescBatch.batchFingerprint(inputFile, outputFile, fpType, verbose);
        }
    }
}
