/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.search.articlelisting;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriSetQuery;
import org.vortikal.web.search.QueryBuilder;

public class FeaturedArticlesQueryBuilder implements QueryBuilder {

    private PropertyTypeDefinition featuredArticlesPropDef;
    private boolean invert;

    @Override
    public Query build(Resource collection, HttpServletRequest request) {

        Property featuredArtilesProp = collection.getProperty(this.featuredArticlesPropDef);
        if (featuredArtilesProp == null) {
            return null;
        }

        Set<String> set = new HashSet<String>();
        for (Value value : featuredArtilesProp.getValues()) {
            try {
                String stringValue = value.getStringValue();
                Path.fromString(stringValue);
                set.add(stringValue);
            } catch (IllegalArgumentException iae) {
                // Just continue...
            }
        }
        if (set.size() > 0) {
            if (!invert) {
                return new UriSetQuery(set, TermOperator.NI);
            } else {
                return new UriSetQuery(set);
            }
        }
        return null;
    }

    @Required
    public void setFeaturedArticlesPropDef(PropertyTypeDefinition featuredArticlesPropDef) {
        this.featuredArticlesPropDef = featuredArticlesPropDef;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

}
