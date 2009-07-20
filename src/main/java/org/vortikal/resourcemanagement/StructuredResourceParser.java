/* Copyright (c) 2009, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.resourcemanagement;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resource.ResourcetreeLexer;
import org.vortikal.repository.resource.ResourcetreeParser;
import org.vortikal.resourcemanagement.EditRule.Type;

@SuppressWarnings("unchecked")
public class StructuredResourceParser implements InitializingBean {

    private String resourceDescriptionFileLocation;
    private org.springframework.core.io.Resource defaultResourceTypeDefinitions;
    private StructuredResourceManager structuredResourceManager;

    public static final String PROPTYPE_STRING = "string";
    public static final String PROPTYPE_HTML = "html";
    public static final String PROPTYPE_SIMPLEHTML = "simple_html";
    public static final String PROPTYPE_BOOLEAN = "boolean";
    public static final String PROPTYPE_INT = "int";
    public static final String PROPTYPE_DATETIME = "datetime";
    public static final String PROPTYPE_IMAGEREF = "image_ref";

    public void registerStructuredResources() throws Exception {

        ResourcetreeParser parser = createParser(resourceDescriptionFileLocation);
        ResourcetreeParser.resources_return resources = parser.resources();
        if (parser.getNumberOfSyntaxErrors() > 0) {
            List<String> messages = parser.getErrorMessages();
            StringBuilder mainMessage = new StringBuilder();
            for (String m : messages) {
                mainMessage.append(m);
            }
            throw new IllegalStateException("Unable to parse resource tree description: "
                    + resourceDescriptionFileLocation + ": " + mainMessage.toString());
        }
        CommonTree resourcetree = (CommonTree) resources.getTree();
        List<CommonTree> children = resourcetree.getChildren();
        if (children.size() == 1) {
            StructuredResourceDescription srd = createStructuredResourceDescription(children
                    .get(0));
            this.structuredResourceManager.register(srd);
        } else {
            for (CommonTree child : children) {
                if (ResourcetreeLexer.RESOURCETYPE == child.getType()) {
                    StructuredResourceDescription srd = createStructuredResourceDescription(child
                            .getChild(0));
                    this.structuredResourceManager.register(srd);
                }
            }
        }

    }

    private StructuredResourceDescription createStructuredResourceDescription(
            Tree resource) {
        StructuredResourceDescription srd = new StructuredResourceDescription(
                this.structuredResourceManager);
        srd.setName(resource.getText());

        List<CommonTree> resourceDescription = ((CommonTree) resource).getChildren();
        if (hasContent(resourceDescription)) {
            for (CommonTree descriptionEntry : resourceDescription) {
                switch (descriptionEntry.getType()) {
                case ResourcetreeLexer.PARENT:
                    srd.setInheritsFrom(descriptionEntry.getChild(0).getText());
                    break;
                case ResourcetreeLexer.PROPERTIES:
                    handlePropertyDescriptions(srd, descriptionEntry.getChildren());
                    break;
                case ResourcetreeLexer.EDITRULES:
                    handleEditRulesDescriptions(srd, descriptionEntry.getChildren());
                    break;
                case ResourcetreeLexer.VIEWCOMPONENTS:
                    handleViewComponents(srd, descriptionEntry.getChildren());
                    break;
                case ResourcetreeLexer.VIEWDEFINITION:
                    if (descriptionEntry.getChild(0) != null) {
                        srd.setDisplayTemplate(new DisplayTemplate(descriptionEntry
                                .getChild(0).getText()));
                    }
                    break;
                case ResourcetreeLexer.LOCALIZATIONPROPERTIES:
                    handleLocalization(srd, descriptionEntry.getChildren());
                    break;
                default:
                    throw new IllegalStateException("Unknown token type: "
                            + descriptionEntry.getType());
                }
            }
        }

        return srd;
    }

    private void handlePropertyDescriptions(StructuredResourceDescription srd,
            List<CommonTree> propertyDescriptions) {
        List<PropertyDescription> props = new ArrayList<PropertyDescription>();
        if (hasContent(propertyDescriptions)) {
            for (CommonTree propDesc : propertyDescriptions) {
                PropertyDescription p = new PropertyDescription();
                p.setName(propDesc.getText());
                setPropertyDescription(p, propDesc.getChildren());
                props.add(p);
            }
            srd.setPropertyDescriptions(props);
        }
    }

    private void handleLocalization(StructuredResourceDescription srd,
            List<CommonTree> propertyDescriptions) {
        if (hasContent(propertyDescriptions)) {
            for (CommonTree propDesc : propertyDescriptions) {
                // TODO: Gjøre om til HashMap <Locale, String>....
                HashMap<String, String> m = new HashMap<String, String>();
                for (CommonTree lang : (List<CommonTree>) propDesc.getChildren()) {
                    for (CommonTree label : (List<CommonTree>) lang.getChildren()) {
                        m.put(lang.getText(), label.getText());
                    }
                }
                srd.addLocalization(propDesc.getText(), (Map<String, String>) m);
            }
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
            default:
                throw new IllegalStateException("Unknown token type: "
                        + descEntry.getType());
            }
        }
    }

    private void handleEditRulesDescriptions(StructuredResourceDescription srd,
            List<CommonTree> editRuleDescriptions) {
        if (hasContent(editRuleDescriptions)) {
            for (CommonTree editRuleDescription : editRuleDescriptions) {
                if (ResourcetreeLexer.GROUP == editRuleDescription.getType()) {
                    handleGroupedEditRuleDescription(srd, editRuleDescription);
                } else {
                    String propName = editRuleDescription.getText();
                    CommonTree editRule = (CommonTree) editRuleDescription.getChild(0);
                    switch (editRule.getType()) {
                    case ResourcetreeLexer.BEFORE:
                        srd.addEditRule(new EditRule(propName, Type.POSITION_BEFORE,
                                editRule.getChild(0).getText()));
                        break;
                    case ResourcetreeLexer.AFTER:
                        srd.addEditRule(new EditRule(propName, Type.POSITION_AFTER,
                                editRule.getChild(0).getText()));
                        break;
                    case ResourcetreeLexer.EDITHINT:
                        srd.addEditRule(new EditRule(propName, Type.EDITHINT, editRule
                                .getText()));
                        break;
                    default:
                        break;
                    }
                }
            }
        }
    }

    private void handleGroupedEditRuleDescription(StructuredResourceDescription srd,
            CommonTree groupRuleDescription) {

        CommonTree groupingNameElement = (CommonTree) groupRuleDescription.getChild(0);
        if (ResourcetreeLexer.NAME != groupingNameElement.getType()) {
            throw new IllegalStateException(
                    "Firs element in a grouping definition must be a name");
        }
        String groupingName = groupingNameElement.getText();
        List<String> groupedProps = new ArrayList<String>();
        for (CommonTree prop : (List<CommonTree>) groupingNameElement.getChildren()) {
            groupedProps.add(prop.getText());
        }
        srd.addEditRule(new EditRule(groupingName, Type.GROUP, groupedProps));

        CommonTree positioningElement = (CommonTree) groupRuleDescription.getChild(1);
        int groupingType = positioningElement.getType();
        if (ResourcetreeLexer.AFTER == groupingType
                || ResourcetreeLexer.BEFORE == groupingType) {
            Type positioningType = ResourcetreeLexer.AFTER == groupingType ? Type.POSITION_AFTER
                    : Type.POSITION_BEFORE;
            srd.addEditRule(new EditRule(groupingName, positioningType,
                    positioningElement.getChild(0).getText()));
        }

        CommonTree oriantationElement = (CommonTree) groupRuleDescription.getChild(2);
        if (oriantationElement != null) {
            srd.addEditRule(new EditRule(groupingName, Type.EDITHINT, oriantationElement
                    .getText()));
        }
    }

    private void handleViewComponents(StructuredResourceDescription srd,
            List<CommonTree> viewComponentDefinitions) {
        if (!hasContent(viewComponentDefinitions)) {
            return;
        }
        for (CommonTree viewComponentDescription : viewComponentDefinitions) {

            if (viewComponentDescription.getChildren().size() == 1) {
                String name = viewComponentDescription.getText();
                String def = viewComponentDescription.getChild(0).getText();
                srd.addComponentDefinition(new ComponentDefinition(name, def));
            }
        }
    }

    private boolean hasContent(List<CommonTree> tree) {
        return tree != null && tree.size() > 0;
    }

    private ResourcetreeParser createParser(String filename) throws IOException {
        InputStream in = getResourceTypeDefinitionAsStream(this.resourceDescriptionFileLocation);
        ResourcetreeLexer lexer = new ResourcetreeLexer(new ANTLRInputStream(in));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ResourcetreeParser parser = new ResourcetreeParser(tokens);
        return parser;
    }

    private InputStream getResourceTypeDefinitionAsStream(
            String resourceTypeDefinitionsLocation) throws IOException {
        InputStream in = null;
        if (!StringUtils.isBlank(resourceTypeDefinitionsLocation)) {
            if (resourceTypeDefinitionsLocation.matches("^(http(s?)\\:\\/\\/|www)\\S*")) {
                URL url = new URL(resourceTypeDefinitionsLocation);
                in = url.openStream();
            } else {
                in = new BufferedInputStream(new FileInputStream(
                        resourceTypeDefinitionsLocation));
            }
        } else {
            in = this.defaultResourceTypeDefinitions.getInputStream();
        }
        return in;
    }

    public StructuredResourceDescription getResourceDescription(String name) {
        return this.structuredResourceManager.get(name);
    }

    public void setResourceDescriptionFileLocation(String resourceDescriptionFileLocation) {
        this.resourceDescriptionFileLocation = resourceDescriptionFileLocation;
    }

    @Required
    public void setDefaultResourceTypeDefinitions(
            org.springframework.core.io.Resource defaultResourceTypeDefinitions) {
        this.defaultResourceTypeDefinitions = defaultResourceTypeDefinitions;
    }

    @Required
    public void setStructuredResourceManager(
            StructuredResourceManager structuredResourceManager) {
        this.structuredResourceManager = structuredResourceManager;
    }

    public void afterPropertiesSet() throws Exception {
        this.registerStructuredResources();
    }

}
