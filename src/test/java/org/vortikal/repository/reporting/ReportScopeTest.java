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

package org.vortikal.repository.reporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vortikal.repository.Path;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.testing.mocktypes.MockResourceTypeTree;

/**
 * Test some implementations of <code>ReportScope</code>
 * for correct equals/hashcode and clone. Necessary to get proper and safe
 * cache key for report queries.
 */
public class ReportScopeTest {

    private ResourceTypeTree rtTree;
    private ResourceTypeDefinition fooType;
    private ResourceTypeDefinition barType;
    private PropertyTypeDefinition propDef;

    public ReportScopeTest() {
        this.rtTree = new MockResourceTypeTree();
        this.fooType = this.rtTree.getResourceTypeDefinitionByName("fooType");
        this.barType = this.rtTree.getResourceTypeDefinitionByName("barType");
        this.propDef = this.rtTree.getPropertyDefinitionByPrefix(null, "barProp");
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    private List<ReportScope> getNewTestScopes(int salt) {
        if (salt < 0){
            throw new IllegalArgumentException("salt must be int >= 0");
        }
        
        List<ReportScope> scopes = new ArrayList<ReportScope>();

        UriPrefixScope uriPrefixScope = new UriPrefixScope();
        for (int i=0; i<salt; i++) {
            uriPrefixScope.addUriPrefix(Path.fromString("/foo/bar" + salt));
        }
        scopes.add(uriPrefixScope);

        Set<ResourceTypeDefinition> defs = new HashSet<ResourceTypeDefinition>();
        defs.add(this.fooType);
        defs.add(this.barType);
        ResourceTypeScope typeScope = new ResourceTypeScope(defs);
        scopes.add(typeScope);

        scopes.add(new ResourceReadableACLScope(String.valueOf(salt)));

        Value value1 = new Value("FooValue" + salt, PropertyType.Type.STRING);
        Value value2 = new Value("BarValue", PropertyType.Type.STRING);
        Set<Value> values = new HashSet<Value>();
        values.add(value1);
        values.add(value2);
        ResourcePropertyValueScope propValScope = new ResourcePropertyValueScope(this.propDef, values);
        scopes.add(propValScope);
        
        return scopes;
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of isProhibited method, of class ReportScope.
     */
    @Test
    public void testHashCodeEquals() {

        List<ReportScope> scopes1 = getNewTestScopes(0);
        List<ReportScope> scopes2 = getNewTestScopes(0);

        // Salt is same, so should be equal, but not same object instances.
        assertNotSame(scopes1, scopes2);
        assertEquals(scopes1, scopes2);
        assertEquals(scopes1.hashCode(), scopes2.hashCode());

        List<ReportScope> scopes3 = getNewTestScopes(1);
        assertFalse(scopes3.equals(scopes1));
        assertFalse(scopes3.equals(scopes2));


        scopes1 = getNewTestScopes(4);
        scopes2 = getNewTestScopes(4);

        ResourcePropertyValueScope rpscope = (ResourcePropertyValueScope)scopes2.get(3);
        rpscope.setProhibited(true);

        assertFalse(scopes1.equals(scopes2));
        rpscope.setProhibited(false);
        assertEquals(scopes1, scopes2);
}

    /**
     * Test of clone method, of class ReportScope.
     */
    @Test
    public void testClone() {

        List<ReportScope> scopes = getNewTestScopes(4);

        List<ReportScope> clones = new ArrayList<ReportScope>();
        for (ReportScope scope: scopes) {
            clones.add((ReportScope)scope.clone());
        }

        assertEquals(scopes, clones);

        Object removed = clones.remove(0);
        clones.add((ReportScope)removed); // Shuffle list

        assertFalse(scopes.equals(clones));

    }

}