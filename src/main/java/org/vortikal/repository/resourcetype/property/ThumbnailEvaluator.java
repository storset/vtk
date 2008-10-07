package org.vortikal.repository.resourcetype.property;

import java.awt.image.BufferedImage;
import java.util.Date;

import org.apache.log4j.Logger;
import org.vortikal.graphics.ImageService;
import org.vortikal.graphics.ScaledImage;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.security.Principal;

public class ThumbnailEvaluator implements ContentModificationPropertyEvaluator {
	
	private static final Logger log = Logger.getLogger(ThumbnailEvaluator.class);
	
	private ImageService imageService;
	private String width;

	public boolean contentModification(Principal principal, Property property,
			PropertySet ancestorPropertySet, Content content, Date time) throws PropertyEvaluationException {
		
        try {
            
            BufferedImage image = (BufferedImage) content.getContentRepresentation(BufferedImage.class);
            if (image == null) {
                return false;
            }

            String imageFormat = ancestorPropertySet.getURI().toString();
            imageFormat = imageFormat.substring(imageFormat.lastIndexOf(".") + 1);
            ScaledImage thumbnail = imageService.scaleImage(image, imageFormat, width, "");
            property.setBinaryValue(thumbnail.getImageBytes());
            return true;


        } catch (Exception e) {
            log.warn("Unable to get BufferedImage representation of content", e);
            return false;
        }
        
	}

	public void setImageService(ImageService imageService) {
		this.imageService = imageService;
	}

	public void setWidth(String width) {
		this.width = width;
	}

}
