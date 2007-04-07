package net.guha.apps.cdkdesc;

import org.openscience.cdk.interfaces.IMolecule;

/**
 * @author Rajarshi Guha
 */
public class ExceptionInfo {
    int serial;
    IMolecule molecule;
    Exception exception;

    public ExceptionInfo(int serial, IMolecule molecule, Exception exception) {
        this.serial = serial;
        this.molecule = molecule;
        this.exception = exception;
    }

    public int getSerial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    public IMolecule getMolecule() {
        return molecule;
    }

    public void setMolecule(IMolecule molecule) {
        this.molecule = molecule;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
