package org.vortikal.repository.store.jcr;

/**
 * JCR path <-> URI path mapping and other related utility functions. 
 *
 */
public class JcrPathUtil {

    
    /**
     * Escapes all illegal JCR name characters of a string.
     * The encoding is loosely modeled after URI encoding, but only encodes
     * the characters it absolutely needs to in order to make the resulting
     * string a valid JCR name.
     * Use {@link #unescapeIllegalJcrChars(String)} for decoding.
     * <p/>
     * QName EBNF:<br>
     * <xmp>
     * simplename ::= onecharsimplename | twocharsimplename | threeormorecharname
     * onecharsimplename ::= (* Any Unicode character except: '.', '/', ':', '[', ']', '*', ''', '"', '|' or any whitespace character *)
     * twocharsimplename ::= '.' onecharsimplename | onecharsimplename '.' | onecharsimplename onecharsimplename
     * threeormorecharname ::= nonspace string nonspace
     * string ::= char | string char
     * char ::= nonspace | ' '
     * nonspace ::= (* Any Unicode character except: '/', ':', '[', ']', '*', ''', '"', '|' or any whitespace character *)
     * </xmp>
     *
     * @param name the name to escape
     * @return the escaped name
     */
    public static String escapeIllegalJcrChars(String path) {
        StringBuilder buffer = new StringBuilder(path.length() * 2);
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '%' || ch == ':' || ch == '[' || ch == ']'
                || ch == '*' || ch == '\'' || ch == '"' || ch == '|'
                || (ch == '.' && path.length() < 3)
                || (ch == ' ' && (i == 0 || i == path.length() - 1))
                || ch == '\t' || ch == '\r' || ch == '\n') {
                buffer.append('%');
                buffer.append(Character.toUpperCase(Character.forDigit(ch / 16, 16)));
                buffer.append(Character.toUpperCase(Character.forDigit(ch % 16, 16)));
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    
    /**
     * Unescapes previously escaped jcr chars.
     * <p/>
     * Please note, that this does not exactly the same as the url related
     * {@link #unescape(String)}, since it handles the byte-encoding
     * differently.
     *
     * @param name the name to unescape
     * @return the unescaped name
     */
    public static String unescapeIllegalJcrChars(String path) {
        StringBuilder buffer = new StringBuilder(path.length());
        int i = path.indexOf('%');
        while (i > -1 && i + 2 < path.length()) {
            buffer.append(path.toCharArray(), 0, i);
            int a = Character.digit(path.charAt(i + 1), 16);
            int b = Character.digit(path.charAt(i + 2), 16);
            if (a > -1 && b > -1) {
                buffer.append((char) (a * 16 + b));
                path = path.substring(i + 3);
            } else {
                buffer.append('%');
                path = path.substring(i + 1);
            }
            i = path.indexOf('%');
        }
        buffer.append(path);
        return buffer.toString();
    }


    public static String pathToUri(String path) {
        if (JcrDaoConstants.VRTX_ROOT.equals(path)) {
            return "/";
        }
        return unescapeIllegalJcrChars(path.substring(JcrDaoConstants.VRTX_ROOT.length()));
    }


    public static String uriToPath(String uri) {
        if ("/".equals(uri)) {
            return JcrDaoConstants.VRTX_ROOT;
        }
        return JcrDaoConstants.VRTX_ROOT + escapeIllegalJcrChars(uri);
    }
    
}
