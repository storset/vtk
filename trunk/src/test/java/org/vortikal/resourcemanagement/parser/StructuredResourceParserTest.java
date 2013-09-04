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
package org.vortikal.resourcemanagement.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.springframework.core.io.ClassPathResource;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.StructuredResourceManager;

public class StructuredResourceParserTest {

    protected static StructuredResourceParser RESOURCE_PARSER;;

    @BeforeClass
    public static void init() throws Exception {
        RESOURCE_PARSER = new StructuredResourceParser();
        RESOURCE_PARSER.setDefaultResourceTypeDefinitions(new ClassPathResource(
                "/vortikal/beans/vhost/structured-resources.vrtx"));
        RESOURCE_PARSER.setStructuredResourceManager(new MockStructuredResourceManager());
        RESOURCE_PARSER.afterPropertiesSet();
    }

    private static class MockStructuredResourceManager extends StructuredResourceManager {

        private Map<String, StructuredResourceDescription> types = new HashMap<String, StructuredResourceDescription>();

        @Override
        public void register(StructuredResourceDescription description) throws Exception {
            description.validate();
            this.types.put(description.getName(), description);
        }

        @Override
        public void registrationComplete() {
            // Ignore, do nothing
        }

        @Override
        public StructuredResourceDescription get(String name) {
            StructuredResourceDescription description = this.types.get(name);
            return description;
        }

        @Override
        public List<StructuredResourceDescription> list() {
            List<StructuredResourceDescription> result = new ArrayList<StructuredResourceDescription>();
            result.addAll(this.types.values());
            return result;
        }

    }

}
