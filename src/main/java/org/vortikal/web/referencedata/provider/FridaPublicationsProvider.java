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
package org.vortikal.web.referencedata.provider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;

public class FridaPublicationsProvider implements ReferenceDataProvider {

    private Repository repository = null;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    @SuppressWarnings("unchecked")
    public void referenceData(Map model, HttpServletRequest request) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        Resource resource = this.repository
                .retrieve(securityContext.getToken(), requestContext.getResourceURI(), false);

        Property prop = (Property) resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE,
                "externalScientificInformation");

        Property prop2 = (Property) resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE,
                "selectedPublications");

        if (prop != null) {
            ContentStream cs = prop.getBinaryStream();
            InputStream is = cs.getStream();

            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            isr.close();
            is.close();

            JSONArray publications = JSONArray.fromObject(sb.toString());

            Iterator it = publications.listIterator();

            // Differantiate
            JSONArray pBooks = new JSONArray();
            JSONArray pSciArtBookChapters = new JSONArray();
            JSONArray pOther = new JSONArray();

            while (it.hasNext()) {
                JSONObject publication = (JSONObject) it.next();
                String mainCategory = (String) publication.get("mainCategoryCode");
                String subCategory = (String) publication.get("subCategoryNO");

                if (mainCategory.equals("BOK")) {
                    pBooks.add(publication);
                } else if (subCategory.equals("Vitenskapelig artikkel")
                        || subCategory.equals("Populærvitenskapelig artikkel") || mainCategory.equals("BOKRAPPORTDEL")) {
                    pSciArtBookChapters.add(publication);
                } else {
                    pOther.add(publication);
                }
            }

            model.put("pBooks", pBooks);
            model.put("pSciArtBookChapters", pSciArtBookChapters);
            model.put("pOther", pOther);

        }

        if (prop2 != null) {
            model.put("selectedPublications", "here comes the selectedPublications");
        } else {
            model.put("selectedPublications", null);
        }

    }
}