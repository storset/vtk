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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.text.html.EnclosingHtmlContent;
import org.vortikal.text.html.HtmlComment;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlElementImpl;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.html.HtmlText;
import org.vortikal.text.html.HtmlUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

/**
 * Gets a comment from form input and adds it to the current resource.
 * Optionally stores the binding errors object in the session.
 */
public class PostCommentController extends SimpleFormController {

    private String formSessionAttributeName;
    private HtmlPageParser parser;
    private HtmlPageFilter htmlFilter;
    private int maxCommentLength = 10000;
    private boolean requireCommentTitle = false;

    @Required
    public void setHtmlParser(HtmlPageParser parser) {
        this.parser = parser;
    }

    public void setFormSessionAttributeName(String formSessionAttributeName) {
        this.formSessionAttributeName = formSessionAttributeName;
    }

    @Required
    public void setHtmlFilter(HtmlPageFilter htmlFilter) {
        this.htmlFilter = htmlFilter;
    }

    public void setMaxCommentLength(int maxCommentLength) {
        this.maxCommentLength = maxCommentLength;
    }

    public void setRequireCommentTitle(boolean requireCommentTitle) {
        this.requireCommentTitle = requireCommentTitle;
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Service service = requestContext.getService();
        Resource resource = repository.retrieve(token, 
                requestContext.getResourceURI(), false);
        URL url = service.constructURL(resource, requestContext.getPrincipal());
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
        if (commentCommand.getCancelAction() != null)
            return;

        if (this.requireCommentTitle && StringUtils.isBlank(commentCommand.getTitle())) {
            errors.rejectValue("title", "commenting.post.title.missing",
                            "You must provide a title");
        }

        String commentText = commentCommand.getText();
        if (StringUtils.isBlank(commentText)) {
            errors.rejectValue("text", "commenting.post.text.missing",
                    "You must type something in the comment field");
        }
        
        String sanitizedText = sanitizeContent(commentText);

        // TODO Should only use OWASP library for parsing/cleaning content (avoid two stages doing essentially the same thing).
        
        String parsedText = parseContent(sanitizedText);
        if (StringUtils.isBlank(parsedText)) {
            errors.rejectValue("text", "commenting.post.text.missing",
                    "You must type something in the comment field");
        } else if (parsedText.length() > this.maxCommentLength) {
            errors.rejectValue("text", "commenting.post.text.toolong", new Object[] {
                    parsedText.length(), this.maxCommentLength },
                    "Value too long: maximum length is " + this.maxCommentLength);
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
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Resource resource = repository.retrieve(token, uri, false);

        PostCommentCommand commentCommand = (PostCommentCommand) command;
        if (commentCommand.getCancelAction() != null) {
            commentCommand.setDone(true);
            return;
        }
        String title = commentCommand.getTitle();
        String text = commentCommand.getParsedText();
        repository.addComment(token, resource, title, text);
    }
    
    /**
     * Initial integration of owasp library
     */
    protected String sanitizeContent(String text) throws Exception {
        logger.debug("Text before sanitizing: '" + text + "'");
        PolicyFactory policy = new HtmlPolicyBuilder()
        	.allowStandardUrlProtocols()
        	.allowAttributes("href").onElements("a")
        	.allowElements("a", "p", "ul", "li", "ol", "em", "strong", "cite", "code", "strike", "u")
        	.toFactory();
        String sanitizedText = policy.sanitize(text);
        logger.debug("After sanitizing: '" + sanitizedText + "'");
        return sanitizedText;
    }

    protected String parseContent(String text) throws Exception {
        if (this.parser != null) {
            HtmlFragment fragment = this.parser.parseFragment(text);
            fragment.filter(this.htmlFilter);
            List<HtmlContent> nodes = fragment.getContent();
            boolean empty = true;
            for (HtmlContent c : nodes) {
                if (!isEmptyContent(c)) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                return null;
            }
            nodes = trimNodes(nodes);

            List<HtmlContent> content = new ArrayList<HtmlContent>();
            HtmlElement currentParagraph = new HtmlElementImpl("p", true, false);
            content.add(currentParagraph);
            for (HtmlContent c : nodes) {
                if (c instanceof HtmlElement && "p".equals(((HtmlElement) c).getName())) {
                    content.add(c);
                    currentParagraph = new HtmlElementImpl("p", true, false);
                    content.add(currentParagraph);
                } else {
                    currentParagraph.addContent(c);
                }
            }

            StringBuilder result = new StringBuilder();
            for (HtmlContent c : content) {
                if (c instanceof HtmlElement) {
                    result.append(((HtmlElement) c).getEnclosedContent());
                } else if (c instanceof HtmlText) {
                    result.append(HtmlUtil.escapeHtmlString(c.getContent()));
                }
            }
            return result.toString();
        }
        return text;
    }


    private List<HtmlContent> trimNodes(List<HtmlContent> nodes) {
        List<HtmlContent> result = new ArrayList<HtmlContent>();
        boolean contentBegun = false;
        for (HtmlContent node : nodes) {
            if (!contentBegun && (node instanceof HtmlText)) {
                String childContent = node.getContent();
                if (childContent.trim().equals("")) {
                    continue;
                } else if (!contentBegun) {
                    contentBegun = true;
                }
            }
            result.add(node);
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
            for (HtmlContent child : children) {
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
