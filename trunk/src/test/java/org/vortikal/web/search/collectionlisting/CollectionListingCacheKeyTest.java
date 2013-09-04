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
package org.vortikal.web.search.collectionlisting;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.vortikal.repository.Path;

public class CollectionListingCacheKeyTest {

    @SuppressWarnings("unused")
    @Test
    public void testInvalid() {

        // Null name
        try {
            CollectionListingCacheKey cacheKey = new CollectionListingCacheKey(null, null, null, null);
            fail();
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        // Null resource path
        try {
            CollectionListingCacheKey cacheKey = new CollectionListingCacheKey("name", null, null, null);
            fail();
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        // Null last modified date
        try {
            CollectionListingCacheKey cacheKey = new CollectionListingCacheKey("name", Path.fromString("/path"), null,
                    null);
            fail();
        } catch (IllegalArgumentException iae) {
            // Expected
        }

    }

    @Test
    public void testNegativeEquality() {

        // Authenticated/anonymous
        CollectionListingCacheKey cacheKey1 = new CollectionListingCacheKey("name", Path.fromString("/path"),
                "2013-05-16 12:00:05", "token");
        CollectionListingCacheKey cacheKey2 = new CollectionListingCacheKey("name", Path.fromString("/path"),
                "2013-05-16 12:00:05", null);
        assertFalse(cacheKey1.equals(cacheKey2));

        // Different path
        cacheKey1 = new CollectionListingCacheKey("name", Path.fromString("/path"), "2013-05-16 12:00:05", "token");
        cacheKey2 = new CollectionListingCacheKey("name", Path.fromString("/path/2"), "2013-05-16 12:00:05", "token");
        assertFalse(cacheKey1.equals(cacheKey2));

        // Different names
        cacheKey1 = new CollectionListingCacheKey("name", Path.fromString("/path"), "2013-05-16 12:00:05", "token");
        cacheKey2 = new CollectionListingCacheKey("name2", Path.fromString("/path"), "2013-05-16 12:00:05", "token");
        assertFalse(cacheKey1.equals(cacheKey2));

        // Different last modified
        cacheKey1 = new CollectionListingCacheKey("name", Path.fromString("/path"), "2013-05-16 12:00:05", "token");
        cacheKey2 = new CollectionListingCacheKey("name", Path.fromString("/path"), "2013-05-17 13:15:00", "token");
        assertFalse(cacheKey1.equals(cacheKey2));

        // Different all
        cacheKey1 = new CollectionListingCacheKey("nameX", Path.fromString("/path/xx"), "2012-08-12 19:14:18", "token");
        cacheKey2 = new CollectionListingCacheKey("name", Path.fromString("/path"), "2013-05-17 13:15:00", "tokenXX");
        assertFalse(cacheKey1.equals(cacheKey2));

        // Different class
        assertFalse(cacheKey1.equals(new Object()));

    }

    @Test
    public void testEquality() {

        // Authenticated
        CollectionListingCacheKey cacheKey1 = new CollectionListingCacheKey("name", Path.fromString("/path"),
                "2013-05-16 12:00:05", "token");
        CollectionListingCacheKey cacheKey2 = new CollectionListingCacheKey("name", Path.fromString("/path"),
                "2013-05-16 12:00:05", "token");
        assertTrue(cacheKey1.equals(cacheKey2));

        // Anonymous
        cacheKey1 = new CollectionListingCacheKey("name", Path.fromString("/path"), "2013-05-16 12:00:05", null);
        cacheKey2 = new CollectionListingCacheKey("name", Path.fromString("/path"), "2013-05-16 12:00:05", null);
        assertTrue(cacheKey1.equals(cacheKey2));

    }

}
