/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.graphics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
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
        g.drawImage(scaledImage, 0, 0, width, height, null);
        g.dispose();

        return newScaledImage;
    }
    
    public void setRepository(Repository repository) {
    	this.repository = repository;
    }

}
