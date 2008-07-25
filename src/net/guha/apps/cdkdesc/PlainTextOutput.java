package net.guha.apps.cdkdesc;

import net.guha.apps.cdkdesc.interfaces.ITextOutput;

import java.io.IOException;
import java.io.Writer;

public class PlainTextOutput extends TextOutput implements ITextOutput {

    public PlainTextOutput(Writer writer) {
        super(writer);
    }

    public void writeHeader(String[] items) throws IOException {
        int nitem = items.length;
        for (int i = 0; i < nitem - 1; i++) {
            writer.write(items[i] + itemSep);
        }
        writer.write(items[nitem - 1] + lineSep);
    }

    public void writeLine(String[] items) throws IOException {
        int nitem = items.length;
        for (int i = 0; i < nitem - 1; i++) {
            writer.write(items[i] + itemSep);
        }
        writer.write(items[nitem - 1] + lineSep);
    }
}
