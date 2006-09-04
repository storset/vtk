package org.vortikal.edit.xml;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface ActionHandler {

    /**
     * Internal request handler method. Gets called after editing
     * session initialization (i.e. making sure there an editing
     * session actually exists, that the document is locked,
     * etc.). Subclasses must implement this method.
     * 
     * @param request the servlet request
     * @param response the servlet response
     * @return a model and view
     */
    public Map handleRequestInternal(
            HttpServletRequest request, 
            EditDocument document,
            SchemaDocumentDefinition documentDefinition) throws IOException, XMLEditException;

}
