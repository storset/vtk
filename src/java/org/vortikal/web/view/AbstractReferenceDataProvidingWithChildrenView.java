package org.vortikal.web.view;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;
import org.vortikal.web.referencedataprovider.Provider;

public abstract class AbstractReferenceDataProvidingWithChildrenView extends
        AbstractReferenceDataProvidingView {

    /**
     * Gets the set of reference data providers. The list returned is the union
     * of the providers set on this view and all providers for the list of
     * views.
     */
    public Provider[] getReferenceDataProviders() {
        Set providers = new HashSet();
        if (super.getReferenceDataProviders() != null) {
            providers.addAll(Arrays.asList(super.getReferenceDataProviders()));

        }

        View[] viewList = getViews();

        for (int i = 0; i < viewList.length; i++) {
            if (viewList[i] instanceof ReferenceDataProviding) {
                Provider[] providerList = ((ReferenceDataProviding) viewList[i])
                        .getReferenceDataProviders();
                if (providerList != null && providerList.length > 0) {
                    providers.addAll(Arrays.asList(providerList));
                }
            }
        }
        return (Provider[]) providers.toArray(new Provider[0]);
    }

    protected abstract View[] getViews();

}
