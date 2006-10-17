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


/**
 * The <code>IfList</code> class extends the <code>ArrayList</code> class
 * with the limitation to only support adding {@link IfListEntry} objects
 * and adding a {@link #match} method.
 * <p>
 * This class is a container for data contained in the <em>If</em>
 * production <em>IfList</em>
 * <pre>
     IfList = { [ "Not" ] ( ("<" Word ">" ) | ( "[" Word "]" ) ) } .
 * </pre>
 * <p>
 */
class IfList extends ArrayList {

    private static final long serialVersionUID = -2368752136718198410L;

    /**
     * Throws an <code>IllegalStateException</code> because only
     * {@link IfListEntry} objects are supported in this list.
     *
     * @param o The <code>Object</code> to add.
     * @return <code>true</code> if successfull
     *
     * @throws IllegalStateException because only {@link IfListEntry}
     *      objects are supported in this list.
     */
    public boolean add(Object o) {
        throw new IllegalArgumentException("Only IfListEntry instances allowed");
    }

    /**
     * Throws an <code>IllegalStateException</code> because only
     * {@link IfListEntry} objects are supported in this list.
     *
     * @param index The position at which to add the object.
     * @param element The <code>Object</code> to add.
     *
     * @throws IllegalStateException because only {@link IfListEntry}
     *      objects are supported in this list.
     */
    public void add(int index, Object element) {
        throw new IllegalArgumentException("Only IfListEntry instances allowed");
    }

    /**
     * Adds the {@link IfListEntry} at the end of the list.
     *
     * @param entry The {@link IfListEntry} to add to the list
     *
     * @return <code>true</code> (as per the general contract of
     *      Collection.add).
     */
    public boolean add(IfListEntry entry) {
        return super.add(entry);
    }

    /**
     * Adds the {@link IfListEntry} at the indicated position of the list.
     *
     * @param index
     * @param entry
     *
     * @throws IndexOutOfBoundsException if index is out of range
     *      <code>(index &lt; 0 || index &gt; size())</code>.
     */
    public void add(int index, IfListEntry entry) {
        super.add(index, entry);
    }

    /**
     * Returns <code>true</code> if all {@link IfListEntry} objects in the
     * list match the given token and etag. If the list is entry, it is
     * considered to match the token and etag.
     *
     * @param token The token to compare.
     * @param etag The etag to compare.
     *
     * @return <code>true</code> if all entries in the list matche the
     *      given tag and token.
     */
    public boolean match(String token, String etag) {
        IfHeaderImpl.logger.debug("match: Trying to match token="+token+", etag="+etag);
        for (int i=0; i < size(); i++) {
            IfListEntry ile = (IfListEntry) get(i);
            if (!ile.match(token, etag)) {
                IfHeaderImpl.logger.debug("match: Entry "+String.valueOf(i)+"-"+ile+" does not match");
                return false;
            }
        }
        // invariant: all entries matched

        return true;
    }
    
    public boolean matchEtags(String etag) {
        IfHeaderImpl.logger.debug("matchEtag: Trying to match etag="+etag);
        for (int i=0; i < size(); i++) {
            IfListEntry ile = (IfListEntry) get(i);
            if (ile.isEtag() && !ile.match(etag)) {
                IfHeaderImpl.logger.debug("matchEtag: Entry "+String.valueOf(i)+"-"+ile+" does not match");
                return false;
            }
        }
        // invariant: all entries matched

        return true;
    }
    
}
