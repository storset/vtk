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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.vortikal.repository.Path;
import org.vortikal.repository.search.query.Query;
import org.vortikal.web.service.URL;

public class CollectionListingAggregatedResourcesTest {

    private static CollectionListingAggregatedResources CLAR;

    private static URL HTTP_TEST_HOST = URL.parse("http://www.test.url");
    private static URL HTTPS_TEST_HOST = URL.parse("https://www.test.url");
    private static URL OTHER_TEST_HOST = URL.parse("http://www.other.url");
    private static URL MAN_APPR_TEST_HOST = URL.parse("http://www.man-appr.url");

    private static Set<Path> HTTP_TEST_HOST_AGGREGATION_PATHS;
    private static Set<Path> HTTPS_TEST_HOST_AGGREGATION_PATHS;
    private static Set<Path> OTHER_TEST_HOST_AGGREGATION_PATHS;

    private static Set<Path> OTHER_TEST_HOST_MAN_APPR_PATHS;
    private static Set<Path> MAN_APPR_TEST_HOST_PATHS;

    @BeforeClass
    public static void init() {

        HTTP_TEST_HOST_AGGREGATION_PATHS = new HashSet<Path>();
        HTTP_TEST_HOST_AGGREGATION_PATHS.add(Path.fromString("/test/path/one"));
        HTTP_TEST_HOST_AGGREGATION_PATHS.add(Path.fromString("/test/path/two"));

        HTTPS_TEST_HOST_AGGREGATION_PATHS = new HashSet<Path>();
        HTTPS_TEST_HOST_AGGREGATION_PATHS.add(Path.fromString("/test/path/https"));

        OTHER_TEST_HOST_AGGREGATION_PATHS = new HashSet<Path>();
        OTHER_TEST_HOST_AGGREGATION_PATHS.add(Path.fromString("/other/path"));

        Map<URL, Set<Path>> aggregationSet = new HashMap<URL, Set<Path>>();
        aggregationSet.put(HTTP_TEST_HOST, HTTP_TEST_HOST_AGGREGATION_PATHS);
        aggregationSet.put(HTTPS_TEST_HOST, HTTPS_TEST_HOST_AGGREGATION_PATHS);
        aggregationSet.put(OTHER_TEST_HOST, OTHER_TEST_HOST_AGGREGATION_PATHS);

        MAN_APPR_TEST_HOST_PATHS = new HashSet<Path>();
        MAN_APPR_TEST_HOST_PATHS.add(Path.fromString("/test/doc.html"));

        OTHER_TEST_HOST_MAN_APPR_PATHS = new HashSet<Path>();
        OTHER_TEST_HOST_MAN_APPR_PATHS.add(Path.fromString("/other/doc.html"));

        Map<URL, Set<Path>> manuallyApprovedSet = new HashMap<URL, Set<Path>>();
        manuallyApprovedSet.put(MAN_APPR_TEST_HOST, MAN_APPR_TEST_HOST_PATHS);
        manuallyApprovedSet.put(OTHER_TEST_HOST, OTHER_TEST_HOST_MAN_APPR_PATHS);

        CLAR = new CollectionListingAggregatedResources(aggregationSet, manuallyApprovedSet);
    }

    @Test
    public void testAggregationQuery() {

        CollectionListingAggregatedResources empty = new CollectionListingAggregatedResources(null, null);

        Query nullQueryLocal = empty.getAggregationQuery(null, false);
        assertNull(nullQueryLocal);

        Query nullQueryMultiHost = empty.getAggregationQuery(null, true);
        assertNull(nullQueryMultiHost);

        Query actualLocal = CLAR.getAggregationQuery(HTTP_TEST_HOST, false);
        assertNotNull(actualLocal);

        Query actualMultiHost = CLAR.getAggregationQuery(HTTP_TEST_HOST, true);
        assertNotNull(actualMultiHost);

    }

    @Test
    public void testCount() {

        assertEquals(6, CLAR.totalAggregatedResourceCount());

        assertEquals(3, CLAR.aggregatedResourcesCount(HTTP_TEST_HOST, CLAR.getAggregationSet()));
        assertEquals(3, CLAR.aggregatedResourcesCount(HTTPS_TEST_HOST, CLAR.getAggregationSet()));
        assertEquals(1, CLAR.aggregatedResourcesCount(OTHER_TEST_HOST, CLAR.getAggregationSet()));

        assertEquals(4, CLAR.aggregatedResourcesCount(null, CLAR.getAggregationSet()));

        assertEquals(1, CLAR.aggregatedResourcesCount(MAN_APPR_TEST_HOST, CLAR.getManuallyApproved()));
        assertEquals(1, CLAR.aggregatedResourcesCount(OTHER_TEST_HOST, CLAR.getManuallyApproved()));
        assertEquals(2, CLAR.aggregatedResourcesCount(null, CLAR.getManuallyApproved()));
    }

    @Test
    public void testGetHostAggregationSet() {

        Set<Path> pathSet = new HashSet<Path>();
        pathSet.addAll(HTTP_TEST_HOST_AGGREGATION_PATHS);
        pathSet.addAll(HTTPS_TEST_HOST_AGGREGATION_PATHS);

        Set<Path> otherPathSet = new HashSet<Path>();
        otherPathSet.addAll(OTHER_TEST_HOST_AGGREGATION_PATHS);

        this.assertGetHostAggregationSet(HTTP_TEST_HOST, CLAR, pathSet, otherPathSet);
        this.assertGetHostAggregationSet(HTTPS_TEST_HOST, CLAR, pathSet, otherPathSet);
        this.assertGetHostAggregationSet(OTHER_TEST_HOST, CLAR, otherPathSet, pathSet);

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
