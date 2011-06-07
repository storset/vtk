/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repository.content;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;

public class ImageContentFactory implements ContentFactory {

    public ImageContentFactory() {
    }

    @Override
    public Class<?>[] getRepresentationClasses() {
        String prop = System.getProperty("java.awt.headless");
        try {
            System.setProperty("java.awt.headless", "true");
            return new Class[] {BufferedImage.class, Dimension.class};
        } finally {
            if (prop != null) {
                System.setProperty("java.awt.headless", prop);
            }
        }
    }

    @Override
    public Object getContentRepresentation(Class<?> clazz,  InputStream content)
        throws Exception {
        
        try {
            if (clazz == Dimension.class) {
                return getImageDimension(content);
            } else if (clazz == BufferedImage.class) {
                return ImageIO.read(content);                
            } else {
                throw new IllegalArgumentException("Unsupported content representation class: " + clazz.getName());
            }

        } finally {
            // ImageIO.read documentation states that it does not close the input stream.
            content.close();
        }
    }
    
    private Dimension getImageDimension(InputStream content) throws Exception {

        ImageInputStream iis = new FileCacheImageInputStream(content, null);
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                reader.dispose();
                return new Dimension(width, height);
            }
        } finally {
            iis.close();
        }

        return null;
    }
}
