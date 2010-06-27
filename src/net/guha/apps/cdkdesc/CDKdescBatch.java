package net.guha.apps.cdkdesc;

import net.guha.apps.cdkdesc.interfaces.ITextOutput;
import net.guha.apps.cdkdesc.output.PlainTextOutput;
import static net.guha.apps.cdkdesc.CDKDescUtils.loadDescriptorSelections;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.EStateFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.GraphOnlyFingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.iterator.DefaultIteratingChemObjectReader;
import org.openscience.cdk.io.iterator.IteratingMDLReader;
import org.openscience.cdk.io.iterator.IteratingSMILESReader;
import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResult;
import org.openscience.cdk.qsar.result.IntegerResult;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import nu.xom.ParsingException;

/**
 * Batch mode versions of the descriptor and fingerprint calculator.
 *
 * @author Rajarshi Guha
 */
public class CDKdescBatch {
    public static void batchFingerprint(String inputFile, String outputFile, String fpType, boolean verbose) throws CDKException {
        if (verbose) {
            System.out.println("INFO: input:\t" + inputFile);
            System.out.println("INFO: output:\t" + outputFile);
            System.out.println("INFO: type:\t" + fpType);
        }

        IFingerprinter fprinter = null;
        if (fpType.equals("standard")) fprinter = new Fingerprinter();
        else if (fpType.equals("extended")) fprinter = new ExtendedFingerprinter();
        else if (fpType.equals("pubchem")) fprinter = new PubchemFingerprinter();
        else if (fpType.equals("graph")) fprinter = new GraphOnlyFingerprinter();
        else if (fpType.equals("maccs")) fprinter = new MACCSFingerprinter();
        else if (fpType.equals("estate")) fprinter = new EStateFingerprinter();
        else if (fpType.equals("substructure")) fprinter = new SubstructureFingerprinter();
        else throw new CDKException("Invalid fingerprint type specified");

        List<ExceptionInfo> exceptionList = new ArrayList<ExceptionInfo>();
        DefaultIteratingChemObjectReader iterReader = null;
        try {
            BufferedWriter tmpWriter = new BufferedWriter(new FileWriter(outputFile));
            FileInputStream inputStream = new FileInputStream(inputFile);

            String lineSep = System.getProperty("line.separator");
            String itemSep = " ";
            String inputFormat = "invalid";
            if (CDKDescUtils.isSMILESFormat(inputFile)) {
                inputFormat = "smi";
            } else if (CDKDescUtils.isMDLFormat(inputFile)) {
                inputFormat = "mdl";
            }

            if (inputFormat.equals("smi")) iterReader = new IteratingSMILESReader(inputStream);
            else if (inputFormat.equals("mdl")) {
                iterReader = new IteratingMDLReader(inputStream, DefaultChemObjectBuilder.getInstance());
                iterReader.addChemObjectIOListener(new SDFormatListener());
                ((IteratingMDLReader) iterReader).customizeJob();
            }


            int molCount = 0;

            // lets get the header line first
            tmpWriter.write("CDKDescUI " + fprinter.getClass().getName() + " " + fprinter.getSize() + " bits" + lineSep);
            assert iterReader != null;
            double elapsedTime = System.currentTimeMillis();

            while (iterReader.hasNext()) {  // loop over molecules
                IMolecule molecule = (IMolecule) iterReader.next();

                try {
                    molecule = (IMolecule) CDKDescUtils.checkAndCleanMolecule(molecule);
                } catch (CDKException e) {
                    exceptionList.add(new ExceptionInfo(molCount + 1, molecule, e, ""));
                    molCount++;
                    continue;
                }

                try {
                    BitSet fingerprint = fprinter.getFingerprint(molecule);
                    String title = (String) molecule.getProperty(CDKConstants.TITLE);
                    if (title == null) title = "Mol" + String.valueOf(molCount + 1);
                    tmpWriter.write(title + itemSep + fingerprint.toString() + lineSep);
                    tmpWriter.flush();
                    molCount++;
                } catch (Exception e) {
                    exceptionList.add(new ExceptionInfo(molCount + 1, molecule, e, ""));
                    molCount++;
                }

                if (verbose && molCount % 10 == 0) {
                    System.out.print("\rINFO: Processed " + molCount + " molecules");
                    System.out.flush();
                }
            }

            elapsedTime = ((System.currentTimeMillis() - elapsedTime) / 1000.0);
            if (verbose) {
                NumberFormat formatter = new DecimalFormat("#0.000");
                System.out.println("\nINFO: Total time = " + formatter.format(elapsedTime) + "s (" + formatter.format(elapsedTime / molCount) + " s/mol)");
            }
            iterReader.close();
            tmpWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        if (exceptionList.size() > 0) {
            System.out.println("=============== Exceptions ===============");
            for (ExceptionInfo ei : exceptionList) {
                System.out.println(ei.getMolecule().getProperty(CDKConstants.TITLE) + " " + ei.getDescriptorName() + " " + ei.getException());
            }
        }

    }

    /**
     * Calculate descriptors in batch mode.
     *
     * @param inputFile        The input file of molecules
     * @param outputFile       The output file to dump descriptor values to
     * @param descriptorType   Either a descriptor type or a file name containing descriptors
     * @param isDescriptorType if a descriptor type is specified, true otherwise false
     * @param verbose          should output be verbose or not
     */
    public static void batchDescriptor(String inputFile, String outputFile,
                                       String descriptorType,
                                       boolean isDescriptorType,
                                       boolean verbose) {

        Map<String, Boolean> sels = null;

        if (!isDescriptorType) {
            try {
                sels = loadDescriptorSelections(descriptorType);
            } catch (ParsingException e) {
                System.out.println("ERROR: Invalid XML file supplied");
                System.exit(-1);
            } catch (IOException e) {
                System.out.println("Couldn't read selection file");
                System.exit(-1);
            }
        }
        if (verbose) {
            System.out.println("INFO: input:\t" + inputFile);
            System.out.println("INFO: output:\t" + outputFile);
            if (isDescriptorType) System.out.println("INFO: type:\t" + descriptorType);
            else System.out.println("INFO: using selections from:\t" + descriptorType);
        }

        DescriptorEngine engine = new DescriptorEngine(DescriptorEngine.MOLECULAR);
        List<String> classNames = engine.getDescriptorClassNames();

        List<String> validClassNames = new ArrayList<String>();

        if (isDescriptorType) {
            if (descriptorType.equals("all")) {
                validClassNames.addAll(classNames);
            } else {
                for (String className : classNames) {
                    String[] dictClasses = engine.getDictionaryClass(className);
                    if (dictClasses == null) continue;
                    for (String dictClass : dictClasses) {
                        if (dictClass.indexOf(descriptorType) != -1) validClassNames.add(className);
                    }
                }
            }
        } else { // add in selected descriptors
            List<IDescriptor> descs = engine.getDescriptorInstances();
            for (String s : sels.keySet()) {
                Boolean isSelected = sels.get(s);
                for (IDescriptor desc : descs) {
                    if (desc.getSpecification().getSpecificationReference().equals(s) && isSelected)
                        validClassNames.add(desc.getClass().getCanonicalName());
                }
            }
        }

        if (verbose) System.out.println("INFO: Will evaluate " + validClassNames.size() + " descriptors");
        List<IDescriptor> instances = engine.instantiateDescriptors(validClassNames);
        if (verbose) System.out.println("INFO: Got " + validClassNames.size() + " descriptor instances");
        engine.setDescriptorInstances(instances);

        // ok, we've got the desc engine set up, lets check inputs and start the fun
        String inputFormat = "invalid";
        if (CDKDescUtils.isSMILESFormat(inputFile)) inputFormat = "smi";
        else if (CDKDescUtils.isMDLFormat(inputFile)) inputFormat = "mdl";
        else {
            System.out.println("Currently only SMILES of SDF formats are supported");
            System.exit(-1);
        }

        DefaultIteratingChemObjectReader iterReader = null;
        BufferedWriter tmpWriter = null;
        try {
            tmpWriter = new BufferedWriter(new FileWriter(outputFile));

            FileInputStream inputStream = new FileInputStream(inputFile);
            if (inputFormat.equals("smi")) iterReader = new IteratingSMILESReader(inputStream);
            else if (inputFormat.equals("mdl")) {
                iterReader = new IteratingMDLReader(inputStream, DefaultChemObjectBuilder.getInstance());
                iterReader.addChemObjectIOListener(new SDFormatListener());
                ((IteratingMDLReader) iterReader).customizeJob();
            }
        } catch (IOException exception) {
            System.out.println("ERROR: Error opening input file");
            System.out.println(exception.toString());
            System.exit(-1);
        }

        ITextOutput textOutput = new PlainTextOutput(tmpWriter);
        textOutput.setItemSeparator("\t");


        // lets get the header line first
        List<String> headerItems = new ArrayList<String>();
        headerItems.add("Title");
        for (IDescriptor descriptor : instances) {
            String[] names = descriptor.getDescriptorNames();
            headerItems.addAll(Arrays.asList(names));
        }

        try {
            assert textOutput != null;
            textOutput.writeHeader(headerItems.toArray(new String[]{}));
        } catch (IOException e) {
            System.out.println("ERROR: Error writing header line");
            System.out.println(e.toString());
            System.exit(-1);
        }

        double elapsedTime = System.currentTimeMillis();

        List<ExceptionInfo> exceptionList = new ArrayList<ExceptionInfo>();
        int nmol = 0;

        while (iterReader.hasNext()) {  // loop over molecules
            IMolecule molecule = (IMolecule) iterReader.next();
            String title = (String) molecule.getProperty(CDKConstants.TITLE);
            if (title == null) title = "Mol" + String.valueOf(nmol + 1);

            try {
                molecule = (IMolecule) CDKDescUtils.checkAndCleanMolecule(molecule);
            } catch (CDKException e) {
                exceptionList.add(new ExceptionInfo(nmol + 1, molecule, e, ""));
                nmol++;
                continue;
            }

            // OK, we can now eval the descriptors
            List<String> dataItems = new ArrayList<String>();
            dataItems.add(title);

            int ndesc = 0;
            for (Object object : instances) {
                IMolecularDescriptor descriptor = (IMolecularDescriptor) object;
                String[] comps = descriptor.getSpecification().getSpecificationReference().split("#");

                DescriptorValue value = descriptor.calculate(molecule);
                if (value.getException() != null) {
                    exceptionList.add(new ExceptionInfo(nmol + 1, molecule, value.getException(), comps[1]));
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

                ndesc++;

                if (verbose) {
                    System.out.print("\rINFO: Processed " + ndesc + " descriptors for " + (nmol+1) + " molecules");
                    System.out.flush();
                }
            }

            for (int i = 0; i < dataItems.size(); i++) {
                if (dataItems.get(i).equals("NaN")) dataItems.set(i, "NA");
            }

            try {
                textOutput.writeLine(dataItems.toArray(new String[]{}));
            } catch (IOException e) {
                System.out.println("\nERROR: Error writing data line");
                System.out.println(e.toString());
                System.exit(-1);
            }

            nmol++;
        }

        // calculation is done, lets eval the elapsed time
        elapsedTime = ((System.currentTimeMillis() - elapsedTime) / 1000.0);

        try {
            iterReader.close();
            tmpWriter.close();
        } catch (IOException e) {
            System.out.println("\nERROR: Error closing files");
            System.out.println(e.toString());
            System.exit(-1);
        }

        if (verbose) System.out.println("\nINFO: Completed in " + elapsedTime + " sec");

        if (exceptionList.size() > 0) {
            System.out.println("=============== Exceptions ===============");
            for (ExceptionInfo ei : exceptionList) {
                System.out.println(ei.getMolecule().getProperty(CDKConstants.TITLE) + " " + ei.getDescriptorName() + " " + ei.getException());
            }
        }
    }
}
