package net.guha.apps.cdkdesc.ui;


import net.guha.apps.cdkdesc.AppOptions;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.IDescriptor;

/**
 * @author Rajarshi Guha
 */
public class DescriptorTreeLeaf extends Object implements Comparable {
    IDescriptor instance;
    DescriptorSpecification spec;
    String definition;
    String name;

    public DescriptorSpecification getSpec() {
        return spec;
    }


    public DescriptorTreeLeaf(IDescriptor instance, String definition) {
        this.instance = instance;
        this.spec = instance.getSpecification();

        String[] tmp = this.spec.getSpecificationReference().split("#");
        this.name = AppOptions.getEngine().getDictionaryTitle(spec);
        this.definition = definition;
    }

    public IDescriptor getInstance() {
        return instance;
    }

    public String getDefinition() {
        return definition;
    }

    public String getName() {
        return name;
    }

    public boolean equals(DescriptorTreeLeaf aLeaf) {
        return name.equals(aLeaf.getName());
    }

    public String toString() {
        return name;
    }

    public int compareTo(Object o) throws ClassCastException {
        if (!(o instanceof DescriptorTreeLeaf)) throw new ClassCastException("Expected a DescriptorTreeLeaf");
        DescriptorTreeLeaf aLeaf = (DescriptorTreeLeaf) o;
        return this.name.compareTo(aLeaf.getName());
    }
}
