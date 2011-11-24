package org.vortikal.repository.search.context;

import java.util.Locale;

import org.vortikal.web.service.URL;

public interface NearestContextResolver {
    public URL getClosestContext(URL url, Locale locale);
}
