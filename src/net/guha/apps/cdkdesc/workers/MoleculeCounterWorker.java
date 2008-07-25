package net.guha.apps.cdkdesc.workers;

import net.guha.apps.cdkdesc.CDKDescUtils;
import net.guha.apps.cdkdesc.SwingWorker;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.io.iterator.IteratingMDLReader;

import java.io.*;

/**
 * @author rguha
 */
public class MoleculeCounterWorker {
    private int lengthOfTask;
    private int current = 0;
    private boolean done = false;
    private boolean canceled = false;

    private String filename;
    private int numMol = 0;

    public MoleculeCounterWorker(String fileName) {
        this.filename = fileName;
    }

    public void go() {
        final SwingWorker worker = new SwingWorker() {
            public Object construct() {
                current = 0;
                done = false;
                canceled = false;
                return new ActualTask();
            }
        };
        worker.start();
    }


    public int getLengthOfTask() {
        return lengthOfTask;
    }

    public int getCurrent() {
        return current;
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

    public int getNumMol() {
        return numMol;
    }

    private class ActualTask {
        boolean countmols() {
            numMol = 0;
            if (CDKDescUtils.isSMILESFormat(filename)) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(filename));
                    while (true) {
                        if (canceled) return false;
                        String line = in.readLine();
                        if (line == null) break;
                        else
                            numMol++;
                    }
                    in.close();
                    done = true;
                } catch (FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } else if (CDKDescUtils.isMDLFormat(filename)) {
                try {
                    FileInputStream inputStream = new FileInputStream(filename);
                    IteratingMDLReader mdlReader = new IteratingMDLReader(inputStream, DefaultChemObjectBuilder.getInstance());
                    while (mdlReader.hasNext()) {
                        if (canceled) return false;
                        mdlReader.next();
                        numMol++;
                    }
                    mdlReader.close();
                    done = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        ActualTask() {
            countmols();
        }
    }
}





