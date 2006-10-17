/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private static final long serialVersionUID = 8227690761189581870L;

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
        }
        return list.matches(resource);
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
        }
        return list.matches(resource);
    }
}
