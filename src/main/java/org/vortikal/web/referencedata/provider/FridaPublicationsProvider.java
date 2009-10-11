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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.JSONUtil;
import org.vortikal.util.io.StreamUtil;
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

        Resource resource = this.repository.retrieve(securityContext.getToken(), requestContext.getResourceURI(), true);

        Property fridaPublicationsProp = (Property) resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE,
                "externalScientificInformation");

        if (fridaPublicationsProp != null) {
            ContentStream cs = fridaPublicationsProp.getBinaryStream();
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

            // Differantiate
            JSONArray pBooks = new JSONArray();
            JSONArray pSciArtBookChapters = new JSONArray();
            JSONArray pOther = new JSONArray();

            if (publications.size() > 2) {

                for (int i = 0; i < (publications.size() - 2); i++) {
                    JSONObject publication = (JSONObject) publications.get(i);

                    String mainCategory = (String) publication.get("mainCategoryCode");
                    String subCategory = (String) publication.get("subCategoryNO");

                    if (mainCategory.equals("BOK")) {
                        pBooks.add(publication);
                    } else if (subCategory.equals("Vitenskapelig artikkel")
                            || subCategory.equals("Populærvitenskapelig artikkel")
                            || mainCategory.equals("BOKRAPPORTDEL")) {
                        pSciArtBookChapters.add(publication);
                    } else {
                        pOther.add(publication);
                    }
                }
            }

            if (publications.size() >= 2) {

                JSONObject publication = (JSONObject) publications.get(publications.size() - 2);
                model.put("publicationsUrl", publication.get("publicationsUrl"));

                publication = (JSONObject) publications.get(publications.size() - 1);
                model.put("totalNumberOfPublications", publication.get("totalNumberOfPublications"));

            }

            model.put("pBooks", pBooks);
            model.put("pSciArtBookChapters", pSciArtBookChapters);
            model.put("pOther", pOther);

        }

        // XXX Do we have any utility stuff for doing this?
        // i.e. fetching content from the json doc?
        InputStream stream = this.repository.getInputStream(securityContext.getToken(),
                requestContext.getResourceURI(), true);
        String encoding = resource.getCharacterEncoding();
        encoding = encoding == null ? "utf-8" : encoding;
        byte[] bytes = StreamUtil.readInputStream(stream);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(new String(bytes, encoding));
        Object selectedPublications = JSONUtil.select(jsonObject, "properties.selectedPublications");
        model.put("selectedPublications", selectedPublications);

    }
}