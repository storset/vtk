package org.vortikal.resourcemanagement.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.vortikal.repository.resource.ResourcetreeLexer;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.StructuredResourceDescription;

public class PropertyDescriptionParser {

    @SuppressWarnings("unchecked")
    public void parsePropertyDescriptions(StructuredResourceDescription srd,
            List<CommonTree> propertyDescriptions) {
        List<PropertyDescription> props = new ArrayList<PropertyDescription>();
        if (propertyDescriptions != null) {
            for (CommonTree propDesc : propertyDescriptions) {
                PropertyDescription p = new PropertyDescription();
                p.setName(propDesc.getText());
                setPropertyDescription(p, propDesc.getChildren());
                props.add(p);
            }
            srd.setPropertyDescriptions(props);
        }
    }

    private void setPropertyDescription(PropertyDescription p,
            List<CommonTree> propertyDescription) {
        for (CommonTree descEntry : propertyDescription) {
            switch (descEntry.getType()) {
            case ResourcetreeLexer.PROPTYPE:
                p.setType(descEntry.getText());
                break;
            case ResourcetreeLexer.REQUIRED:
                p.setRequired(true);
                break;
            case ResourcetreeLexer.NOEXTRACT:
                p.setNoExtract(true);
                break;
            case ResourcetreeLexer.OVERRIDES:
                p.setOverrides(descEntry.getChild(0).getText());
                break;
            case ResourcetreeLexer.MULTIPLE:
                p.setMultiple(true);
                break;
            default:
                throw new IllegalStateException("Unknown token type: "
                        + descEntry.getType());
            }
        }
    }

}
