package org.vortikal.web.view.decorating.ssi;

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
    //Is not private since we need access to this pattern from the unittests
    static final Pattern INCLUDE_REGEXP = Pattern.compile(
            "<!--#include\\s+([\000-\377]*?)\\s*?=\"([\000-\377]*?)\"\\s*?-->",
            +Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    
    private int timeoutInMillisecondsWhenGettingIncludedFiles = 3000;
    private int maxlengthForIncludedFiles = 1024000; //set low for testing
    private String defaultEncodingForIncludedFiles = "iso-8859-1";

    public String parse(final String content) {
        String docContentProcessed = content;
        int indexStartIncludeStatements;
        int indexEndIncludeStatements;
        StringBuffer sb;

        Matcher matcherInclude = INCLUDE_REGEXP.matcher(docContentProcessed);
        while (matcherInclude.find()) {            
            if (logger.isDebugEnabled()) {
                logger.debug("matcherInclude.group(0): " + matcherInclude.group(0));
            }

            indexStartIncludeStatements = matcherInclude.start();
            indexEndIncludeStatements = matcherInclude.end();
            
            String url = docContentProcessed.substring(matcherInclude.start(2), matcherInclude.end(2));
            if (url != null && !url.trim().equals("")) {
                String ssiContent = null;
                
                // if we can't fetch the included resource ssiContent is set to an empty String
                // and the SSI directive is removed by the replaceFirst statement bellow
                ssiContent = fetchIncludedFile(url);

                sb = new StringBuffer();        
                sb.append(docContentProcessed.substring(0, indexStartIncludeStatements));
                sb.append(ssiContent);
                sb.append(docContentProcessed.substring(indexEndIncludeStatements, docContentProcessed.length()));
                docContentProcessed = sb.toString();

                // We have to reset the matcher because we are using replaceFirst. From the
                // javadoc for Matcher:
                // "Invoking this method changes this matcher's state. If the matcher is to be
                // used in further matching operations then it should first be reset."
                matcherInclude.reset(docContentProcessed);
            }
        }
        return docContentProcessed;
    }

    
    private String fetchIncludedFile(final String address) {
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
    

}
