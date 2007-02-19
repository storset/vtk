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

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.Remark;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.tags.DoctypeTag;
import org.htmlparser.tags.Html;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.NodeVisitor;

import org.vortikal.web.view.decorating.HtmlComment;
import org.vortikal.web.view.decorating.HtmlContent;
import org.vortikal.web.view.decorating.HtmlElement;
import org.vortikal.web.view.decorating.HtmlNodeFilter;
import org.vortikal.web.view.decorating.HtmlPage;
import org.vortikal.web.view.decorating.HtmlPageParser;
import org.vortikal.web.view.decorating.HtmlPageParserException;
import org.vortikal.web.view.decorating.HtmlText;


public class HtmlPageParserImpl implements HtmlPageParser {

    private String defaultDoctype =
        "html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
        + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"";
    

    public void setDefaultDoctype(String defaultDoctype) {
        this.defaultDoctype = defaultDoctype;
    }


    public HtmlPage parse(InputStream in, String encoding) throws Exception {
        HtmlNodeFilter filter = new HtmlNodeFilter() {
           public HtmlContent filterNode(HtmlContent node) {
              return node;
           }
        };
        return parse(in, encoding, filter);
    }
    

    public HtmlPage parse(InputStream in, String encoding, HtmlNodeFilter filter)
        throws Exception {
        Page page = new Page(in, encoding);

        Lexer lexer = new Lexer(page);

        Parser parser = new Parser(lexer);
        NodeList nodeList = parser.parse(null);

        Node root = findRootNode(nodeList);
        String doctype = findDoctype(nodeList);
        if (doctype == null) {
            doctype = this.defaultDoctype;
        }
        boolean xhtml = isXhtml(doctype);

        HtmlElement rootElement = (HtmlElement) buildHtml(root, filter, xhtml);
        if (rootElement == null) {
            throw new HtmlPageParserException("Unable to parse input: no root element");
        }
        return new HtmlPageImpl(rootElement, doctype);
    }


    private boolean isXhtml(String doctype) {
        boolean xhtml = doctype.toUpperCase().startsWith(
            "HTML PUBLIC \"-//W3C//DTD XHTML");
        return xhtml;
    }
    

    private Node findRootNode(NodeList nodeList) {
        Node root = null;
        for (int i = 0; i < nodeList.size(); i++) {
            Node node = nodeList.elementAt(i);
            if (node instanceof DoctypeTag) {
                continue;
            } else if (node instanceof Tag) {
                root = node;
                break;
            }
        }
        return root;
    }
    
    private String findDoctype(NodeList nodeList) {
        for (int i = 0; i < nodeList.size(); i++) {
            Node node = nodeList.elementAt(i);
            if (node != null && node instanceof DoctypeTag) {
                DoctypeTag doctypeTag = (DoctypeTag) node;
                String text = doctypeTag.getText();
                if (text != null && text.startsWith("!DOCTYPE ")) {
                    text = text.substring("!DOCTYPE ".length());
                }
                return text;
            }
        }
        return null;
    }
    


    private void addAttributes(HtmlElementImpl element, Tag tag) {
        String name = tag.getRawTagName();
        Vector attrs = tag.getAttributesEx();
        for (int i = 0; i < attrs.size(); i++) {
            Attribute attr = (Attribute) attrs.get(i);
            if (attr != null && !attr.isWhitespace()) {
                String attrName = attr.getName();
                if (attrName != null && !name.equals(attrName) && !"/".equals(attrName)) {
                    String attrValue = attr.getValue();
                    element.addAttribute(new HtmlAttributeImpl(attrName, attrValue));
                }
            }
        }
    }
    

    private HtmlContent buildHtml(Node node, HtmlNodeFilter filter, boolean xhtml) {

        if (node instanceof Tag) {
            Tag tag = (Tag) node;
            String name = tag.getRawTagName();
            boolean empty = tag.isEmptyXmlTag() || tag.getEndTag() == null;
            HtmlElementImpl element = new HtmlElementImpl(name, empty, xhtml);

            if (tag.isEndTag()) {
                return null;
            } 

            addAttributes(element, tag);
            
            NodeList children = tag.getChildren();
            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    Node childNode = children.elementAt(i);

                    // Handle "flattened" nodes (tags that should be
                    // nested, but aren't):

                    if (i <= children.size() - 3) {
                        Node firstSibling = children.elementAt(i + 1);
                        Node nextSibling = children.elementAt(i + 2);

                        if (childNode instanceof Tag
                            && firstSibling != null
                            && nextSibling != null
                            && firstSibling instanceof Text
                            && nextSibling instanceof Tag
                            && ((Tag) nextSibling).isEndTag()
                            && ((Tag) nextSibling).getTagName()
                               .equals(((Tag)childNode).getTagName())) {

                            HtmlElementImpl child = new HtmlElementImpl(
                                ((Tag) childNode).getRawTagName(), false, xhtml);

                            if (child != null) {
                                HtmlText textNode = new HtmlTextImpl(
                                    ((Text) firstSibling).getText());
                                child.addContent(textNode);
                                element.addContent(child);

                                i+=2; // Skip the text node and end tag:
                            }
                            continue;
                        }
                    } 
                    HtmlContent child = buildHtml(children.elementAt(i), filter, xhtml);
                    if (child != null) {
                        element.addContent(child);
                    }

                }
            }
            return filter.filterNode(element);

        } else if (node instanceof Text) {
            Text text = (Text) node;
            HtmlTextImpl textNode = new HtmlTextImpl(text.getText());
            return filter.filterNode(textNode);
            
        } else if (node instanceof Remark) {
            Remark remark = (Remark) node;
            HtmlCommentImpl comment = new HtmlCommentImpl(
                new HtmlTextImpl(remark.getText()), remark.toHtml());
            return filter.filterNode(comment);
        }
        // Unhandled node type:
        return null;
    }
}
