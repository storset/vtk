package org.vortikal.resourcemanagement.studies;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.web.referencedata.ReferenceDataProvider;

/**
 * Provides shared text values in model under key 'sharedTextProps' (a map).
 * 
 * Used by editor code.
 * 
 */
public class SharedTextProvider implements ReferenceDataProvider {

    private SharedTextResolver sharedTextResolver;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void referenceData(Map model, HttpServletRequest request) throws Exception {
        Map<String, Map<String, JSONObject>> sharedTextPropsMap = sharedTextResolver.resolveSharedTexts(request);
        model.put("sharedTextProps", sharedTextPropsMap);
    }

    @Required
    public void setSharedTextResolver(SharedTextResolver sharedTextResolver) {
        this.sharedTextResolver = sharedTextResolver;
    }
}
