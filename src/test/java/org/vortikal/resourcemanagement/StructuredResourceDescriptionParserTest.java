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

import junit.framework.TestCase;

import org.springframework.core.io.ClassPathResource;
import org.vortikal.resourcemanagement.DisplayTemplate;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.StructuredResourceDescriptionParser;

public class StructuredResourceDescriptionParserTest extends TestCase {

    private StructuredResourceDescriptionParser srdp;

    protected void setUp() throws Exception {
        super.setUp();
        srdp = new StructuredResourceDescriptionParser();
        srdp.setDefaultResourceTypeDefinitions(new ClassPathResource(
                "vortikal/beans/vhost/structured-resources.vrtx"));
    }

    public void testGetResourceDescriptions() throws Exception {
        List<StructuredResourceDescription> resourceDescriptions = srdp
                .getResourceDescriptions();
        assertNotNull(resourceDescriptions);
        assertTrue(resourceDescriptions.size() > 0);
        for (StructuredResourceDescription srd : resourceDescriptions) {
            String inheritsFrom = srd.getInheritsFrom();
            System.out.println(srd.getName()
                    + (inheritsFrom != null ? (" > " + srd.getInheritsFrom()) : ""));
            List<PropertyDescription> propertyDescriptions = srd
                    .getPropertyDescriptions();
            for (PropertyDescription pd : propertyDescriptions) {
                System.out.println("\t" + pd.getName() + ": " + pd.getType() + " "
                        + pd.isRequired() + " " + pd.isNoExtract());
            }
            DisplayTemplate displayTemplate = srd.getDisplayTemplate();
            if (displayTemplate != null) {
                System.out.println("\t" + displayTemplate.getTemplate());
            }
        }
    }

}
