package org.vortikal.graphics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.security.SecurityContext;

public class ImageServiceImpl implements ImageService {
        
    private Repository repository;
    
    public ScaledImage scaleImage(String path, String width, String height) throws IOException {
    	BufferedImage originalImage = getImage(path);
    	String format = path.substring(path.lastIndexOf(".") + 1);
        return scaleImage(originalImage, format, width, height);
    }

    public ScaledImage scaleImage(BufferedImage image, String format, String width, String height) throws IOException {
        Dimension scaleDimension = getDimension(width, height, image);
        BufferedImage scaledImage = scaleImage(image, scaleDimension.width, scaleDimension.height);
        return new ScaledImage(scaledImage, format);
    }
    
    private BufferedImage getImage(String path) throws IOException {
        
        BufferedImage originalImage = null;
        
        if (isRepoImage(path)) {
        	String token = SecurityContext.getSecurityContext().getToken();
            InputStream imageStream = repository.getInputStream(token, Path.fromString(path), true);
            originalImage = ImageIO.read(imageStream);
        } else if (isWebImage(path)) {
            originalImage = ImageIO.read(new URL(path));
        } else {
            throw new IOException("Can not fetch requested image, uknown pathformat: '" + path + "'");
        }
        
        return originalImage;
    }
    
    private boolean isRepoImage(String path) {
        return path.matches("^(/)\\S*\\.(?i:png|gif|jp(e?)g)$");
    }
    
    private boolean isWebImage(String path) {
        return path.matches("^(http(s?)\\:\\/\\/|www)\\S*\\.(?i:png|gif|jp(e?)g)$");
    }

    private Dimension getDimension(String width, String height, BufferedImage originalImage) {
        
        // Width has precedence over height
        
        if (StringUtils.isNotBlank(width)) {
            // If width is defined, use it as base for calculating
            // new dimensions (ignore height)
            int x = Integer.parseInt(width);
            int y = x * originalImage.getHeight() / originalImage.getWidth();
            return new Dimension(x, y);
        } else if (StringUtils.isNotBlank(height)) {
            // No width specified, use height as base instead
            int y = Integer.parseInt(height);
            int x = y * originalImage.getWidth() / originalImage.getHeight();
            return new Dimension(x, y);
        }
        // Both parameters were blank, return original dimensions
        return new Dimension(originalImage.getWidth(), originalImage.getHeight());
        
    }
    
    private BufferedImage scaleImage(BufferedImage image, int width, int height) {

        int type = (image.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage newScaledImage = new BufferedImage(width, height, type);
        Graphics2D g = newScaledImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(scaledImage, 0, 0, width, height, null);
        g.dispose();

        return newScaledImage;
    }
    
    public void setRepository(Repository repository) {
    	this.repository = repository;
    }

}
