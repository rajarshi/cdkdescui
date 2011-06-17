package net.guha.apps.cdkdesc.workers;


import net.guha.apps.cdkdesc.AppOptions;
import net.guha.apps.cdkdesc.CDKDescConstants;
import net.guha.apps.cdkdesc.CDKDescUtils;
import net.guha.apps.cdkdesc.ExceptionInfo;
import net.guha.apps.cdkdesc.interfaces.ISwingWorker;
import net.guha.apps.cdkdesc.ui.ApplicationUI;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.EStateFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.GraphOnlyFingerprinter;
import org.openscience.cdk.fingerprint.HybridizationFingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.iterator.DefaultIteratingChemObjectReader;
import org.openscience.cdk.io.iterator.IteratingMDLReader;
import org.openscience.cdk.io.iterator.IteratingSMILESReader;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;


/**
 * @author Rajarshi Guha
 */
public class FingerprintSwingWorker implements ISwingWorker {

    private ApplicationUI ui;
    private List<ExceptionInfo> exceptionList;

    private String inputFormat = "mdl";
    private File tempFile;

    private int lengthOfTask = 1;
    private int current = 0;
    private int molCount = 0;
    private boolean done = false;
    private boolean canceled = false;

    private double elapsedTime;


    public FingerprintSwingWorker(ApplicationUI ui, JProgressBar progressBar, File tempFile) {
        this.ui = ui;
        this.tempFile = tempFile;

        exceptionList = new ArrayList<ExceptionInfo>();

        // see what type of file we have
        inputFormat = "invalid";
        if (CDKDescUtils.isSMILESFormat(ui.getSdfFileTextField().getText())) {
            inputFormat = "smi";
        } else if (CDKDescUtils.isMDLFormat(ui.getSdfFileTextField().getText())) {
            inputFormat = "mdl";
        }

        if (inputFormat.equals("invalid")) {
            done = true;
            canceled = true;
            progressBar.setIndeterminate(false);
            JOptionPane.showMessageDialog(null,
                    "Input file format was not recognized. It should be SDF or SMI" +
                            "\nYou should avoid supplying Markush structures since will be" +
                            "\nignored anyway",
                    "CDKDescUI Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void go() {
        final net.guha.apps.cdkdesc.SwingWorker worker = new net.guha.apps.cdkdesc.SwingWorker() {
            public Object construct() {
                current = 0;
                done = false;
                canceled = false;
                elapsedTime = System.currentTimeMillis();
                try {
                    return new ActualTask();
                } catch (CDKException e) {
                    System.out.println("Problem! Contact rajarshi.guha@gmail.com\n\n" + e.toString());
                    System.exit(0);
                }
                return null;
            }
        };
        worker.start();
    }

    public List<ExceptionInfo> getExceptionList() {
        return exceptionList;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public int getLengthOfTask() {
        return lengthOfTask;
    }

    public int getCurrent() {
        return molCount;
    }

    public void stop() {
        canceled = true;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isCancelled() {
        return canceled;
    }

    public double getElapsedTime() {
        return elapsedTime;
    }


    private IAtomContainer checkAndCleanMolecule(IAtomContainer molecule) throws CDKException {
        boolean isMarkush = false;
        for (IAtom atom : molecule.atoms()) {
            if (atom.getSymbol().equals("R")) {
                isMarkush = true;
                break;
            }
        }

        if (isMarkush) {
            throw new CDKException("Skipping Markush structure");
        }

        // Check for salts and such
        if (!ConnectivityChecker.isConnected(molecule)) {
            // lets see if we have just two parts if so, we assume its a salt and just work
            // on the larger part. Ideally we should have a check to ensure that the smaller
            //  part is a metal/halogen etc.
            IMoleculeSet fragments = ConnectivityChecker.partitionIntoMolecules(molecule);
            if (fragments.getMoleculeCount() > 2) {
                throw new CDKException("More than 2 components. Skipped");
            } else {
                IMolecule frag1 = fragments.getMolecule(0);
                IMolecule frag2 = fragments.getMolecule(1);
                if (frag1.getAtomCount() > frag2.getAtomCount()) molecule = frag1;
                else molecule = frag2;
            }
        }

        // Do the configuration
        try {
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
        } catch (CDKException e) {
            throw new CDKException("Error in atom typing" + e.toString());
        }

        // do a aromaticity check
        try {
            CDKHueckelAromaticityDetector.detectAromaticity(molecule);
        } catch (CDKException e) {
            throw new CDKException("Error in aromaticity detection");
        }

        return molecule;
    }

    class ActualTask {

        private boolean evalToTextFile(String sdfFileName, String outputFormat) throws CDKException {

            String fptype = AppOptions.getSelectedFingerprintType();
            IFingerprinter printer;
            if (fptype.equals("Standard")) printer = new Fingerprinter();
            else if (fptype.equals("Extended")) printer = new ExtendedFingerprinter();
            else if (fptype.equals("Graph only")) printer = new GraphOnlyFingerprinter();
            else if (fptype.equals("EState")) printer = new EStateFingerprinter();
            else if (fptype.equals("MACCS")) printer = new MACCSFingerprinter();
            else if (fptype.equals("Pubchem")) printer = new PubchemFingerprinter();
            else if (fptype.equals("Hybridization")) printer = new HybridizationFingerprinter();
            else printer = new SubstructureFingerprinter();

            String lineSep = System.getProperty("line.separator");
            String itemSep = " ";

            if (outputFormat.equals(CDKDescConstants.OUTPUT_TAB)) {
                itemSep = "\t";
            } else if (outputFormat.equals(CDKDescConstants.OUTPUT_CSV)) {
                itemSep = ",";
            } else if (outputFormat.equals(CDKDescConstants.OUTPUT_SPC)) {
                itemSep = " ";
            }

            DefaultIteratingChemObjectReader iterReader = null;
            try {
                BufferedWriter tmpWriter = new BufferedWriter(new FileWriter(tempFile));
                FileInputStream inputStream = new FileInputStream(sdfFileName);
                if (inputFormat.equals("smi")) iterReader = new IteratingSMILESReader(inputStream);
                else if (inputFormat.equals("mdl")) {
                    iterReader = new IteratingMDLReader(inputStream, DefaultChemObjectBuilder.getInstance());
                }


                molCount = 0;

                // lets get the header line first
                tmpWriter.write("CDKDescUI " + printer.getClass().getName() + " " + printer.getSize() + " bits" + lineSep);
                assert iterReader != null;
                while (iterReader.hasNext()) {  // loop over molecules
                    if (canceled) return false;
                    IMolecule molecule = (IMolecule) iterReader.next();

                    try {
                        molecule = (IMolecule) checkAndCleanMolecule(molecule);
                    } catch (CDKException e) {
                        exceptionList.add(new ExceptionInfo(molCount + 1, molecule, e, ""));
                        molCount++;
                        continue;
                    }

                    try {
                        BitSet fingerprint = printer.getFingerprint(molecule);
                        String title = (String) molecule.getProperty(CDKConstants.TITLE);
                        if (title == null) title = "Mol" + String.valueOf(molCount + 1);
                        tmpWriter.write(title + itemSep + fingerprint.toString() + lineSep);
                        tmpWriter.flush();
                        molCount++;
                    } catch (Exception e) {
                        exceptionList.add(new ExceptionInfo(molCount + 1, molecule, e, ""));
                        molCount++;
                    }
                }

                elapsedTime = ((System.currentTimeMillis() - elapsedTime) / 1000.0);

                iterReader.close();
                tmpWriter.close();
                done = true;
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            return true;
        }


        ActualTask() throws CDKException {
            String outputMethod = AppOptions.getInstance().getOutputMethod();
            String sdfFileName = ui.getSdfFileTextField().getText();
            boolean status = evalToTextFile(sdfFileName, outputMethod);
            if (!status) return;
        }
    }
}