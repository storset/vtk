package org.vortikal.web.view.decorating.components;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UrlRetriever {

    private static Log logger = LogFactory.getLog(UrlRetriever.class);

    static final Pattern HTTP_REGEXP = Pattern.compile(
            "^http(s)?://",
            +Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    
    private int timeoutInMillisecondsWhenGettingIncludedFiles = 3000;
    private int maxlengthForIncludedFiles = 1024000; //set low for testing
    private String defaultEncodingForIncludedFiles = "iso-8859-1";

    
    boolean match(String uri) {
        Matcher matcher = HTTP_REGEXP.matcher(uri);
        return matcher.find();
    }
    
    String fetchIncludedUrl(final String address) {
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

}
