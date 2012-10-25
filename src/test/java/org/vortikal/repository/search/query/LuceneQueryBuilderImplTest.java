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
package org.vortikal.repository.search.query;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.TermsFilter;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.index.mapping.FieldNames;
import org.vortikal.repository.index.mapping.FieldValueMapper;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.query.security.QueryAuthorizationFilterFactory;
import org.vortikal.security.PrincipalFactory;

public class LuceneQueryBuilderImplTest {

    final String dummyToken = "dummy_token";
    final IndexReader nullIndexReader = null;

    private LuceneQueryBuilderImpl luceneQueryBuilder = new LuceneQueryBuilderImpl();
    private Mockery context = new JUnit4Mockery();
    private QueryAuthorizationFilterFactory mockQueryAuthorizationFilterFactory;
    private TermsFilter dummyAclFilter = new TermsFilter();

    @Before
    public void setUp() {
        this.mockQueryAuthorizationFilterFactory = context.mock(QueryAuthorizationFilterFactory.class);
        luceneQueryBuilder.setQueryAuthorizationFilterFactory(mockQueryAuthorizationFilterFactory);

        PropertyTypeDefinitionImpl publishedPropDef = new PropertyTypeDefinitionImpl();
        publishedPropDef.setNamespace(Namespace.DEFAULT_NAMESPACE);
        publishedPropDef.setName("published");
        luceneQueryBuilder.setPublishedPropDef(publishedPropDef);

        PropertyTypeDefinitionImpl obsoletedPropDef = new PropertyTypeDefinitionImpl();
        obsoletedPropDef.setNamespace(Namespace.DEFAULT_NAMESPACE);
        obsoletedPropDef.setName("obsoleted");
        luceneQueryBuilder.setObsoletedPropDef(obsoletedPropDef);

        luceneQueryBuilder.setFieldValueMapper(new FieldValueMapper());

        dummyAclFilter.addTerm(new Term(FieldNames.ACL_READ_PRINCIPALS_FIELD_NAME, PrincipalFactory.ALL
                .getQualifiedName()));

        luceneQueryBuilder.afterPropertiesSet();
    }

    @Test
    public void testGetSearchFilterDefaultExcludesFalseNoAcl() {

        this.assertGetSearchFilter(null, new Search(), null);

    }

    @Test
    public void testGetSearchFilterDefaultExcludesFalseAndAcl() {

        Filter expected = dummyAclFilter;
        this.assertGetSearchFilter(expected, new Search(), dummyAclFilter);

    }

    @Test
    public void testGetSearchFilterDefaultExcludesNoAcl() {

        Filter expected = luceneQueryBuilder.buildDefaultExcludesFilter();
        Search search = new Search();
        search.setUseDefaultExcludes(true);
        this.assertGetSearchFilter(expected, search, null);

    }

    @Test
    public void testGetSearchFilterDefaultExcludesAndAcl() {

        BooleanFilter expected = luceneQueryBuilder.buildDefaultExcludesFilter();
        expected.add(dummyAclFilter, BooleanClause.Occur.MUST);
        
        Search search = new Search();
        search.setUseDefaultExcludes(true);
        this.assertGetSearchFilter(expected, search, dummyAclFilter);

    }

    private void assertGetSearchFilter(final Filter expected, final Search search, final Filter expectedReturnAclQuery) {

        context.checking(new Expectations() {
            {
                one(mockQueryAuthorizationFilterFactory).authorizationQueryFilter(dummyToken, nullIndexReader);
                will(returnValue(expectedReturnAclQuery));
            }
        });

        Filter actual = luceneQueryBuilder.buildSearchFilter(dummyToken, search, nullIndexReader);

        // Nothing more to do, requested configuration yields no filter
        if (!search.isUseDefaultExcludes() && expectedReturnAclQuery == null) {
            assertNull("Filter is supposed to be NULL", actual);
            return;
        }

        assertNotNull("Filter is NOT supposed to be NULL", actual);
        assertTrue("Filter is not as expected", expected.equals(actual));

    }

}
