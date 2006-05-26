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
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
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
        tagNames = customMappings;
    }

    protected String LINE_SEPARATOR = "\n";
    protected String SPACE = " ";
    protected String LISTITEM_MARKER = "-";
    protected String LISTITEM_START = LINE_SEPARATOR + LISTITEM_MARKER + SPACE;
    protected String NUMLISTITEM_MARKER = "#";
    protected String NUMLISTITEM_START = LINE_SEPARATOR + NUMLISTITEM_MARKER + SPACE;
    protected String LIST_START = LISTITEM_START;
    protected String LIST_CLOSE = LINE_SEPARATOR + LINE_SEPARATOR;
    protected String NUMLIST_START = NUMLISTITEM_START;
    protected String NUMLIST_CLOSE = LINE_SEPARATOR + LINE_SEPARATOR;
    protected String PARAGRAPH_START = LINE_SEPARATOR + LINE_SEPARATOR;
    protected String PARAGRAPH_CLOSE = LINE_SEPARATOR + LINE_SEPARATOR;
    protected String BOLD_START = "*";
    protected String BOLD_CLOSE = "*";
    protected String ITALIC_START = "_";
    protected String ITALIC_CLOSE = "_";
    protected String LINK_START = "\"";
    protected String LINK_MIDDLE = "\":";
    protected String REF_START = "[";
    protected String REF_ATTRIBUTE = ":";
    protected String REF_CLOSE = "]";
    protected String SUB_START = "sub:\"";
    protected String SUB_END = "\"";
    protected String SUPER_START = "super:\"";
    protected String SUPER_END = "\"";
    protected char NEWLINE = '\n';
    protected char ESCAPE = '\\';

    protected Map tagNames = new HashMap();
    
    
    public Set getTagNames() {
        return tagNames.keySet();
    }
    
    
    private void initTagNames() {
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
        tagNames.put("super", "sup");
        tagNames.put("newline", "linjeskift");
    }
    
    
    protected boolean escapeAtPos(String text, int pos) {
        return (text.indexOf(ESCAPE, pos) == pos);
    }
    
        
    protected boolean paragraphAtPos(String text, int pos) {
        return (text.indexOf(PARAGRAPH_START, pos) == pos);
    }
    
        
    // test that current char is NEWLINE which i NOT followed
    // by another NEWLINE (newline + newline = paragraph)
    protected boolean newlineAtPos(String text, int pos) {
        if (pos < text.length() - 1
                && text.charAt(pos) == NEWLINE
                && text.charAt(pos + 1) != NEWLINE)
            return true;
        else
            return false;
    }
    
    
    protected boolean listAtPos(String text, int pos) {
        return (text.indexOf(LIST_START, pos) == pos);
    }
    
    
    protected boolean numlistAtPos(String text, int pos) {
        return (text.indexOf(NUMLIST_START, pos) == pos);
    }
    
    
    protected boolean listitemAtPos(String text, int pos) {
        return (text.indexOf(LISTITEM_START, pos) == pos);
    }
    
    
    protected boolean numlistitemAtPos(String text, int pos) {
        return (text.indexOf(NUMLISTITEM_START, pos) == pos);
    }
    
    
    protected boolean boldAtPos(String text, int pos) {
        if (text.indexOf(BOLD_START, pos) != pos)
            return false;
        int endPos = text.indexOf(BOLD_CLOSE, pos + BOLD_START.length());

        if (endPos > 0)
            return !(endOfBlockLevelElement(text, pos + BOLD_START.length(),
                    endPos));

        return false;
    }
    
    
    protected boolean italicAtPos(String text, int pos) {
        if (text.indexOf(ITALIC_START, pos) != pos)
            return false;
        int endPos = text.indexOf(ITALIC_CLOSE, pos + ITALIC_START.length());

        if (endPos > 0)
            return !endOfBlockLevelElement(text, pos + ITALIC_START.length(),
                    endPos);

        return false;
    }
    
    
    protected boolean linkAtPos(String text, int pos) {
        // links look like: "description":http://url-of-something
        if (text.indexOf(LINK_START, pos) != pos)
            return false;

        int middlePos = text.indexOf(LINK_MIDDLE, pos + LINK_START.length());

        if (middlePos > 0) {

            // Check that the " at pos is the closest one to ":
            // (unless the " character is escaped; such are ignored)
            int testPos = pos + LINK_START.length();
            
            do {
                // contine searching for the real closing "
                testPos = text.indexOf(LINK_START, ++testPos);
            } while ( text.charAt(testPos-1) == ESCAPE );
            
            if (testPos != middlePos)
                return false;

            // Check that no LINE_SEPERATOR come before LINK_MIDDLE 
            // (except when escaped)
            int lineSepPos = text.indexOf(LINE_SEPARATOR, pos
                    + LINK_START.length());
            if ((lineSepPos > 0) && (lineSepPos < middlePos) 
                    && text.charAt(lineSepPos-1) != ESCAPE)
                return false;
            
            // Check that no PARAGRAPH_START or LIST_START come before
            // LINK_MIDDLE
            if (endOfBlockLevelElement(text, pos + LINK_START.length(),
                    middlePos))
                return false;

            // Check that middlePos is not at the end of the text
            if (middlePos == text.length() - LINK_MIDDLE.length())
                return false;

            // Check that LINE_SEPARATOR or whitespace is not righT after
            // LINK_MIDDLE
            if ((text.charAt(middlePos + LINK_MIDDLE.length()) == ' ')
                    || (text
                            .indexOf(LINE_SEPARATOR, pos + LINK_MIDDLE.length()) == pos
                            + LINK_MIDDLE.length()))
                return false;

            return true;
        }

        return false;
    }
    
    
    protected boolean refrenceAtPos(String text, int pos) {
        if (text.indexOf(REF_START, pos) != pos)
            return false;

        int endPos = text.indexOf(REF_CLOSE, pos + REF_START.length());

        if (endPos > 0)
            return !(endOfBlockLevelElement(text, pos + REF_START.length(),
                    endPos));

        return false;
    }
    
    
    // sjekker om visse spesialtegn dukker opp f�r sluttposisjonen til elementet
    // som sjekkes.
    protected boolean endOfBlockLevelElement(String text, int pos, int endPos) {
        int paragraphPos = text.indexOf(PARAGRAPH_START, pos);
        if (paragraphPos > 0 && paragraphPos < endPos)
            return true;

        int listPos = text.indexOf(LIST_START, pos);
        if (listPos > 0 && listPos < endPos)
            return true;

        int numListPos = text.indexOf(NUMLIST_START, pos);
        if (numListPos > 0 && numListPos < endPos)
            return true;

        return false;
    }
    
    
    protected boolean subAtPos(String text, int pos) {
        // sub is given on the form: sub:"text"

        int startPos = text.indexOf(SUB_START, pos);

        if (startPos != pos) {
            return false;
        }

        int endPos = text.indexOf(SUB_END, pos + SUB_START.length());

        String subtext = text.substring(startPos, endPos);

        if (endPos < 0) {
            return false;
        } else if (subtext.indexOf(LINE_SEPARATOR) != -1 
                && subtext.charAt( subtext.indexOf(LINE_SEPARATOR) -1 ) != ESCAPE ) {
            return false;
        } else {
            return true;
        }   
    }
    
    
    protected boolean superAtPos(String text, int pos) {
        // super is given on the form: super:"text"

        int startPos = text.indexOf(SUPER_START, pos);

        if (startPos != pos) {
            return false;
        }

        int endPos = text.indexOf(SUPER_END, pos + SUPER_START.length());

        String supertext = text.substring(startPos, endPos);

        if (endPos < 0) {
            return false;
        } else if (supertext.indexOf(LINE_SEPARATOR) != -1 
                && supertext.charAt( supertext.indexOf(LINE_SEPARATOR) -1 ) != ESCAPE ) {
            return false;
        } else {
            return true;
        }
    }
    
    
    public Element parseStructuredText(String text) {

        String structureText = unifyNewlines(text);

        // All structuretext m� inn i avsnitt eller liste
        if ((!structureText.startsWith(PARAGRAPH_START))
                || (!structureText.startsWith(LIST_START))
                || (!structureText.startsWith(NUMLIST_START))) {
            structureText = PARAGRAPH_START + structureText;
        }

        Element root = new Element(lookupTag("root"));

        int pos = 0;
        int nextPos = 0;

        while (pos < structureText.length()) {
            if (listAtPos(structureText, pos + LINE_SEPARATOR.length())) {
                nextPos = parseList(structureText, pos
                        + LINE_SEPARATOR.length(), root);
            } else if (numlistAtPos(structureText, pos
                    + LINE_SEPARATOR.length())) { // telle opp riktig
                nextPos = parseNumlist(structureText, pos
                        + LINE_SEPARATOR.length(), root);
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
        text = text.replaceAll("[\r\n]", "\n");
        text = text.replaceAll("\r", "\n");
        return text;
    }
    
    
    protected int parseParagraph(String text, int startPos, Element root) {

        Element paragraphElement = new Element(lookupTag("paragraph"));
        root.addContent(paragraphElement);

        int pos = startPos + PARAGRAPH_START.length();

        // slutte avsnitt n�r nytt avsnitt eller liste begynner, eller ved slutt
        // p� tekst.
        int endPos = text.length();
        int nextParagraph = text.indexOf(PARAGRAPH_START, pos);
        int nextList = text.indexOf(LIST_START, pos);
        int nextNumlist = text.indexOf(NUMLIST_START, pos);

        if (!(nextParagraph < 0) && (nextParagraph < endPos))
            endPos = nextParagraph;
        if (!(nextList < 0) && (nextList < endPos))
            endPos = nextList;
        if (!(nextNumlist < 0) && (nextNumlist < endPos))
            endPos = nextNumlist;

        while (pos < endPos) {
            // inneholdet skal n� v�re basictekst
            pos = parseBasicText(text, pos, paragraphElement);
        }

        return endPos;
        // returenerer uten � legge til PARAGRAPH_CLOSE fordi linjeskift brukes
        // til � starte nytt element
    }
    
        
    protected int parseEscape(String text, int startPos, Element root) {
        char escaped = text.charAt(startPos+1);
                
        // if escapable character
        if ( escaped == NEWLINE 
                || escaped == ESCAPE
                || String.valueOf(escaped).equals(LISTITEM_MARKER)
                || String.valueOf(escaped).equals(NUMLISTITEM_MARKER) ) {   
            // Add char at position AFTER the escape char
            root.addContent( String.valueOf(escaped) );
            return 2+startPos; // re-start parsing AFTER escaped character                                    
        } 
        // else ignore
        else {
            root.addContent( String.valueOf(ESCAPE) );
            return ++startPos;
        }
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

        int pos = startPos + LIST_START.length();

        // slutte avsnitt n�r nytt avsnitt begynner eller liste
        // slutter, eller ved slutt p� tekst.
        int endPos = text.length();
        int nextParagraph = text.indexOf(PARAGRAPH_START, pos);
        int listClose = text.indexOf(LIST_CLOSE, pos);
        int numListStart = text.indexOf(NUMLIST_START, pos);

        if (!(nextParagraph < 0) && (nextParagraph < endPos))
            endPos = nextParagraph;
        if (!(listClose < 0) && (listClose < endPos))
            endPos = listClose;
        if (!(numListStart < 0) && (numListStart < endPos))
            endPos = numListStart;

        pos = pos - LISTITEM_START.length();
        // Move pos back to listitem start...
        while (pos < endPos) {
            // only listitems are defined
            pos = parseListitem(text, pos, listElement);
        }
        return endPos;
        // returenerer uten � legge til PARAGRAPH_CLOSE fordi linjeskift brukes
        // til � starte nytt element
    }
    
    
    protected int parseNumlist(String text, int startPos, Element root) {

        Element listElement = new Element(lookupTag("ordered-list"));
        root.addContent(listElement);

        int pos = startPos + NUMLIST_START.length();

        // slutte avsnitt n�r nytt avsnitt begynner eller liste slutter, eller
        // ved slutt p� tekst.
        int endPos = text.length();
        int nextParagraph = text.indexOf(PARAGRAPH_START, pos);
        int numlistClose = text.indexOf(NUMLIST_CLOSE, pos);
        int listStart = text.indexOf(LIST_START, pos);

        if (!(nextParagraph < 0) && (nextParagraph < endPos))
            endPos = nextParagraph;
        if (!(numlistClose < 0) && (numlistClose < endPos))
            endPos = numlistClose;
        if (!(listStart < 0) && (listStart < endPos))
            endPos = listStart;

        pos = pos - NUMLISTITEM_START.length();
        // Move pos back to listitem start...
        while (pos < endPos) {
            // only listitems are defined
            pos = parseNumlistitem(text, pos, listElement);
        }
        return endPos;
        // returenerer uten � legge til PARAGRAPH_CLOSE fordi linjeskift brukes
        // til � starte nytt element
    }
    
    
    protected int parseListitem(String text, int startPos, Element parent) {
        int pos = startPos + LISTITEM_START.length();

        int listitemClosePos = text.indexOf(LISTITEM_START, pos);
        // Close when new listitem starts
        int listClosePos = text.indexOf(LIST_CLOSE, pos);
        int numlistStartPos = text.indexOf(NUMLISTITEM_START, pos);

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
        int pos = startPos + NUMLISTITEM_START.length();

        int numlistitemClosePos = text.indexOf(NUMLISTITEM_START, pos);
        // Close when new listitem starts
        int numlistClosePos = text.indexOf(NUMLIST_CLOSE, pos);
        int listStartPos = text.indexOf(LISTITEM_START, pos);

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
            // inneholdet skal n� v�re basictekst
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
    private StringBuffer removeESCAPEchars(String text, int startPos, int endPos) {
        StringBuffer substring = new StringBuffer( text.substring(startPos, endPos) );
        for (int i = 0; i < substring.length(); i++) {
            if (substring.charAt(i) == ESCAPE) {
                substring.deleteCharAt(i);
            }
        }
        return substring;
    }
    
        
    protected int parseBold(String text, int pos, Element element) {
        int startPos = pos + BOLD_START.length();
        //int endPos = text.indexOf(BOLD_CLOSE, startPos);
        int endPos = startPos+1; 
        
        do {
            // contine searching for the real BOLD_CLOSE
            endPos = text.indexOf(BOLD_CLOSE, ++endPos);
        } while ( text.charAt(endPos-1) == ESCAPE );
                
        StringBuffer substring = removeESCAPEchars(text, startPos, endPos);
        String boldText = substring.toString();
        
        Element bold = new Element(lookupTag("bold"));
        bold.addContent(boldText);
        element.addContent(bold);

        return endPos + BOLD_CLOSE.length();
    }
    
    
    protected int parseItalic(String text, int pos, Element element) {
        int startPos = pos + ITALIC_START.length();
        //int endPos = text.indexOf(ITALIC_CLOSE, startPos);
        int endPos = startPos+1; 
        
        do {
            // contine searching for the real ITALIC_CLOSE
            endPos = text.indexOf(ITALIC_CLOSE, ++endPos);
        } while ( text.charAt(endPos-1) == ESCAPE );

        StringBuffer substring = removeESCAPEchars(text, startPos, endPos);
        String italicText = substring.toString();

        Element italic = new Element(lookupTag("italic"));
        italic.addContent(italicText);
        element.addContent(italic);

        return endPos + ITALIC_CLOSE.length();
    }
    

    // tar IKKE hensyn til ESCAPE-karakter, da dette IKKE skal brukes i
    // den spesielle syntax'en her
    protected int parseRefrence(String text, int pos, Element element) {
        int startPos = pos + REF_START.length();
        int endPos = text.indexOf(REF_CLOSE, startPos);

        String refrenceText = text.substring(startPos, endPos);
        Element refrence = new Element(lookupTag("reference"));

        int attributePos = refrenceText.indexOf(REF_ATTRIBUTE);

        if (attributePos > -1) {
            String elementValue = refrenceText.substring(0, attributePos);
            String typeAttributeValue = refrenceText.substring(attributePos
                    + REF_ATTRIBUTE.length(), refrenceText.length());

            Attribute typeAttribute = new Attribute(
                    lookupTag("reference-type"), typeAttributeValue);
            refrence.setAttribute(typeAttribute);
            refrence.addContent(elementValue);

        } else
            refrence.addContent(refrenceText);

        element.addContent(refrence);

        return endPos + REF_CLOSE.length();
    }
    
    
    // The only escapeable char for URLs will be the '"' within a description! 
    protected int parseLink(String text, int pos, Element parent) {
        
        String description = text.substring(pos + LINK_START.length(), text
                .indexOf(LINK_MIDDLE, pos));
        
        // replace '\"' (escaped '"') with '"'
        description = description.replaceAll("\\\\\"", "\"");
        // replace '\NEWLINE' (escaped newline) with 'NEWLINE'
        description = description.replaceAll("\\\\"+NEWLINE, "\n");
        
        int endUrl = pos + description.length() + LINK_MIDDLE.length();
        while (endUrl < text.length() 
                && text.indexOf(" ", endUrl) != endUrl
                && text.indexOf(LINE_SEPARATOR, endUrl) != endUrl
                && text.indexOf(". ", endUrl) != endUrl
                && text.indexOf(", ", endUrl) != endUrl
                && text.indexOf("; ", endUrl) != endUrl) {
            endUrl++;
        }
        String url = text.substring(text.indexOf(LINK_MIDDLE, pos)
                + LINK_MIDDLE.length(), endUrl);

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
        int startPos = pos + SUB_START.length();
        int endPos = startPos+1; 
        
        do {
            // contine searching for the real SUB_END
            endPos = text.indexOf(SUB_END, ++endPos);
        } while ( text.charAt(endPos-1) == ESCAPE );
        
        StringBuffer substring = removeESCAPEchars(text, startPos, endPos);
        String subtext = substring.toString();
        
        Element sub = new Element(lookupTag("sub"));
        sub.addContent(subtext);
        parent.addContent(sub);

        return endPos + SUB_END.length();
    }
    
      
    protected int parseSuper(String text, int pos, Element parent) {
        int startPos = pos + SUPER_START.length();
        int endPos = startPos+1; 
        
        do {
            // contine searching for the real SUPER_END
            endPos = text.indexOf(SUPER_END, ++endPos);
        } while ( text.charAt(endPos-1) == ESCAPE );
        
        StringBuffer substring = removeESCAPEchars(text, startPos, endPos);
        String supertext = substring.toString();
        
        Element sup = new Element(lookupTag("super"));
        sup.addContent(supertext);
        parent.addContent(sup);

        return endPos + SUPER_END.length();
    }
    

    protected int parsePlainText(String text, int pos, Element parent) {
        int startPos = pos;

        // parse ett og ett tegn til man treffer et spesialtegn
        while (pos < text.length()) {
            pos++; // f�rste posisjon er allerede sjekket
                        
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
                    buffer.append(BOLD_START);
                    //buffer.append(generateStructuredText(child));
                    buffer.append( escapeBold(child.getText()) );
                    buffer.append(BOLD_CLOSE);
                } else if (tagName.equals("italic")) {
                    buffer.append(ITALIC_START); 
                    //buffer.append(generateStructuredText(child));
                    buffer.append( escapeItalic(child.getText()) );
                    buffer.append(ITALIC_CLOSE);
                } else if (tagName.equals("reference")) {
                    // ingen ESCAPE-karakter kan brukes i referanse 
                    buffer.append(REF_START);
                    buffer.append(generateStructuredText(child));
                    if (child.getAttributeValue(lookupTag("reference-type")) != null) {
                        buffer.append(REF_ATTRIBUTE);
                        buffer.append( 
                                child.getAttributeValue(lookupTag("reference-type")) );
                    }
                    buffer.append(REF_CLOSE);
                } else if (tagName.equals("link")) {
                    // buffer.append();
                    buffer.append(generateStructuredText(child));
                    // buffer.append();
                } else if (tagName.equals("url-description")) {
                    buffer.append(LINK_START);
                    //buffer.append(generateStructuredText(child));
                    buffer.append(addEscapeChar(escapeQuotes(child.getText())));
                    //buffer.append( escapeQuotes(child.getText()) );
                    buffer.append(LINK_MIDDLE);
                } else if (tagName.equals("url")) {
                    // buffer.append();
                    //buffer.append(child.getText());
                    buffer.append(generateStructuredText(child));
                    // buffer.append();
                } else if (tagName.equals("unordered-list")
                        || tagName.equals("ordered-list")) {
                    buffer.append(LINE_SEPARATOR);
                    buffer.append(generateStructuredText(child));
                    // buffer.append(LIST_CLOSE);
                } else if (tagName.equals("listitem")) {
                    String parentName = reverseLookupTag(element.getName());
                    if (parentName.equals("ordered-list"))
                        buffer.append(NUMLISTITEM_START);
                    else
                        buffer.append(LISTITEM_START);
                    buffer.append(generateStructuredText(child));
                    // buffer.append();
                } else if (tagName.equals("paragraph")) {
                    buffer.append(PARAGRAPH_START);
                    buffer.append(generateStructuredText(child));
                    // buffer.append();
                } else if (tagName.equals("newline")) {
                    buffer.append(NEWLINE);
                } else if (tagName.equals("sub")) {
                    buffer.append(SUB_START);
                    //buffer.append(generateStructuredText(child));
                    buffer.append(addEscapeChar(escapeQuotes(child.getText())));
                    buffer.append(SUB_END);
                } else if (tagName.equals("super")) {
                    buffer.append(SUPER_START);
                    //buffer.append(generateStructuredText(child));
                    buffer.append(addEscapeChar(escapeQuotes(child.getText())));
                    buffer.append(SUPER_END);
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
                    //buffer.append( contents );
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
     * - "\n-" ('-' as first char in paragraph or after newline)
     * - "\n#" ('#' as first char in paragraph or after newline)
     */
    protected String addEscapeChar(String contents) {        
        contents = unifyNewlines(contents);        
        /**
         * FIXME
         * Ikke sikkert dette er kompatibelt med andre versjoner av JDOM!
         * (forutsetter at i det escapet char legges til foreldre-noden,
         * se parseEscapeChar(), forblir dette et separat element i DOM-treet)
         */
        if ( contents.equals( String.valueOf(ESCAPE) )
                || contents.equals( String.valueOf(NEWLINE) )
                || contents.equals(LISTITEM_MARKER)
                || contents.equals(NUMLISTITEM_MARKER) ) {
            return String.valueOf(ESCAPE).concat(contents);
        }
        // if escapable char is not a node itself,
        // add ESCAPE to '\n' within text elements of complex nodes
        // (other escapable characters are already handled individually)
        else {
            return contents.replaceAll("([\\n])", "\\\\$1");
        }
    }
    
    
    public String parseElement(Element root) {
        String text = generateStructuredText(root);

        if (root.getChildren().size() == 0)
            return "";

        Element child = (Element) root.getChildren().get(0);

        // Delete first paragraph/list start

        if ("paragraph".equals(reverseLookupTag(child.getName()))) {
            return text.substring(PARAGRAPH_START.length());
        }

        if ("unordered-list".equals(reverseLookupTag(child.getName()))) {
            return text.substring((LINE_SEPARATOR + LINE_SEPARATOR).length());
        }

        if ("ordered-list".equals(reverseLookupTag(child.getName()))) {
            return text.substring((LINE_SEPARATOR + LINE_SEPARATOR).length());
        }
        return text;
    }
    
    
    // Ymse metoder
    private String lookupTag(String tagName) {
        String tag = (String) tagNames.get(tagName);
        if (tag == null)
            tag = tagName;
        return tag;
    }
    
    
    private String reverseLookupTag(String mappedTag) {
        for (Iterator i = tagNames.keySet().iterator(); i.hasNext();) {
            String tagName = (String) i.next();
            if (((String) tagNames.get(tagName)).equals(mappedTag)) {
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
    
    
    public static void main(String[] args) {
        try {
            EvenStructuredText parser = new EvenStructuredText();
            
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
            tagNames.put("unordered-list", "usortertliste");
            tagNames.put("ordered-list", "sortertliste");
            tagNames.put("listitem", "listepunkt");
            tagNames.put("paragraph", "avsnitt");
            tagNames.put("sub", "sub");
            tagNames.put("super", "sup");
            tagNames.put("newline", "linjeskift");
            
            parser.setTextMappings(tagNames);
            
            if (args.length < 1) {
                System.out.println("Usage: " + parser.getClass().getName()
                        + " <textfile>");
                return;
            }
            
            File textFile = new File(args[0]);
            if (!textFile.exists()) {
                System.out.println("No such file: " + textFile.getName());
                return;
            }
            
            
            char[] buffer = new char[(int) textFile.length()];
            FileReader reader = new FileReader(textFile);
            reader.read(buffer);
            
            String text = new String(buffer);
            
            System.out.println("Orginal tekst: " + "\n" + text);
            
            Element root = parser.parseStructuredText(text);
            Document doc = new Document(root);
            
            dumpXML(doc, System.out);
            
            // make structuredtext
            String structuredtext = parser.parseElement(root);
            
            System.out.println("Converted back: ");
            System.out.println(structuredtext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // end main
}
