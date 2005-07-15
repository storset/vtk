package org.vortikal.web.view;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

public abstract class AbstractWrappingViewResolver implements ViewResolver {

    private WrappingView wrappingView;
    
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        View view = resolveViewNameInternal(viewName, locale);
        
        if (view != null && wrappingView != null) {
            return new ProxyWrapper(view, wrappingView);
        }
        return view;
    }

    public abstract View resolveViewNameInternal(String viewName, Locale locale) throws Exception;

    /**
     * @param wrappingView The wrappingView to set.
     */
    public void setWrappingView(WrappingView wrappingView) {
        this.wrappingView = wrappingView;
    }

    private class ProxyWrapper implements View {
        
        private View view;
        private WrappingView wrappingView;
        
        public ProxyWrapper(View view, WrappingView wrappingView) {
            this.view = view;
            this.wrappingView = wrappingView;
        }

        public void render(Map model, HttpServletRequest request, HttpServletResponse respone) throws Exception {
            wrappingView.renderView(view, model, request, respone);
        }
        
    }

}
