package net.guha.apps.cdkdesc.interfaces;

import java.io.IOException;

public interface ITextOutput {

    public void setItemSeparator(String itemSep);

    public void writeHeader(String[] items) throws IOException;

    public void writeLine(String[] items) throws IOException;

    public void flush() throws IOException;

}
