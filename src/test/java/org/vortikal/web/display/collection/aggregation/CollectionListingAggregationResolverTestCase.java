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
package org.vortikal.web.display.collection.aggregation;

import junit.framework.TestCase;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.ValueFactoryImpl;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;

public class CollectionListingAggregationResolverTestCase extends TestCase {

    private AggregationReslover aggregationReslover;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.aggregationReslover = new CollectionListingAggregationResolver();
        ((CollectionListingAggregationResolver) this.aggregationReslover)
                .setAggregationPropDef(getAggregatedPropTypeDef());

    }

    public void testExtend() {
        runTest(new UriPrefixQuery("/someuri/", TermOperator.EQ, false), true, true);
        runTest(getAndQuery(), true, true);
        runTest(getNestedQuery(), true, true);
    }

    public void testNotExtended() {
        runTest(getAndQuery(), false, false);
        OrQuery original = new OrQuery();
        original.add(new TypeTermQuery("someterm", TermOperator.IN));
        original.add(new TypeTermQuery("someotherterm", TermOperator.IN));
        runTest(original, true, false);
    }

    private void runTest(Query original, boolean withAggregationProp, boolean expectExtended) {
        Query extended = this.aggregationReslover.extend(original, getCollection(withAggregationProp));
        assertNotNull(extended);
        if (expectExtended) {
            assertNotSame(extended.toString(), original.toString());
        } else {
            assertEquals(extended.toString(), original.toString());
        }
    }

    private Query getAndQuery() {
        AndQuery andQuery = new AndQuery();
        andQuery.add(new UriDepthQuery(2));
        andQuery.add(new UriPrefixQuery("/someuri/", TermOperator.EQ, false));
        return andQuery;
    }

    private Query getNestedQuery() {
        AndQuery query = new AndQuery();
        query.add(new UriPrefixQuery("/uri", TermOperator.EQ, false));
        query.add(new TypeTermQuery("term", TermOperator.IN));

        AndQuery query2 = new AndQuery();
        query2.add(new UriPrefixQuery("/uri2", TermOperator.EQ, false));
        query2.add(new TypeTermQuery("term2", TermOperator.IN));

        AndQuery andQuery = new AndQuery();
        andQuery.add(query);
        andQuery.add(query2);

        OrQuery nestedQuery = new OrQuery();
        nestedQuery.add(andQuery);
        nestedQuery.add(new UriDepthQuery(2));
        return nestedQuery;
    }

    private Resource getCollection(boolean withAggregationProp) {
        ResourceImpl collection = new ResourceImpl();

        if (withAggregationProp) {
            String[] values = { "/barfolder/", "/foo/bar/" };
            Property aggregatedProp = getAggregatedPropTypeDef().createProperty(values);
            collection.addProperty(aggregatedProp);
        }

        return collection;
    }

    private PropertyTypeDefinition getAggregatedPropTypeDef() {
        PropertyTypeDefinitionImpl aggregatedFoldersPropDef = new PropertyTypeDefinitionImpl();
        aggregatedFoldersPropDef.setNamespace(Namespace.DEFAULT_NAMESPACE);
        aggregatedFoldersPropDef.setName("aggregation");
        aggregatedFoldersPropDef.setType(Type.STRING);
        aggregatedFoldersPropDef.setMultiple(true);
        aggregatedFoldersPropDef.setValueFactory(new ValueFactoryImpl());
        return aggregatedFoldersPropDef;
    }

}
