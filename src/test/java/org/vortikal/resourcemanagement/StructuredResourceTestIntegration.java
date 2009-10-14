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

public class StructuredResourceTestIntegration extends StructuredResourceTestSetup {

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testGetResourceDescriptions() throws Exception {
        String[] resourceNames = { "person" };
        for (String resourceName : resourceNames) {
            StructuredResourceDescription srd = srdp.getResourceDescription(resourceName);
            assertNotNull(srd);
            printResourceDescription(srd);
        }
    }

    private void printResourceDescription(StructuredResourceDescription srd) {
        String inheritsFrom = srd.getInheritsFrom();
        System.out.println(srd.getName() + (inheritsFrom != null ? (" > " + srd.getInheritsFrom()) : ""));
        List<PropertyDescription> propertyDescriptions = srd.getAllPropertyDescriptions();
        if (propertyDescriptions != null) {
            System.out.println("\tProperties:");
            for (PropertyDescription d : propertyDescriptions) {
                if (d instanceof BinaryPropertyDescription) {
                    System.out.println("\t\t" + d.getName() + ": " + d.getType());
                } else if (d instanceof DerivedPropertyDescription) {
                    DerivedPropertyDescription dp = (DerivedPropertyDescription) d;
                    System.out.println("\t\t" + dp.getName() + ": " + dp.getType() + " " + dp.isMultiple()
                            + " derived from: " + dp.getDependentProperties() + " " + dp.getOverrides());

                } else if (d instanceof JSONPropertyDescription) {
                    JSONPropertyDescription jd = (JSONPropertyDescription) d;
                    System.out.println("\t\t" + jd.getName() + ": " + jd.getType() + " " + jd.isMultiple() + " "
                            + jd.isNoExtract() + " " + jd.getOverrides()
                            + (jd.hasExternalService() ? "(external:" + jd.getExternalService() + ")" : ""));
                } else {
                    SimplePropertyDescription sd = (SimplePropertyDescription) d;
                    System.out.println("\t\t" + sd.getName() + ": " + sd.getType() + " " + sd.isMultiple() + " "
                            + sd.isRequired() + " " + sd.isNoExtract() + " " + sd.getOverrides()
                            + (sd.hasExternalService() ? "(external:" + sd.getExternalService() + ")" : ""));
                }
            }
        }
        List<EditRule> editRules = srd.getEditRules();
        if (editRules != null) {
            System.out.println("\tEditRules:");
            for (EditRule editRule : editRules) {
                System.out.println("\t\t" + editRule.getName() + " " + editRule.getType() + " " + editRule.getValue());
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
                System.out.println("\t\t" + sd.getName() + " " + sd.getType() + " " + sd.getParams());
            }
        }
        List<ServiceDefinition> services = srd.getServices();
        if (services != null) {
            System.out.println("\tServices:");
            for (ServiceDefinition sd : services) {
                System.out.println("\t\t" + sd.getName() + " " + sd.getServiceName() + " " + sd.getRequires());
            }
        }

    }
}
