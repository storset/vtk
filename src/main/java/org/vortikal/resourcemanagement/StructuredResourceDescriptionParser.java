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
import java.util.List;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resource.ResourcetreeLexer;
import org.vortikal.repository.resource.ResourcetreeParser;

/**
 * XXX TEMP solution (?) Convert resourcetree to list of
 * StructuredResourceDescriptions Use the walker (ResourcetreeWalker) instead
 */
public class StructuredResourceDescriptionParser {

    private String resourceDescriptionFileLocation;
    private org.springframework.core.io.Resource defaultResourceTypeDefinitions;
    private StructuredResourceManager structuredResourceManager;

    @SuppressWarnings("unchecked")
    public List<StructuredResourceDescription> getResourceDescriptions() throws Exception {

        ResourcetreeParser parser = createParser(resourceDescriptionFileLocation);
        ResourcetreeParser.resources_return resources = parser.resources();
        CommonTree resourcetree = (CommonTree) resources.getTree();
        List<StructuredResourceDescription> structuredResources = new ArrayList<StructuredResourceDescription>();
        List<CommonTree> children = resourcetree.getChildren();

        if (children.size() == 1) {
            StructuredResourceDescription srd = createStructuredResourceDescription(children
                    .get(0));
            structuredResources.add(srd);
            return structuredResources;
        }

        for (CommonTree child : children) {
            if (ResourcetreeLexer.RESOURCETYPE == child.getType()) {
                StructuredResourceDescription srd = createStructuredResourceDescription(child
                        .getChild(0));
                structuredResources.add(srd);
            }
        }

        return structuredResources;
    }

    @SuppressWarnings("unchecked")
    private StructuredResourceDescription createStructuredResourceDescription(
            Tree resource) {
        StructuredResourceDescription srd = new StructuredResourceDescription(
                this.structuredResourceManager);
        srd.setName(resource.getText());

        List<CommonTree> resourceDescription = ((CommonTree) resource).getChildren();
        for (CommonTree desciptionEntry : resourceDescription) {
            switch (desciptionEntry.getType()) {
            case ResourcetreeLexer.PARENT:
                srd.setInheritsFrom(desciptionEntry.getChild(0).getText());
                break;
            case ResourcetreeLexer.PROPERTIES:
                handlePropertyDescriptions(srd, desciptionEntry.getChildren());
                break;
            case ResourcetreeLexer.EDITRULES:
                handleEditRulesDescriptions(srd, desciptionEntry.getChildren());
                break;
            case ResourcetreeLexer.VIEWDEFINITION:
                srd.setDisplayTemplate(new DisplayTemplate(desciptionEntry.getChild(0)
                        .getText()));
                break;
            default:
                // XXX throw exception? -> uknown token type
                break;
            }
        }

        return srd;
    }

    @SuppressWarnings("unchecked")
    private void handlePropertyDescriptions(StructuredResourceDescription srd,
            List<CommonTree> propertyDescriptions) {
        List<PropertyDescription> props = new ArrayList<PropertyDescription>();
        for (CommonTree propDesc : propertyDescriptions) {
            PropertyDescription p = new PropertyDescription();
            p.setName(propDesc.getText());
            setPropertyDescription(p, propDesc.getChildren());
            props.add(p);
        }
        srd.setPropertyDescriptions(props);
    }

    private void setPropertyDescription(PropertyDescription p, List<CommonTree> propertyDescription) {
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
                // XXX implement
                break;
            default:
                // XXX throw exception? -> uknown token type 
                break;
            }
        }
    }

    private void handleEditRulesDescriptions(StructuredResourceDescription srd,
            List<CommonTree> editRuleDescriptions) {
        for (CommonTree editRuleDescription : editRuleDescriptions) {
            if (ResourcetreeLexer.GROUP == editRuleDescription.getType()) {
                handleGroupedEditRuleDescription(srd, editRuleDescription);
            } else {
                // XXX implement
            }
        }
    }

    private void handleGroupedEditRuleDescription(StructuredResourceDescription srd,
            CommonTree editRuleDescription) {
        // XXX implement
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

}
