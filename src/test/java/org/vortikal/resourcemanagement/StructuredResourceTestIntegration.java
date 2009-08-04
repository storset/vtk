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

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.vortikal.repository.AbstractBeanContextTestIntegration;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatterRegistry;
import org.vortikal.resourcemanagement.parser.StructuredResourceParser;
import org.vortikal.web.service.JSONObjectSelectAssertion;

public class StructuredResourceTestIntegration extends AbstractBeanContextTestIntegration {

    private StructuredResourceParser srdp;
    private StructuredResourceManager srm;

    protected void setUp() throws Exception {
        super.setUp();

        ApplicationContext ctx = getApplicationContext(false,
                "resource-types/resource.xml", "resource-types/json.xml",
                "resource-types/file.xml", "repository.xml");

        srdp = new StructuredResourceParser();
        srdp.setDefaultResourceTypeDefinitions(new ClassPathResource(
                "vortikal/beans/vhost/structured-resources.vrtx"));

        srm = new StructuredResourceManager();
        JSONObjectSelectAssertion assertion = (JSONObjectSelectAssertion) ctx
                .getBean("json.objectHasResourceTypeAssertion");
        srm.setAssertion(assertion);
        PrimaryResourceTypeDefinition baseType = (PrimaryResourceTypeDefinition) ctx
                .getBean("json.managedObjectResourceType");
        srm.setBaseType(baseType);
        ResourceTypeTree resourceTypeTree = (ResourceTypeTree) ctx
                .getBean("resourceTypeTree");
        srm.setResourceTypeTree(resourceTypeTree);
        ValueFactory valueFactory = (ValueFactory) ctx.getBean("valueFactory");
        srm.setValueFactory(valueFactory);
        ValueFormatterRegistry valueFormatterRegistry = (ValueFormatterRegistry) ctx
                .getBean("valueFormatterRegistry");
        srm.setValueFormatterRegistry(valueFormatterRegistry);
        srdp.setStructuredResourceManager(srm);

        srdp.registerStructuredResources();
    }

    public void testGetResourceDescriptions() throws Exception {
        String[] resourceNames = { "jperson" };
        for (String resourceName : resourceNames) {
            StructuredResourceDescription srd = srdp.getResourceDescription(resourceName);
            assertNotNull(srd);
            printResourceDescription(srd);
        }
    }

    private void printResourceDescription(StructuredResourceDescription srd) {
        String inheritsFrom = srd.getInheritsFrom();
        System.out.println(srd.getName()
                + (inheritsFrom != null ? (" > " + srd.getInheritsFrom()) : ""));
        List<PropertyDescription> propertyDescriptions = srd.getAllPropertyDescriptions();
        if (propertyDescriptions != null) {
            System.out.println("\tProperties:");
            for (PropertyDescription pd : propertyDescriptions) {
                System.out.println("\t\t" + pd.getName() + ": " + pd.getType() + " "
                        + pd.isMultiple() + " " + pd.isRequired() + " "
                        + pd.isNoExtract() + " " + pd.getOverrides());
                Map<String, Object> edithints = pd.getEdithints();
                if (edithints != null) {
                    System.out.println("\t\t\tEdithints: " + edithints);
                }
            }
        }
        List<EditRule> editRules = srd.getEditRules();
        if (editRules != null) {
            System.out.println("\tEditRules:");
            for (EditRule editRule : editRules) {
                System.out.println("\t\t" + editRule.getName() + " " + editRule.getType()
                        + " " + editRule.getValue());
            }
        }
        DisplayTemplate displayTemplate = srd.getDisplayTemplate();
        if (displayTemplate != null) {
            System.out.println("\tViewDefinition:");
            System.out.println("\t\t" + displayTemplate.getTemplate());
        }
        List<ScriptDefinition> scripts = srd.getScripts();
        if (scripts != null) {
            System.out.println("\tScripts:");
            for (ScriptDefinition sd : scripts) {
                System.out.println("\t\t" + sd.getName() + " " + sd.getType() + " "
                        + sd.getParams());
            }
        }
    }
}
