package net.guha.apps.cdkdesc.interfaces;

public interface ITextOutput {

    public void setItemSeparator(String itemSep);

    public void writeHeader(String[] items);

    public void writeLine(String[] items);

}
