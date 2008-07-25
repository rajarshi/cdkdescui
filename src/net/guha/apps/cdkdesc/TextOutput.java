package net.guha.apps.cdkdesc;

import net.guha.apps.cdkdesc.interfaces.ITextOutput;

import java.io.IOException;
import java.io.Writer;


public abstract class TextOutput implements ITextOutput {
    protected Writer writer;
    protected String itemSep = CDKDescConstants.OUTPUT_SPC;
    protected String lineSep = System.getProperty("line.separator");

    public TextOutput(Writer writer) {
        this.writer = writer;
    }


    public void setItemSeparator(String itemSep) {
        this.itemSep = itemSep;
    }

    public abstract void writeHeader(String[] items) throws IOException;

    public abstract void writeLine(String[] items) throws IOException;
}
