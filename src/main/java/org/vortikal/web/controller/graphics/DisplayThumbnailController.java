package org.vortikal.web.controller.graphics;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class DisplayThumbnailController implements Controller {
	
	private static final Logger log = Logger.getLogger(DisplayThumbnailController.class);
	
	private Repository repository;

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();

        Resource image = this.repository.retrieve(token, uri, true);        
        Property thumbnail = this.repository.loadBinaryContent(image);
        
        if (thumbnail == null) {
        	// No thumbnail for this image...
        	// TODO handle properly -> how?
        	//   * return original image?
        	//   * resize image "on-the-run"?
        	log.warn("No thumbnail was found for resource: " + uri);
        	return null;
        }
        
        String mimetype = thumbnail.getBinaryMimeType();
        response.setContentType(mimetype);
        
        byte[] binaryContent = thumbnail.getBinaryValue();
        ByteArrayInputStream in = new ByteArrayInputStream(binaryContent);
    	BufferedImage imageFromBytes = ImageIO.read(in);
    	in.close();
        
    	String format = mimetype.substring(mimetype.indexOf("/") + 1);
    	OutputStream out = response.getOutputStream();
        ImageIO.write(imageFromBytes, format, out);
        out.flush();
        out.close();
        
		return null;
	}
	
	@Required
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

}
