/* Copyright (c) 2012, University of Oslo, Norway
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

package org.vortikal.repository.systemjob;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyExistsQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;

@Deprecated
public class ResourcePublishingPathSelector extends IndexQueryPathSelector {

    private PropertyTypeDefinition publishedPropDef;
    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition unpublishDatePropDef;

    @Override
    protected Query getQuery(SystemChangeContext context) {
        OrQuery or = new OrQuery();

        long now = Calendar.getInstance().getTimeInMillis();

        AndQuery notPublished = new AndQuery();
        notPublished.add(new PropertyTermQuery(this.publishedPropDef, "false", TermOperator.EQ));
        notPublished.add(new PropertyTermQuery(this.publishDatePropDef, String.valueOf(now), TermOperator.LT));
        AndQuery subAnd = new AndQuery();
        OrQuery subOr = new OrQuery();
        subOr.add(new PropertyExistsQuery(this.unpublishDatePropDef, true));
        subOr.add(new PropertyTermQuery(this.unpublishDatePropDef, String.valueOf(now), TermOperator.GT));
        subAnd.add(subOr);
        notPublished.add(subAnd);

        AndQuery alreadyPublished = new AndQuery();
        alreadyPublished.add(new PropertyTermQuery(this.publishedPropDef, "true", TermOperator.EQ));
        alreadyPublished.add(new PropertyTermQuery(this.unpublishDatePropDef, String.valueOf(now), TermOperator.LT));

        or.add(notPublished);
        or.add(alreadyPublished);

        AndQuery and = new AndQuery();
        and.add(or);
        and.add(new TypeTermQuery("json-resource", TermOperator.IN));

        return and;
    }

    @Override
    public boolean isOnlyPublishedResources() {
        return false;
    }
    
    @Required
    public void setPublishedPropDef(PropertyTypeDefinition publishedPropDef) {
        this.publishedPropDef = publishedPropDef;
    }

    @Required
    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    @Required
    public void setUnpublishDatePropDef(PropertyTypeDefinition unpublishDatePropDef) {
        this.unpublishDatePropDef = unpublishDatePropDef;
    }
    
}
