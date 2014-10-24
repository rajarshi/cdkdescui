package net.guha.apps.cdkdesc;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.formats.IResourceFormat;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rajarshi Guha
 */

public class CDKDescUtils {

    CDKDescUtils() {
    }

    /**
     * Checks whether the input file is in SMI format.
     * <p/>
     * The approach I take is to read the first line of the file. This should splittable to give two parts. The first
     * part should be a valid SMILES string. If so this method returns true, otherwise false
     *
     * @param filename The file to consider
     * @return true if the file is in SMI format, otherwise false
     */
    public static boolean isSMILESFormat(String filename) {
        String line1 = null;
        String line2 = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            line1 = in.readLine();
            line2 = in.readLine();
            in.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        assert line1 != null;

        String[] tokens = line1.split("\\s+");
        if (tokens.length == 0) return false;

        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        try {
            IAtomContainer m = sp.parseSmiles(tokens[0].trim());
        } catch (InvalidSmilesException ise) {
            return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }

        // now check the second line
        // if there is no second line this probably a smiles
        // file
        if (line2 == null) return true;

        // o0k we have a second line, so lets see if it's a smiles
        tokens = line2.split("\\s+");
        if (tokens.length == 0) return false;

        sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        try {
            IAtomContainer m = sp.parseSmiles(tokens[0].trim());
        } catch (InvalidSmilesException ise) {
            return false;
        }

        return true;
    }

    public static boolean isMDLFormat(String filename) {
        boolean flag;
        try {
            File file = new File(filename);
            ReaderFactory factory = new ReaderFactory();
            IChemObjectReader reader = factory.createReader(new FileReader(file));
            if (reader == null) return false;
            IResourceFormat format = reader.getFormat();
            flag = format.getFormatName().startsWith("MDL ");
        } catch (Exception exception) {
            return false;
        }
        return flag;
    }

    public static int countMolecules(String filename) throws Exception {
        int numMol = 0;
        if (isSMILESFormat(filename)) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(filename));
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    else
                        numMol++;
                }
                in.close();
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else if (isMDLFormat(filename)) {
            try {
                FileInputStream inputStream = new FileInputStream(filename);
                IteratingSDFReader mdlReader = new IteratingSDFReader(inputStream, SilentChemObjectBuilder.getInstance());
                while (mdlReader.hasNext()) {
                    mdlReader.next();
                    numMol++;
                }
                mdlReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            throw new Exception("Invalid file format supplied");
        return numMol;
    }

    public static boolean isMacOs() {
        String lcOSName = System.getProperty("os.name").toLowerCase();
        return lcOSName.startsWith("mac os x");
    }

    public static Comparator getDescriptorComparator() {
        return new Comparator() {
            public int compare(Object o1, Object o2) {
                IDescriptor desc1 = (IDescriptor) o1;
                IDescriptor desc2 = (IDescriptor) o2;

                String[] comp1 = desc1.getSpecification().getSpecificationReference().split("#");
                String[] comp2 = desc2.getSpecification().getSpecificationReference().split("#");

                return comp1[1].compareTo(comp2[1]);
            }
        };
    }


    public static IAtomContainer checkAndCleanMolecule(IAtomContainer molecule) throws CDKException {
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
            IAtomContainerSet fragments = ConnectivityChecker.partitionIntoMolecules(molecule);
            if (fragments.getAtomContainerCount() > 2) {
                throw new CDKException("More than 2 components. Skipped");
            } else {
                IAtomContainer frag1 = fragments.getAtomContainer(0);
                IAtomContainer frag2 = fragments.getAtomContainer(1);
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

        // add explicit H's if required
        if (AppOptions.getInstance().isAddH()) {
            CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(molecule.getBuilder());
            adder.addImplicitHydrogens(molecule);
            AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);

        }

        // do a aromaticity check
        try {
            CDKHueckelAromaticityDetector.detectAromaticity(molecule);
        } catch (CDKException e) {
            throw new CDKException("Error in aromaticity detection");
        }

        return molecule;
    }

    public static Map<String, Boolean> loadDescriptorSelections(String fileName) throws ParsingException, IOException {
        Map<String, Boolean> selDecMap = new HashMap<String, Boolean>();
        Builder parser = new Builder();
        Document doc;
        doc = parser.build(fileName);
        Element root = doc.getRootElement();
        Elements elems = root.getChildElements("descriptorList");
        Elements descriptorList = elems.get(0).getChildElements();
        for (int i = 0; i < descriptorList.size(); i++) {
            Element desc = descriptorList.get(i);
            String spec = desc.getAttribute("spec").getValue();
            String value = desc.getFirstChildElement("selected").getValue();
            Boolean isSelected = value.equals("true");
            selDecMap.put(spec, isSelected);
        }
        return selDecMap;
    }
}