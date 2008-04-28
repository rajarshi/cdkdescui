/**
 *
 * @author Rajarshi Guha
 */
package net.guha.apps.cdkdesc;

import org.openscience.cdk.qsar.DescriptorEngine;

public class AppOptions {

    private static String outputMethod = CDKDescConstants.OUTPUT_SPC;
    private static DescriptorEngine engine = new DescriptorEngine(DescriptorEngine.MOLECULAR);

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
