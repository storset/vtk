/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repository.search;

import org.vortikal.repository.search.query.ACLReadForAllQuery;
import org.vortikal.testing.mocktypes.MockResourceTypeTree;
import java.util.List;

import junit.framework.TestCase;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.search.query.ACLExistsQuery;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.NameTermQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyExistsQuery;
import org.vortikal.repository.search.query.PropertyPrefixQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.PropertyWildcardQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.repository.search.query.UriTermQuery;

public class QueryParserTest extends TestCase {

    private QueryParser queryParser;

    public QueryParserTest() {
        this.queryParser = new QueryParserImpl(new MockResourceTypeTree());
    }

    public void testSimplePropertyQuery() {
        Query q = queryParser.parse("a=b");

        assertTrue(q instanceof PropertyTermQuery);

        PropertyTermQuery ptq = (PropertyTermQuery) q;

        assertEquals(Namespace.DEFAULT_NAMESPACE, ptq.getPropertyDefinition().getNamespace());
        assertEquals("a", ptq.getPropertyDefinition().getName());
        assertEquals("b", ptq.getTerm());
    }

    public void testPropertyQueryWithComplexValueAttributeSpecifier() {
        Query q = queryParser.parse("a@foo.bar = v@lue");

        assertTrue(q instanceof PropertyTermQuery);
        PropertyTermQuery ptq = (PropertyTermQuery) q;
        assertEquals(Namespace.DEFAULT_NAMESPACE, ptq.getPropertyDefinition().getNamespace());
        assertEquals("a", ptq.getPropertyDefinition().getName());
        assertEquals("foo.bar", ptq.getComplexValueAttributeSpecifier());
        assertEquals("v@lue", ptq.getTerm());

        q = queryParser.parse("prefix:propname@jsonblob.field =:v@lue:");

        assertTrue(q instanceof PropertyTermQuery);
        ptq = (PropertyTermQuery) q;
        assertEquals("prefix", ptq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("propname", ptq.getPropertyDefinition().getName());
        assertEquals("jsonblob.field", ptq.getComplexValueAttributeSpecifier());
        assertEquals(":v@lue:", ptq.getTerm());

        q = queryParser.parse("prefix:propname@foo=*bar*");
        assertTrue(q instanceof PropertyWildcardQuery);
        PropertyWildcardQuery pwq = (PropertyWildcardQuery) q;
        assertEquals("prefix", pwq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("propname", pwq.getPropertyDefinition().getName());
        assertEquals("foo", pwq.getComplexValueAttributeSpecifier());
        assertEquals("*bar*", pwq.getTerm());

        q = queryParser.parse("prefix:propname@foo=pre*");
        assertTrue(q instanceof PropertyPrefixQuery);
        PropertyPrefixQuery ppq = (PropertyPrefixQuery) q;
        assertEquals("prefix", ppq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("propname", ppq.getPropertyDefinition().getName());
        assertEquals("foo", ppq.getComplexValueAttributeSpecifier());
        assertEquals("pre", ppq.getTerm());

        q = queryParser.parse("prefix:propname@foo exists");
        assertTrue(q instanceof PropertyExistsQuery);
        PropertyExistsQuery peq = (PropertyExistsQuery) q;
        assertEquals("prefix", peq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("propname", peq.getPropertyDefinition().getName());
        assertEquals("foo", peq.getComplexValueAttributeSpecifier());
        assertFalse(peq.isInverted());

        q = queryParser.parse("prefix:propname@foo   not exists");
        assertTrue(q instanceof PropertyExistsQuery);
        peq = (PropertyExistsQuery) q;
        assertEquals("prefix", peq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("propname", peq.getPropertyDefinition().getName());
        assertEquals("foo", peq.getComplexValueAttributeSpecifier());
        assertTrue(peq.isInverted());

        q = queryParser.parse("propname@foo   !exists");
        assertTrue(q instanceof PropertyExistsQuery);
        peq = (PropertyExistsQuery) q;
        assertNull(peq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("propname", peq.getPropertyDefinition().getName());
        assertEquals("foo", peq.getComplexValueAttributeSpecifier());
        assertTrue(peq.isInverted());

        q = queryParser.parse("propname@.!exists");
        assertTrue(q instanceof PropertyExistsQuery);
        peq = (PropertyExistsQuery) q;
        assertNull(peq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("propname", peq.getPropertyDefinition().getName());
        assertEquals(".", peq.getComplexValueAttributeSpecifier());
        assertTrue(peq.isInverted());
    }

    public void testExistsQuery() {
        Query q = queryParser.parse("p:r exists");

        assertTrue(q instanceof PropertyExistsQuery);

        PropertyExistsQuery peq = (PropertyExistsQuery) q;
        assertEquals("r", peq.getPropertyDefinition().getName());
        assertEquals("p", peq.getPropertyDefinition().getNamespace().getPrefix());
        assertFalse(peq.isInverted());


        q = queryParser.parse("p:r not exists");

        assertTrue(q instanceof PropertyExistsQuery);

        peq = (PropertyExistsQuery) q;
        assertEquals("r", peq.getPropertyDefinition().getName());
        assertEquals("p", peq.getPropertyDefinition().getNamespace().getPrefix());
        assertTrue(peq.isInverted());


        q = queryParser.parse("p:r EXISTS");

        assertTrue(q instanceof PropertyExistsQuery);

        peq = (PropertyExistsQuery) q;
        assertEquals("r", peq.getPropertyDefinition().getName());
        assertEquals("p", peq.getPropertyDefinition().getNamespace().getPrefix());
        assertFalse(peq.isInverted());
        

        q = queryParser.parse("p:r NOT EXISTS");

        assertTrue(q instanceof PropertyExistsQuery);

        peq = (PropertyExistsQuery) q;
        assertEquals("r", peq.getPropertyDefinition().getName());
        assertEquals("p", peq.getPropertyDefinition().getNamespace().getPrefix());
        assertTrue(peq.isInverted());

        q = queryParser.parse("r !EXISTS");

        assertTrue(q instanceof PropertyExistsQuery);

        peq = (PropertyExistsQuery) q;
        assertEquals("r", peq.getPropertyDefinition().getName());
        assertEquals(Namespace.DEFAULT_NAMESPACE, peq.getPropertyDefinition().getNamespace());
        assertTrue(peq.isInverted());
    }
    
    public void testAclQuery() {
        Query q = queryParser.parse("acl EXISTS");
        assertTrue(q instanceof ACLExistsQuery);
        assertFalse(((ACLExistsQuery)q).isInverted());

        q = queryParser.parse("acl !EXISTS");
        assertTrue(q instanceof ACLExistsQuery);
        assertTrue(((ACLExistsQuery)q).isInverted());

        q = queryParser.parse("acl NOT EXISTS");
        assertTrue(q instanceof ACLExistsQuery);
        assertTrue(((ACLExistsQuery)q).isInverted());
        
        q = queryParser.parse("acl ALL");
        assertTrue(q instanceof ACLReadForAllQuery);
        assertFalse(((ACLReadForAllQuery)q).isInverted());

        q = queryParser.parse("acl !ALL");
        assertTrue(q instanceof ACLReadForAllQuery);
        assertTrue(((ACLReadForAllQuery)q).isInverted());

        q = queryParser.parse("acl NOT ALL");
        assertTrue(q instanceof ACLReadForAllQuery);
        assertTrue(((ACLReadForAllQuery)q).isInverted());
    }

    public void testEscaping() {
        Query q = queryParser.parse("uri = /i\\ am\\ a\\ file\\ with\\ spaces\\(YES\\)\\?\\*\\>\\<\\=\\x\\\\\\/\\ AND\\ uri\\=/hoho.txt");

        assertTrue(q instanceof UriTermQuery);
        assertEquals("/i am a file with spaces(YES)?*><=x\\\\/ AND uri=/hoho.txt", ((UriTermQuery) q).getUri());
        assertEquals(((UriTermQuery) q).getOperator(), TermOperator.EQ);
    }
    
    public void testComplexQuery() {

        Query q = queryParser.parse("(type IN emne && emne:emnekode exists && emne:emnenavn exists" 
                + " && foo:bar not exists && emne:status=gjeldende-versjon && depth=6)" 
                + " AND uri = /studier/emner/jus/* AND name=index.xml AND foo@bar.bing.bong >= baz");

        assertTrue(q instanceof AndQuery);
        AndQuery aqTop = (AndQuery) q;
        assertEquals(4, aqTop.getQueries().size());

        assertTrue(aqTop.getQueries().get(0) instanceof AndQuery);
        assertTrue(aqTop.getQueries().get(1) instanceof UriPrefixQuery);
        UriPrefixQuery upq = (UriPrefixQuery) aqTop.getQueries().get(1);
        assertEquals("/studier/emner/jus/", upq.getUri());
        assertEquals(TermOperator.EQ, upq.getOperator());

        assertTrue(aqTop.getQueries().get(2) instanceof NameTermQuery);
        NameTermQuery ntq = (NameTermQuery) aqTop.getQueries().get(2);
        assertEquals("index.xml", ntq.getTerm());
        assertEquals(TermOperator.EQ, ntq.getOperator());

        assertTrue(aqTop.getQueries().get(3) instanceof PropertyTermQuery);
        PropertyTermQuery ptq = (PropertyTermQuery) aqTop.getQueries().get(3);
        assertNull(ptq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("foo", ptq.getPropertyDefinition().getName());
        assertEquals("baz", ptq.getTerm());
        assertEquals("bar.bing.bong", ptq.getComplexValueAttributeSpecifier());
        assertEquals(TermOperator.GE, ptq.getOperator());

        // Level 2, first top node
        AndQuery sub1 = (AndQuery) aqTop.getQueries().get(0);
        assertEquals(6, sub1.getQueries().size());
        assertTrue(sub1.getQueries().get(0) instanceof TypeTermQuery);
        TypeTermQuery ttq = (TypeTermQuery) sub1.getQueries().get(0);
        assertEquals("emne", ttq.getTerm());
        assertEquals(TermOperator.IN, ttq.getOperator());

        assertTrue(sub1.getQueries().get(1) instanceof PropertyExistsQuery);
        PropertyExistsQuery peq = (PropertyExistsQuery) sub1.getQueries().get(1);
        assertFalse(peq.isInverted());
        assertEquals("emne", peq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("emnekode", peq.getPropertyDefinition().getName());

        assertTrue(sub1.getQueries().get(2) instanceof PropertyExistsQuery);
        peq = (PropertyExistsQuery) sub1.getQueries().get(2);
        assertFalse(peq.isInverted());
        assertEquals("emne", peq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("emnenavn", peq.getPropertyDefinition().getName());

        assertTrue(sub1.getQueries().get(3) instanceof PropertyExistsQuery);
        peq = (PropertyExistsQuery) sub1.getQueries().get(3);
        assertTrue(peq.isInverted());
        assertEquals("foo", peq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("bar", peq.getPropertyDefinition().getName());

        assertTrue(sub1.getQueries().get(4) instanceof PropertyTermQuery);
        ptq = (PropertyTermQuery) sub1.getQueries().get(4);
        assertEquals("emne", ptq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("status", ptq.getPropertyDefinition().getName());
        assertEquals(TermOperator.EQ, ptq.getOperator());

        assertTrue(sub1.getQueries().get(5) instanceof UriDepthQuery);
        UriDepthQuery udq = (UriDepthQuery) sub1.getQueries().get(5);
        assertEquals(6, udq.getDepth());

    }

    public void testPropertyWildcardAndPrefixQuery() {

        Query q = queryParser.parse("foo:bar = *suffix || foo:bar =~ PrE\\ fIx* OR foo:bar = \\(prefix\\)*" + " || a!=x* OR a=*x OR a=?x OR a !=~ *fo??o* OR foo:bar =~ *TEXAS\\ HOLD*");

        assertTrue(q instanceof OrQuery);

        List<Query> subs = ((OrQuery) q).getQueries();

        assertEquals(8, subs.size());

        assertTrue(subs.get(0) instanceof PropertyWildcardQuery);
        PropertyWildcardQuery pwq = (PropertyWildcardQuery) subs.get(0);
        assertEquals("foo", pwq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("bar", pwq.getPropertyDefinition().getName());
        assertEquals("*suffix", pwq.getTerm());
        assertEquals(TermOperator.EQ, pwq.getOperator());
        assertNull(pwq.getComplexValueAttributeSpecifier());

        assertTrue(subs.get(1) instanceof PropertyPrefixQuery);
        PropertyPrefixQuery ppq = (PropertyPrefixQuery) subs.get(1);
        assertEquals("foo", ppq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("bar", ppq.getPropertyDefinition().getName());
        assertEquals("PrE fIx", ppq.getTerm());
        assertEquals(TermOperator.EQ_IGNORECASE, ppq.getOperator());
        assertNull(ppq.getComplexValueAttributeSpecifier());

        assertTrue(subs.get(2) instanceof PropertyPrefixQuery);
        ppq = (PropertyPrefixQuery) subs.get(2);
        assertEquals("foo", ppq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("bar", ppq.getPropertyDefinition().getName());
        assertEquals("(prefix)", ppq.getTerm());
        assertEquals(TermOperator.EQ, ppq.getOperator());
        assertNull(ppq.getComplexValueAttributeSpecifier());

        assertTrue(subs.get(3) instanceof PropertyPrefixQuery);
        ppq = (PropertyPrefixQuery) subs.get(3);
        assertEquals(null, ppq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("a", ppq.getPropertyDefinition().getName());
        assertEquals("x", ppq.getTerm());
        assertEquals(TermOperator.NE, ppq.getOperator());
        assertNull(ppq.getComplexValueAttributeSpecifier());

        assertTrue(subs.get(4) instanceof PropertyWildcardQuery);
        pwq = (PropertyWildcardQuery) subs.get(4);
        assertEquals(null, pwq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("a", pwq.getPropertyDefinition().getName());
        assertEquals("*x", pwq.getTerm());
        assertEquals(TermOperator.EQ, pwq.getOperator());
        assertNull(pwq.getComplexValueAttributeSpecifier());

        assertTrue(subs.get(5) instanceof PropertyWildcardQuery);
        pwq = (PropertyWildcardQuery) subs.get(5);
        assertEquals(null, pwq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("a", pwq.getPropertyDefinition().getName());
        assertEquals("?x", pwq.getTerm());
        assertEquals(TermOperator.EQ, pwq.getOperator());
        assertNull(pwq.getComplexValueAttributeSpecifier());

        assertTrue(subs.get(6) instanceof PropertyWildcardQuery);
        pwq = (PropertyWildcardQuery) subs.get(6);
        assertEquals(null, pwq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("a", pwq.getPropertyDefinition().getName());
        assertEquals("*fo??o*", pwq.getTerm());
        assertEquals(TermOperator.NE_IGNORECASE, pwq.getOperator());
        assertNull(pwq.getComplexValueAttributeSpecifier());

        assertTrue(subs.get(7) instanceof PropertyWildcardQuery);
        pwq = (PropertyWildcardQuery) subs.get(7);
        assertEquals("foo", pwq.getPropertyDefinition().getNamespace().getPrefix());
        assertEquals("bar", pwq.getPropertyDefinition().getName());
        assertEquals("*TEXAS HOLD*", pwq.getTerm());
        assertEquals(TermOperator.EQ_IGNORECASE, pwq.getOperator());
        assertNull(pwq.getComplexValueAttributeSpecifier());
    }
}
