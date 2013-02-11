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
package org.vortikal.web.search;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.web.service.URL;

public class VHostScopeQueryRestricterTest {

    @Test
    public void testRestrictQueryToVhost() {

        AndQuery expected = new AndQuery();
        UriPrefixQuery original = new UriPrefixQuery("/test/path");
        expected.add(original);

        URL url = URL.parse("http://www.usit.uio.no");
        String vhost = url.getHost();
        expected.add(new PropertyTermQuery(VHostScopeQueryRestricter.vHostPropDef, vhost, TermOperator.EQ));

        Query actual = VHostScopeQueryRestricter.vhostRestrictedQuery(original, url);
        assertEquals(expected, actual);

        actual = VHostScopeQueryRestricter.vhostRestrictedQuery(original, vhost);
        assertEquals(expected, actual);

    }

    @Test
    public void testRestrictQueryToVhostList() {

        testRestrictQueryToVhostList(Arrays.asList("www.usit.uio.no"));
        testRestrictQueryToVhostList(Arrays.asList("www.usit.uio.no", "www.hf.uio.no"));

    }

    private void testRestrictQueryToVhostList(List<String> vhosts) {

        AndQuery expected = new AndQuery();
        UriPrefixQuery original = new UriPrefixQuery("/test/path");
        expected.add(original);
        if (vhosts.size() == 1) {
            expected.add(new PropertyTermQuery(VHostScopeQueryRestricter.vHostPropDef, vhosts.get(0), TermOperator.EQ));
        } else {
            OrQuery vhostOr = new OrQuery();
            for (String vhost : vhosts) {
                vhostOr.add(new PropertyTermQuery(VHostScopeQueryRestricter.vHostPropDef, vhost, TermOperator.EQ));
            }
            expected.add(vhostOr);
        }

        Query actual = VHostScopeQueryRestricter.vhostRestrictedQuery(original, vhosts);
        assertEquals(expected, actual);
    }

}
