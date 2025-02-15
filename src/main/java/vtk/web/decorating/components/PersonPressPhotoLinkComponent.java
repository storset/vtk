/* Copyright (c) 2011, University of Oslo, Norway
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
package vtk.web.decorating.components;

import java.util.Map;

import vtk.repository.Namespace;
import vtk.repository.Path;
import vtk.repository.Property;
import vtk.repository.Repository;
import vtk.repository.Resource;
import vtk.web.RequestContext;
import vtk.web.decorating.DecoratorRequest;
import vtk.web.decorating.DecoratorResponse;
import vtk.web.service.Service;

public class PersonPressPhotoLinkComponent extends ViewRenderingDecoratorComponent {

    private static final String PRESS_PHOTO_PROROPERTY_NAME = "pressPhoto";

    private Service viewAsWebpage;

    protected void processModel(Map<String, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Resource currentDocument = repository.retrieve(token, requestContext.getResourceURI(), true);

        if (!currentDocument.getResourceType().equals("person")) {
            return;
        }

        Property pictureProp = currentDocument.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE,
                PRESS_PHOTO_PROROPERTY_NAME);

        if (pictureProp == null) {
            return;
        }

        Path imageUri = null;
        Resource pictureResource = null;
        try {
            if (pictureProp.getStringValue().startsWith("/")) {
                imageUri = Path.fromString(pictureProp.getStringValue());
            } else {
                imageUri = requestContext.getCurrentCollection().expand(pictureProp.getStringValue());
            }
            pictureResource = repository.retrieve(token, imageUri, true);
        } catch (Exception e) {
            model.put(PRESS_PHOTO_PROROPERTY_NAME, pictureProp.getStringValue());
            return;
        }

        if (pictureResource != null && "image".equals(pictureResource.getResourceType())) {
            model.put(PRESS_PHOTO_PROROPERTY_NAME, getViewAsWebpage().constructLink(imageUri));
        } else {
            model.put(PRESS_PHOTO_PROROPERTY_NAME, pictureProp.getStringValue());
        }

    }

    public void setViewAsWebpage(Service viewAsWebpage) {
        this.viewAsWebpage = viewAsWebpage;
    }

    public Service getViewAsWebpage() {
        return viewAsWebpage;
    }
}
