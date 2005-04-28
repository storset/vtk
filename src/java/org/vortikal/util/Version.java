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
package org.vortikal.util;


import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Date;



/**
 * Class representing the current version of the framework.
 */
public class Version {

    public final static String BUILD_DATE_PARSE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /* Retrieved from the FRAMEWORK_VERSION file in the class path: */
    private static Date buildDate = new Date(0);
    private static String buildHost = "unknown";
    private static String versionControlID = "$Id$";

    /* Picked up from META-INF/MANIFEST.MF */
    private static String frameworkVersion = "unknown";
    private static String buildVendor = "unknown";
    private static String frameworkTitle = "Vortikal Web Application Framework";



    /**
     * See <a href="http://java.sun.com/j2se/1.3/docs/guide/versioning/spec/VersioningSpecification.html#PackageVersionSpecification">Java Package Version Specification</a>
     */
    static {
        
        Package p = Version.class.getPackage();
        String versionFileLocation =
            p.getName().replace('.', '/') + "/" + "FRAMEWORK_VERSION";

        ClassLoader classLoader = Version.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(versionFileLocation);
        Properties props = new Properties();

        try {
            props.load(is);
        } catch (Throwable t) {
            System.out.println("Error reading version information: " +
                               t.getMessage());
            t.printStackTrace();
        }
        
        if (props.containsKey("framework.version")) {
            frameworkVersion = props.getProperty("framework.version");
        }
        
        if (props.containsKey("build.date")) {

            String dateStr = props.getProperty("build.date");
            try {
                SimpleDateFormat df = new SimpleDateFormat(BUILD_DATE_PARSE_FORMAT);
                buildDate = df.parse(dateStr);
            } catch (java.text.ParseException e) {
                System.out.println(
                    "Error parsing build date using format \""
                    + BUILD_DATE_PARSE_FORMAT + "\": " + e.getMessage());
            }
        }
        
        if (props.containsKey("build.host")) {
            buildHost = props.getProperty("build.host");
        }

        if (p.getImplementationVendor() != null) {
            buildVendor = p.getImplementationVendor();
        }

//         if (p.getImplementationTitle() != null) {
//             frameworkTitle = p.getImplementationTitle();
//         }
        
        if (p.getSpecificationTitle() != null) {
            frameworkTitle = p.getSpecificationTitle();
        }
        
        if (p.getImplementationVersion() != null) {
            frameworkVersion = p.getImplementationVersion();
        }
    }



    /**
     * Gets the current version of the framework.
     *
     * @return the current version, or <code>"unknown"</code> if no
     * version information is available.
     */
    public static final String getVersion() {
        return frameworkVersion;
    }


    /**
     * Gets the build date of the framework.
     *
     * @return the build date, or the date
     * <code>1970-01-01T01:00:00</code> if no such information is
     * available.
     */
    public static final Date getBuildDate() {
        return buildDate;
    }
    

    /**
     * Gets the name of the host on which the framework was built.
     *
     * @return the host name, or <code>"unknown"</code> if no such
     * information is available.
     */
    public static final String getBuildHost() {
        return buildHost;
    }

    
    /**
     * Gets the build vendor.
     */
    public static final String getBuildVendor() {
        return buildVendor;
    }


    /**
     * Gets the title of the framework.
     */
    public static final String getFrameworkTitle() {
        return frameworkTitle;
    }
    

    /**
     * Gets the ID as provided by the version control system. Examples
     * of this include a CVS tag, or a Subversion global revision
     * number. This information should only be used to track different
     * development versions.
     *
     * @return the ID of the version control system.
     */
    public static final String getVersionControlID() {
        return versionControlID;
    }
    
}
