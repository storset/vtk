/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.beans.vhost;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

public class ResourceContextTestIntegration extends AbstractBeanContextTestIntegration {

    public void testResources() throws Exception {
        List<String> beanDefs = new ArrayList<String>();

        // Resource beans
        beanDefs.add("collectionResourceTypeDefinition");
        beanDefs.add("articleListingResourceTypeDefinition");
        beanDefs.add("eventListingResourceTypeDefinition");
        beanDefs.add("imageResourceTypeDefinition");
        beanDefs.add("audioResourceTypeDefinition");
        beanDefs.add("videoResourceTypeDefinition");
        beanDefs.add("pdfResourceTypeDefinition");
        beanDefs.add("textResourceTypeDefinition");
        beanDefs.add("aptResource");
        beanDefs.add("phpResourceTypeDefinition");
        beanDefs.add("htmlResourceTypeDefinition");
        beanDefs.add("xhtml10TransResourceTypeDefinition");
        beanDefs.add("documentResourceTypeDefinition");
        beanDefs.add("eventResourceTypeDefinition");
        beanDefs.add("xmlResource");
        beanDefs.add("managedXmlResource");

        // Legacy beans
        beanDefs.add("artikkelXmlResource");
        beanDefs.add("treatyXmlResource");
        beanDefs.add("emneXmlResource");
        beanDefs.add("arrangementXmlResource");

        validateBeanConfig("backend/resource/resources.xml", beanDefs);
    }


    public void testResourceTypeConfig() {
        validateSingleConfig("file.xml");

        validateSingleConfig("collection.xml");
        validateSingleConfig("article-listing.xml");
        validateSingleConfig("event-listing.xml");

        validateSingleConfig("image.xml");
        validateSingleConfig("audio.xml");
        validateSingleConfig("video.xml");
        validateSingleConfig("pdf.xml");
        validateSingleConfig("text.xml");
        validateSingleConfig("apt.xml");
        validateSingleConfig("php.xml");
        validateSingleConfig("html.xml");
        validateSingleConfig("xhtml10trans.xml");
        validateSingleConfig("article.xml");
        validateSingleConfig("event.xml");
        validateSingleConfig("xml.xml");
        validateSingleConfig("managed-xml.xml");
    }


    private void validateSingleConfig(String resourceType) {
        validateBeanConfig("backend/resource/resource-types/" + resourceType,
                new ArrayList<String>());
    }


    private void validateBeanConfig(String configFile, List<String> beanDefs) {

        /*
         * Must add repository to context because resources require
         * authorizationManager and binaryDao :(
         */
        List<String> configLocations = new ArrayList<String>();
        configLocations.add(configFile);
        configLocations.add("backend/repository/repository.xml");

        ApplicationContext ctx = getApplicationContext(false, configLocations
                .toArray(new String[configLocations.size()]));
        for (String beanDef : beanDefs) {
            assertTrue("Expected bean not found in context: " + beanDef, ctx.containsBean(beanDef));
        }
    }

}
