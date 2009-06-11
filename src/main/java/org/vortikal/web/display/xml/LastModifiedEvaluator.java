package org.vortikal.web.display.xml;

import org.vortikal.repository.Resource;

public interface LastModifiedEvaluator {

    /**
     * 
     * @param resource
     *            The resource we want to test if we should report last-modified for
     * @return true if we should report last-modified, else false
     */
    public boolean reportLastModified(Resource resource);

}
