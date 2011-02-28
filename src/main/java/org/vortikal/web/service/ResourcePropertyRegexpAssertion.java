/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;

/**
 * XXX: Rewrite to use propTypeDef instead!
 */
public class ResourcePropertyRegexpAssertion
  extends AbstractRepositoryAssertion {

    private Namespace namespace;
    private String name;
    private Pattern pattern = null;
    private boolean invert = false;
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    public void setPattern(String pattern) {
        if (pattern == null) throw new IllegalArgumentException(
            "Property 'pattern' cannot be null");
    
        this.pattern = Pattern.compile(pattern);
    }
    
    public void setInvert(boolean invert) {
        this.invert = invert;
    }
    
    
    public boolean conflicts(Assertion assertion) {
        return false;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("property.").append(this.name);
        sb.append(this.invert ? " !~ " : " ~ ");
        sb.append(this.pattern.pattern());
        return sb.toString();
    }

    public boolean matches(Resource resource, Principal principal) {
        if (resource != null) {
            Property property = resource.getProperty(this.namespace, this.name);

            if (property != null) {
                Matcher m = this.pattern.matcher(property.getStringValue());
                return this.invert != m.matches();
            }
        }
        
        return this.invert;
    }

}
