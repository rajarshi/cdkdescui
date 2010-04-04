package net.guha.apps.cdkdesc;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.listener.IChemObjectIOListener;
import org.openscience.cdk.io.setting.IOSetting;

public class SDFormatListener implements IChemObjectIOListener {
    public void processIOSettingQuestion(IOSetting setting) {
        if ("ForceReadAs3DCoordinates".equals(setting.getName())) {
            try {
                setting.setSetting("true");
            } catch (CDKException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}