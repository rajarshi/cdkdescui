package net.guha.apps.cdkdesc.ui;

import net.guha.apps.cdkdesc.AppOptions;
import net.guha.apps.cdkdesc.CDKDescConstants;
import nu.xom.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Rajarshi Guha
 */
public class ApplicationMenu {

    private ApplicationUI ui;

    private JMenuItem aboutMenuItem;
    private JMenuItem pluginMenuItem;

    public ApplicationMenu(ApplicationUI ui) {
        this.ui = ui;
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
        saveSel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

        loadSel.addActionListener(new SelectionAction());
        loadSel.setName("loadSel");
        loadSel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));

        selectionMenu.add(saveSel);
        selectionMenu.add(loadSel);
        optionMenu.add(selectionMenu);

        JMenu outputFormatMenu = new JMenu("Output Method");
        ButtonGroup group = new ButtonGroup();

        JRadioButtonMenuItem formatItem = new JRadioButtonMenuItem("ARFF");
        formatItem.setActionCommand(CDKDescConstants.OUTPUT_ARFF);
        formatItem.addActionListener(new OutputFormatAction());
        formatItem.setSelected(false);
        group.add(formatItem);
        outputFormatMenu.add(formatItem);

        formatItem = new JRadioButtonMenuItem("Space delimited");
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
        private void showErrorDialog(String msg) {
            JOptionPane.showMessageDialog(null,
                    msg,
                    "CDKDescUI Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        public void actionPerformed(ActionEvent e) {
            if (((JMenuItem) e.getSource()).getName().equals("saveSel")) {
                // first get a filename, using the loaded filename if available
                JFileChooser fileDialog = new JFileChooser();
                int status = fileDialog.showOpenDialog(ui.getSubpanel());
                if (status != JFileChooser.APPROVE_OPTION) return;

                File settingsFile = fileDialog.getSelectedFile();
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(settingsFile);
                } catch (FileNotFoundException e1) {
                    showErrorDialog("Error opening the file to save descriptor selection");
                }

                // now create the XML document
                Element root = new Element("CDKDescUISettings");
                root.addAttribute(new Attribute("version", "1.0"));
                Element descList = new Element("descriptorList");
                Map<String, Boolean> selDescMap = AppOptions.getInstance().getSelectedDescriptors();

                List<String> keys = new ArrayList<String>();
                for (String key : selDescMap.keySet()) keys.add(key);
                Collections.sort(keys);

                for (String key : keys) {
                    Element desc = new Element("descriptor");
                    desc.addAttribute(new Attribute("spec", key));
                    Element selected = new Element("selected");
                    selected.appendChild(selDescMap.get(key).toString());
                    desc.appendChild(selected);
                    descList.appendChild(desc);
                }
                root.appendChild(descList);

                // now write it out
                Document doc = new Document(root);
                Serializer serializer = null;
                try {
                    serializer = new Serializer(out, "ISO-8859-1");
                } catch (UnsupportedEncodingException e1) {
                    showErrorDialog("Error in serialization of XML document");
                }
                serializer.setIndent(4);
                serializer.setMaxLength(128);
                try {
                    serializer.write(doc);
                } catch (IOException e1) {
                    showErrorDialog("Error when writing XML document");
                }
            } else if (((JMenuItem) e.getSource()).getName().equals("loadSel")) {
                JFileChooser fileDialog = new JFileChooser();
                int status = fileDialog.showOpenDialog(ui.getSubpanel());
                if (status == JFileChooser.APPROVE_OPTION) {
                    File settingsFile = fileDialog.getSelectedFile();
                    String settingsFileName = settingsFile.getAbsolutePath();
                    AppOptions.getInstance().setSettingsFile(settingsFileName);

                    Map<String, Boolean> selDecMap = AppOptions.getInstance().getSelectedDescriptors();
                    Builder parser = new Builder();
                    Document doc = null;
                    try {
                        doc = parser.build(settingsFile);
                    } catch (ParsingException pe) {
                        showErrorDialog("The settings file contained invalid XML");
                    } catch (IOException e1) {
                        showErrorDialog("There was an IO error when reading\n" + settingsFileName);
                    }
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

                    // OK, now lets actually check the selected descriptors
                    ui.checkSelectedDescriptors();
                }

            }
        }
    }
}
