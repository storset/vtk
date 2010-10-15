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
package org.vortikal.text.html;


public interface HtmlElement extends EnclosingHtmlContent {

    /**
     * Gets the name of this element.
     */
    public String getName();


    /**
     * Gets the attributes of this element.
     */
    public HtmlAttribute[] getAttributes();

    
    /**
     * Sets the attributes of this element.
     */
    public void setAttributes(HtmlAttribute[] attributes);
    
    
    /**
     * Adds an attribute to this element.
     */
    public void addAttribute(HtmlAttribute attribute);
    
    /**
     * Adds or replaces an attribute on this element.
     */
    public void setAttribute(HtmlAttribute attribute);

    
    /**
     * Gets a named attribute of this element.
     */
    public HtmlAttribute getAttribute(String name);


    /**
     * Gets the child elements of this element.
     */
    public HtmlElement[] getChildElements();


    /**
     * Gets named child elements of this element.
     *
     * @parameter name the element name 
     */
    public HtmlElement[] getChildElements(String name);


    /**
     * Gets the child nodes of this node. A filter is applied to
     * decide inclusion of child nodes.
     * 
     * @param filter the node filter to apply
     */
    public HtmlContent[] getChildNodes(HtmlNodeFilter filter);


    /**
     * Sets the child nodes of this node.
     */
    public void setChildNodes(HtmlContent[] childNodes);
    

    /**
     * Adds a node at the end of this node's list of children.
     */
    public void addContent(HtmlContent child);
    

    /**
     * Adds a node at a specified position in this node's list 
     * of children.
     */
    public void addContent(int pos, HtmlContent child);

    
    /**
     * Removes the specified child node.
     */
    public void removeContent(HtmlContent child);
    
    /**
     * Gets the contents of this node as a string. A filter is applied
     * to decide inclusion of child content.
     *
     * @param filter the node filter to apply
     */
    public String getContent(HtmlNodeFilter filter);


    /**
     * Gets the contents of this node (including child nodes) with the
     * enclosing tag and attributes as a string. A filter is applied
     * to decide inclusion of child content.
     * 
     * @param filter the node filter to apply
     */
    public String getEnclosedContent(HtmlNodeFilter filter);


}
