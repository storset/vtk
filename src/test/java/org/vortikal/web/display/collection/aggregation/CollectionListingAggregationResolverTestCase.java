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

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
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
import org.vortikal.web.AbstractControllerTest;

public class CollectionListingAggregationResolverTestCase extends AbstractControllerTest {

    private CollectionListingAggregationResolver aggregationReslover;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.aggregationReslover = new CollectionListingAggregationResolver();
        this.aggregationReslover.setAggregationPropDef(getAggregatedPropTypeDef());
        this.aggregationReslover.setRecursiveAggregationPropDef(getRecursiveAggregatedPropDef());
        this.aggregationReslover.setRepository(mockRepository);
    }

    public void testRecursiveAggregation() throws Exception {

        // XXX god damn it, all the mocking to do a simple test...

        // runTest(getAndQuery(), true, true, true);
    }

    public void testExtend() {
        runTest(new UriPrefixQuery("/someuri/", TermOperator.EQ, false), true, false, true);
        runTest(getAndQuery(), true, false, true);
        runTest(getNestedQuery(), true, false, true);
    }

    public void testNullTermOperator() {
        AndQuery query = new AndQuery();
        query.add(new UriPrefixQuery("/someuri", null, false));
        runTest(query, true, false, true);
    }

    public void testNotExtended() {
        runTest(getAndQuery(), false, false, false);
        OrQuery original = new OrQuery();
        original.add(new TypeTermQuery("someterm", TermOperator.IN));
        original.add(new TypeTermQuery("someotherterm", TermOperator.IN));
        runTest(original, true, false, false);
    }

    private void runTest(Query original, boolean withAggregationProp, boolean withRecursiveAggregationProp,
            boolean expectExtended) {
        Query extended = this.aggregationReslover.getAggregationQuery(original, getCollection(withAggregationProp,
                withRecursiveAggregationProp));
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

    private Resource getCollection(boolean withAggregationProp, boolean withRecursiveAggregationProp) {
        ResourceImpl collection = new ResourceImpl();
        collection.setUri(Path.fromString("/rootCollection"));

        if (withAggregationProp) {
            String[] values = { "/barfolder", "/foo/bar" };
            Property aggregatedProp = getAggregatedPropTypeDef().createProperty(values);
            collection.addProperty(aggregatedProp);
        }

        if (withRecursiveAggregationProp) {
            Property recursiveAggregationProp = getRecursiveAggregatedPropDef().createProperty(Boolean.TRUE);
            collection.addProperty(recursiveAggregationProp);
        }

        return collection;
    }

    private PropertyTypeDefinition getAggregatedPropTypeDef() {
        return getPropDef("aggregation", Type.STRING, true);
    }

    private PropertyTypeDefinition getRecursiveAggregatedPropDef() {
        return getPropDef("recursiveAggregation", Type.BOOLEAN, false);
    }

    private PropertyTypeDefinition getPropDef(String name, Type type, boolean multiple) {
        PropertyTypeDefinitionImpl propDef = new PropertyTypeDefinitionImpl();
        propDef.setNamespace(Namespace.DEFAULT_NAMESPACE);
        propDef.setName(name);
        propDef.setType(type);
        propDef.setMultiple(multiple);
        propDef.setValueFactory(new ValueFactoryImpl());
        return propDef;
    }

    @Override
    protected Path getRequestPath() {
        return null;
    }

}
