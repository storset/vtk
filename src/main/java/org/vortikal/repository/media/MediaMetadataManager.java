/* Copyright (c) 2012, University of Oslo, Norway
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

package org.vortikal.repository.media;

import org.vortikal.repository.Path;
import org.vortikal.repository.store.Metadata;

public interface MediaMetadataManager {

    /*
     * Generates a thumbnail of a media file
     * 
     * @param path          Path to media file
     * @param width         Width of generated image
     * @param fileType      Vortikal file type
     * @param scaleUp       Scale image if smaller then with
     * 
     * @return              Returns null on failure
     */
    public MediaImage getThumbnail(Path path, int width, String fileType, boolean scaleUp);

    /*
     * Generates a image at a given time in a video
     * 
     * @param path          Path to media file
     * @param maxWidth      Max width of generated thumbnail. Set to 0 if not in use. 
     * @param secounds      Seconds into the video
     * 
     * @return              Returns null on failure
     */
    public MediaImage getVideoScreenshot(Path path, int maxWidth, int maxHeight, int seconds);
    
    /*
     * Returns the metadata of a media file
     * 
     * @param path          Path to media file
     * @param fileType      Vortikal file type
     * 
     * @return              Returns null on failure
     */
    public Metadata getMetadata(Path path, String fileType);
       
}
