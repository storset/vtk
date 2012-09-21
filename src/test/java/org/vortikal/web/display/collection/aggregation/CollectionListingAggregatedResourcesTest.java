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
package org.vortikal.web.display.collection.aggregation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.vortikal.repository.Path;
import org.vortikal.web.service.URL;

public class CollectionListingAggregatedResourcesTest {

    @Test
    public void testGetHostAggregationSet() {

        URL testHostHttp = URL.parse("http://www.test.url");
        Path testHostHttpPathOne = Path.fromString("/test/path/one");
        Path testHostHttpPathTwo = Path.fromString("/test/path/two");
        Set<Path> testHostHttpPaths = new HashSet<Path>();
        testHostHttpPaths.add(testHostHttpPathOne);
        testHostHttpPaths.add(testHostHttpPathTwo);

        URL testHostHttps = URL.parse("https://www.test.url");
        Path testHostHttpsPath = Path.fromString("/test/path/https");
        Set<Path> testHostHttpsPaths = new HashSet<Path>();
        testHostHttpsPaths.add(testHostHttpsPath);

        URL testHostOther = URL.parse("https://www.other.url");
        Set<Path> testHostOtherPaths = new HashSet<Path>();
        Path testHostOtherPath = Path.fromString("/other/path");
        testHostOtherPaths.add(testHostOtherPath);

        Map<URL, Set<Path>> aggregationSet = new HashMap<URL, Set<Path>>();
        aggregationSet.put(testHostHttp, testHostHttpPaths);
        aggregationSet.put(testHostHttps, testHostHttpsPaths);
        aggregationSet.put(testHostOther, testHostOtherPaths);

        CollectionListingAggregatedResources clar = new CollectionListingAggregatedResources(aggregationSet, null);

        Set<Path> pathSet = new HashSet<Path>();
        pathSet.addAll(testHostHttpPaths);
        pathSet.addAll(testHostHttpsPaths);

        Set<Path> otherPathSet = new HashSet<Path>();
        otherPathSet.addAll(testHostOtherPaths);

        this.assertGetHostAggregationSet(testHostHttp, clar, pathSet, otherPathSet);
        this.assertGetHostAggregationSet(testHostHttps, clar, pathSet, otherPathSet);
        this.assertGetHostAggregationSet(testHostOther, clar, otherPathSet, pathSet);

    }

    private void assertGetHostAggregationSet(URL testHostHttp, CollectionListingAggregatedResources clar,
            Set<Path> expected, Set<Path> notExpected) {

        Set<Path> hostPaths = clar.getHostAggregationSet(testHostHttp);
        for (Path exp : expected) {
            assertTrue("Expected path is missing for test url '" + testHostHttp + "'", hostPaths.contains(exp));
        }
        for (Path nExp : notExpected) {
            assertFalse("Unexpected path for test url '" + testHostHttp + "'", hostPaths.contains(nExp));
        }

    }

}
