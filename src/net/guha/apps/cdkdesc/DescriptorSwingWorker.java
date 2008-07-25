package net.guha.apps.cdkdesc;


import net.guha.apps.cdkdesc.interfaces.ISwingWorker;
import net.guha.apps.cdkdesc.interfaces.ITextOutput;
import net.guha.apps.cdkdesc.output.ARFFTextOutput;
import net.guha.apps.cdkdesc.output.PlainTextOutput;
import net.guha.apps.cdkdesc.ui.ApplicationUI;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.MDLWriter;
import org.openscience.cdk.io.iterator.DefaultIteratingChemObjectReader;
import org.openscience.cdk.io.iterator.IteratingMDLReader;
import org.openscience.cdk.io.iterator.IteratingSMILESReader;
import org.openscience.cdk.io.listener.IChemObjectIOListener;
import org.openscience.cdk.io.setting.IOSetting;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.*;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * @author Rajarshi Guha
 */
public class DescriptorSwingWorker implements ISwingWorker {

    private ApplicationUI ui;
    private List<IDescriptor> descriptors;
    private List<ExceptionInfo> exceptionList;

    private String inputFormat = "mdl";
    private File tempFile;

    private int lengthOfTask = 1;
    private int current = 0;
    private int molCount = 0;
    private boolean done = false;
    private boolean canceled = false;


    public DescriptorSwingWorker(List<IDescriptor> descriptors,
                                 ApplicationUI ui, JProgressBar progressBar, File tempFile) {
        this.descriptors = descriptors;
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
        final SwingWorker worker = new SwingWorker() {
            public Object construct() {
                current = 0;
                done = false;
                canceled = false;
                try {
                    return new ActualTask();
                } catch (CDKException e) {
                    System.out.println("Problem! Contact rguha@indiana.edu\n\n" + e.toString());
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


    private IAtomContainer checkAndCleanMolecule(IAtomContainer molecule) throws CDKException {
        Iterator<IAtom> atoms = molecule.atoms();
        boolean isMarkush = false;
        while (atoms.hasNext()) {
            IAtom atom = atoms.next();
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

    class MyListener implements IChemObjectIOListener {

        public void processIOSettingQuestion(IOSetting setting) {
            if ("ForceReadAs3DCoordinates".equals(setting.getName())) {
                try {
                    setting.setSetting("true");
                } catch (CDKException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

    }

    class ActualTask {

        private boolean evalToTextFile(String sdfFileName, String outputFormat) throws CDKException {
            DefaultIteratingChemObjectReader iterReader = null;
            BufferedWriter tmpWriter;
            try {
                tmpWriter = new BufferedWriter(new FileWriter(tempFile));

                FileInputStream inputStream = new FileInputStream(sdfFileName);
                if (inputFormat.equals("smi")) iterReader = new IteratingSMILESReader(inputStream);
                else if (inputFormat.equals("mdl")) {
                    iterReader = new IteratingMDLReader(inputStream, DefaultChemObjectBuilder.getInstance());
                    iterReader.addChemObjectIOListener(new MyListener());
                    ((IteratingMDLReader) iterReader).customizeJob();
                }
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(null, "Error opening input or output files",
                        "CDKDescUI Error", JOptionPane.ERROR_MESSAGE);
                done = true;
                return false;
            }

            ITextOutput textOutput = null;
            if (outputFormat.equals(CDKDescConstants.OUTPUT_TAB)) {
                textOutput = new PlainTextOutput(tmpWriter);
                textOutput.setItemSeparator("\t");
            } else if (outputFormat.equals(CDKDescConstants.OUTPUT_CSV)) {
                textOutput = new PlainTextOutput(tmpWriter);
                textOutput.setItemSeparator(",");
            } else if (outputFormat.equals(CDKDescConstants.OUTPUT_SPC)) {
                textOutput = new PlainTextOutput(tmpWriter);
                textOutput.setItemSeparator(" ");
            } else if (outputFormat.equals(CDKDescConstants.OUTPUT_ARFF)) {
                textOutput = new ARFFTextOutput(tmpWriter);
            }


            molCount = 0;

            // lets get the header line first
            List<String> headerItems = new ArrayList<String>();
            headerItems.add("Title");
            for (IDescriptor descriptor : descriptors) {
                String[] names = descriptor.getDescriptorNames();
                headerItems.addAll(Arrays.asList(names));
            }
            try {
                assert textOutput != null;
                textOutput.writeHeader(headerItems.toArray(new String[]{}));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error writing header line",
                        "CDKDescUI Error", JOptionPane.ERROR_MESSAGE);
                done = true;
                return false;
            }

            assert iterReader != null;
            while (iterReader.hasNext()) {  // loop over molecules
                if (canceled) return false;
                IMolecule molecule = (IMolecule) iterReader.next();
                String title = (String) molecule.getProperty(CDKConstants.TITLE);
                if (title == null) title = "Mol" + String.valueOf(molCount + 1);

                try {
                    molecule = (IMolecule) checkAndCleanMolecule(molecule);
                } catch (CDKException e) {
                    exceptionList.add(new ExceptionInfo(molCount + 1, molecule, e, ""));
                    molCount++;
                    continue;
                }

                // OK, we can now eval the descriptors
                List<String> dataItems = new ArrayList<String>();
                dataItems.add(title);

                for (Object object : descriptors) {
                    if (canceled) return false;
                    IMolecularDescriptor descriptor = (IMolecularDescriptor) object;
                    String[] comps = descriptor.getSpecification().getSpecificationReference().split("#");


                    DescriptorValue value = descriptor.calculate(molecule);
                    if (value.getException() != null) {
                        exceptionList.add(new ExceptionInfo(molCount + 1, molecule, value.getException(), comps[1]));
                        for (int i = 0; i < value.getNames().length; i++)
                            dataItems.add("NA");
                        continue;
                    }

                    IDescriptorResult result = value.getValue();
                    if (result instanceof DoubleResult) {
                        dataItems.add(String.valueOf(((DoubleResult) result).doubleValue()));
                    } else if (result instanceof IntegerResult) {
                        dataItems.add(String.valueOf(((IntegerResult) result).intValue()));
                    } else if (result instanceof DoubleArrayResult) {
                        for (int i = 0; i < ((DoubleArrayResult) result).length(); i++) {
                            dataItems.add(String.valueOf(((DoubleArrayResult) result).get(i)));
                        }
                    } else if (result instanceof IntegerArrayResult) {
                        for (int i = 0; i < ((IntegerArrayResult) result).length(); i++) {
                            dataItems.add(String.valueOf(((IntegerArrayResult) result).get(i)));
                        }
                    }
                    current++;
                }

                for (int i = 0; i < dataItems.size(); i++) {
                    if (dataItems.get(i).equals("NaN")) dataItems.set(i, "NA");
                }

                try {
                    textOutput.writeLine(dataItems.toArray(new String[]{}));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Error writing data line",
                            "CDKDescUI Error", JOptionPane.ERROR_MESSAGE);
                    done = true;
                    return false;
                }

                molCount++;
            }

            try {
                iterReader.close();
                tmpWriter.close();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error closing output files",
                        "CDKDescUI Error", JOptionPane.ERROR_MESSAGE);
                done = true;
                return false;
            }

            done = true;

            return true;
        }


        private boolean evalToSDF(String sdfFileName) {
            DefaultIteratingChemObjectReader iterReader = null;
            try {
                MDLWriter tmpWriter = new MDLWriter(new FileWriter(tempFile));

                FileInputStream inputStream = new FileInputStream(sdfFileName);
                if (inputFormat.equals("smi")) iterReader = new IteratingSMILESReader(inputStream);
                else if (inputFormat.equals("mdl"))
                    iterReader = new IteratingMDLReader(inputStream, DefaultChemObjectBuilder.getInstance());

                int counter = 1;

                while (iterReader.hasNext()) {
                    if (canceled) return false;
                    IMolecule molecule = (IMolecule) iterReader.next();

                    try {
                        molecule = (IMolecule) checkAndCleanMolecule(molecule);
                    } catch (CDKException e) {
                        exceptionList.add(new ExceptionInfo(molCount + 1, molecule, e, ""));
                        molCount++;
                        continue;
                    }

                    HashMap<String, Object> map = new HashMap<String, Object>();
                    for (Object object : descriptors) {
                        if (canceled) return false;
                        IMolecularDescriptor descriptor = (IMolecularDescriptor) object;
                        String[] comps = descriptor.getSpecification().getSpecificationReference().split("#");

                        DescriptorValue value = descriptor.calculate(molecule);
                        if (value.getException() != null) {
                            exceptionList.add(new ExceptionInfo(counter, molecule, value.getException(), comps[1]));
                            String[] names = value.getNames();
                            for (String name : names) map.put(name, "NA");
                            continue;
                        }
                        String[] names = value.getNames();

                        IDescriptorResult result = value.getValue();
                        if (result instanceof DoubleResult) {
                            map.put(value.getNames()[0], ((DoubleResult) result).doubleValue());
                        } else if (result instanceof IntegerResult) {
                            map.put(value.getNames()[0], ((IntegerResult) result).intValue());
                        } else if (result instanceof DoubleArrayResult) {
                            for (int i = 0; i < ((DoubleArrayResult) result).length(); i++) {
                                map.put(names[i], ((DoubleArrayResult) result).get(i));
                            }
                        } else if (result instanceof IntegerArrayResult) {
                            for (int i = 0; i < ((IntegerArrayResult) result).length(); i++)
                                map.put(names[i], ((IntegerArrayResult) result).get(i));
                        }
                        current++;

                    }
                    tmpWriter.setSdFields(map);
                    tmpWriter.write(molecule);
                    counter++;
                }
                iterReader.close();
                tmpWriter.close();
                done = true;
            } catch (IOException exception) {
                exception.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }


        ActualTask() throws CDKException {
            String outputMethod = AppOptions.getInstance().getOutputMethod();
            String sdfFileName = ui.getSdfFileTextField().getText();

            if (outputMethod.equals(CDKDescConstants.OUTPUT_TAB) ||
                    outputMethod.equals(CDKDescConstants.OUTPUT_CSV) ||
                    outputMethod.equals(CDKDescConstants.OUTPUT_ARFF) ||
                    outputMethod.equals(CDKDescConstants.OUTPUT_SPC)) {
                boolean status = evalToTextFile(sdfFileName, outputMethod);
                if (!status) return;
            } else if (outputMethod.equals(CDKDescConstants.OUTPUT_SDF)) {
                boolean status = evalToSDF(sdfFileName);
                if (!status) return;
            }

        }
    }
}



