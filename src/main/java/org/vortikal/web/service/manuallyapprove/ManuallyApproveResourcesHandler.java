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

    // XXX RENAME! -> May also be complete urls to other hosts
    @Deprecated
    private static final String locationS_PARAM = "locations";
    private static final String AGGREGATE_PARAM = "aggregate";

    private PropertyTypeDefinition manuallyApproveFromPropDef;
    private PropertyTypeDefinition manuallyApprovedResourcesPropDef;
    private PropertyTypeDefinition aggregationPropDef;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Repository repository = RequestContext.getRequestContext().getRepository();
        Path currentCollectionPath = RequestContext.getRequestContext().getCurrentCollection();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource currentCollection = repository.retrieve(token, currentCollectionPath, false);
        Property manuallyApproveFromProp = currentCollection.getProperty(this.manuallyApproveFromPropDef);
        Property manuallyApprovedResourcesProp = currentCollection.getProperty(this.manuallyApprovedResourcesPropDef);
        Property aggregationProp = currentCollection.getProperty(this.aggregationPropDef);
        String[] manuallyApproveFromParam = request.getParameterValues(locationS_PARAM);
        String[] aggregateParam = request.getParameterValues(AGGREGATE_PARAM);

        // Nothing to work with, need at least one of these
        if (manuallyApproveFromProp == null && manuallyApprovedResourcesProp == null
                && manuallyApproveFromParam == null) {
            return null;
        }

        Set<String> locations = new HashSet<String>();
        // Parameter "locations" overrides property, because user might change
        // content and update service before storing resource
        if (manuallyApproveFromParam != null) {
            for (String location : manuallyApproveFromParam) {
                if (this.isValid(location, currentCollectionPath, repository, token)) {
                    locations.add(location);
                }
            }
        } else if (manuallyApproveFromProp != null) {
            Value[] manuallyApproveFromValues = manuallyApproveFromProp.getValues();
            for (Value manuallyApproveFromValue : manuallyApproveFromValues) {
                String location = manuallyApproveFromValue.getStringValue();
                if (this.isValid(location, currentCollectionPath, repository, token)) {
                    locations.add(location);
                }
            }
        }

        // Make sure current collection path is not among set of locations to
        // manually approve from
        locations.remove(currentCollectionPath.toString());

        // Aggregation must also be considered. Any location to aggregate from
        // will be discarded from location set to manually approve from, because
        // aggregation overrides manual approval. Parameter "aggregate"
        // overrides property, again because user might change content and
        // update service before storing resource
        if (aggregateParam != null) {
            for (String agg : aggregateParam) {
                locations.remove(agg);
            }
        } else if (aggregationProp != null) {
            Value[] aggregationValues = aggregationProp.getValues();
            for (Value aggregationValue : aggregationValues) {
                locations.remove(aggregationValue.toString());
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
        List<ManuallyApproveResource> result = this.searcher.getManuallyApproveResources(currentCollection, locations,
                alreadyApproved);

        if (result == null || result.size() == 0) {
            return null;
        }

        boolean approvedOnly = request.getParameter("approved-only") != null;
        JSONArray arr = new JSONArray();
        for (ManuallyApproveResource m : result) {
            boolean approved = m.isApproved();
            if (approvedOnly && !approved) {
                continue;
            }
            JSONObject obj = new JSONObject();
            obj.put("title", m.getTitle());
            obj.put("uri", m.getUrl().toString());
            obj.put("source", m.getSource());
            obj.put("published", m.getPublishDateAsString());
            obj.put("approved", approved);
            arr.add(obj);
        }
        response.setContentType("text/plain;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.print(arr);
        writer.flush();
        writer.close();

        return null;
    }

    private boolean isValid(String location, Path currentCollectionPath, Repository repository, String token) {

        try {
            URL.parse(location);
            return true;
        } catch (Exception e) {
            // Not a url, but might be a local path. Ignore and continue.
        }

        try {

            // Make sure path is valid (be lenient on trailing slash)
            location = location.endsWith("/") && !location.equals("/") ? location.substring(0, location.length() - 1)
                    : location;
            Path locationPath = Path.fromString(location);

            // Do not allow manual approval from self
            if (currentCollectionPath.equals(locationPath)) {
                return false;
            }

            // Make sure resource exists
            if (!repository.exists(token, locationPath)) {
                return false;
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

}
