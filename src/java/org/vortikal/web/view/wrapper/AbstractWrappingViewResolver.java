package org.vortikal.web.view.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;

public abstract class AbstractWrappingViewResolver implements ViewResolver {

    private Log logger = LogFactory.getLog(this.getClass());
    
    private ViewWrapper wrappingView;
    private ReferenceDataProvider[] providers;

    public View resolveViewName(String viewName, Locale locale) throws Exception {
        View view = resolveViewNameInternal(viewName, locale);
        
        if (view != null && wrappingView != null) {
            return new ProxyView(view, this.providers, wrappingView);
        }
        return view;
    }

    /**
     * Actually resolves the view. Must be implemented by subclasses.
     * 
     * @param viewName
     *            a the name of the view.
     * @return the resolved view.
     */
    protected abstract View resolveViewNameInternal(String viewName, Locale locale);


    /**
     * @param providers
     *            The providers to set.
     */
    public void setProviders(ReferenceDataProvider[] providers) {
        this.providers = providers;
    }

    
    /**
     * @param wrappingView The wrappingView to set.
     */
    public void setWrappingView(ViewWrapper wrappingView) {
        this.wrappingView = wrappingView;
    }


    /**
     * Wrapper class for the resolved view, running the <code>providers</code>
     * before the wrapped view is run (and the necessary model is available)
     */
    private class ProxyView implements View {

        private ReferenceDataProvider[] providers;
        private View view;
        private ViewWrapper viewWrapper;
        
        /**
         * @param view -
         *            the view to eventually run
         * @param providers -
         *            the set of reference data providers for this view
         */
        public ProxyView(View view, ReferenceDataProvider[] resolverProviders, ViewWrapper viewWrapper) {

            if (view == null)
                throw new IllegalArgumentException(
                        "The wrapped view cannot be null");

            this.view = view;
            this.viewWrapper = viewWrapper;

            List providerList = new ArrayList();

            if (resolverProviders != null) {
                providerList.addAll(Arrays.asList(resolverProviders));
            }

            if (viewWrapper != null && (viewWrapper instanceof ReferenceDataProviding)) {
                ReferenceDataProvider[] wrapperProviders = null;

                wrapperProviders = ((ReferenceDataProviding) viewWrapper)
                        .getReferenceDataProviders();
                if (wrapperProviders != null) {
                    providerList.addAll(Arrays.asList(wrapperProviders));
                }
            }
            
            if (view instanceof ReferenceDataProviding) {
                ReferenceDataProvider[] viewProviders = null;

                viewProviders = ((ReferenceDataProviding) view)
                        .getReferenceDataProviders();
                if (viewProviders != null) {
                    providerList.addAll(Arrays.asList(viewProviders));
                }
            }

            if (providerList.size() > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found reference data providers for view "
                            + view + ": " + providerList);
                }

                providers = (ReferenceDataProvider[])providerList.toArray(new ReferenceDataProvider[providerList.size()]);
            }        
        }

        public void render(Map model, HttpServletRequest request,
                HttpServletResponse response) throws Exception {

            if (this.providers != null && this.providers.length > 0) {
                if (model == null)
                    model = new HashMap();

                for (int i = 0; i < this.providers.length; i++) {
                    ReferenceDataProvider provider = this.providers[i];
                    if (logger.isDebugEnabled())
                        logger.debug("Invoking reference data provider '"
                                + provider + "'");
                    provider.referenceData(model, request);
                }
            }
            
            if (viewWrapper != null)
                viewWrapper.renderView(view, model, request, response);
            else
                view.render(model, request, response);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getClass().toString()).append(":");
            sb.append("view = ").append(this.view.toString());
            sb.append("viewWrapper = ").append(this.viewWrapper.toString());
            return sb.toString();
        }

    }
}
