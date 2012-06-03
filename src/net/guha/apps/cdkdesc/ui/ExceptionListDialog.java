package net.guha.apps.cdkdesc.ui;

import net.guha.apps.cdkdesc.ExceptionInfo;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author rguha
 */
public class ExceptionListDialog extends JDialog {
    private String errorText;

    private void makeText(List<ExceptionInfo> list) {
        errorText = "<html><body>";
        errorText += "<table>" +
                "<tr>" +
                "<td><b>Serial Number</b></td><td><b>Title</b></td><td><b>Exception Message</b></td>" +
                "<td><b>Descriptor</b></td>" +
                "</tr>";
        for (ExceptionInfo ei : list) {
            int molnum = ei.getSerial();
            IAtomContainer molecule = ei.getMolecule();
            String title = (String) molecule.getProperty(CDKConstants.TITLE);
            if (title == null) title = "";
            String excepText = ei.getException().getMessage();
            String descName = ei.getDescriptorName();

            errorText += "<tr>" + "<td>" + molnum + "</td>" +
                    "<td>" + title + "</td>" +
                    "<td>" + excepText + "</td><td>" + descName + "</td></tr>";
        }
        errorText += "</table></body></html>";
    }

    public ExceptionListDialog(List<ExceptionInfo> list) throws HeadlessException {
        super();

        makeText(list);

        setTitle("CDK Descriptor UI - Exceptions");

        JPanel panel = new JPanel(new BorderLayout());
        JEditorPane textArea = new JEditorPane("text/html", errorText);
        textArea.setMargin(new Insets(5, 5, 5, 5));
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(closeButton);
        buttonPanel.add(saveButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        panel.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setContentPane(panel);
        setSize(400, 200);
    }

    private void onOK() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.showSaveDialog(this.getParent());
        File aFile = fileChooser.getSelectedFile();
        if (aFile == null) return;

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(aFile));
            out.write(errorText);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

}
