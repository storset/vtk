/* Copyright (c) 2006, 2009 University of Oslo, Norway
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
package org.vortikal.util.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.WordUtils;

/**
 * Various text parsing utility functions.
 */
public class TextUtils {

    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f'
        };

    public static char[] toHex(byte[] buffer) {
        char[] result = new char[buffer.length * 2];
        for (int i = 0; i < buffer.length; i++) {
            result[i << 1] = HEX[(buffer[i] & 0xF0) >>> 4];
            result[(i << 1)+1] = HEX[buffer[i] & 0x0F];
        }
        return result;
    }
    
    /**
     * Parsing flag indicating that values should be trimmed.
     */
    public static final int TRIM = 0x01;
    
    /**
     * Parsing flag indicating that empty string values should be discarded
     * from results.
     */
    public static final int DISCARD = 0x02;
    
    /**
     * Parsing flag indicating that illegal escape sequences should be ignored.
     */
    public static final int IGNORE_ILLEGAL_ESCAPE = 0x04;
    
    /**
     * Parsing flag indicating that unescaped separators in value of key-value
     * pair should not cause an exception to be thrown.
     * @see #parseKeyValue(java.lang.String, char, int) 
     */
    public static final int IGNORE_UNESCAPED_SEP_IN_VALUE = 0x08;
    
    /**
     * Extracts a field from a string using the character <code>,</code> as field delimiter.
     * 
     * @param string the string in question
     * @param name   the name of the field wanted
     * @return the value of the field, or <code>null</code> if not found.
     */
    public static String extractField(String string, String name) {
        return extractField(string, name, ",");
    }


    /**
     * Extracts a field from a string using a given field delimiter.
     * 
     * @param string the string in question
     * @param name the name of the field wanted
     * @param fieldDelimiter the field delimiter
     * @return the value of the field, or <code>null</code> if not found.
     */
    public static String extractField(String string, String name, String fieldDelimiter) {

        int pos = string.indexOf(":") + 1;
        if (pos == -1 || pos >= string.length() - 1) {
            return null;
        }

        StringTokenizer tokenizer = new StringTokenizer(string.substring(pos).trim(), fieldDelimiter);

        while (tokenizer.hasMoreTokens()) {

            String token = tokenizer.nextToken().trim();

            if (token.startsWith(name + "=\"")) {
                int startPos = token.indexOf("\"") + 1;
                int endPos = token.indexOf("\"", startPos);

                if (startPos > 0 && endPos > startPos) {
                    return token.substring(startPos, endPos);
                }
            } else if (token.startsWith(name + "=")) {
                int startPos = token.indexOf("=") + 1;
                if (startPos > 0) {
                    return token.substring(startPos);
                }
            }
        }
        return null;
    }


    /**
     * Removes duplicates in a String between delimiter and capitalize, and return spaces and delimiter
     * 
     * @param string
     *            string in question
     * @param stringDelimiter
     *            the splitter for the string (examples: "," "." "-" )
     * 
     * @return the capitalized string without duplicates
     */
    public static String removeDuplicatesIgnoreCaseCapitalized(String string, String stringDelimiter) {
        return removeDuplicatesIgnoreCase(string, stringDelimiter, false, false, true);
    }


    /**
     * Removes duplicates in a String between delimiter, and return spaces and delimiter
     * 
     * @param string
     *            string in question
     * @param stringDelimiter
     *            the splitter for the string (examples: "," "." "-" )
     * 
     * @return the string without duplicates
     */
    public static String removeDuplicatesIgnoreCase(String string, String stringDelimiter) {
        return removeDuplicatesIgnoreCase(string, stringDelimiter, false, false, false);
    }


    /**
     * Removes duplicates in a String between delimiter
     * 
     * @param string
     *            string in question
     * @param stringDelimiter
     *            the splitter for the string (examples: "," "." "-" )
     * @param removeSpaces
     *            remove spaces between tokens in return String
     * @param removeDelimiter
     *            remove delimiter between tokens in return String
     * @param capitalizeWords
     *            capitalize words between delimiter
     * 
     * @return the string without duplicates
     */
    public static String removeDuplicatesIgnoreCase(String string, String stringDelimiter, boolean removeSpaces,
            boolean removeDelimiter, boolean capitalizeWords) {

        StringTokenizer tokens = new StringTokenizer(string, stringDelimiter, false);
        Set<String> set = new HashSet<String>(tokens.countTokens() + 10);

        int count = 0;
        StringBuilder noDupes = new StringBuilder();
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken().trim();
            if (set.add(token.toLowerCase())) {
                if (count++ > 0) {
                    if (!removeDelimiter) {
                        noDupes.append(stringDelimiter);
                    }
                    if (!removeSpaces) {
                        noDupes.append(" ");
                    }
                }
                if (capitalizeWords) {
                    token = WordUtils.capitalize(token);
                }
                noDupes.append(token);
            }
        }
        return noDupes.toString();
    }

    /**
     * Parse a string of character-separated values, with no parsing flags set.
     * @see #parseCsv(java.lang.String, char, int) 
     */
    public static String[] parseCsv(String input, char sep) {
        return parseCsv(input, sep, 0);
    }
    
    /**
     * Parse a string of character-separated values, optionally trimming each
     * value. The chosen separator character may be escaped using backslash '\\',
     * and any backslashes need to be escaped themselves.
     * 
     * Empty strings between consecutive separator characters are included
     * in results unless <code>discard</code> is <code>true</code>.
     * 
     * @param input the input string to parse.
     * @param sep the character to use as value separator.
     * @param flags Parsing flags.
     *        <ul><li>If {@link #TRIM} is set, then each value to be trimmed.
     *            <li>If {@link #DISCARD} is set, then empty string values
     *        will not to be included in result. When combined with {@link #TRIM}, empty
     *        values after trimming are discarded.
     *            <li>If {@link #IGNORE_ILLEGAL_ESCAPE} is set, then illegal
     *                backslash escape sequences are ignored.
     *                The backslash character will be included in output
     *                in such cases, preceding the non-special character.
     *        </ul>
     * @return an array of strings containing the values.
     */
    public static String[] parseCsv(String input, char sep, int flags) {
        if (input == null) throw new IllegalArgumentException("null input");
        final boolean trim = hasFlag(TRIM, flags);
        final boolean discard = hasFlag(DISCARD, flags);
        final boolean ignoreIllegalEscape = hasFlag(IGNORE_ILLEGAL_ESCAPE, flags);
        
        if (trim ? input.trim().isEmpty() : input.isEmpty()) {
            if (discard) return new String[0];
            else return new String[] {""};
        }
        
        List<String> values = new ArrayList<String>();
        boolean escape = false;
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && escape) {
                buf.append('\\');
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == sep && escape) {
                buf.append(c);
                escape = false;
                continue;
            }
            if (escape) {
                if (ignoreIllegalEscape) {
                    buf.append('\\');
                    escape = false;
                } else {
                    throw new IllegalArgumentException("Illegal escape char: '\\" 
                        + c + "' in value: " + input);
                }
            }
            if (c == sep) {
                String v = trim ? buf.toString().trim() : buf.toString();
                buf.setLength(0);
                if (!v.isEmpty() || !discard) {
                    values.add(v);
                }
                if (i == input.length()-1) {
                    // Input ended with separator, be consistent
                    if (!discard) values.add("");
                }
                continue;
            }
            buf.append(c);
        }
        if (escape && !ignoreIllegalEscape) {
            throw new IllegalArgumentException(
                    "Illegal escape sequence '\\' at end of key-value: '" + input + "'");
        }
        if (buf.length() > 0) {
            String v = trim ? buf.toString().trim() : buf.toString();
            if (!v.isEmpty() || !discard) {
                values.add(v);
            }
        }
        
        return values.toArray(new String[values.size()]);
    }
    
    /**
     * Parses key-value with no parsing flags set.
     * @see #parseKeyValue(java.lang.String, char, int) 
     */
    public static String[] parseKeyValue(String input, char sep) {
        return parseKeyValue(input, sep, 0);
    }
    
    /**
     * Parses a string into a key and a value using a single character
     * as separator. The chosen separator character may be escaped using backslash
     * character '\\'. If no separator is found in input, then the returned string
     * array will have the entire input string as key part and <code>null</code>
     * as value part.
     * 
     * @param input The input string to parse.
     * @param sep The character to use as separator between key and value.
     * @param flags Set parsing flags. Valid flags:
     *        <ul><li>If {@link #TRIM} is set, then each value to be trimmed.
     *            <li>If {@link #IGNORE_ILLEGAL_ESCAPE} is set, then
     *                then illegal backslash escape sequences are ignored.
     *                The backslash character will be included in output
     *                in such cases, preceding the non-special character.
     *            <li>If {@link #IGNORE_UNESCAPED_SEP_IN_VALUE} is set, then
     *             do not throw exception if unescaped separator characters
     *             are encountered in the value part.
     *        </ul>

     * @return A string array with position 0 containing the key and position
     *         1 containing the value.
     */
    public static String[] parseKeyValue(String input, char sep, int flags) {
        boolean trim = hasFlag(TRIM, flags);
        boolean ignoreIllegalEscape = hasFlag(IGNORE_ILLEGAL_ESCAPE, flags);
        StringBuilder key = new StringBuilder();
        StringBuilder value = null;
        StringBuilder cur = key;
        boolean escape = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (cur == value && value == null) {
                value = new StringBuilder();
                cur = value;
            }
            if (c == '\\' && escape) {
                cur.append('\\');
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == sep && escape) {
                cur.append(c);
                escape = false;
                continue;
            }
            if (c == sep && cur == value) {
                if (!hasFlag(IGNORE_UNESCAPED_SEP_IN_VALUE, flags)) {
                    throw new IllegalArgumentException(
                        "Unescaped separator '" + sep + "' in  value: \"" + input + "\"");
                }
                cur.append(c);
                continue;
            }
            if (escape) {
                if (ignoreIllegalEscape) {
                    escape = false;
                    cur.append('\\');
                } else {
                    throw new IllegalArgumentException("Illegal escape char: '\\" 
                        + c + "' in key-value: " + input);
                }
            }
            if (c == sep) {
                value = new StringBuilder();
                cur = value;
                continue;
            }
            cur.append(c);
        }
        if (escape && !ignoreIllegalEscape) {
            throw new IllegalArgumentException(
                    "Illegal escape sequence '\\' at end of key-value: '" + input + "'");
        }
        
        return new String[] { trim ? key.toString().trim() : key.toString(), 
            value == null ? null : trim ? value.toString().trim() : value.toString() };
    }

    private static boolean hasFlag(int flag, int flags) {
        return (flag & flags) != 0;
    }
}
