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
package org.vortikal.util.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Utility class for guessing MIME types based on file
 * extensions. Reads its properties set from a class path resource
 * specified by the system property
 * <code>org.vortikal.mime.properties.file</code>. The default path
 * (if not specified) is
 * <code>org/vortikal/util/repository/mime.properties</code>.
 *
 * <p>The format of the properties file is
 * <pre>
 * extension1 = MIME type 1
 * ...
 * extensionN = MIME type N
 * </pre>
 */
public class MimeHelper {


    /**
     * The default MIME type, returned when no other mapping exists
     * for a given file extension. Its value is
     * <code>application/octet-stream</code>.
     */
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static Properties properties;

    static {
        load();
    }

    /**
     * Maps a file name to a MIME type based on its file extension.
     *
     * @param fileName a file name (or path).
     * @return the MIME type, or {@link #DEFAULT_MIME_TYPE} if no
     * mapping exists for the file extension in question.
     */
    public static String map(String fileName) {
        String extension = findExtension(fileName);

        return properties.getProperty(extension, DEFAULT_MIME_TYPE);
    }

    protected static String findExtension(String fileName) {
        if (fileName.indexOf(".") < 0) {
            return "";
        }

        if (fileName.lastIndexOf(".") >= (fileName.length() - 1)) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf(".") + 1,
            fileName.length()).toLowerCase();
    }

    protected static void load() {
        properties = new Properties();

        Log logger = LogFactory.getLog(MimeHelper.class);

        String fileName = System.getProperty("org.vortikal.mime.properties.file",
                "org/vortikal/util/repository/mime.properties");

        try {
            InputStream inStream = MimeHelper.class.getClassLoader()
                                                   .getResourceAsStream(fileName);
            properties.load(inStream);
            inStream.close();
            logger.info("Loaded MIME type properties from classpath resource: "
                        + fileName);
        } catch (IOException e) {
            logger.warn("Caught IOException while reading MIME " +
                "properties file: '" + fileName + "'.", e);
        }
    }
}
