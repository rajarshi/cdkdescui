package net.guha.apps.cdkdesc.ui;

import net.guha.apps.cdkdesc.AppOptions;
import net.guha.apps.cdkdesc.CDKDescConstants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Rajarshi Guha
 */
public class ApplicationMenu {

    private JMenuItem aboutMenuItem;
    private JMenuItem pluginMenuItem;

    public ApplicationMenu() {
        aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(new AboutAction());

        pluginMenuItem = new JMenuItem("Plugins");
        pluginMenuItem.addActionListener(new PluginAction());
    }

    public JMenuBar createMenu() {
        JMenuBar mb = new JMenuBar();

        JMenu optionMenu = new JMenu("Options");
        mb.add(optionMenu);

        JMenu selectionMenu = new JMenu("Selection");
        JMenuItem saveSel = new JMenuItem("Save descriptor selections");
        JMenuItem loadSel = new JMenuItem("Load descriptor selections");
        saveSel.addActionListener(new SelectionAction());
        saveSel.setName("saveSel");
        loadSel.addActionListener(new SelectionAction());
        loadSel.setName("loadSel");
        selectionMenu.add(saveSel);
        selectionMenu.add(loadSel);
        optionMenu.add(selectionMenu);

        JMenu outputFormatMenu = new JMenu("Output Method");
        ButtonGroup group = new ButtonGroup();


        JRadioButtonMenuItem formatItem = new JRadioButtonMenuItem("Space delimited");
        formatItem.setActionCommand(CDKDescConstants.OUTPUT_SPC);
        formatItem.addActionListener(new OutputFormatAction());
        formatItem.setSelected(true);
        group.add(formatItem);
        outputFormatMenu.add(formatItem);

        formatItem = new JRadioButtonMenuItem("Tab delimited");
        formatItem.setActionCommand(CDKDescConstants.OUTPUT_TAB);
        formatItem.addActionListener(new OutputFormatAction());
        formatItem.setSelected(false);
        group.add(formatItem);
        outputFormatMenu.add(formatItem);

        formatItem = new JRadioButtonMenuItem("Comma delimited");
        formatItem.setActionCommand(CDKDescConstants.OUTPUT_CSV);
        formatItem.addActionListener(new OutputFormatAction());
        formatItem.setSelected(false);
        group.add(formatItem);
        outputFormatMenu.add(formatItem);


        formatItem = new JRadioButtonMenuItem("Annotated SDF");
        formatItem.setActionCommand(CDKDescConstants.OUTPUT_SDF);
        formatItem.addActionListener(new OutputFormatAction());
        formatItem.setSelected(false);
        group.add(formatItem);
        outputFormatMenu.add(formatItem);

        formatItem = new JRadioButtonMenuItem("CML");
        formatItem.setActionCommand(CDKDescConstants.OUTPUT_CML);
        formatItem.addActionListener(new OutputFormatAction());
        formatItem.setEnabled(false);
        group.add(formatItem);
        outputFormatMenu.add(formatItem);

        optionMenu.add(outputFormatMenu);
        optionMenu.add(pluginMenuItem);

        JMenu helpMenu = new JMenu("Help");
        mb.add(helpMenu);
        helpMenu.add(aboutMenuItem);
        return mb;
    }

    class AboutAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            AboutDialog dlg = new AboutDialog();
            dlg.setVisible(true);
        }
    }

    class PluginAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            PluginManager pluginManager = new PluginManager();
            pluginManager.setTitle("CDK Descriptor Plugin Manager");
            pluginManager.pack();
            pluginManager.setVisible(true);
        }
    }

    class OutputFormatAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            AppOptions.getInstance().setOutputMethod(e.getActionCommand());
        }
    }

    class SelectionAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (((JMenuItem) e.getSource()).getName().equals("saveSel")) {
                System.out.println("save selection");
            } else if (((JMenuItem) e.getSource()).getName().equals("loadSel")) {
                System.out.println("load selection");
            }
        }
    }
}
