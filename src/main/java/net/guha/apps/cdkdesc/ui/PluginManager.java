package net.guha.apps.cdkdesc.ui;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @author Rajarshi Guha
 */
public class PluginManager extends JDialog {
    private JPanel contentPane;
    private JList pluginList;
    private JScrollPane scrollPane;
    private JButton addButton;
    private JButton deleteButton;
    private JButton disableButton;
    private JButton buttonOK;
    private JButton buttonCancel;


    public PluginManager() {

        contentPane = new JPanel(new GridBagLayout());
        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        setContentPane(contentPane);

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });


        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deletePlugin();
            }
        });

        addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addPlugin();
            }
        });

        disableButton = new JButton("Disable");
        disableButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disablePlugin();
            }
        });

        buttonOK = new JButton("OK");
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel = new JButton("Cancel");
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });


        pluginList = new JList();
        pluginList.setCellRenderer(new CustomCellRenderer());

        // populate the plugin list
        Preferences prefs = Preferences.userRoot().node("/net/guha/apps/cdkdesc/pluginJars");
        String[] keys = {};
        try {
            keys = prefs.keys();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        if (keys.length != 0) {
            Vector<PluginListEntry> l = new Vector<PluginListEntry>();
            for (String key : keys) {
                String value = prefs.get(key, "");
                if (value.equals("")) continue;


                if (value.startsWith("T"))
                    l.add(new PluginListEntry(key, true));
                else
                    l.add(new PluginListEntry(key, false));
            }
            pluginList.setListData(l);
        }
        JScrollPane scrollPane = new JScrollPane(pluginList);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        contentPane.add(scrollPane, c);


        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.PAGE_AXIS));
        p1.add(addButton);
        p1.add(deleteButton);
        p1.add(disableButton);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;

        contentPane.add(p1, c);


        JPanel p2 = new JPanel();
        p2.setLayout(new BoxLayout(p2, BoxLayout.LINE_AXIS));
        p2.add(buttonOK);
        p2.add(Box.createRigidArea(new Dimension(2, 0)));
        p2.add(buttonCancel);


        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.ipady = 0;
        contentPane.add(p2, c);


        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

    }


    private void onCancel() {
        dispose();
    }

    private void onOK() {
        Preferences prefs = Preferences.userRoot().node("/net/guha/apps/cdkdesc/pluginJars");

        Object[] values = getAllListEntries().toArray();
        for (Object object : values) {
            PluginListEntry ple = (PluginListEntry) object;
            String key = ple.getText();
            String value;
            if (ple.isEnabled()) value = "T" + key;
            else
                value = "F" + key;
            prefs.put(key, value);
        }
        dispose();
    }


    private void addPlugin() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
//        fileChooser.addChoosableFileFilter(new JarFilter());


        int status = fileChooser.showOpenDialog(contentPane);
        if (status == JFileChooser.APPROVE_OPTION) {
            File[] selectedFile = fileChooser.getSelectedFiles();
            Vector<PluginListEntry> v = new Vector<PluginListEntry>();
            for (File file : selectedFile) {
                v.add(new PluginListEntry(file.getAbsolutePath(), true));
            }
            ArrayList currentEntries = getAllListEntries();
            if (currentEntries.size() != 0) v.addAll(currentEntries);

            pluginList.setListData(v);
        }
    }

    private ArrayList getAllListEntries() {
        int n = pluginList.getModel().getSize();
        ArrayList l = new ArrayList();
        for (int i = 0; i < n; i++)
            l.add(pluginList.getModel().getElementAt(i));
        return l;
    }

    private void disablePlugin() {
        Object[] selectedValues = pluginList.getSelectedValues();
        for (Object object : selectedValues) {
            PluginListEntry ple = (PluginListEntry) object;
            if (ple.isEnabled()) ple.setEnabled(false);
            else
                ple.setEnabled(true);
        }
        pluginList.repaint();

    }

    private void deletePlugin() {
    }


    class PluginListEntry {
        String text;
        boolean enabled;

        public String getText() {
            return text;
        }

        public PluginListEntry(String text, boolean isEnabled) {
            this.text = text;
            this.enabled = isEnabled;
        }

        public String toString() {
            return text;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean flag) {
            enabled = flag;
        }
    }

    class JarFilter extends FileFilter {
        public String getDescription() {
            return "JAR file filter";
        }

        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                return true;
            }

            String[] s = pathname.getAbsolutePath().split("\\.");
            if (s == null || s.length == 1) return false;


            String extension = s[1];
            if (extension != null) {
                return extension.equals("jar") ||
                        extension.equals("JAR") ||
                        extension.equals("Jar");
            }
            return false;
        }
    }


    class CustomCellRenderer extends JLabel implements ListCellRenderer {


        public CustomCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            PluginListEntry ple = (PluginListEntry) value;

            setText(value.toString());

            if (isSelected) {
                setBackground(new Color(153, 204, 255));
                if (ple.isEnabled()) disableButton.setText("Disable");
                else
                    disableButton.setText("Enable");
            } else {
                if (!ple.isEnabled())
                    setBackground(Color.gray);
                else
                    setBackground(Color.white);
            }


            return this;
        }
    }

}


