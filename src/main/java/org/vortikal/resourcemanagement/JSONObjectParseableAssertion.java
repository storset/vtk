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
package org.vortikal.resourcemanagement;

import org.vortikal.repository.RepositoryContentEvaluationAssertion;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.security.Principal;

/**
 * XXX Not usable as web service assertion.
 *
 */
public class JSONObjectParseableAssertion implements RepositoryContentEvaluationAssertion {

//    private Repository repository;
//    private String token;

    public boolean matches(Resource resource, Principal principal) {
        return matches(resource, principal, null);
//        if (resource.isCollection()) {
//            return false;
//        }
//        try {
//            InputStream inputStream = this.repository.getInputStream(
//                this.token, resource.getURI(), true);
//            byte[] buffer = StreamUtil.readInputStream(inputStream);
//            String content = new String(buffer, "utf-8");
//            JSONObject.fromObject(content);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
    }

//    public boolean conflicts(Assertion assertion) {
//        return false;
//    }

    public boolean matches(Resource resource, Principal principal, Content content) {
        // Could fallback to Resource.getInputStream instead,but that will not work in all cases when
        // this assertion is called from evaluation framework.
        if (content == null) return false;
        if (resource.isCollection()) return false;
        
        try {
            content.getContentRepresentation(net.sf.json.JSONObject.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

//    @Required public void setRepository(Repository repository) {
//        this.repository = repository;
//    }
//
//    @Required public void setToken(String token) {
//        this.token = token;
//    }
}
