/* Copyright (c) 2004, 2007, 2008, University of Oslo, Norway
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
package org.vortikal.util.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;


/**
 * Various URL utility methods.
 *
 */
public class URLUtil {

    /**
     * Splits a URI into path elements. For example, the URI
     * <code>/foo/bar</code> would be split into the following
     * components: <code>{"/", "foo", "bar"}</code>
     *
     * @param uri the uri to split
     * @return an array consisting of the components
     */
    public static String[] splitUri(String uri) {
         ArrayList<String> list = new ArrayList<String>();
         StringTokenizer st = new StringTokenizer(uri, "/");
 
         while (st.hasMoreTokens()) {
             String name = st.nextToken();
             list.add(name);
         }
 
         list.add(0, "/");
         return list.toArray(new String[0]);
     }


    /**
     * Splits a URI into incremental path elements. For example, the
     * URI <code>/foo/bar/baaz</code> produce the result: 
     * <code>{"/", "/foo", "/foo/bar", "/foo/bar/baaz"}</code>
     *
     * @param uri the uri to split
     * @return an array consisting of the components
     */
    public static String[] splitUriIncrementally(String uri) {

        if (uri == null || uri.equals("") || uri.equals("/")) {
            return new String[] { "/" };
        }
        
        ArrayList<String> elements = new ArrayList<String>();
        ArrayList<String> incremental = new ArrayList<String>();
        
        StringTokenizer tokenizer = new StringTokenizer(uri, "/");

        while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken();
            elements.add(s);
            StringBuilder path = new StringBuilder();
            for (String element: elements) {
                path.append("/").append(element);
            }
            incremental.add(path.toString());
        }
        
        incremental.add(0, "/");

        return incremental.toArray(new String[]{});
    }


    /**
     * This method is probably not needed
     *
     * @param uri a <code>String</code> value
     * @return a <code>String</code>
     */
    public static String urlDecode(String uri) {

        try {

            return urlDecode(uri, "utf-8");
            
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                "Java doesn't seem to support utf-8. Not much to do about that.");
        }
    }


    /**
     * This method is probably not needed
     *
     * @param uri a <code>String</code> value
     * @return a <code>String</code>
     */
    public static String urlDecode(String uri, String encoding)
        throws  UnsupportedEncodingException {

        if (uri.equals("/")) {

            return "/";
        }

        if (!uri.startsWith("/")) {

            return URLDecoder.decode(uri, encoding);
        }

        String[] path = splitUri(uri);
        StringBuilder result = new StringBuilder();
        
        for (int i = 1; i < path.length; i++) {

            result.append("/");
            result.append(URLDecoder.decode(path[i], encoding));
        }

        return result.toString();
    }
    

    /**
     * URL encodes a URI using encoding UTF-8, preserving path
     * separators ('/').
     *
     * @param uri a <code>String</code> value
     * @return a <code>String</code>
     */
    public static String urlEncode(String uri) {
        
        try {

            return urlEncode(uri, "utf-8");
            
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                "Java doesn't seem to support utf-8. Not much to do about that.");
        }
    }


    /**
     * URL encodes a URI using specified encoding, preserving path
     * separators ('/').
     *
     * @param uri a <code>String</code> value
     * @return a <code>String</code>
     */
    public static String urlEncode(String uri, String encoding)
        throws  UnsupportedEncodingException {

        if (uri.equals("/")) {

            return "/";
        }

        if (!uri.startsWith("/")) {

            return URLEncoder.encode(uri, encoding);
        }        

        String[] path = splitUri(uri);
        StringBuilder result = new StringBuilder();
        
        for (int i = 1; i < path.length; i++) {

            result.append("/");

            result.append(URLEncoder.encode(path[i], encoding));
        }
        
        // The method 'splitUri' removes trailing slashes. Handles this.
        if (uri.endsWith("/") && !result.toString().endsWith("/")) {
            result.append("/");
        }
        
        // Force hex '%20' instead of '+' as space representation:
        return result.toString().replaceAll("\\+", "%20");
    }

    

    /**
     * Splits the request query string into a map of (name, value[])
     * pairs.
     *
     * @param request the servlet request
     * @return the (name, value[]) map
     */
    public static Map<String, String[]> splitQueryString(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return splitQueryString(queryString);
    }
    

    /**
     * Splits a query string into a map of (name, value[]) pairs.
     *
     * @param request the query string
     * @return the (name, value) map
     */
    public static Map<String, String[]> splitQueryString(String queryString) {
        Map<String, String[]> queryMap = new HashMap<String, String[]>();
        if (queryString != null) {
            if (queryString.startsWith("?")) { 
                queryString = queryString.substring(1);
            }
            String[] pairs = queryString.split("&");
            for (int i = 0; i < pairs.length; i++) {
                if (pairs[i].length() == 0) {
                    continue;
                }
                int equalsIdx = pairs[i].indexOf("=");
                if (equalsIdx == -1) {
                    String[] existing = queryMap.get(pairs[i]);
                    if (existing == null) {
                        queryMap.put(pairs[i], new String[]{""});
                    } else {
                        String[] newVal = new String[existing.length + 1];
                        System.arraycopy(existing, 0, newVal, 0, existing.length);
                        newVal[existing.length] = "";
                        queryMap.put(pairs[i], newVal);
                    }
                } else {
                    String key = pairs[i].substring(0, equalsIdx);
                    String value = pairs[i].substring(equalsIdx + 1);
                    String[] existing = queryMap.get(key);
                    if (existing == null) {
                        queryMap.put(key, new String[]{value});
                    } else {
                        String[] newVal = new String[existing.length + 1];
                        System.arraycopy(existing, 0, newVal, 0, existing.length);
                        newVal[existing.length] = value;
                        queryMap.put(key, newVal);
                    }
                }
            }
        }
        return queryMap;
    }
    

}
