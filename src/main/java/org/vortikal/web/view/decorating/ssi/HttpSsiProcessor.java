/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating.ssi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

/** SSI processor that handles inclusion documents from http urls. 
 * 
 * <p>The format of the processed ssi includes is
 * <br/>
 * <code>&lt;!--#include virtual="http://www.usit.uio.no/it/web/produktkatalog/ssi/tavlehode.ssi" --></code>
 *
 */
public class HttpSsiProcessor implements SsiProcessor {

    private static Logger logger = Logger.getLogger(HttpSsiProcessor.class.getName());

    
    private int timeoutInMillisecondsWhenGettingIncludedFiles = 3000;
    private int maxlengthForIncludedFiles = 1024000; //set low for testing
    private String defaultEncodingForIncludedFiles = "iso-8859-1";

    // FIXME: changed from virtual
    private String identifier = "http";
    
    
    public String getIdentifier() {
        return this.identifier;
    }


    public String resolve(final String address) {
        String contentAsString = "";
        
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(timeoutInMillisecondsWhenGettingIncludedFiles);
        client.getHttpConnectionManager().getParams().setSoTimeout(timeoutInMillisecondsWhenGettingIncludedFiles);
        
        // Create a method instance.
        GetMethod getMethod = new GetMethod(address);

        // Retry only one time
        getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(1, false));
        BufferedInputStream bis = null;
        try {
            // Execute the method.
            int statusCode = client.executeMethod(getMethod);

            if (statusCode != HttpStatus.SC_OK) {
                logger.debug("Couldn't fetch the document included by SSI (" + address
                        + ") since method failed: " + getMethod.getStatusLine());
                getMethod.getResponseBody();
            } else {
                String charsetForIncludedDocument = getMethod.getResponseCharSet();
                if (charsetForIncludedDocument == null || charsetForIncludedDocument.equals("")) {
                    charsetForIncludedDocument = defaultEncodingForIncludedFiles;
                }
                InputStream is = getMethod.getResponseBodyAsStream();
                bis = new BufferedInputStream(is);
                byte[] bytes = new byte[maxlengthForIncludedFiles];

                int count = bis.read(bytes);
                if (count > -1) {
                    contentAsString = new String(bytes, charsetForIncludedDocument);
                } else {
                    logger.debug("Empty document (count == -1)");
                }
            }

        } catch (HttpException e) {
            logger.debug("Fatal protocol violation when fetching the document included by SSI ("
                    + address + "): " + e.getMessage(), e);
        } catch (IOException e) {
            logger.debug("Fatal transport error when fetching the document included by SSI ("
                    + address + "): " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.debug("Fatal error when fetching the document included by SSI ("
                    + address + "): " + e.getMessage(), e);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    logger.debug(
                            "Fatal error when closing stream for the document included by SSI ("
                                    + address + "): " + e.getMessage(), e);
                }
            }
            // Release the connection.
            getMethod.releaseConnection();
        }
        return contentAsString;

    } 

    
    /**
     * 
     * @param maxlengthForIncludedFiles The maximal length in bytes for a file included by SSI
     */
    public void setMaxlengthForIncludedFiles(int maxlengthForIncludedFiles) {
        this.maxlengthForIncludedFiles = maxlengthForIncludedFiles;
    }

    
    /**
     * see {@link org.apache.commons.httpclient.params.HttpConnectionParams}
     * 
     * @param timeoutInMillisecondsWhenGettingIncludedFiles
     */
    public void setTimeoutInMillisecondsWhenGettingIncludedFiles(
            int timeoutInMillisecondsWhenGettingIncludedFiles) {
        this.timeoutInMillisecondsWhenGettingIncludedFiles = timeoutInMillisecondsWhenGettingIncludedFiles;
    }
    
    /**
     * 
     * @param defaultEncodingForIncludedFiles
     *            The encoding used for the files included by SSI if we can't figure out the
     *            encoding a file
     */
    public void setDefaultEncodingForIncludedFiles(String defaultEncodingForIncludedFiles) {
        this.defaultEncodingForIncludedFiles = defaultEncodingForIncludedFiles;
    }


    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    

}
