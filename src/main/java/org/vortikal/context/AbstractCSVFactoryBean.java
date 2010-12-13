/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.context;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Abstract superclass for CSV style factory beans. Parses a comma separated string
 * into an array of the individual elements. Optionally trims each string element.
 * 
 * If no CSV list is set by calling {@link #setCsvList(String)}, 
 * the array of elements will be of length 0 (not <code>null</code>).
 *
 */
public abstract class AbstractCSVFactoryBean extends AbstractFactoryBean {

    protected String[] elements = new String[0];
    
    private boolean trim = true;
    
    public void setCsvList(String csvList) {
        if (csvList != null) {
            if ("".equals(csvList.trim())) {
                this.elements = new String[0];
                return;
            }
            this.elements = csvList.split(",");
            if (this.trim) {
                for (int i = 0; i < this.elements.length; i++) {
                    this.elements[i] = this.elements[i].trim();
                } 
            }
        } 
    }

    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    protected abstract Object createInstance() throws Exception;

    @SuppressWarnings("rawtypes")
    public abstract Class getObjectType();

}
