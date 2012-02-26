package org.vortikal.resourcemanagement.edit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.velocity.runtime.resource.ResourceManager;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;

public class GetSharedTextDropdown implements ReferenceDataProvider {

    private ResourceTypeTree resourceTypeTree;

    public Map<String, JSONObject> getSharedTextValues(String docType, String propName) {
        Path p = Path.fromString("/vrtx/fellestekst/" + docType + "/" + propName + ".html");
        try {
            List json = (List) getJson(p).getJSONObject("properties").get("shared-text-box");

            Map<String, JSONObject> m = new HashMap<String, JSONObject>();
            for (Object x : json) {
                JSONObject j = JSONObject.fromObject(x);
                m.put(j.getString("id"), j);
            }

            return m;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getJson(Path uri) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        InputStream is = repository.getInputStream(token, uri, false);
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);

        String line;
        String result = "";
        while ((line = br.readLine()) != null) {
            result += line;
        }
        is.close();

        return JSONObject.fromObject(result);
    }

    @Override
    public void referenceData(Map model, HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path currentResource = requestContext.getResourceURI();
        Resource r = repository.retrieve(token, currentResource, false);
        ResourceTypeDefinition rtd = resourceTypeTree.getResourceTypeDefinitionByName(r.getResourceType());
        PropertyTypeDefinition[] ptdl = rtd.getPropertyTypeDefinitions();
        int x = ptdl.length;
        if (rtd.getPropertyTypeDefinitions() != null) {
            Map<String, Map<String, JSONObject>> m = new HashMap<String, Map<String, JSONObject>>();
            for (int i = 0; i < ptdl.length; i++) {
                if (ptdl[i] != null) {
                    Map editHints = (Map) ptdl[i].getMetadata().get("editingHints");
                    if (editHints != null && "vrtx-shared-text".equals(editHints.get("class"))) {
                        m.put(ptdl[i].getName(), getSharedTextValues(r.getResourceType(), ptdl[i].getName()));
                    }
                }
            }

            model.put("sharedTextProps",m);
        }
        
    }

    public ResourceTypeTree getResourceTypeTree() {
        return resourceTypeTree;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }
}
