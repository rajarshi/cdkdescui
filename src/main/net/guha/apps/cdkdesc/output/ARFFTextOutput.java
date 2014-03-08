package net.guha.apps.cdkdesc.output;

import net.guha.apps.cdkdesc.interfaces.ITextOutput;

import java.io.IOException;
import java.io.Writer;

public class ARFFTextOutput extends TextOutput implements ITextOutput {

    public ARFFTextOutput(Writer writer) {
        super(writer);
    }

    public void writeHeader(String[] items) throws IOException {
        writer.write("@RELATION descriptors" + lineSep + lineSep);
        writer.write("@ATTRIBUTE title STRING" + lineSep);
        for (int i = 1; i < items.length; i++) {
            writer.write("@ATTRIBUTE " + items[i] + " NUMERIC" + lineSep);
        }
        writer.write(lineSep + "@DATA" + lineSep);
    }

    public void writeLine(String[] items) throws IOException {
        for (int i = 0; i < items.length - 1; i++) writer.write(items[i] + ",");
        writer.write(items[items.length - 1] + lineSep);
    }
}