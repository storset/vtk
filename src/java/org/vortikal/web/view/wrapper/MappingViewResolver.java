package org.vortikal.web.view.wrapper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/** 
 * Simple viewresolver
 * FIXME: remove this
 *
 */
public class MappingViewResolver extends AbstractWrappingViewResolver implements ViewResolver {

    private Map views = new HashMap();
    
    protected View resolveViewNameInternal(String viewName, Locale locale) {

        View view = (View) views.get(viewName);
        return view;
    }
    
    /**
     * @param views The views to set.
     */
    public void setViews(Map views) {
        this.views = views;
    }

}
