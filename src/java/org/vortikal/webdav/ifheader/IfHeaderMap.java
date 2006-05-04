package org.vortikal.webdav.ifheader;

import java.util.HashMap;

import org.vortikal.repository.Resource;

/**
 * The <code>IfHeaderMap</code> clss implements the {@link StateEntryList}
 * interface to support tagged lists of {@link IfList}s. This class
 * implements the data container for the production :
 * <pre>
     Tagged = { "<" Word ">" "(" IfList ")" } .
 * </pre>
 */
class IfHeaderMap extends HashMap implements StateEntryList {

    /**
     * Matches the token and etag for the given resource. If the resource is
     * not mentioned in the header, a match is assumed and <code>true</code>
     * is returned in this case.
     *
     * @param resource The absolute URI of the resource for which to find
     *      a match.
     * @param token The token to compare.
     * @param etag The etag to compare.
     *
     * @return <code>true</code> if either no entry exists for the resource
     *      or if the entry for the resource matches the token and etag.
     */
    public boolean matches(Resource resource) {
        String token = null;
        if (resource.getLock() != null) {
            token = resource.getLock().getLockToken();
        }
        String etag = resource.getEtag();
        IfHeaderImpl.logger.debug("matches: Trying to match resource="+resource+", token="+token+","+etag);

        StateEntryListImpl list = (StateEntryListImpl) get(resource);
        if (list == null) {
            IfHeaderImpl.logger.debug("matches: No entry for tag "+resource+", assuming match");
            return true;
        } else {
            return list.matches(resource);
        }
    }

    public boolean matchesEtags(Resource resource) {
        String token = null;
        if (resource.getLock() != null) {
            token = resource.getLock().getLockToken();
        }
        String etag = resource.getEtag();
        IfHeaderImpl.logger.debug("matchesEtags: Trying to match resource="+resource+", token="+token+","+etag);

        StateEntryListImpl list = (StateEntryListImpl) get(resource);
        if (list == null) {
            IfHeaderImpl.logger.debug("matchesEtags: No entry for tag "+resource+", assuming match");
            return true;
        } else {
            return list.matches(resource);
        }
    }
}