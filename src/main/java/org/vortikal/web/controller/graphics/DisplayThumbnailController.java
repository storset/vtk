package org.vortikal.web.controller.graphics;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class DisplayThumbnailController implements Controller {
	
	private static final Logger log = Logger.getLogger(DisplayThumbnailController.class);
	
	private Repository repository;

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();

        Resource image = this.repository.retrieve(token, uri, true);   
        Property thumbnail = image.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.THUMBNAIL_PROP_NAME);
        
        if (thumbnail == null) {
        	log.warn("No thumbnail was found for image: " + uri);
        	response.sendRedirect(uri.toString());
        }
        
        try {
        	
        	InputStream in = thumbnail.getBinaryStream();
        	BufferedImage imageFromStream = ImageIO.read(in);
        	in.close();
        	
        	String mimetype = thumbnail.getBinaryMimeType();
            response.setContentType(mimetype);
            
        	String format = mimetype.substring(mimetype.indexOf("/") + 1);
        	OutputStream out = response.getOutputStream();
            ImageIO.write(imageFromStream, format, out);
            out.flush();
            out.close();
            
        } catch (Throwable t) {
        	log.error("An error occured while regenerating thumbnail for image " + uri, t);
        	response.sendRedirect(uri.toString());
        }
        
		return null;
	}

	@Required
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

}
