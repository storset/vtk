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
package org.vortikal.resourcemanagement.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.vortikal.repository.resource.ResourcetreeLexer;
import org.vortikal.repository.resource.ResourcetreeParser;
import org.vortikal.resourcemanagement.ComponentDefinition;
import org.vortikal.resourcemanagement.DisplayTemplate;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.StructuredResourceManager;


@SuppressWarnings("unchecked")
public class StructuredResourceParser implements InitializingBean {

    private Resource defaultResourceTypeDefinitions;
    private StructuredResourceManager structuredResourceManager;
    private List<ParsedResourceDescription> parsedResourceDescriptions;

    private PropertyDescriptionParser propertyDescriptionParser;
    private EditRuleParser editRuleParser;
    private ScriptDefinitionParser scriptDefinitionParser;
    private ServiceDefinitionParser serviceDefinitionParser;
    private VocabularyDefinitionParser vocabularyDefinitionParser;
    
    private static Log logger = LogFactory.getLog(StructuredResourceParser.class);

    public void afterPropertiesSet() throws Exception {
        this.parsedResourceDescriptions = new ArrayList<ParsedResourceDescription>();

        this.propertyDescriptionParser = new PropertyDescriptionParser();
        this.editRuleParser = new EditRuleParser();
        this.scriptDefinitionParser = new ScriptDefinitionParser();
        this.serviceDefinitionParser = new ServiceDefinitionParser();
        this.vocabularyDefinitionParser = new VocabularyDefinitionParser();

        this.registerStructuredResources();
    }

    private void registerStructuredResources() throws Exception {
        ParseUnit parseUnit = createParser(null);
        parseResourceTypeDefinition(parseUnit);

        List<ParsedResourceDescription> tmp = new ArrayList<ParsedResourceDescription>();
        for (ParsedResourceDescription prd : this.parsedResourceDescriptions) {
            if (prd.hasParent()) {
                ParsedResourceDescription parent = getParent(this.parsedResourceDescriptions, prd);
                if (parent != null) {
                    parent.addChild(prd);
                }
                tmp.add(prd);
            }
        }
        this.parsedResourceDescriptions.removeAll(tmp);
        registerParsedResourceDescriptions(this.parsedResourceDescriptions);
        this.structuredResourceManager.registrationComplete();
    }

    private void registerParsedResourceDescriptions(List<ParsedResourceDescription> l) throws Exception {
        for (ParsedResourceDescription prd : l) {
            this.structuredResourceManager.register(prd.getStructuredResourceDescription());
            if (prd.hasChildren()) {
                registerParsedResourceDescriptions(prd.getChildren());
            }
        }
    }

    private void parseResourceTypeDefinition(ParseUnit parseUnit) throws Exception {
        logger.info("Parse: " + parseUnit.description);
        
        ResourcetreeParser parser = parseUnit.parser;
        ResourcetreeParser.resources_return resources = parser.resources();
        if (parser.getNumberOfSyntaxErrors() > 0) {
            
            List<String> messages = parser.getErrorMessages();
            StringBuilder mainMessage = new StringBuilder();
            for (String m : messages) {
                mainMessage.append(parseUnit.description + ": " + m);
            }
            throw new IllegalStateException("Unable to parse resource tree description: " + mainMessage.toString());
        }
        
        CommonTree resourcetree = (CommonTree) resources.getTree();
        List<CommonTree> children = resourcetree.getChildren();
        if (children.size() == 1) {
            handleResourceTypeDefinition(children.get(0));
        } else {
            for (CommonTree child : children) {
                handleResourceTypeDefinition((CommonTree) child.getChild(0));
            }
        }
    }

    private void handleResourceTypeDefinition(CommonTree definition) throws Exception {
        if (ResourcetreeLexer.RESOURCETYPE == definition.getParent().getType()) {
            StructuredResourceDescription srd = createStructuredResourceDescription(definition);
            parsedResourceDescriptions.add(new ParsedResourceDescription(srd));
        } else if (ResourcetreeLexer.INCLUDE == definition.getParent().getType()) {
            String includeFileName = definition.getText();
            ParseUnit parseUnit = createParser(includeFileName);
            parseResourceTypeDefinition(parseUnit);
        }
    }

    private StructuredResourceDescription createStructuredResourceDescription(Tree resource) {
        StructuredResourceDescription srd = new StructuredResourceDescription(this.structuredResourceManager);
        srd.setName(resource.getText());

        List<CommonTree> resourceDescription = ((CommonTree) resource).getChildren();
        if (hasContent(resourceDescription)) {
            for (CommonTree descriptionEntry : resourceDescription) {
                switch (descriptionEntry.getType()) {
                case ResourcetreeLexer.PARENT:
                    srd.setInheritsFrom(descriptionEntry.getChild(0).getText());
                    break;
                case ResourcetreeLexer.PROPERTIES:
                    this.propertyDescriptionParser.parsePropertyDescriptions(srd, descriptionEntry.getChildren());
                    break;
                case ResourcetreeLexer.EDITRULES:
                    this.editRuleParser.parseEditRulesDescriptions(srd, descriptionEntry.getChildren());
                    break;
                case ResourcetreeLexer.VIEWCOMPONENTS:
                    handleViewComponents(srd, descriptionEntry.getChildren());
                    break;
                case ResourcetreeLexer.VIEW:
                    if (descriptionEntry.getChild(0) != null) {
                        srd.setDisplayTemplate(new DisplayTemplate(descriptionEntry.getChild(0).getText()));
                    }
                    break;
                case ResourcetreeLexer.LOCALIZATION:
                    handleLocalization(srd, descriptionEntry.getChildren());
                    break;
                case ResourcetreeLexer.SCRIPTS:
                    this.scriptDefinitionParser.parseScripts(srd, descriptionEntry.getChildren());
                    break;
                case ResourcetreeLexer.SERVICES:
                    this.serviceDefinitionParser.parseServices(srd, descriptionEntry.getChildren());
                    break;
                case ResourcetreeLexer.VOCABULARY:
                    this.vocabularyDefinitionParser.handleVocabulary(srd, descriptionEntry.getChildren());
                    break;
                default:
                    throw new IllegalStateException(srd.getName() + ": unknown token type: " + descriptionEntry.getText());
                }
            }
        }
        return srd;
    }
    
    private void handleLocalization(StructuredResourceDescription srd, List<CommonTree> propertyDescriptions) {
        if (!hasContent(propertyDescriptions)) {
            return;
        }
        for (CommonTree propDesc : propertyDescriptions) {
            Map<Locale, String> localizationMap = new HashMap<Locale, String>();
            for (CommonTree lang : (List<CommonTree>) propDesc.getChildren()) {
                for (CommonTree label : (List<CommonTree>) lang.getChildren()) {
                    Locale locale = LocaleUtils.toLocale(lang.getText());
                    localizationMap.put(locale, label.getText());
                }
            }
            srd.addLocalization(propDesc.getText(), localizationMap);
        }
    }

    private void handleViewComponents(StructuredResourceDescription srd, List<CommonTree> viewComponentDefinitions) {
        if (!hasContent(viewComponentDefinitions)) {
            return;
        }
        for (CommonTree viewComponentDescription : viewComponentDefinitions) {

            if (viewComponentDescription.getChildren().size() >= 1) {
                String name = viewComponentDescription.getText();
                String def = viewComponentDescription.getChild(0).getText();
                ComponentDefinition compDef = new ComponentDefinition(name, def);
                List<String> parameters = new ArrayList<String>();
                if (viewComponentDescription.getChildCount() > 1) {
                    for (int i = 1; i < viewComponentDescription.getChildCount(); i++) {
                        String param = viewComponentDescription.getChild(i).getText();
                        parameters.add(param);
                    }
                    compDef.setParameters(parameters);
                }
                srd.addComponentDefinition(compDef);
            }
        }
    }

    private boolean hasContent(List<CommonTree> tree) {
        return tree != null && tree.size() > 0;
    }

    private class ParseUnit {
        ResourcetreeParser parser;
        String description;
    }
    
    private ParseUnit createParser(String filename) throws IOException {
        Resource resource = resolveLocation(filename);
        if (resource == null) {
            throw new IllegalArgumentException("Unable to resolve location: " + filename);
        }
        InputStream in = resource.getInputStream();
        ResourcetreeLexer lexer = new ResourcetreeLexer(new ANTLRInputStream(in, "UTF-8"));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ResourcetreeParser parser = new ResourcetreeParser(tokens);
        ParseUnit parseUnit = new ParseUnit();
        parseUnit.parser = parser;
        parseUnit.description = resource.getDescription();
        return parseUnit;
    }

    private Resource resolveLocation(String filename) throws IOException {
        if (filename != null) {
            return this.defaultResourceTypeDefinitions.createRelative(filename);
        } else {
            return this.defaultResourceTypeDefinitions;
        }
    }

    public StructuredResourceDescription getResourceDescription(String name) {
        return this.structuredResourceManager.get(name);
    }

    @Required
    public void setDefaultResourceTypeDefinitions(Resource defaultResourceTypeDefinitions) {
        this.defaultResourceTypeDefinitions = defaultResourceTypeDefinitions;
    }

    @Required
    public void setStructuredResourceManager(StructuredResourceManager structuredResourceManager) {
        this.structuredResourceManager = structuredResourceManager;
    }

    private class ParsedResourceDescription {

        private StructuredResourceDescription srd;
        private List<ParsedResourceDescription> children;

        private ParsedResourceDescription(StructuredResourceDescription srd) {
            this.srd = srd;
        }

        public StructuredResourceDescription getStructuredResourceDescription() {
            return this.srd;
        }

        public void addChild(ParsedResourceDescription prd) {
            if (this.children == null) {
                this.children = new ArrayList<ParsedResourceDescription>();
            }
            children.add(prd);
        }

        public boolean hasChildren() {
            return this.children != null && this.children.size() > 0;
        }

        public List<ParsedResourceDescription> getChildren() {
            return this.children;
        }

        public String getName() {
            return this.srd.getName();
        }

        public boolean hasParent() {
            return this.srd.getInheritsFrom() != null;
        }

        public String getParentName() {
            return this.srd.getInheritsFrom();
        }

        public String toString() {
            return this.srd.getName() + (this.hasChildren() ? this.children : "");
        }

    }

    private ParsedResourceDescription getParent(List<ParsedResourceDescription> l, ParsedResourceDescription prd) {
        for (ParsedResourceDescription p : l) {
            if (p.getName().equals(prd.getParentName())) {
                return p;
            } else if (p.hasChildren()) {
                getParent(p.getChildren(), prd);
            }
        }
        return null;
    }

}
