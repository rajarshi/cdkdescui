package net.guha.apps.cdkdesc.ui;

import net.guha.apps.cdkdesc.AppOptions;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Rajarshi Guha
 */
public class FingerprintPanel extends JPanel {

    public FingerprintPanel() {
        super(true);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        String[] labels = {"Standard", "Extended", "Graph only", "EState", "MACCS", "Pubchem"};
        FPRadioButtonListener fpButtonListener = new FPRadioButtonListener();

        ButtonGroup buttonGroup = new ButtonGroup();
        for (String label : labels) {
            JRadioButton button = new JRadioButton(label);
            button.setActionCommand(label);
            button.addActionListener(fpButtonListener);
            buttonGroup.add(button);
            add(button);
        }
    }

    class FPRadioButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            AppOptions.setSelectedFingerprintType(event.getActionCommand());
        }
    }

}
