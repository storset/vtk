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
package org.vortikal.web.actions;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.vortikal.graphics.ImageServiceImpl;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;

/**
 * Controller that saves image content on resource from base64
 */
public class SaveImageController extends AbstractController {

    private String viewName;
    private ImageServiceImpl imageService;

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    
    @Required
    public void setImageService(ImageServiceImpl imageService) {
        this.imageService = imageService;
    }
    
    @SuppressWarnings("unchecked")
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            HttpServletResponse response) throws Exception {     
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Resource resource = repository.retrieve(token, uri, true);

        String cropXStr = request.getParameter("crop-x");
        String cropYStr = request.getParameter("crop-y");
        String cropWidthStr = request.getParameter("crop-width");
        String cropHeightStr = request.getParameter("crop-height");
        String newWidthStr = request.getParameter("new-width");
        String newHeightStr = request.getParameter("new-height");
                
        if(cropXStr == null || cropYStr == null || cropWidthStr == null 
           ||  cropHeightStr == null ||  newWidthStr == null || newHeightStr == null) {
            return new ModelAndView(this.viewName);        
        }

        int cropX = Integer.parseInt(cropXStr);
        int cropY = Integer.parseInt(cropYStr);
        int cropWidth = Integer.parseInt(cropWidthStr);
        int cropHeight = Integer.parseInt(cropHeightStr);
        int newWidth = Integer.parseInt(newWidthStr);
        int newHeight = Integer.parseInt(newHeightStr);

        // Crop and scale (downscale bilinear and upscale bicubic)
        BufferedImage image = ImageIO.read(repository.getInputStream(token, uri, true)).getSubimage(cropX, cropY, cropWidth, cropHeight);
        BufferedImage scaledImage = imageService.getScaledInstance(image, newWidth, newHeight); // Bilinear

        // Find a writer
        ImageWriter writer = null;
        Iterator iter = null;
        if("image/jpeg".equals(resource.getContentType()) || "image/pjpeg".equals(resource.getContentType())) {
          iter = ImageIO.getImageWritersByFormatName("jpg");
        } else if("image/png".equals(resource.getContentType())) {
          iter = ImageIO.getImageWritersByFormatName("png");  
        }
        if (iter.hasNext()) {
          writer = (ImageWriter)iter.next();
        }

        // Prepare output file
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
        writer.setOutput(ios);
            
        // Set the compression quality
        if("image/jpeg".equals(resource.getContentType()) || "image/pjpeg".equals(resource.getContentType())) {
          ImageWriteParam iwp = writer.getDefaultWriteParam();
          iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
          iwp.setCompressionQuality(0.8f);
          writer.write(null, new IIOImage(scaledImage, null, null), iwp);
        } else {
          writer.write(new IIOImage(scaledImage, null, null));  
        }
        // Cleanup
        ios.flush();
        writer.dispose();
        ios.close();

        repository.storeContent(token, uri, new ByteArrayInputStream(bos.toByteArray()));

        return new ModelAndView(this.viewName);
    }

}
