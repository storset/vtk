package org.vortikal.repository.resourcetype.property;

import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.graphics.ImageService;
import org.vortikal.graphics.ScaledImage;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.Principal;

public class ThumbnailEvaluator implements ContentModificationPropertyEvaluator {
	
	private static final Logger log = Logger.getLogger(ThumbnailEvaluator.class);
	
	private ImageService imageService;
	private String width;
	private Set<String> supportedFormats;

	public boolean contentModification(Principal principal, Property property,
			PropertySet ancestorPropertySet, Content content, Date time) throws PropertyEvaluationException {
		
        try {
            
            BufferedImage image = (BufferedImage) content.getContentRepresentation(BufferedImage.class);
            if (image == null) {
                return false;
            }

            Property contentType = ancestorPropertySet.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTTYPE_PROP_NAME);
            String mimetype = contentType.getStringValue();
            String imageFormat = mimetype.substring(mimetype.lastIndexOf("/") + 1);
            
            if (!supportedFormats.contains(imageFormat.toLowerCase())) {
            	log.warn("Unable to get create thumbnail, unsupported format: " + imageFormat);
            	return false;
            }
            
            ScaledImage thumbnail = imageService.scaleImage(image, imageFormat, width, "");
            
            // TODO lossy-compression -> jpeg
            String thumbnailFormat = !imageFormat.equalsIgnoreCase("png") ? "png" : imageFormat;
            
            String binaryRef = ancestorPropertySet.getURI().toString();
            property.setBinaryValue(thumbnail.getImageBytes(thumbnailFormat), binaryRef, "image/" + thumbnailFormat);
            return true;


        } catch (Exception e) {
            log.warn("Unable to get create thumbnail of content", e);
            return false;
        }
        
	}

	@Required
	public void setImageService(ImageService imageService) {
		this.imageService = imageService;
	}

	@Required
	public void setWidth(String width) {
		this.width = width;
	}
	
	@Required
	public void setSupportedFormats(Set<String> supportedFormats) {
		this.supportedFormats = supportedFormats;
	}

}
