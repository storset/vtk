package org.vortikal.repositoryimpl;

public class URIValidator {

    public static final int MAX_URI_LENGTH = 1500;

    public boolean validateURI(String uri) {
        if (uri == null) {
            return false;
        }

        if (uri.trim().equals("")) {
            return false;
        }

        if (!uri.startsWith("/")) {
            return false;
        }

        if (uri.indexOf("//") != -1) {
            return false;
        }

        if (uri.length() >= MAX_URI_LENGTH) {
            return false;
        }

        if (uri.indexOf("/../") != -1) {
            return false;
        }

        if (uri.endsWith("/..")) {
            return false;
        }

        if (uri.endsWith("/.")) {
            return false;
        }

        if (uri.indexOf("%") != -1) {
            return false;
        }

        //         /* SQL Strings: */
        //         if (uri.indexOf("'") != -1) {
        //             return false;
        //         }
        //         /* SQL comments: */
        //         if (uri.indexOf("--") != -1) {
        //             return false;
        //         }
        //         /* SQL (C-style) comments: */
        //         if (uri.indexOf("/*") != -1 || uri.indexOf("*/") != -1) {
        //             return false;
        //         }
        return true;
    }

}
