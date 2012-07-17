/* Copyright (c) 2009, University of Oslo, Norway
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.vortikal.repository.store;

import java.util.Locale;
import java.util.Set;

import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;

/**
 * Generic principal search.
 */
public class PrincipalSearchImpl implements PrincipalSearch {

    protected String searchString;
    protected Principal.Type principalType;
    private SearchType searchType;
    private Locale preferredLocale;
    private Set<String> uids;

    public PrincipalSearchImpl(String searchString) {
        this(Principal.Type.USER, searchString, null, null);
    }

    public PrincipalSearchImpl(Principal.Type type, String searchString) {
        this(type, searchString, null, null);
    }

    public PrincipalSearchImpl(Principal.Type type, String searchString, Locale preferredLocale) {
        this(type, searchString, preferredLocale, null);
    }

    public PrincipalSearchImpl(Principal.Type type, String searchString, Locale preferredLocale, SearchType searchType) {
        this.principalType = type;
        this.searchString = searchString;
        this.preferredLocale = preferredLocale;
        this.searchType = searchType;
    }

    public PrincipalSearchImpl(Set<String> uids, Locale preferredLocale) {
        this.principalType = Principal.Type.USER;
        this.searchType = SearchType.MULTIPLE_IDS_SEARCH;
        this.preferredLocale = preferredLocale;
        this.uids = uids;
    }

    @Override
    public Type getPrincipalType() {
        return this.principalType;
    }

    @Override
    public SearchType getSearchType() {
        return this.searchType;
    }

    @Override
    public String getSearchString() {
        return this.searchString;
    }

    @Override
    public Locale getPreferredLocale() {
        return this.preferredLocale;
    }

    public Set<String> getUids() {
        return uids;
    }

    @Override
    public int hashCode() {
        int code = 7;

        code = 31 * code + this.principalType.hashCode();

        if (this.searchString != null) {
            code = 31 * code + this.searchString.hashCode();
        }

        if (this.preferredLocale != null) {
            code = 31 * code + this.preferredLocale.hashCode();
        }

        return code;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || (this.getClass() != other.getClass())) {
            return false;
        }

        PrincipalSearchImpl po = (PrincipalSearchImpl) other;

        if (this.principalType != po.principalType)
            return false;

        if (this.preferredLocale != null) {
            if (!this.preferredLocale.equals(po.preferredLocale))
                return false;
        } else {
            if (po.preferredLocale != null)
                return false;
        }

        if (this.searchString == null) {
            return (po.searchString == null);
        } else {
            return this.searchString.equals(po.searchString);
        }
    }

}
