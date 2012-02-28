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

package org.vortikal.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ResourceImplTest extends PropertySetImplTest {

    protected ResourceImpl resource;
    
    @Before
    @Override
    public void setup() {
        super.setup();
        resource = new ResourceImpl(Path.fromString("/"));
        resource.setResourceType("collection");
    }
    
    @Test
    public void testIterationRemove() {
        resource.addProperty(title);
        resource.addProperty(modifiedBy);
        resource.addProperty(custom);
        
        for (Iterator<Property> it = resource.iterator(); it.hasNext();) {
            Property p = it.next();
            if (p == custom) {
                it.remove();
            }
        }
        
        for (Property p: resource) {
            collector.add(p);
        }
        assertEquals(2, collector.size());
        assertTrue(collector.contains(title));
        assertTrue(collector.contains(modifiedBy));
        
    }
    
    @Test
    public void testIterationRemoveIllegalState() {
        resource.addProperty(title);
        resource.addProperty(modifiedBy);
        resource.addProperty(custom);
        
        Iterator<Property> it = resource.iterator();
        try {
            it.remove();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }
    
    @Test
    public void testIterationEmptyRemoveIllegalState() {
        Iterator<Property> it = resource.iterator();
        try {
            it.remove();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }
}

