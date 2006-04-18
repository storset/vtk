package org.vortikal.webdav;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.vortikal.util.web.HttpUtil;

/**
 * Some WebDAV utility methods.
 * 
 * @author oyviste
 *
 */
public final class WebdavUtil {

    /**
     * Gets the HTTP "status message" for the status codes defined in
     * this class, i.e. <code>SC_MULTI_STATUS</code> will map to
     * <code>207 Multi-Status</code>, etc.
     *
     * @param statusCode an <code>int</code> value
     * @return a <code>String</code>
     */
    public static String getStatusMessage(int statusCode) {

        StringBuffer message = new StringBuffer("HTTP/");
        message.append(WebdavConstants.HTTP_VERSION_USED).append(" ");
        message.append(statusCode).append(" ");
        message.append(HttpUtil.getStatusMessage(statusCode));
        
        return message.toString();
       
    }

    
    /**
     * Utility method for parsing date values of controlled properties for WebDAV.  
     * @param dateValue
     * @return
     */
    public static Date parsePropertyDateValue(String dateValue) 
       throws ParseException {
       SimpleDateFormat parser = 
           new SimpleDateFormat(WebdavConstants.WEBDAV_PROPERTY_DATE_VALUE_FORMAT, 
                   Locale.US);
       
       TimeZone tz = TimeZone.getTimeZone(WebdavConstants.WEBDAV_PROPERTY_DATE_VALUE_TIMEZONE);
       parser.setTimeZone(tz);
       
       return parser.parse(dateValue);
    }

    /**
     * Utility method for formatting date values of controlled properties for WebDAV.
     * @param date
     * @return
     */
    public static String formatPropertyDateValue(Date date) {
        SimpleDateFormat formatter =
            new SimpleDateFormat(WebdavConstants.WEBDAV_PROPERTY_DATE_VALUE_FORMAT, 
                    Locale.US);
        
        formatter.setTimeZone(TimeZone.getTimeZone(WebdavConstants.WEBDAV_PROPERTY_DATE_VALUE_TIMEZONE));
        
        return formatter.format(date);
    }

}
