package net.guha.apps.cdkdesc;

import net.guha.apps.cdkdesc.interfaces.ITextOutput;
import net.guha.apps.cdkdesc.output.PlainTextOutput;
import nu.xom.ParsingException;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.DefaultIteratingChemObjectReader;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.io.iterator.IteratingSMILESReader;
import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.*;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import static net.guha.apps.cdkdesc.CDKDescUtils.loadDescriptorSelections;

/**
 * Batch mode versions of the descriptor and fingerprint calculator.
 *
 * @author Rajarshi Guha
 */
public class CDKdescBatch {
    Logger log;

    public CDKdescBatch() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void batchFingerprint(String inputFile, String outputFile, String fpType, boolean verbose) throws CDKException {
        log.info("input:\t" + inputFile);
        log.info("output:\t" + outputFile);
        log.info("type:\t" + fpType);

        IFingerprinter fprinter = null;
        if (fpType.equals("standard")) fprinter = new Fingerprinter();
        else if (fpType.equals("extended")) fprinter = new ExtendedFingerprinter();
        else if (fpType.equals("pubchem")) fprinter = new PubchemFingerprinter(SilentChemObjectBuilder.getInstance());
        else if (fpType.equals("graph")) fprinter = new GraphOnlyFingerprinter();
        else if (fpType.equals("maccs")) fprinter = new MACCSFingerprinter();
        else if (fpType.equals("estate")) fprinter = new EStateFingerprinter();
        else if (fpType.equals("substructure")) fprinter = new SubstructureFingerprinter();
        else if (fpType.equals("signature")) fprinter = new SignatureFingerprinter();
        else if (fpType.equals("circular")) fprinter = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4);
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

            if (inputFormat.equals("smi"))
                iterReader = new IteratingSMILESReader(inputStream, SilentChemObjectBuilder.getInstance());
            else if (inputFormat.equals("mdl")) {
                iterReader = new IteratingSDFReader(inputStream, DefaultChemObjectBuilder.getInstance());
                iterReader.addChemObjectIOListener(new SDFormatListener());
                ((IteratingSDFReader) iterReader).customizeJob();
            }


            int molCount = 0;

            // lets get the header line first
            tmpWriter.write("CDKDescUI " + fprinter.getClass().getName() + " " + fprinter.getSize() + " bits" + lineSep);
            assert iterReader != null;
            double elapsedTime = System.currentTimeMillis();


            while (iterReader.hasNext()) {  // loop over molecules
                IAtomContainer molecule = (IAtomContainer) iterReader.next();

                try {
                    molecule = CDKDescUtils.checkAndCleanMolecule(molecule);
                } catch (CDKException e) {
                    exceptionList.add(new ExceptionInfo(molCount + 1, molecule, e, ""));
                    molCount++;
                    continue;
                }

                try {
                    String title = (String) molecule.getProperty(CDKConstants.TITLE);
                    if (title == null) title = "Mol" + String.valueOf(molCount + 1);
                    String repr = null;

                    if (fprinter instanceof SignatureFingerprinter) {
                        IBitFingerprint fp = fprinter.getBitFingerprint(molecule);
                        int[] trueBits = fp.getSetbits();
                        StringBuilder b = new StringBuilder();
                        String delim = "";
                        for (int i : trueBits) {
                            b.append(delim).append(i);
                            delim = ",";
                        }
                        repr = b.toString();
                    } else {
                        BitSet fingerprint = fprinter.getBitFingerprint(molecule).asBitSet();
                        repr = fingerprint.toString();
                    }

                    tmpWriter.write(title + itemSep + repr + lineSep);
                    tmpWriter.flush();
                    molCount++;
                } catch (Exception e) {
                    exceptionList.add(new ExceptionInfo(molCount + 1, molecule, e, ""));
                    molCount++;
                }

                if (molCount % 10 == 0) {
                    log.info("Processed " + molCount + " molecules");
                }
            }

            elapsedTime = ((System.currentTimeMillis() - elapsedTime) / 1000.0);
            if (verbose) {
                NumberFormat formatter = new DecimalFormat("#0.000");
                log.info("Total time = " + formatter.format(elapsedTime) + "s (" + formatter.format(elapsedTime / molCount) + " s/mol)");
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
    public void batchDescriptor(String inputFile, String outputFile,
                                String descriptorType,
                                boolean isDescriptorType,
                                boolean verbose) {

        Map<String, Boolean> sels = null;

        if (!isDescriptorType) {
            try {
                sels = loadDescriptorSelections(descriptorType);
            } catch (ParsingException e) {
                log.error("Invalid XML file supplied");
                System.exit(-1);
            } catch (IOException e) {
                log.error("Couldn't read selection file");
                System.exit(-1);
            }
        }
        log.info("input:\t" + inputFile);
        log.info("output:\t" + outputFile);
        if (isDescriptorType) log.info("type:\t" + descriptorType);
        log.info("using selections from:\t" + descriptorType);
        log.info("Adding explicit H\t" + AppOptions.getInstance().isAddH());

        DescriptorEngine engine = new DescriptorEngine(IMolecularDescriptor.class, SilentChemObjectBuilder.getInstance());
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

        log.info("Will evaluate " + validClassNames.size() + " descriptors");
        List<IDescriptor> instances = engine.instantiateDescriptors(validClassNames);
        log.info("Got " + validClassNames.size() + " descriptor instances");
        engine.setDescriptorInstances(instances);

        // ok, we've got the desc engine set up, lets check inputs and start the fun
        String inputFormat = "invalid";
        if (CDKDescUtils.isMDLFormat(inputFile)) inputFormat = "mdl";
        else if (CDKDescUtils.isSMILESFormat(inputFile)) inputFormat = "smi";
        else {
            log.error("Currently only SMILES of SDF formats are supported");
            System.exit(-1);
        }

        DefaultIteratingChemObjectReader iterReader = null;
        BufferedWriter tmpWriter = null;
        try {
            tmpWriter = new BufferedWriter(new FileWriter(outputFile));

            FileInputStream inputStream = new FileInputStream(inputFile);
            if (inputFormat.equals("smi"))
                iterReader = new IteratingSMILESReader(inputStream, SilentChemObjectBuilder.getInstance());
            else if (inputFormat.equals("mdl")) {
                iterReader = new IteratingSDFReader(inputStream, DefaultChemObjectBuilder.getInstance());
                iterReader.addChemObjectIOListener(new SDFormatListener());
                ((IteratingSDFReader) iterReader).customizeJob();
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
            log.error("ERROR: Error writing header line");
            log.error(e.toString());
            System.exit(-1);
        }

        double elapsedTime = System.currentTimeMillis();

        List<ExceptionInfo> exceptionList = new ArrayList<ExceptionInfo>();
        int nmol = 0;

        while (iterReader.hasNext()) {  // loop over molecules
            IAtomContainer molecule = (IAtomContainer) iterReader.next();
            String title = (String) molecule.getProperty(CDKConstants.TITLE);
            if (title == null) title = "Mol" + String.valueOf(nmol + 1);

            try {
                molecule = (IAtomContainer) CDKDescUtils.checkAndCleanMolecule(molecule);
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


            }
            log.info("Processed " + ndesc + " descriptors for " + (nmol + 1) + " molecules");

            for (int i = 0; i < dataItems.size(); i++) {
                if (dataItems.get(i).equals("NaN")) dataItems.set(i, "NA");
            }

            try {
                textOutput.writeLine(dataItems.toArray(new String[]{}));
            } catch (IOException e) {
                log.error("Error writing data line");
                log.error(e.toString());
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
            log.error("Error closing files");
            log.error(e.toString());
            System.exit(-1);
        }

        log.info("Completed in " + elapsedTime + " sec");

        if (exceptionList.size() > 0) {
            System.out.println("=============== Exceptions ===============");
            for (ExceptionInfo ei : exceptionList) {
                System.out.println(ei.getMolecule().getProperty(CDKConstants.TITLE) + " " + ei.getDescriptorName() + " " + ei.getException());
            }
        }
    }
}
