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
package org.vortikal.repository.store.db;

import java.util.Arrays;

import org.vortikal.repository.Path;
import org.vortikal.repository.store.db.SqlDaoUtils.PropHolder;

import junit.framework.TestCase;


public class SqlDaoUtilsTest extends TestCase {


    public void testGetUriSqlWildcard() {

        assertEquals("/%", SqlDaoUtils.getUriSqlWildcard(Path.ROOT, '@'));

        assertEquals("/foo/bar/%", SqlDaoUtils.getUriSqlWildcard(Path.fromString("/foo/bar"), '@'));

        assertEquals("/foo@@bar/%",
                SqlDaoUtils.getUriSqlWildcard(Path.fromString("/foo@bar"), '@'));

        assertEquals("/foo@@bar@%/%",
                SqlDaoUtils.getUriSqlWildcard(Path.fromString("/foo@bar%"), '@'));

        assertEquals("/vrtx/@_@_vrtx/@%foo@%/@@children/%",
                SqlDaoUtils.getUriSqlWildcard(Path.fromString("/vrtx/__vrtx/%foo%/@children"), '@'));

    }

    // PropHolder is used for aggregation of multi-value property values from database
    // because of de-normalized storage. Usage of PropHolder class in SqlMapDataAccessor
    // relies on correct equals hand hashCode implementations.
    public void testPropHolderHashcodeEquals() {
        
        PropHolder holder1 = newTestHolder();
        
        // Test basic requirements of equals and hashcode wrt. consistency
        assertFalse(holder1.equals(null));
        assertTrue(holder1.equals(holder1)); // Reflexivity
        assertEquals(holder1.hashCode(), holder1.hashCode());
        
        // Create another identical holder instance for comparison
        PropHolder holder2 = newTestHolder();
        
        assertEquals(holder1.hashCode(), holder2.hashCode());
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        
        // Test that propId does not affect hashcode or equals (a single property in
        // application level can map to multiple propIDs, because of multi-value).
        holder2.propID = null;
        assertEquals(holder1.hashCode(), holder2.hashCode());
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        holder2 = newTestHolder();
        
        // Test that values does not affect hashcode or equals
        holder2.values = null;
        assertEquals(holder1.hashCode(), holder2.hashCode());
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        holder2 = newTestHolder();
        
        // Test that propTypeId does not affect hashcode or equals
        holder2.propTypeId = 5;
        assertEquals(holder1.hashCode(), holder2.hashCode());
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        holder2 = newTestHolder();
        
        // Test that binary does not affect hashcode or equals
        holder2.binary = true;
        assertEquals(holder1.hashCode(), holder2.hashCode());
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        holder2 = newTestHolder();
        
        
        // Test that namespaceUri affects equals
        holder2.namespaceUri = "http://www.uio.no/custom-properties";
        assertFalse(holder1.equals(holder2));
        assertFalse(holder2.equals(holder1)); // Symmetry
        
        holder1.namespaceUri = "http://www.uio.no/custom-properties";
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        assertEquals(holder1.hashCode(), holder2.hashCode());

        holder2.namespaceUri = "foobarbaz";
        assertFalse(holder1.equals(holder2));
        assertFalse(holder2.equals(holder1)); // Symmetry
        
        holder2.namespaceUri = null;
        assertFalse(holder1.equals(holder2));
        assertFalse(holder2.equals(holder1)); // Symmetry
        
        holder1.namespaceUri = null;
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        assertEquals(holder1.hashCode(), holder2.hashCode());

        
        // Test that name affects equals
        holder2.name = "foo";
        assertFalse(holder1.equals(holder2));
        assertFalse(holder2.equals(holder1)); // Symmetry

        holder1.name = "foo";
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        assertEquals(holder1.hashCode(), holder2.hashCode());
        
        holder2.name = "title";
        assertFalse(holder1.equals(holder2));
        assertFalse(holder2.equals(holder1)); // Symmetry

        holder1.name = "title";
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        assertEquals(holder1.hashCode(), holder2.hashCode());
        

        // Test that resourceId affects equals
        holder2.resourceId = 1001;
        assertFalse(holder1.equals(holder2));
        assertFalse(holder2.equals(holder1)); // Symmetry
        
        holder1.resourceId = 1001;
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        assertEquals(holder1.hashCode(), holder2.hashCode());

        holder2.resourceId = 1000;
        assertFalse(holder1.equals(holder2));
        assertFalse(holder2.equals(holder1)); // Symmetry

        holder1.resourceId = 1000;
        assertTrue(holder1.equals(holder2));
        assertTrue(holder2.equals(holder1)); // Symmetry
        assertEquals(holder1.hashCode(), holder2.hashCode());
        
    }
    
    private PropHolder newTestHolder() {
        PropHolder holder = new PropHolder();
        holder.name = "title";
        holder.namespaceUri = null;
        holder.resourceId = 1000;
        holder.values = Arrays.asList(new Object[]{"Root"});
        holder.propID = new Integer(1000);
        holder.binary = false;
        holder.propTypeId = 0;
        return holder;
    }
    
}
