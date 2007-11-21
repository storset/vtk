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
package org.vortikal.web.commenting;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.EnclosingHtmlContent;
import org.vortikal.text.html.HtmlComment;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlElementDescriptor;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.html.HtmlText;
import org.vortikal.text.html.SimpleHtmlPageFilter;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;


/**
 * Gets a comment from form input and adds it to the current
 * resource. Optionally stores the binding errors object in the
 * session.
 */
public class PostCommentController extends SimpleFormController {

    private Repository repository = null;
    private String formSessionAttributeName;
    private HtmlPageParser parser;
    private int maxCommentLength = 100000;
    private boolean requireCommentTitle = false;
    
    private Set<String> illegalElements = new HashSet<String>();
    private Set<HtmlElementDescriptor> validElements = new HashSet<HtmlElementDescriptor>();


    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required public void setHtmlParser(HtmlPageParser parser) {
        this.parser = parser;
    }
    
    public void setFormSessionAttributeName(String formSessionAttributeName) {
        this.formSessionAttributeName = formSessionAttributeName;
    }

    @Required public void setIllegalElements(Set<String> illegalElements) {
        for (String elem: illegalElements) {
            this.illegalElements.add(elem);
        }
    }
    
    @Required public void setValidElements(Set<HtmlElementDescriptor> validElements) {
        for (HtmlElementDescriptor desc: validElements) {
            this.validElements.add(desc);
        }
    }

    public void setMaxCommentLength(int maxCommentLength) {
        this.maxCommentLength = maxCommentLength;
    }
    
    public void setRequireCommentTitle(boolean requireCommentTitle) {
        this.requireCommentTitle = requireCommentTitle;
    }
    

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        Resource resource = this.repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        URL url = service.constructURL(resource, securityContext.getPrincipal());
        PostCommentCommand command = new PostCommentCommand(url);
        return command;
    }


    protected void onBindAndValidate(HttpServletRequest request, Object command,
                          BindException errors) throws Exception {

        if (!"POST".equals(request.getMethod()) && this.formSessionAttributeName != null) {
            if (request.getSession(false) != null) {
                request.getSession().removeAttribute(this.formSessionAttributeName);
            }
        }

        PostCommentCommand commentCommand = (PostCommentCommand) command;
        if (commentCommand.getCancelAction() != null) return;

        if (this.requireCommentTitle &&
            (commentCommand.getTitle() == null
             || commentCommand.getTitle().trim().equals(""))) {
            errors.rejectValue("title", "commenting.post.title.missing",
                               "You must provide a title");
        }
        if (commentCommand.getText() == null
            || commentCommand.getText().trim().equals("")) {
            errors.rejectValue("text", "commenting.post.text.missing",
                               "You must type something in the comment field");
        } else if (commentCommand.getText().length() > this.maxCommentLength) {
            errors.rejectValue("text", "commenting.post.text.toolong",
                               new Object[] {commentCommand.getText().length(), this.maxCommentLength},
                               "Value too long: maximum length is " + this.maxCommentLength);
        }
        String parsedText = parseContent(commentCommand.getText());
        if (parsedText == null || "".equals(parsedText.trim())) {
            errors.rejectValue("text", "commenting.post.text.missing",
                               "You must type something in the comment field");
        }
        commentCommand.setParsedText(parsedText);

        if (this.formSessionAttributeName == null) {
            return;
        }
        if (errors.hasErrors()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("form", command);
            map.put("errors", errors);
            request.getSession(true).setAttribute(this.formSessionAttributeName, map);
        } else {
            if (request.getSession(false) != null) {
                request.getSession().removeAttribute(this.formSessionAttributeName);
            }
        }
    }
    

    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        Resource resource = this.repository.retrieve(token, uri, false);
        
        PostCommentCommand commentCommand = (PostCommentCommand) command;
        if (commentCommand.getCancelAction() != null) {
            commentCommand.setDone(true);
            return;
        }
        String title = commentCommand.getTitle();
        String text = commentCommand.getParsedText();
        repository.addComment(token, resource, title, text);
    }


    protected String parseContent(String text) throws Exception {
        if (this.parser != null) {
            text = "<html><head></head><body>" + text + "</body></html>";
            InputStream is = new ByteArrayInputStream(text.getBytes("utf-8"));
            HtmlPage page = this.parser.parse(is, "utf-8");

            page.filter(new SimpleHtmlPageFilter(this.illegalElements, this.validElements, false));

            if (isEmptyContent(page.getRootElement())) {
                return null;
            }

            HtmlContent[] toplevel = page.getRootElement().getChildNodes();
            StringBuilder result = new StringBuilder();

            List<HtmlContent> nodes = trimChildren(page.getRootElement());
            for (int i = 0; i < nodes.size(); i++) {
                HtmlContent c = nodes.get(i);
                
                String childContent = "";

                if (c instanceof HtmlElement) {
                    childContent = ((HtmlElement) c).getEnclosedContent();
                } else if (c instanceof HtmlText) {
                    childContent = c.getContent();
                } 
                if ((i == 0 || i == toplevel.length - 1) && !childContent.trim().startsWith("<")) {
                    // Wrap top-level text nodes in a <p>:
                    result.append("<p>").append(childContent).append("</p>");
                } else {
                    result.append(childContent);
                }
            }
            return result.toString();
        }
        return text;
    }

    private List<HtmlContent> trimChildren(HtmlElement root) {
        List<HtmlContent> result = new ArrayList<HtmlContent>();
        boolean contentBegun = false;
        for (HtmlContent child: root.getChildNodes()) {
            if (!contentBegun && (child instanceof HtmlText)) {
                String childContent = child.getContent();
                if (childContent.trim().equals("")) {
                    continue;
                } else if (!contentBegun) {
                    contentBegun = true;
                }
            }
            result.add(child);
        }

        for (int i = result.size() - 1; i > 0; i--) {
            HtmlContent c = result.get(i);
            if (c instanceof HtmlText && "".equals(c.getContent())) {
                result.remove(i);
            }
        }

        return result;
    }
    
    
    private boolean isEmptyContent(HtmlContent node) {
        if (node instanceof EnclosingHtmlContent) {
            HtmlContent[] children = ((EnclosingHtmlContent) node).getChildNodes();
            for (HtmlContent child: children) {
                if (!isEmptyContent(child)) {
                    return false;
                }
            }
            return true;
        } else if (node instanceof HtmlText) {
            return "".equals(node.getContent().trim());

        } else if (node instanceof HtmlComment) {
            return true;
        }
        return false;
    }
    
}
