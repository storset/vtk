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

import java.util.ArrayList;

import org.vortikal.repository.Resource;

/**
 * The <code>IfHeaderList</code> clss implements the {@link StateEntryList}
 * interface to support untagged lists of {@link IfList}s. This class
 * implements the data container for the production :
 * <pre>
     Untagged = { "(" IfList ")" } .
 * </pre>
 */
class StateEntryListImpl extends ArrayList implements StateEntryList {

    /**
     * Matches a list of {@link IfList}s against the token and etag. If any of
     * the {@link IfList}s matches, the method returns <code>true</code>.
     * On the other hand <code>false</code> is only returned if non of the
     * {@link IfList}s match.
     *
     * @param resource The resource to match, which is ignored by this
     *      implementation. A value of <code>null</code> is therefor
     *      acceptable.
     * @param token The token to compare.
     * @param etag The ETag value to compare.
     *
     * @return <code>True</code> if any of the {@link IfList}s matches the token
     *      and etag, else <code>false</code> is returned.
     */
    public boolean matches(Resource resource) {
        String token = null;
        if (resource.getLock() != null) {
            token = resource.getLock().getLockToken();
        }
        String etag = resource.getEtag();
        IfHeaderImpl.logger.debug("matches: Trying to match token="+token+", etag="+etag);

        for (int i=0; i < size(); i++) {
            IfList il = (IfList) get(i);
            if (il.match(token, etag)) {
                IfHeaderImpl.logger.debug("matches: Found match with "+il);
                return true;
            }
        }
        // invariant: no match found

        return false;
    }

    public boolean matchesEtags(Resource resource) {
        String token = null;
        if (resource.getLock() != null) {
            token = resource.getLock().getLockToken();
        }
        String etag = resource.getEtag();
        IfHeaderImpl.logger.debug("matchesEtags: Trying to match token="+token+", etag="+etag);

        for (int i=0; i < size(); i++) {
            IfList il = (IfList) get(i);
            if (il.matchEtags(etag)) {
                IfHeaderImpl.logger.debug("matchesEtags: Found match with "+il);
                return true;
            }
        }
        // invariant: no match found

        return false;
    }
}
