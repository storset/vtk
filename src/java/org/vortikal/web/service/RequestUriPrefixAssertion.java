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
/*
 * Created on 05.jul.2004
 *
 */
package org.vortikal.web.service;

import javax.servlet.http.HttpServletRequest;

/**
 */
public class RequestUriPrefixAssertion extends AssertionSupport implements RequestAssertion {

	private String prefix;
	
	/**
	 * @return Returns the prefix.
	 */
	public String getPrefix() {
		return prefix;
	}
	
	/**
	 * @param prefix The prefix to set.
	 */
	public void setPrefix(String prefix) {
            if (prefix == null) throw new IllegalArgumentException(
                "Prefix not specified, is null");

            if (!prefix.startsWith("/")) throw new IllegalArgumentException(
                "Prefix must start with a '/' character");

            if (prefix.endsWith("/")) throw new IllegalArgumentException(
                "Prefix must not end with a '/' character");

            this.prefix = prefix;
	}
    

	public boolean matches(HttpServletRequest request) {
            return request.getRequestURI().startsWith(prefix);
	}


	public boolean conflicts(Assertion assertion) {
		if (assertion instanceof RequestUriPrefixAssertion) {
			return ! (this.prefix.equals(
					((RequestUriPrefixAssertion)assertion).getPrefix()));
		}
		return false;
	}


    /** 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(super.toString());
		sb.append("; prefix = ").append(this.prefix);

		return sb.toString();
	}

}
