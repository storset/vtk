/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating.htmlparser;



import com.opensymphony.module.sitemesh.HTMLPage;
import java.util.HashMap;
import java.util.Map;
import org.vortikal.web.view.decorating.HtmlPage;
import org.vortikal.web.view.decorating.HtmlElement;


public class HtmlPageImpl implements HtmlPage {

    private HtmlElement html;
    private HtmlElement head;
    private HtmlElement title;
    private HtmlElement body;
    

    public HtmlPageImpl(HtmlElement html, HtmlElement head,
                        HtmlElement title, HtmlElement body) {
        this.html = html;
        this.head = head;
        this.title = title;
        this.body = body;
    }
    

    public HtmlElement getHtmlElement() {
        return this.html;
    }
    
    
    public HtmlElement getHeadElement() {
        return this.head;
    }
    

    public HtmlElement getTitleElement() {
        return this.title;
    }
    

    public HtmlElement getBodyElement() {
        return this.body;
    }
    
}
