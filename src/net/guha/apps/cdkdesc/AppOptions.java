/**
 *
 * @author Rajarshi Guha
 */
package net.guha.apps.cdkdesc;

import org.openscience.cdk.qsar.DescriptorEngine;

import java.util.HashMap;
import java.util.Map;

public class AppOptions {

    private static String outputMethod = CDKDescConstants.OUTPUT_SPC;
    private static DescriptorEngine engine = new DescriptorEngine(DescriptorEngine.MOLECULAR);
    private static Map<String, Boolean> selectedDescriptors = new HashMap<String, Boolean>();
    private static String settingsFile = "";

    public String getSettingsFile() {
        return settingsFile;
    }

    public void setSettingsFile(String settingsFile) {
        AppOptions.settingsFile = settingsFile;
    }

    public Map<String, Boolean> getSelectedDescriptors() {
        return selectedDescriptors;
    }

    public static DescriptorEngine getEngine() {
        return engine;
    }

    public String getOutputMethod() {
        return outputMethod;
    }

    public void setOutputMethod(String outputMethod) {
        AppOptions.outputMethod = outputMethod;
    }

    private static AppOptions ourInstance = new AppOptions();

    public static AppOptions getInstance() {
        return ourInstance;
    }

    private AppOptions() {
    }
}
