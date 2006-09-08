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
package org.vortikal.util.text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author Even Halvorsen & Kristian Syversen
 *         (based on guru Gorm A. Paulsen's previous version
 *         DefaultStructuredText.java)
 * 
 * This class contains methods for generating XML-elements from structured text
 * and methods for generating structured text from an XML-structure.
 */
public final class EvenStructuredText implements StructuredText {
    
    public EvenStructuredText() {
        initTagNames();
    }

    public void setTextMappings(Map customMappings) {
        this.tagNames = customMappings;
    }
    
    private Log logger = LogFactory.getLog(this.getClass());

    protected String LINE_SEPARATOR = "\n";
    protected String SPACE = " ";
    protected String LISTITEM_MARKER = "-";
    protected String LISTITEM_START = this.LINE_SEPARATOR + this.LISTITEM_MARKER + this.SPACE;
    protected String NUMLISTITEM_MARKER = "#";
    protected String NUMLISTITEM_START = this.LINE_SEPARATOR + this.NUMLISTITEM_MARKER + this.SPACE;
    protected String LIST_START = this.LISTITEM_START;
    protected String LIST_CLOSE = this.LINE_SEPARATOR + this.LINE_SEPARATOR;
    protected String NUMLIST_START = this.NUMLISTITEM_START;
    protected String NUMLIST_CLOSE = this.LINE_SEPARATOR + this.LINE_SEPARATOR;
    protected String PARAGRAPH_START = this.LINE_SEPARATOR + this.LINE_SEPARATOR;
    protected String PARAGRAPH_CLOSE = this.LINE_SEPARATOR + this.LINE_SEPARATOR;
    protected String BOLD_START = "*";
    protected String BOLD_CLOSE = "*";
    protected String ITALIC_START = "_";
    protected String ITALIC_CLOSE = "_";
    protected String LINK_START = "\"";
    protected String LINK_MIDDLE = "\":";
    protected String REF_START = "[";
    protected String REF_ATTRIBUTE = ":";
    protected String REF_CLOSE = "]";
    protected String SUB_START = "[sub:";
    protected String SUB_END = "]";
    protected String SUPER_START = "[super:";
    protected String SUPER_END = "]";
    protected char NEWLINE = '\n';
    protected char ESCAPE = '\\';

    protected Map tagNames = new HashMap();
    
    
    public Set getTagNames() {
        return this.tagNames.keySet();
    }
    
    
    private void initTagNames() {
        this.tagNames.put("root", "evenstructuredtext");
        this.tagNames.put("bold", "bold");
        this.tagNames.put("italic", "it");
        this.tagNames.put("link", "link");
        this.tagNames.put("url", "url");
        this.tagNames.put("url-description", "description");
        this.tagNames.put("unordered-list", "unsortedlist");
        this.tagNames.put("ordered-list", "sortedlist");
        this.tagNames.put("listitem", "listitem");
        this.tagNames.put("paragraph", "paragraph");
        this.tagNames.put("reference", "refrence");
        this.tagNames.put("reference-type", "type");
        this.tagNames.put("sub", "sub");
        this.tagNames.put("sup", "sup");
        this.tagNames.put("newline", "newline");
    }
    
    
    protected boolean escapeAtPos(String text, int pos) {
        return (text.indexOf(this.ESCAPE, pos) == pos);
    }
    
        
    protected boolean paragraphAtPos(String text, int pos) {
        return (text.indexOf(this.PARAGRAPH_START, pos) == pos);
    }
    
        
    // test that current char is NEWLINE which i NOT followed
    // by another NEWLINE (newline + newline = paragraph)
    protected boolean newlineAtPos(String text, int pos) {
        if (pos < text.length() - 1
                && text.charAt(pos) == this.NEWLINE
                && text.charAt(pos + 1) != this.NEWLINE)
            return true;
        return false;
    }
    
    
    protected boolean listAtPos(String text, int pos) {
        return (text.indexOf(this.LIST_START, pos) == pos);
    }
    
    
    protected boolean numlistAtPos(String text, int pos) {
        return (text.indexOf(this.NUMLIST_START, pos) == pos);
    }
    
    
    protected boolean listitemAtPos(String text, int pos) {
        return (text.indexOf(this.LISTITEM_START, pos) == pos);
    }
    
    
    protected boolean numlistitemAtPos(String text, int pos) {
        return (text.indexOf(this.NUMLISTITEM_START, pos) == pos);
    }
    
    
    protected boolean boldAtPos(String text, int pos) {
        if (text.indexOf(this.BOLD_START, pos) != pos)
            return false;
        int endPos = text.indexOf(this.BOLD_CLOSE, pos + this.BOLD_START.length());

        if (endPos > 0)
            return !(endOfBlockLevelElement(text, pos + this.BOLD_START.length(),
                    endPos));

        return false;
    }
    
    
    protected boolean italicAtPos(String text, int pos) {
        if (text.indexOf(this.ITALIC_START, pos) != pos)
            return false;
        int endPos = text.indexOf(this.ITALIC_CLOSE, pos + this.ITALIC_START.length());

        if (endPos > 0)
            return !endOfBlockLevelElement(text, pos + this.ITALIC_START.length(),
                    endPos);

        return false;
    }
    
    
    protected boolean linkAtPos(String text, int pos) {
        // links look like: "description":http://url-of-something
        if (text.indexOf(this.LINK_START, pos) != pos)
            return false;

        int middlePos = text.indexOf(this.LINK_MIDDLE, pos + this.LINK_START.length());

        if (middlePos > 0) {

            // Check that the " at pos is the closest one to ":
            // (unless the " character is escaped; such are ignored)
            int testPos = pos + this.LINK_START.length();
            
            do {
                // contine searching for the real closing "
                testPos = text.indexOf(this.LINK_START, ++testPos);
            } while ( text.charAt(testPos-1) == this.ESCAPE );
            
            if (testPos != middlePos)
                return false;

            // Check that no LINE_SEPERATOR come before LINK_MIDDLE 
            // (except when escaped)
            int lineSepPos = text.indexOf(this.LINE_SEPARATOR, pos
                    + this.LINK_START.length());
            if ((lineSepPos > 0) && (lineSepPos < middlePos) 
                    && text.charAt(lineSepPos-1) != this.ESCAPE)
                return false;
            
            // Check that no PARAGRAPH_START or LIST_START come before
            // LINK_MIDDLE
            if (endOfBlockLevelElement(text, pos + this.LINK_START.length(),
                    middlePos))
                return false;

            // Check that middlePos is not at the end of the text
            if (middlePos == text.length() - this.LINK_MIDDLE.length())
                return false;

            // Check that LINE_SEPARATOR or whitespace is not righT after
            // LINK_MIDDLE
            if ((text.charAt(middlePos + this.LINK_MIDDLE.length()) == ' ')
                    || (text
                            .indexOf(this.LINE_SEPARATOR, pos + this.LINK_MIDDLE.length()) == pos
                            + this.LINK_MIDDLE.length()))
                return false;

            return true;
        }

        return false;
    }
    
    
    protected boolean refrenceAtPos(String text, int pos) {
        if (text.indexOf(this.REF_START, pos) != pos)
            return false;

        int endPos = text.indexOf(this.REF_CLOSE, pos + this.REF_START.length());

        if (endPos > 0)
            return !(endOfBlockLevelElement(text, pos + this.REF_START.length(),
                    endPos));

        return false;
    }
    
    
    // check if certain speacial characters occur before the end pos of the
    // current element
    protected boolean endOfBlockLevelElement(String text, int pos, int endPos) {
        int paragraphPos = text.indexOf(this.PARAGRAPH_START, pos);
        if (paragraphPos > 0 && paragraphPos < endPos)
            return true;

        int listPos = text.indexOf(this.LIST_START, pos);
        if (listPos > 0 && listPos < endPos)
            return true;

        int numListPos = text.indexOf(this.NUMLIST_START, pos);
        if (numListPos > 0 && numListPos < endPos)
            return true;

        return false;
    }
    
    
    protected boolean subAtPos(String text, int pos) {
        // sub is given on the form: sub:"text"

        int startPos = text.indexOf(this.SUB_START, pos);

        if (startPos != pos) {
            return false;
        }

        int endPos = text.indexOf(this.SUB_END, pos + this.SUB_START.length());

        String subtext = text.substring(startPos, endPos);

        if (endPos < 0) {
            return false;
        } else if (subtext.indexOf(this.LINE_SEPARATOR) != -1 
                && subtext.charAt( subtext.indexOf(this.LINE_SEPARATOR) -1 ) != this.ESCAPE ) {
            return false;
        } else {
            return true;
        }   
    }
    
    
    protected boolean superAtPos(String text, int pos) {
        // super is given on the form: super:"text"

        int startPos = text.indexOf(this.SUPER_START, pos);

        if (startPos != pos) {
            return false;
        }

        int endPos = text.indexOf(this.SUPER_END, pos + this.SUPER_START.length());

        String supertext = text.substring(startPos, endPos);

        if (endPos < 0) {
            return false;
        } else if (supertext.indexOf(this.LINE_SEPARATOR) != -1 
                && supertext.charAt( supertext.indexOf(this.LINE_SEPARATOR) -1 ) != this.ESCAPE ) {
            return false;
        } else {
            return true;
        }
    }
    
    
    public Element parseStructuredText(String text) {
        String structureText = unifyNewlines(text);
        structureText = removeStartingAndTrailingNewlines(structureText);
                
        // All structuretext m� inn i avsnitt eller liste
        if ((!structureText.startsWith(this.PARAGRAPH_START))
                || (!structureText.startsWith(this.LIST_START))
                || (!structureText.startsWith(this.NUMLIST_START))) {
            structureText = this.PARAGRAPH_START + structureText;
        }

        Element root = new Element(lookupTag("root"));
        
        int pos = 0;
        int nextPos = 0;
                
        while (pos < structureText.length()) {
            if (listAtPos(structureText, pos + this.LINE_SEPARATOR.length())) {
                nextPos = parseList(structureText, pos
                        + this.LINE_SEPARATOR.length(), root);
            } else if (numlistAtPos(structureText, pos
                    + this.LINE_SEPARATOR.length())) { // telle opp riktig
                nextPos = parseNumlist(structureText, pos
                        + this.LINE_SEPARATOR.length(), root);
            } else if (listAtPos(structureText, pos)) {
                nextPos = parseList(structureText, pos, root);
            } else if (numlistAtPos(structureText, pos)) {
                nextPos = parseNumlist(structureText, pos, root);
            } else if (paragraphAtPos(structureText, pos)) {
                nextPos = parseParagraph(structureText, pos, root);
            }

            pos = nextPos;
        }
        return root;
    }
    
    
    protected String unifyNewlines( String text ) {
        text = text.replaceAll("\r\n", "\n");
        text = text.replaceAll("\r", "\n");
        return text;
    }
    
    
    private String removeStartingAndTrailingNewlines(String structureText) {
        // Strip newlines at start of structuredText 
        // (will else incorrectly be saved as <newline/> 
        int startpos = 0;
        try {
            while (this.NEWLINE == structureText.charAt(startpos) ) {
                ++startpos;
            }
            // Strip newlines at end of structuredText 
            // (will else incorrectly be displayed as '\'
            while (this.NEWLINE == structureText.charAt(structureText.length()-1) ) {
                structureText = structureText.substring(startpos, 
                        structureText.length()-1);
            }
                    
            // Remove trailing ESCAPE
            // (if text has trailing NEWLINE, an ESCAPE character may be added 
            // when converting from XML to structured text)
            if (structureText.charAt(structureText.length()-1) == this.ESCAPE 
                    && structureText.charAt(structureText.length()-2) != this.ESCAPE) {
                structureText = structureText.substring(0, structureText.length()-1);
            }
        } catch (Exception e) {
            this.logger.warn(
                    "Attempting to parse invalid or empty StructuredText string");
        }
                
        return structureText;
    }
        
    
    protected int parseParagraph(String text, int startPos, Element root) {

        Element paragraphElement = new Element(lookupTag("paragraph"));
        root.addContent(paragraphElement);

        int pos = startPos + this.PARAGRAPH_START.length();

        // end paragraph when new paragraph or new list begins, or at end of text
        int endPos = text.length();
        int nextParagraph = text.indexOf(this.PARAGRAPH_START, pos);
        int nextList = text.indexOf(this.LIST_START, pos);
        int nextNumlist = text.indexOf(this.NUMLIST_START, pos);

        if (!(nextParagraph < 0) && (nextParagraph < endPos))
            endPos = nextParagraph;
        if (!(nextList < 0) && (nextList < endPos))
            endPos = nextList;
        if (!(nextNumlist < 0) && (nextNumlist < endPos))
            endPos = nextNumlist;

        while (pos < endPos) {
            // contents will now be basictekst
            pos = parseBasicText(text, pos, paragraphElement);
        }

        return endPos;
        // return without adding PARAGRAPH_CLOSE 
        // (because newline signifies a new element)
    }
    
        
    protected int parseEscape(String text, int startPos, Element root) {
        char escaped = text.charAt(startPos+1);
                
        // if escapable character
        if ( escaped == this.NEWLINE 
                || escaped == this.ESCAPE
                || String.valueOf(escaped).equals(this.LISTITEM_MARKER)
                || String.valueOf(escaped).equals(this.NUMLISTITEM_MARKER) ) {   
            // Add char at position AFTER the escape char
            root.addContent( String.valueOf(escaped) );
            return 2+startPos; // re-start parsing AFTER escaped character                                    
        }
        root.addContent( String.valueOf(this.ESCAPE) );
        return ++startPos;
    }
    
    
    protected int parseNewline(String text, int startPos, Element root) {
        Element paragraphElement = new Element(lookupTag("newline"));
        root.addContent(paragraphElement);
        // Assuming newline is always represented by the "\n" character 
        // while parsing text in this class (length = 1)
        return ++startPos;
    }
    
    
    protected int parseList(String text, int startPos, Element root) {

        Element listElement = new Element(lookupTag("unordered-list"));
        root.addContent(listElement);

        int pos = startPos + this.LIST_START.length();

        // end paragraph when new paragraph or new list begins, or at end of text
        int endPos = text.length();
        int nextParagraph = text.indexOf(this.PARAGRAPH_START, pos);
        int listClose = text.indexOf(this.LIST_CLOSE, pos);
        int numListStart = text.indexOf(this.NUMLIST_START, pos);

        if (!(nextParagraph < 0) && (nextParagraph < endPos))
            endPos = nextParagraph;
        if (!(listClose < 0) && (listClose < endPos))
            endPos = listClose;
        if (!(numListStart < 0) && (numListStart < endPos))
            endPos = numListStart;

        pos = pos - this.LISTITEM_START.length();
        // Move pos back to listitem start...
        while (pos < endPos) {
            // only listitems are defined
            pos = parseListitem(text, pos, listElement);
        }
        return endPos;
        // return without adding PARAGRAPH_CLOSE 
        // (because newline signifies a new element)
    }
    
    
    protected int parseNumlist(String text, int startPos, Element root) {

        Element listElement = new Element(lookupTag("ordered-list"));
        root.addContent(listElement);

        int pos = startPos + this.NUMLIST_START.length();

        // end paragraph when new paragraph or new list begins, or at end of text
        int endPos = text.length();
        int nextParagraph = text.indexOf(this.PARAGRAPH_START, pos);
        int numlistClose = text.indexOf(this.NUMLIST_CLOSE, pos);
        int listStart = text.indexOf(this.LIST_START, pos);

        if (!(nextParagraph < 0) && (nextParagraph < endPos))
            endPos = nextParagraph;
        if (!(numlistClose < 0) && (numlistClose < endPos))
            endPos = numlistClose;
        if (!(listStart < 0) && (listStart < endPos))
            endPos = listStart;

        pos = pos - this.NUMLISTITEM_START.length();
        // Move pos back to listitem start...
        while (pos < endPos) {
            // only listitems are defined
            pos = parseNumlistitem(text, pos, listElement);
        }
        return endPos;
        // return without adding PARAGRAPH_CLOSE 
        // (because newline signifies a new element)
    }
    
    
    protected int parseListitem(String text, int startPos, Element parent) {
        int pos = startPos + this.LISTITEM_START.length();

        int listitemClosePos = text.indexOf(this.LISTITEM_START, pos);
        // Close when new listitem starts
        int listClosePos = text.indexOf(this.LIST_CLOSE, pos);
        int numlistStartPos = text.indexOf(this.NUMLISTITEM_START, pos);

        int endPos = text.length();
        if (!(listitemClosePos < 0))
            endPos = listitemClosePos;
        if (!(listClosePos < 0) && (listClosePos < endPos))
            endPos = listClosePos;
        if (!(numlistStartPos < 0) && (numlistStartPos < endPos))
            endPos = numlistStartPos;

        Element listitemElement = new Element(lookupTag("listitem"));
        parent.addContent(listitemElement);

        while (pos < endPos) {
            // inneholdet skal n� v�re basictekst
            pos = parseBasicText(text, pos, listitemElement);
        }

        return endPos;
    }
    
    
    protected int parseNumlistitem(String text, int startPos, Element parent) {
        int pos = startPos + this.NUMLISTITEM_START.length();

        int numlistitemClosePos = text.indexOf(this.NUMLISTITEM_START, pos);
        // Close when new listitem starts
        int numlistClosePos = text.indexOf(this.NUMLIST_CLOSE, pos);
        int listStartPos = text.indexOf(this.LISTITEM_START, pos);

        int endPos = text.length();
        if (!(numlistitemClosePos < 0))
            endPos = numlistitemClosePos;
        if (!(numlistClosePos < 0) && (numlistClosePos < endPos))
            endPos = numlistClosePos;
        if (!(listStartPos < 0) && (listStartPos < endPos))
            endPos = listStartPos;

        Element listitemElement = new Element(lookupTag("listitem"));
        parent.addContent(listitemElement);

        while (pos < endPos) {
            // contents will now be basictekst
            pos = parseBasicText(text, pos, listitemElement);
        }

        return endPos;
    }
    
    
    // basicText consist of bold, italic, link, refrence, subscript and
    // superscript.
    protected int parseBasicText(String basicText, int pos, Element parent) {

        int nextPos = pos;
        
        if ( escapeAtPos(basicText, pos) ) {
            nextPos = parseEscape(basicText, pos, parent);
        } else if (boldAtPos(basicText, pos)) {
            nextPos = parseBold(basicText, pos, parent);
        } else if (italicAtPos(basicText, pos)) {
            nextPos = parseItalic(basicText, pos, parent);
        } else if (refrenceAtPos(basicText, pos)) {
            nextPos = parseRefrence(basicText, pos, parent);
        } else if (linkAtPos(basicText, pos)) {
            nextPos = parseLink(basicText, pos, parent);
        } else if (subAtPos(basicText, pos)) {
            nextPos = parseSub(basicText, pos, parent);
        } else if (superAtPos(basicText, pos)) {
            nextPos = parseSuper(basicText, pos, parent);
        } else if (newlineAtPos(basicText, pos)) {
            nextPos = parseNewline(basicText, pos, parent);
        } else {
            nextPos = parsePlainText(basicText, pos, parent);
        }

        return nextPos;
    }
    
    
    // helper method to remove ESCAPE characters from an substring
    // (when using escape character for signifficant symbols within a context)
    private StringBuffer removeEscapeChars(String text, int startPos, int endPos) {
        StringBuffer substring = new StringBuffer( text.substring(startPos, endPos) );
        for (int i = 0; i < substring.length(); i++) {
            if (substring.charAt(i) == this.ESCAPE) {
                substring.deleteCharAt(i);
            }
        }
        return substring;
    }
    
        
    protected int parseBold(String text, int pos, Element element) {
        int startPos = pos + this.BOLD_START.length();
        //int endPos = text.indexOf(BOLD_CLOSE, startPos);
        int endPos = startPos+1; 
        
        do {
            // contine searching for the real BOLD_CLOSE
            endPos = text.indexOf(this.BOLD_CLOSE, ++endPos);
        } while ( text.charAt(endPos-1) == this.ESCAPE );
                
        StringBuffer substring = removeEscapeChars(text, startPos, endPos);
        String boldText = substring.toString();
        
        Element bold = new Element(lookupTag("bold"));
        bold.addContent(boldText);
        element.addContent(bold);

        return endPos + this.BOLD_CLOSE.length();
    }
    
    
    protected int parseItalic(String text, int pos, Element element) {
        int startPos = pos + this.ITALIC_START.length();
        //int endPos = text.indexOf(ITALIC_CLOSE, startPos);
        int endPos = startPos+1; 
        
        do {
            // contine searching for the real ITALIC_CLOSE
            endPos = text.indexOf(this.ITALIC_CLOSE, ++endPos);
        } while ( text.charAt(endPos-1) == this.ESCAPE );

        StringBuffer substring = removeEscapeChars(text, startPos, endPos);
        String italicText = substring.toString();

        Element italic = new Element(lookupTag("italic"));
        italic.addContent(italicText);
        element.addContent(italic);

        return endPos + this.ITALIC_CLOSE.length();
    }
    

    // tar IKKE hensyn til ESCAPE-karakter, da dette IKKE skal brukes i
    // den spesielle syntax'en her
    protected int parseRefrence(String text, int pos, Element element) {
        int startPos = pos + this.REF_START.length();
        int endPos = text.indexOf(this.REF_CLOSE, startPos);

        String refrenceText = text.substring(startPos, endPos);
        Element refrence = new Element(lookupTag("reference"));

        int attributePos = refrenceText.indexOf(this.REF_ATTRIBUTE);

        if (attributePos > -1) {
            String elementValue = refrenceText.substring(0, attributePos);
            String typeAttributeValue = refrenceText.substring(attributePos
                    + this.REF_ATTRIBUTE.length(), refrenceText.length());

            Attribute typeAttribute = new Attribute(
                    lookupTag("reference-type"), typeAttributeValue);
            refrence.setAttribute(typeAttribute);
            refrence.addContent(elementValue);

        } else
            refrence.addContent(refrenceText);

        element.addContent(refrence);

        return endPos + this.REF_CLOSE.length();
    }
    
    
    // The only escapeable char for URLs will be the '"' within a description! 
    protected int parseLink(String text, int pos, Element parent) {
        
        String description = text.substring(pos + this.LINK_START.length(), text
                .indexOf(this.LINK_MIDDLE, pos));
        
        // replace '\"' (escaped '"') with '"'
        description = description.replaceAll("\\\\\"", "\"");
        // replace '\NEWLINE' (escaped newline) with 'NEWLINE'
        description = description.replaceAll("\\\\"+this.NEWLINE, "\n");
        
        int endUrl = pos + description.length() + this.LINK_MIDDLE.length();
        while (endUrl < text.length() 
                && text.indexOf(" ", endUrl) != endUrl
                && text.indexOf(this.LINE_SEPARATOR, endUrl) != endUrl
                && text.indexOf(". ", endUrl) != endUrl
                && text.indexOf(", ", endUrl) != endUrl
                && text.indexOf("; ", endUrl) != endUrl) {
            endUrl++;
        }
        String url = text.substring(text.indexOf(this.LINK_MIDDLE, pos)
                + this.LINK_MIDDLE.length(), endUrl);

        Element link = new Element(lookupTag("link"));

        Element e = new Element(lookupTag("url-description"));
                
        e.addContent(description);        
        link.addContent(e);

        e = new Element(lookupTag("url"));
        e.addContent(url);
        link.addContent(e);

        parent.addContent(link);

        return endUrl;
    }
    
    
    protected int parseSub(String text, int pos, Element parent) {
        int startPos = pos + this.SUB_START.length();
        int endPos = startPos-1; 
        
        do {
            // contine searching for the real SUB_END
            endPos = text.indexOf(this.SUB_END, ++endPos);
        } while ( text.charAt(endPos-1) == this.ESCAPE );
        
        StringBuffer substring = removeEscapeChars(text, startPos, endPos);
        String subtext = substring.toString();
        
        Element sub = new Element(lookupTag("sub"));
        sub.addContent(subtext);
        parent.addContent(sub);

        return endPos + this.SUB_END.length();
    }
    
      
    protected int parseSuper(String text, int pos, Element parent) {
        int startPos = pos + this.SUPER_START.length();
        int endPos = startPos-1; 
        
        do {
            // contine searching for the real SUPER_END
            endPos = text.indexOf(this.SUPER_END, ++endPos);
        } while (text.charAt(endPos-1) == this.ESCAPE );
        
        StringBuffer substring = removeEscapeChars(text, startPos, endPos);
        String supertext = substring.toString();
        
        Element sup = new Element(lookupTag("sup"));
        sup.addContent(supertext);
        parent.addContent(sup);

        return endPos + this.SUPER_END.length();
    }
    

    protected int parsePlainText(String text, int pos, Element parent) {
        int startPos = pos;

        // parse single char until one with a special meaning occurs
        while (pos < text.length()) {
            pos++; // first position is already examined
                        
            if ( escapeAtPos(text, pos)
                    || paragraphAtPos(text, pos) 
                    || listAtPos(text, pos)
                    || boldAtPos(text, pos) 
                    || refrenceAtPos(text, pos)
                    || italicAtPos(text, pos) 
                    || listitemAtPos(text, pos)
                    || numlistitemAtPos(text, pos) 
                    || linkAtPos(text, pos)
                    || subAtPos(text, pos) 
                    || superAtPos(text, pos)
                    || newlineAtPos(text, pos) )
                break;
        }

        parent.addContent(text.substring(startPos, pos));

        return pos;
    }

    
    /**
     * Generate structured text from an XML structure.
     * 
     * @param element
     *            the root <code>Element</code> of the XML structure
     * @return a <code>String</code> containing the structured text
     */
    protected String generateStructuredText(Element element) {

        StringBuffer buffer = new StringBuffer();
        
        for (Iterator i = element.getContent().iterator(); i.hasNext();) {

            Object object = i.next();

            if (object instanceof Element) {
                Element child = (Element) object;
                                
                String tagName = reverseLookupTag(child.getName());
                
                if (tagName.equals("bold")) {
                    buffer.append(this.BOLD_START);
                    //buffer.append(generateStructuredText(child));
                    buffer.append( escapeBold(child.getText()) );
                    buffer.append(this.BOLD_CLOSE);
                } else if (tagName.equals("italic")) {
                    buffer.append(this.ITALIC_START); 
                    //buffer.append(generateStructuredText(child));
                    buffer.append( escapeItalic(child.getText()) );
                    buffer.append(this.ITALIC_CLOSE);
                } else if (tagName.equals("reference")) {
                    // ingen ESCAPE-karakter kan brukes i referanse 
                    buffer.append(this.REF_START);
                    buffer.append(generateStructuredText(child));
                    if (child.getAttributeValue(lookupTag("reference-type")) != null) {
                        buffer.append(this.REF_ATTRIBUTE);
                        buffer.append( 
                                child.getAttributeValue(lookupTag("reference-type")) );
                    }
                    buffer.append(this.REF_CLOSE);
                } else if (tagName.equals("link")) {
                    // buffer.append();
                    buffer.append(generateStructuredText(child));
                    // buffer.append();
                } else if (tagName.equals("url-description")) {
                    buffer.append(this.LINK_START);
                    //buffer.append(generateStructuredText(child));
                    buffer.append(addEscapeChar(escapeQuotes(child.getText())));
                    //buffer.append( escapeQuotes(child.getText()) );
                    buffer.append(this.LINK_MIDDLE);
                } else if (tagName.equals("url")) {
                    // buffer.append();
                    //buffer.append(child.getText());
                    buffer.append(generateStructuredText(child));
                    // buffer.append();
                } else if (tagName.equals("unordered-list")
                        || tagName.equals("ordered-list")) {
                    buffer.append(this.LINE_SEPARATOR);
                    buffer.append(generateStructuredText(child));
                    // buffer.append(LIST_CLOSE);
                } else if (tagName.equals("listitem")) {
                    String parentName = reverseLookupTag(element.getName());
                    if (parentName.equals("ordered-list"))
                        buffer.append(this.NUMLISTITEM_START);
                    else
                        buffer.append(this.LISTITEM_START);
                    buffer.append(generateStructuredText(child));
                    // buffer.append();
                } else if (tagName.equals("paragraph")) {
                    buffer.append(this.PARAGRAPH_START);
                    buffer.append(generateStructuredText(child));
                    // buffer.append();
                } else if (tagName.equals("newline")) {
                    buffer.append(this.NEWLINE);
                } else if (tagName.equals("sub")) {
                    buffer.append(this.SUB_START);
                    //buffer.append(generateStructuredText(child));
                    buffer.append(addEscapeChar(escapeQuotes(child.getText())));
                    buffer.append(this.SUB_END);
                } else if (tagName.equals("sup")) {
                    buffer.append(this.SUPER_START);
                    //buffer.append(generateStructuredText(child));
                    buffer.append(addEscapeChar(escapeQuotes(child.getText())));
                    buffer.append(this.SUPER_END);
                } else { // plaintextelement
                    throw new StructuredTextException(
                            "Unexpected element name: " + tagName);
                }
            } else if (object instanceof Text) { // plaintextelement

                Element parent = (Element) ((Text) object).getParent();
                String tagName = reverseLookupTag(parent.getName());

                // Don't include text nodes for the following parent
                // ("block level") elements:
                if (!"root".equals(tagName)
                        && !"unordered-list".equals(tagName)
                        && !"ordered-list".equals(tagName)
                        && !"link".equals(tagName)) {
                    Text child = (Text) object;
                    
                    String contents = child.getText();
                    // add ESCAPE char where needed
                    buffer.append( addEscapeChar(contents) );
                }
            }
        } // end for

        return buffer.toString();
    }
    
    private String escapeBold(String contents) {
        return contents.replaceAll("\\*", "\\\\*");
    }
    
    private String escapeItalic(String contents) {
        return contents.replaceAll("_", "\\\\_");
    }
    
    private String escapeQuotes(String contents) {
        return contents.replaceAll("\"", "\\\\\"");
    }
    
    
    /* 
     * ESCAPE char must be added for:
     * - "\" (escaped backslash)
     * - "\\n" (escaped newline)
     * - "\n-" ('-' as first char in paragraph or after newline, which when 
     *         escaped and parsed to XML will actually be its own element)
     * - "\n#" ('#' as first char in paragraph or after newline)
     */
    protected String addEscapeChar(String contents) {        
        contents = unifyNewlines(contents);        
        // When being parsed from structured text, an escaped listitem-marker
        // will be added as a separate DOM element.
        // BUT: If the XML is edited as text og in external editor, the 
        // element is interpreted as consecutive string
        // HENCE: Will have to test for both posibilities!
        if ( contents.equals( String.valueOf(this.ESCAPE) )
                || contents.equals( String.valueOf(this.NEWLINE) )
                || contents.equals(this.LISTITEM_MARKER)
                || contents.startsWith(this.LISTITEM_MARKER+this.SPACE)
                || contents.equals(this.NUMLISTITEM_MARKER)
                || contents.startsWith(this.NUMLISTITEM_MARKER+this.SPACE) ) {
            return String.valueOf(this.ESCAPE).concat(contents);
        }
        return contents.replaceAll("([\\n])", "\\\\$1");
    }
    
    
    public String parseElement(Element root) {
        String text = generateStructuredText(root);
        text = removeStartingAndTrailingNewlines(text);
        
        if (root.getChildren().size() == 0)
            return "";

        Element child = (Element) root.getChildren().get(0);

        // Delete first paragraph/list start

        if ("paragraph".equals(reverseLookupTag(child.getName()))) {
            return text.substring(this.PARAGRAPH_START.length());
        }

        if ("unordered-list".equals(reverseLookupTag(child.getName()))) {
            return text.substring((this.LINE_SEPARATOR + this.LINE_SEPARATOR).length());
        }

        if ("ordered-list".equals(reverseLookupTag(child.getName()))) {
            return text.substring((this.LINE_SEPARATOR + this.LINE_SEPARATOR).length());
        }
        return text;
    }
    
    
    // Various helper methods
    private String lookupTag(String tagName) {
        String tag = (String) this.tagNames.get(tagName);
        if (tag == null)
            tag = tagName;
        return tag;
    }
    
    
    private String reverseLookupTag(String mappedTag) {
        for (Iterator i = this.tagNames.keySet().iterator(); i.hasNext();) {
            String tagName = (String) i.next();
            if (((String) this.tagNames.get(tagName)).equals(mappedTag)) {
                return tagName;
            }
        }
        return mappedTag;
    }
    
    
    public static void dumpXML(Document doc, PrintStream out) {
        try {
            Format format = Format.getPrettyFormat();
            
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.setFormat(format);
            // XMLOutputter xmlOutputter = new XMLOutputter(" ", true);
            //
            // xmlOutputter.setExpandEmptyElements(true);
            // xmlOutputter.setTextNormalize(false);
            String xml = xmlOutputter.outputString(doc);
            out.println(xml);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    
    // brukes for � printe xml til strucured text
    public static void dumpXML(Element element, PrintStream out) {
        try {
            Format format = Format.getPrettyFormat();
            
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.setFormat(format);
            // //XMLOutputter takes a JDOM tree and formats it to a stream as
            // XML.
            // XMLOutputter xmlOutputter = new XMLOutputter(" ", true);
            // xmlOutputter.setTextNormalize(true);
            String xml = xmlOutputter.outputString(element);
            out.println(xml);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
        
    
    /**
     * Test funtion main()
     * 
     * USAGE:
     * To test convertion of StructuredText -> XML and back, run:
     *    org.vortikal.util.text.EvenStructuredText <en|no> <textfile>
     * or to parse <structuredtext> within an XML-document:
     *    org.vortikal.util.text.EvenStructuredText <en|no> -xml <xmlfile>
     * 
     * REMARKS:
     * - 'no' => norwegian XML (fritekst)
     * - 'en' => english XML (structuredtext) [default]
     * - When parsing XML only, this test function will ONLY test the first
     *   occuring structuredtext/fritekst node contained by the XML root node
     */
    public static void main(String[] args) {
        try {
            EvenStructuredText parser = new EvenStructuredText();            
            
            if (args.length < 1) {
                printUsageText( parser.getClass().getName() );
                return;
            }
            
            // Norwegian XML
            if ( "no".equals(args[0]) ) {
                parser.setTextMappings( getNorwegianTagNames() );                
            } 
            // English XML (default)
            else {
                // error if URI
                if (args[0].lastIndexOf('/') != -1 
                        || args[0].lastIndexOf('\\') != -1 ) {
                    printUsageText( parser.getClass().getName() );
                    return;
                }
                parser.setTextMappings( getEnglishTagNames() );
            }
            
            // Either parse StructuredText -> XML and back
            //  or
            // Parse only XML -> StructuredText 
            Document doc;
            String structuredtext;
            // StructuredText -> XML -> StructuredText
            if ( !"-xml".equals(args[1]) ) {
                File textFile = new File(args[1]);
                if (!textFile.exists()) {
                    System.out.println("No such textfile: " + textFile.getName());
                    return;
                }                
                
                char[] buffer = new char[(int) textFile.length()];
                FileReader reader = new FileReader(textFile);
                reader.read(buffer);
                
                String text = new String(buffer);
                
                System.out.println("Orginal tekst: ");
                System.out.println(text);
                
                Element root = parser.parseStructuredText(text);
                doc = new Document(root); 
                structuredtext = parser.parseElement(root);
            } 
            // XML -> StructuredText only
            else {
                SAXBuilder saxparser = new SAXBuilder();
                String xmlpath = args[2];
                if ( xmlpath.startsWith("http") ) {  
                    try {
                        doc = saxparser.build(xmlpath);    
                    } catch (FileNotFoundException fnfe ) {
                        System.out.println("XML files not found at " + xmlpath);
                        return;
                    }
                    
                } else {
                    File xmlFile = new File(args[2]);                                        
                    if (!xmlFile.exists()) {
                        System.out.println("No such XML file: " + xmlFile.getName());
                        return;
                    }
                    doc = saxparser.build(xmlFile);
                }
                
                Element root = doc.getRootElement();
                Element fritekst = root.getChild("fritekst");
                structuredtext = parser.parseElement(fritekst);
            }
            
            System.out.print("\n\nXML:\n");
            dumpXML(doc, System.out);
            
            // make structuredtext
            System.out.println("Converted back: ");
            System.out.println(structuredtext);
        } catch (Exception e) {
            e.printStackTrace();
        }  
    } // end main

    
    /*
     * Private helper methods used by test function main()
     */
    
    private static void printUsageText(String parser) {
        System.out.print("\nUsage: \n" + 
                parser + " <en|no> <textfile path> \n\t or:\n" +
                parser + " <en|no> -xml <XML-file path|XML-URL>");
        
    }
    
    private static Map getEnglishTagNames() {
        Map tagNames = new HashMap();
        tagNames.put("root", "evenstructuredtext");
        tagNames.put("bold", "bold");
        tagNames.put("italic", "it");
        tagNames.put("link", "link");
        tagNames.put("url", "url");
        tagNames.put("url-description", "description");
        tagNames.put("unordered-list", "unsortedlist");
        tagNames.put("ordered-list", "sortedlist");
        tagNames.put("listitem", "listitem");
        tagNames.put("paragraph", "paragraph");
        tagNames.put("reference", "refrence");
        tagNames.put("reference-type", "type");
        tagNames.put("sub", "sub");
        tagNames.put("sup", "sup");
        tagNames.put("newline", "newline");
        return tagNames;
    }
    
    private static Map getNorwegianTagNames() {
        Map tagNames = new HashMap();
        tagNames.put("root", "fritekst");
        tagNames.put("bold", "fet");
        tagNames.put("italic", "kursiv");
        tagNames.put("reference", "referanse");
        tagNames.put("reference-type", "type");
        tagNames.put("reference-language", "spraak");
        tagNames.put("link", "weblenke");
        tagNames.put("url", "webadresse");
        tagNames.put("url-description", "lenketekst");
        tagNames.put("unordered-list", "punktliste");
        tagNames.put("ordered-list", "nummerertliste");
        tagNames.put("listitem", "listepunkt");
        tagNames.put("paragraph", "avsnitt");
        tagNames.put("sub", "sub");
        tagNames.put("sup", "sup");
        tagNames.put("newline", "linjeskift");
        return tagNames;
    }
}
