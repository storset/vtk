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
package org.vortikal.edit.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.ProcessingInstruction;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.Xml;
import org.vortikal.web.RequestContext;


/**
 * The document being edited.
 */
public class EditDocument extends Document {

    private static final long serialVersionUID = 3256719598073361459L;

    private static Log logger = LogFactory.getLog(EditDocument.class);

    private Repository repository;
    
    private ProcessingInstruction pi = null;

    private String uri = null;

    private Element element = null;

    private Element clone = null;

    private String newElementName = null;

    private Vector elements = null;

    private Resource resource;

    EditDocument(Element root, DocType docType, Resource resource, Repository repository) {
        super(root, docType);
        this.resource = resource;
        this.repository = repository;
        this.uri = resource.getURI();
        this.setBaseURI(resource.getURI());
    }


    public static EditDocument createEditDocument(Repository repository, int lockTimeoutSeconds)
        throws JDOMException, IOException {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        String uri = requestContext.getResourceURI();
        
        repository.lock(token, uri, principal.getQualifiedName(), "0", lockTimeoutSeconds, null);

        Resource resource = repository.retrieve(token, uri, false);
        
        if (logger.isDebugEnabled())
            logger.debug("Locked resource '" + uri + "', principal = '" + principal + "'");

        SAXBuilder builder = 
            new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        builder.setValidation(true);

        /* turn on schema support */
        builder.setFeature("http://apache.org/xml/features/validation/schema",
                true);

        builder.setXMLFilter(new XMLSpaceCorrectingXMLFilter());
        
        Document document = builder.build(repository.getInputStream(token, uri, false));

        Element root = document.getRootElement();
        root.detach();
        return new EditDocument(root, document.getDocType(), resource, repository);
    }



    protected boolean hasProcessingInstruction(String target) {
        for (Iterator i = getContent().iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof ProcessingInstruction) {
                ProcessingInstruction p = (ProcessingInstruction) o;
                if (p.getTarget().equals(target)) { return true; }
            }
        }
        return false;
    }



    public void save() throws XMLEditException, IOException {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        
        String token = securityContext.getToken();
        String uri = requestContext.getResourceURI();
        
        if (logger.isDebugEnabled())
                logger.debug("Saving document '" + uri + "'");

        new Validator().validate(this);
        
        removeProcessingInstructions();

        Format format = Format.getRawFormat();
        format.setLineSeparator("\n");

        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(format);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        xmlOutputter.output(this, outputStream);
        
        InputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
        repository.storeContent(token, uri, stream);

        // Fix character encoding if it is something other than UTF-8:
        if (this.resource.getCharacterEncoding() != null) {
            String encoding = this.resource.getCharacterEncoding().toLowerCase();
            if (!"utf-8".equals(encoding)) {
                this.resource.setUserSpecifiedCharacterEncoding("utf-8");
                repository.store(token, this.resource);
            }
        }

        this.resource = repository.retrieve(token, uri, false);
        if (logger.isDebugEnabled())
            logger.debug("saved document '" + uri + "'");
    }



    public void finish() throws IOException {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        
        String token = securityContext.getToken();
        String uri = requestContext.getResourceURI();

        repository.unlock(token, uri, null);
    }

    public String getDocumentMode() {
        ProcessingInstruction pi = Xml.findProcessingInstruction(getRootElement(), "mode");
        if (pi != null)
            return pi.getData(); 

        return "default";
    }

    public void setDocumentMode(String mode) {
        Element rootElement = getRootElement();
        ProcessingInstruction pi = Xml.findProcessingInstruction(rootElement, "mode");
        if (pi != null) {
            rootElement.removeContent(pi);
        }
        if (!"default".equals(mode)) {
            rootElement.addContent(new ProcessingInstruction("mode", mode));
        }
    }

    public boolean hasDocumentPI() {
        return this.pi != null;
    }



    public void setDocumentPI(ProcessingInstruction newPI) {
        this.pi = newPI;
        addContent(newPI);
    }



    public void removeDocumentPI() {
        removeContent(this.pi);
        this.pi = null;
    }

    public Element getEditingElement() {
        return this.element;
    }



    public void setEditingElement(Element e) {
        this.element = e;
    }



    public void resetEditingElement() {
        ProcessingInstruction pi = null;
        for (Iterator i = this.element.getContent().iterator(); i.hasNext();) {
            Object o = i.next();
            if ((o instanceof ProcessingInstruction)
                    && "expanded".equals((((ProcessingInstruction) o)
                            .getTarget()))) {
                pi = (ProcessingInstruction) o;
            }
        }
        this.element.removeContent(pi);
        this.element = null;
    }



    public Vector getElements() {
        return this.elements;
    }



    public void setElements(Vector e) {
        this.elements = e;
    }



    public void resetElements(Vector vector) {
        // FIXME: lag denne! brukes under og fra edithandler!
        Enumeration enumeration = vector.elements();
        HashMap removalSet = new HashMap();

        while (enumeration.hasMoreElements()) {
            Object ob = enumeration.nextElement();

            Element elem = (Element) ob;//enum.nextElement();
            logger.debug("Resetting element " + elem.getName());

            for (Iterator i = elem.getContent().iterator(); i.hasNext();) {
                Object o = i.next();
                if (o instanceof ProcessingInstruction) {
                    removalSet.put(elem, o);
                }
            }
        }
        for (Iterator i = removalSet.keySet().iterator(); i.hasNext();) {
            Element e = (Element) i.next();
            ProcessingInstruction pi = (ProcessingInstruction) removalSet
                    .get(e);
            e.removeContent(pi);
        }
    }



    public void resetElements() {
        resetElements(this.elements);
        this.elements = null;
    }



    protected void removeProcessingInstructions() {
        Stack s = new Stack();
        for (Iterator i = getContent().iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof ProcessingInstruction) {
                s.push(o);
            }
        }
        while (s.size() > 0) {
            ProcessingInstruction i = (ProcessingInstruction) s.pop();
            removeContent(i);
        }
    }



    /**
     * Go through all descendants of element, and map input against leaf nodes
     * 
     * @param element
     * @param parameters
     * @param documentDefinition
     */
    public void addContentsToElement(Element element, Map parameters,
            SchemaDocumentDefinition documentDefinition) {

        addAttributesToElement(element, parameters);

        Map modifiedElements = new HashMap();


        String path = Xml.createNumericPath(element);
        String input = (String) parameters.get(path);
        if (input != null) {
            modifiedElements.put(element, input);
        }

        for (Iterator iter = element.getDescendants(); iter.hasNext();) {
            Object o = iter.next();

            if (o instanceof Element) {
                Element e = (Element) o;
                path = Xml.createNumericPath(e);
                input = (String) parameters.get(path);
                if (input != null) {
                    modifiedElements.put(e, input);
                }
            }
        }

        for (Iterator iterator = modifiedElements.keySet().iterator(); iterator
                .hasNext();) {
            Element e = (Element) iterator.next();

            documentDefinition.setElementContents(
                e, (String) modifiedElements.get(e));
        }
    }



    private void addAttributesToElement(Element element, Map parameters) {
        for (Iterator iter = parameters.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            /*
             * If the input parameter is on the path of the current element and
             * matches the attribute syntax.
             */
            if (key.matches(Xml.createNumericPath(element)
                    + "(\\.\\d+)*:[a-zA-Z].*")) {
                String elementPath = key.substring(0, key.indexOf(":"));
                String attributeName = key.substring(key.indexOf(":") + 1);
                Element e = findElementByPath(elementPath);
 
                if (e == null)
                    throw new EditException(
                        "The document does not contain an element with path "
                        + elementPath, this.uri);
                e.setAttribute(attributeName, (String) parameters.get(key));
            }
        }
    }


    /**
     * Finds elements in document based on "numeric path" (element positions
     * separated by dots). An example of such a path is "1.3.9", which means
     * "the ninth element of the third element of the first (root) element".
     * 
     * @param path
     *            the path to search
     * @return an <code>Element</code> if found, <code>null</code>
     *         otherwise
     */
    public Element findElementByPath(String path) {
        return Xml.findElementByNumericPath(this, path);
    }



    public void putElementByPath(String path, Element e) {
        Element currentElement = getRootElement();
        String currentPath = new String(path);
        if (currentPath.indexOf(".") >= 0) {
            // Strip away the leading '1.' (root element)
            currentPath = currentPath.substring(2, currentPath.length());
        }
        while (true) {
            int index = 0;
            if (currentPath.indexOf(".") == -1) {
                index = Integer.parseInt(currentPath);
            } else {
                index = Integer.parseInt(currentPath.substring(0, currentPath
                        .indexOf(".")));
            }
            if (currentPath.indexOf(".") == -1) {
                /* Found the parent element. Put child elements and
                 * processing instructions into a list */
                List l = new ArrayList(
                    currentElement.getContent(
                        new Filter() {
                            public boolean matches(Object o) {
                                return (o instanceof ProcessingInstruction)
                                    || (o instanceof Element);
                            }
                            private static final long serialVersionUID = 4746825449858085648L;
                        }
                    ));

                /* Add the new element to the list at the specified
                 * index: */
                l.add(index, e);                

                /* Remove old content from parent, replace it with the
                 * list: */
                currentElement.removeContent();
                currentElement.setContent(l);
                break;
            }
            currentElement = (Element) currentElement.getChildren().get(
                    index - 1);
            currentPath = currentPath.substring(currentPath.indexOf(".") + 1,
                    currentPath.length());
        }
    }


    /**
     * @return Returns the clone.
     */
    public Element getClone() {
        return this.clone;
    }


    /**
     * @param clone The clone to set.
     */
    public void setClone(Element clone) {
        this.clone = clone;
    }


    /**
     * @return Returns the newElementName.
     */
    public String getNewElementName() {
        return this.newElementName;
    }


    /**
     * @param newElementName The newElementName to set.
     */
    public void setNewElementName(String newElementName) {
        this.newElementName = newElementName;
    }
    
    /**
     * @return Returns the resource.
     */
    public Resource getResource() {
        return this.resource;
    }
    
    public String toStringDetail() {
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        
        return outputter.outputString(this);
    }
}
