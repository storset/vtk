/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.web.service.manuallyapprove;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;

public class ManuallyApproveResourcesHandler implements Controller {

    private ManuallyApproveResourcesSearcher searcher;

    private static final String FOLDERS_PARAM = "folders";
    private static final String AGGREGATE_PARAM = "aggregate";

    private PropertyTypeDefinition manuallyApproveFromPropDef;
    private PropertyTypeDefinition manuallyApprovedResourcesPropDef;
    private PropertyTypeDefinition aggregationPropDef;
    private PropertyTypeDefinition recursivePropDef;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Repository repository = RequestContext.getRequestContext().getRepository();
        Path currentCollectionPath = RequestContext.getRequestContext().getCurrentCollection();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource currentCollection = repository.retrieve(token, currentCollectionPath, false);
        Property manuallyApproveFromProp = currentCollection.getProperty(this.manuallyApproveFromPropDef);
        Property manuallyApprovedResourcesProp = currentCollection.getProperty(this.manuallyApprovedResourcesPropDef);
        Property aggregationProp = currentCollection.getProperty(this.aggregationPropDef);
        Property recursiveProp = currentCollection.getProperty(this.recursivePropDef);
        String[] foldersParam = request.getParameterValues(FOLDERS_PARAM);
        String[] aggregateParam = request.getParameterValues(AGGREGATE_PARAM);

        // Nothing to work with, need at least one of these
        if (manuallyApproveFromProp == null && manuallyApprovedResourcesProp == null && foldersParam == null) {
            return null;
        }

        Set<String> folders = new HashSet<String>();
        // Parameter "folders" overrides property, because user might change
        // content and update service before storing resource
        if (foldersParam != null) {
            for (String folder : foldersParam) {
                if (this.isValid(folder, currentCollectionPath, recursiveProp)) {
                    folders.add(folder);
                }
            }
        } else if (manuallyApproveFromProp != null) {
            Value[] manuallyApproveFromValues = manuallyApproveFromProp.getValues();
            for (Value manuallyApproveFromValue : manuallyApproveFromValues) {
                String folder = manuallyApproveFromValue.getStringValue();
                if (this.isValid(folder, currentCollectionPath, recursiveProp)) {
                    folders.add(folder);
                }
            }
        }

        // Make sure current collection path is not among set of folders to
        // manually approve from
        folders.remove(currentCollectionPath.toString());

        // Aggregation must also be considered. Any folder to aggregate from
        // will be discarded from folder set to manually approve from, because
        // aggregation overrides manual approval. Parameter "aggregate"
        // overrides property, again because user might change content and
        // update service before storing resource
        if (aggregateParam != null) {
            for (String agg : aggregateParam) {
                folders.remove(agg);
            }
        } else if (aggregationProp != null) {
            Value[] aggregationValues = aggregationProp.getValues();
            for (Value aggregationValue : aggregationValues) {
                folders.remove(aggregationValue.toString());
            }
        }

        Set<String> alreadyApproved = new HashSet<String>();
        if (manuallyApprovedResourcesProp != null) {
            Value[] manuallyApprovedResourcesValues = manuallyApprovedResourcesProp.getValues();
            for (Value manuallyApprovedResourcesValue : manuallyApprovedResourcesValues) {
                alreadyApproved.add(manuallyApprovedResourcesValue.toString());
            }
        }

        // That's it... we now have a set of resources to manually approve from,
        // along with a set of resources that are already manually approved...
        // Let's search!
        List<ManuallyApproveResource> result = this.searcher.getManuallyApproveResources(currentCollection, folders,
                alreadyApproved);

        if (result == null || result.size() == 0) {
            return null;
        }

        JSONArray arr = new JSONArray();
        for (ManuallyApproveResource m : result) {
            JSONObject obj = new JSONObject();
            obj.put("title", m.getTitle());
            obj.put("uri", m.getUri());
            obj.put("source", m.getSource());
            obj.put("published", m.getPublishDateAsString());
            obj.put("approved", m.isApproved());
            arr.add(obj);
        }
        response.setContentType("text/plain;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.print(arr);
        writer.flush();
        writer.close();

        return null;
    }

    private boolean isValid(String folder, Path currentCollectionPath, Property recursiveProp) {

        try {
            URL.parse(folder);
            return true;
        } catch (Exception e) {
            // Not a url, but might be a local path. Ignore and continue.
        }

        try {

            // Make sure path is valid (be lenient on trailing slash)
            folder = folder.endsWith("/") && !folder.equals("/") ? folder.substring(0, folder.length() - 1) : folder;
            Path folderPath = Path.fromString(folder);

            // Also remove from folders set any values which are children of
            // current collection
            if (recursiveProp != null && recursiveProp.getBooleanValue()) {
                if (currentCollectionPath.isAncestorOf(folderPath)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Required
    public void setSearcher(ManuallyApproveResourcesSearcher searcher) {
        this.searcher = searcher;
    }

    @Required
    public void setManuallyApproveFromPropDef(PropertyTypeDefinition manuallyApproveFromPropDef) {
        this.manuallyApproveFromPropDef = manuallyApproveFromPropDef;
    }

    @Required
    public void setManuallyApprovedResourcesPropDef(PropertyTypeDefinition manuallyApprovedResourcesPropDef) {
        this.manuallyApprovedResourcesPropDef = manuallyApprovedResourcesPropDef;
    }

    @Required
    public void setAggregationPropDef(PropertyTypeDefinition aggregationPropDef) {
        this.aggregationPropDef = aggregationPropDef;
    }

    @Required
    public void setRecursivePropDef(PropertyTypeDefinition recursivePropDef) {
        this.recursivePropDef = recursivePropDef;
    }

}
