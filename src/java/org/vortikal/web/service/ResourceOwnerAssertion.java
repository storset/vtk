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

import org.vortikal.repository.Resource;

/**
 *
 */
public class ResourceOwnerAssertion extends AssertionSupport implements ResourceAssertion {

    private String owner = "";


	/**
	 * @return Returns the owner.
	 */
	public String getOwner() {
		return owner;
	}

	
	public void setOwner(String owner) {
        if (owner == null) throw new IllegalArgumentException(
            "Property 'owner' cannot be null");
        
        this.owner = owner;
    }
    

    public boolean matches(Resource resource) {
        return owner.equals(resource.getOwner());
    }


    public boolean conflicts(Assertion assertion) {
		if (assertion instanceof ResourceOwnerAssertion) {
			return ! (this.owner.equals(
					((ResourceOwnerAssertion)assertion).getOwner()));
		}
		return false;
	}


	/** 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(super.toString());
		sb.append("; owner = ").append(this.owner);

		return sb.toString();
	}

}
