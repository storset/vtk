/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.actions.permissions;
public class ACLEditValidation {
    
    // TODO: hmm.. how can this be done nicer? which data-structure?
    private boolean notFound = false;
    private boolean illegalBlacklisted = false;
    private boolean illegal = false;
    
    public ACLEditValidation(boolean notFound, boolean illegalBlaclisted, boolean illegal) {
        this.notFound = notFound;
        this.illegalBlacklisted = illegalBlaclisted;
        this.illegal = illegal;
    }
    
    public boolean isNotFound() {
        return notFound;
    }
    public void setNotFound(boolean notFound) {
        this.notFound = notFound;
    }
    public boolean isIllegalBlacklisted() {
        return illegalBlacklisted;
    }
    public void setIllegalBlacklisted(boolean illegalBlacklisted) {
        this.illegalBlacklisted = illegalBlacklisted;
    }
    public boolean isIllegal() {
        return illegal;
    }
    public void setIllegal(boolean illegal) {
        this.illegal = illegal;
    }
    
    public boolean isValid() {
        return !this.notFound && !this.illegalBlacklisted && !this.illegal;
    }
    
}
