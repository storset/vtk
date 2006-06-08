/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vortikal.webdav.ifheader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Resource;

public class IfHeaderImpl implements IfHeader {

    protected static Log logger = LogFactory.getLog(IfHeaderImpl.class);
    
    /**
     * The string representation of the header value
     */
    private final String headerValue;

    /**
     * The list of untagged state entries
     */
    private final StateEntryList stateEntryList;

    /**
     * The list of all positive tokens present in the If header.
     */
    private List allTokens = new ArrayList();
    /**
     * The list of all NOT tokens present in the If header.
     */
    private List allNotTokens = new ArrayList();
    
    
    public IfHeaderImpl(HttpServletRequest request) {
        super();
        headerValue = request.getHeader("If");
        logger.debug("if-header: " + headerValue);
        stateEntryList = parse();
    }
    
    
    /**
     * Tries to match the contents of the <em>If</em> header with the given
     * token and etag values with the restriction to only check for the tag.
     * <p>
     * If the <em>If</em> header is of untagged type, the untagged <em>IfList</em>
     * is matched against the token and etag given: A match of the token and
     * etag is found if at least one of the <em>IfList</em> entries match the
     * token and etag tupel.
     *
     * @param tag The tag to identify the <em>IfList</em> to match the token
     * and etag against.
     * @param token The token to compare.
     * @param etag The ETag value to compare.
     *
     * @return If the <em>If</em> header is of untagged type the result is
     *      <code>true</code> if any of the <em>IfList</em> entries matches
     *      the token and etag values. For tagged type <em>If</em> header the
     *      result is <code>true</code> if either no entry for the given tag
     *      exists in the <em>If</em> header or if the <em>IfList</em> for the
     *      given tag matches the token and etag given.
     */
    public boolean matches(Resource resource, boolean shouldMatchOnNoIfHeader) {
        if (stateEntryList == null) {
            logger.debug("matches: No If header, assume match: " + shouldMatchOnNoIfHeader);
            return shouldMatchOnNoIfHeader;
        } else {
            return stateEntryList.matches(resource);
        }
    }
        

    /**
     * @return an interator over all tokens present in the if header, that were not denied by a
     *         leading NOT statement.
     */
    public Iterator getAllTokens() {
        return allTokens.iterator();
    }

    public boolean hasTokens() {
        return !allTokens.isEmpty();
    }
    
    /**
     * @return an interator over all NOT tokens present in the if header, that
     * were explicitely denied.
     */
    public Iterator getAllNotTokens() {
        return allNotTokens.iterator();
    }
    
    
    /**
     * Parse the original header value and build th internal IfHeaderInterface
     * object that is easy to query.
     */
    private StateEntryList parse() {
        StateEntryList ifHeader;
        if (headerValue != null && headerValue.length() > 0) {
            StringReader reader = null;
            int firstChar = 0;

            try {
                reader = new StringReader(headerValue);
                // get the first character to decide - expect '(' or '<'
                try {
                    reader.mark(1);
                    firstChar = readWhiteSpace(reader);
                    reader.reset();
                } catch (IOException ignore) {
                    // may be thrown according to API but is only thrown by the
                    // StringReader class if the reader is already closed.
                }

                if (firstChar == '(') {
                    ifHeader = parseUntagged(reader);
                } else if (firstChar == '<') {
                    ifHeader = parseTagged(reader);
                } else {
                    logIllegalState("If", firstChar, "(<", null);
                    ifHeader = null;
                }

            } finally  {
                if (reader != null) {
                    reader.close();
                }
            }

        } else {
            logger.debug("IfHeader: No If header in request");
            ifHeader = null;
        }
        return ifHeader;
    }
    
    
    //---------- internal IF header parser -------------------------------------
    /**
     * Parses a tagged type <em>If</em> header. This method implements the
     * <em>Tagged</em> production given in the class comment :
     * <pre>
         Tagged = { "<" Word ">" Untagged } .
     * </pre>
     *
     * @param reader
     * @return
     */
    private IfHeaderMap parseTagged(StringReader reader) {
        IfHeaderMap map = new IfHeaderMap();
        try {
            while (true) {
                // read next non-whitespace
                int c = readWhiteSpace(reader);
                if (c < 0) {
                    // end of input, no more entries
                    break;
                } else if (c == '<') {
                    // start a tag with an IfList
                    String resource = readWord(reader, '>');
                    if (resource != null) {
                        // go to untagged after reading the resource
                        map.put(resource, parseUntagged(reader));
                    } else {
                        break;
                    }
                } else {
                    // unexpected character
                    // catchup to end of input or start of a tag
                    logIllegalState("Tagged", c, "<", reader);
                }
            }
        } catch (IOException ioe) {
            logger.error("parseTagged: Problem parsing If header: "+ioe.toString());
        }

        return map;
    }

    /**
     * Parses an untagged type <em>If</em> header. This method implements the
     * <em>Untagged</em> production given in the class comment :
     * <pre>
         Untagged = { "(" IfList ")" } .
     * </pre>
     *
     * @param reader The <code>StringReader</code> to read from for parsing
     *
     * @return An <code>ArrayList</code> of {@link IfList} entries.
     */
    private StateEntryListImpl parseUntagged(StringReader reader) {
        StateEntryListImpl list = new StateEntryListImpl();
        try {
            while (true) {
                // read next non-whitespace
                reader.mark(1);
                int c = readWhiteSpace(reader);
                if (c < 0) {
                    // end of input, no more IfLists
                    break;

                } else if (c == '(') {
                    // start of an IfList, parse
                    list.add(parseIfList(reader));

                } else if (c == '<') {
                    // start of a tag, return current list
                    reader.reset();
                    break;

                } else {
                    // unexpected character
                    // catchup to end of input or start of an IfList
                    logIllegalState("Untagged", c, "(", reader);
                }
            }
        } catch (IOException ioe) {
            logger.error("parseUntagged: Problem parsing If header: "+ioe.toString());
        }
        return list;
    }

    /**
     * Parses an <em>IfList</em> in the <em>If</em> header. This method
     * implements the <em>Tagged</em> production given in the class comment :
     * <pre>
         IfList = { [ "Not" ] ( ("<" Word ">" ) | ( "[" Word "]" ) ) } .
     * </pre>
     *
     * @param reader The <code>StringReader</code> to read from for parsing
     *
     * @return The {@link IfList} for the input <em>IfList</em>.
     *
     * @throws IOException if a problem occurrs during reading.
     */
    private IfList parseIfList(StringReader reader) throws IOException {
        IfList res = new IfList();
        boolean positive = true;
        String word;

        ReadLoop:
        while (true) {
            int nextChar = readWhiteSpace(reader);
            switch (nextChar) {
                case 'N':
                case 'n':
                    // read not

                    // check whether o or O
                    int not = reader.read();
                    if (not != 'o' && not != 'O') {
                        logIllegalState("IfList-Not", not, "o", null);
                        break;
                    }

                    // check whether t or T
                    not = reader.read();
                    if (not !='t' || not != 'T') {
                        logIllegalState("IfList-Not", not, "t", null);
                        break;
                    }

                    // read Not ok
                    positive = false;
                    break;

                case '<':
                    // state token
                    word = readWord(reader, '>');
                    if (word != null) {
                        res.add(new IfListEntryToken(word, positive));
                        // also add the token to the list of all tokens
                        if (positive) {
                        allTokens.add(word);
                        } else {
                            allNotTokens.add(word);
                        }
                        positive = true;
                    }
                    break;

                case '[':
                    // etag
                    word = readWord(reader, ']');
                    if (word != null) {
                        res.add(new IfListEntryEtag(word, positive));
                        positive = true;
                    }
                    break;

                case ')':
                    // correct end of list, end the loop
                    logger.debug("parseIfList: End of If list, terminating loop");
                    break ReadLoop;

                default:
                    logIllegalState("IfList", nextChar, "nN<[)", reader);

                    // abort loop if EOF
                    if (nextChar < 0) {
                        break ReadLoop;
                    }

                    break;
            }
        }

        // return the current list anyway
        return res;
    }

    /**
     * Returns the first non-whitespace character from the reader or -1 if
     * the end of the reader is encountered.
     *
     * @param reader The <code>Reader</code> to read from
     *
     * @return The first non-whitespace character or -1 in case of EOF.
     *
     * @throws IOException if a problem occurrs during reading.
     */
    private int readWhiteSpace(Reader reader) throws IOException {
        int c = reader.read();
        while (c >= 0 && Character.isWhitespace((char) c)) {
             c = reader.read();
        }
        return c;
    }

    /**
     * Reads from the input until the end character is encountered and returns
     * the string upto but not including this end character. If the end of input
     * is reached before reading the end character <code>null</code> is
     * returned.
     * <p>
     * Note that this method does not support any escaping.
     *
     * @param reader The <code>Reader</code> to read from
     * @param end The ending character limitting the word.
     *
     * @return The string read upto but not including the ending character or
     *      <code>null</code> if the end of input is reached before the ending
     *      character has been read.
     *
     * @throws IOException if a problem occurrs during reading.
     */
    private String readWord(Reader reader, char end) throws IOException {
        StringBuffer buf = new StringBuffer();

        // read the word value
        int c = reader.read();
        for (; c >= 0 && c != end; c=reader.read()) {
            buf.append((char) c);
        }

        // check whether we succeeded
        if (c < 0) {
            logger.error("readWord: Unexpected end of input reading word");
            return null;
        }

        // build the string and return it
        return buf.toString();
    }

    /**
     * Logs an unexpected character with the corresponding state and list of
     * expected characters. If the reader parameter is not null, characters
     * are read until either the end of the input is reached or any of the
     * characters in the expChar string is read.
     *
     * @param state The name of the current parse state. This method logs this
     *      name in the message. The intended value would probably be the
     *      name of the EBNF production during which the error occurrs.
     * @param effChar The effective character read.
     * @param expChar The list of characters acceptable in the current state.
     * @param reader The reader to be caught up to any of the expected
     *      characters. If <code>null</code> the input is not caught up to
     *      any of the expected characters (of course ;-).
     */
    private void logIllegalState(String state, int effChar, String expChar,
                                 StringReader reader) {

        // format the effective character to be logged
        String effString = (effChar < 0) ? "<EOF>" : String.valueOf((char) effChar);

        // log the error
        logger.error("logIllegalState: Unexpected character '"+effString+" in state "+state+", expected any of "+expChar);
        logger.error("logIllegalState: headerValue: " + headerValue);
        // catch up if a reader is given
        if (reader != null && effChar >= 0) {
            try {
                logger.debug("logIllegalState: Catch up to any of "+expChar);
                do {
                    reader.mark(1);
                    effChar = reader.read();
                } while (effChar >= 0 && expChar.indexOf(effChar) < 0);
                if (effChar >= 0) {
                    reader.reset();
                }
            } catch (IOException ioe) {
                logger.error("logIllegalState: IO Problem catching up to any of "+expChar);
            }
        }
    }

}
