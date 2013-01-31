/* Copyright (c) 2013, University of Oslo, Norway
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
package org.vortikal.web.search;

import java.util.List;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyPrefixQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.web.service.URL;

public class VHostScopeQueryRestricter {

    public static final PropertyTypeDefinition vHostPropDef;

    static {
        PropertyTypeDefinitionImpl tmp = new PropertyTypeDefinitionImpl();
        tmp.setNamespace(Namespace.DEFAULT_NAMESPACE);
        tmp.setName("vhost");
        tmp.setType(PropertyType.Type.STRING);
        vHostPropDef = tmp;
    }

    /**
     * 
     * Restrict a query to a vhost, given by param "vhost"
     */
    public static Query vhostRestrictedQuery(Query original, URL vhost) {
        return vhostRestrictedQuery(original, vhost.getHost());
    }

    public static Query vhostRestrictedQuery(Query original, String vhost) {
        Query vhostQuery = new PropertyPrefixQuery(vHostPropDef, vhost, TermOperator.EQ);

        if (original instanceof AndQuery) {
            AndQuery and = (AndQuery) original;
            and.add(vhostQuery);
            return and;
        }

        AndQuery and = new AndQuery();
        and.add(original);
        and.add(vhostQuery);
        return and;
    }

    /**
     * 
     * Restrict a query to a given list of vhosts
     */
    public static Query vhostRestrictedQuery(Query original, List<String> vhosts) {

        if (vhosts.size() == 1) {
            return VHostScopeQueryRestricter.vhostRestrictedQuery(original, vhosts.get(0));
        } else {
            OrQuery vHostOr = new OrQuery();
            for (String vhost : vhosts) {
                vHostOr.add(new PropertyPrefixQuery(VHostScopeQueryRestricter.vHostPropDef, vhost, TermOperator.EQ));
            }
            AndQuery and = new AndQuery();
            and.add(original);
            and.add(vHostOr);
            return and;
        }

    }

}
