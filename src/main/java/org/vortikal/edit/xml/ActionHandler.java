package org.vortikal.edit.xml;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface ActionHandler {

    /**
     * Edit handler method. Gets called after editing
     * session initialization (i.e. making sure that an editing
     * session actually exists, that the document is locked,
     * etc.).
     * 
     * @param request the servlet request
     * @param document the edit document
     * @return a model
     */
    public Map handle(HttpServletRequest request, EditDocument document,
            SchemaDocumentDefinition documentDefinition) 
    throws IOException, XMLEditException;

}
