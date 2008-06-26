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
package org.vortikal.web.view.decorating;

import java.util.Map;

import org.springframework.web.servlet.View;


/**
 * Unchecked exception indicating that a problem occurred while
 * rendering a wrapped view. The MVC model and view objects that
 * failed are made available.
 * 
 * @see DecoratingViewWrapper
 */
public class ViewWrapperException extends RuntimeException {

    private static final long serialVersionUID = 2796471474145665926L;

    @SuppressWarnings("unchecked")
    private Map model = null;
    private View view = null;
    

    @SuppressWarnings("unchecked")
    public ViewWrapperException(Throwable cause, Map model, View view) {
        super(cause);
        this.model = model;
        this.view = view;
    }
    
    @SuppressWarnings("unchecked")
    public ViewWrapperException(String message, Throwable cause, Map model, View view) {
        super(message, cause);
        this.model = model;
        this.view = view;
    }
    
    @SuppressWarnings("unchecked")
    public Map getModel() {
        return this.model;
    }

    public View getView() {
        return this.view;
    }
}
