/* Copyright (c) 2006, University of Oslo, Norway
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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class EvenStructuredTextTest {

    private static final String TEST_XML = "src/test/resources/org/vortikal/util/text/nyhet.xml";

    private EvenStructuredText est;

    Document testDocument;

    @Before
    public void setUp() throws Exception {
        this.est = new EvenStructuredText();

        SAXBuilder builder = new SAXBuilder(
                "org.apache.xerces.parsers.SAXParser");

        try {
            File testXML = new File(TEST_XML);
            this.testDocument = builder.build(testXML);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Couldn't instantiate test schema " + e.getMessage());
        }
    }

    @Test
    public void stripLeadingTrailingNewLines() {
        String s = "\n\n\nlala\n\n\n";
        String result = "lala";
        assertEquals(result, this.est.stripLeadingTrailingNewLines(s));
        
        s = "";
        assertEquals(s, this.est.stripLeadingTrailingNewLines(s));

        s = "\n";
        result = "";
        assertEquals(result, this.est.stripLeadingTrailingNewLines(s));
        
    }
    
    @Test
    public void startsEndsWithEmptyParagraph() {
        String result = "Avsnitt1";
        Element element = this.testDocument.getRootElement().getChild(
                "fritekst");

        assertEquals(result, this.est.parseElement(element));

        element = this.testDocument.getRootElement().getChild("fritekst2");
        result = "- aaa";

        assertEquals(result, this.est.parseElement(element));
    }

    @Test
    public void newlinesWithParagraph() {
        String result = "paragraph";

        String s = "paragraph";
        assertEquals(result, this.est.parseElement(this.est.parseStructuredText(s)));

        s = "paragraph\n";
        assertEquals(result, this.est.parseElement(this.est.parseStructuredText(s)));

        s = "paragraph\n\n\n";
        assertEquals(result, this.est.parseElement(this.est
                .parseStructuredText(s)));

        s = "\nparagraph\n\n\n";
        assertEquals(result, this.est.parseElement(this.est
                .parseStructuredText(s)));

        s = "\n\n\n\nparagraph\n\n\n\n\n";
        assertEquals(result, this.est.parseElement(this.est
                .parseStructuredText(s)));

    }

    @Test
    public void newlinesWithList() {
        String result = "- list item";

        String s = "- list item";
        assertEquals(result, this.est.parseElement(this.est.parseStructuredText(s)));

        s = "- list item\n";
        assertEquals(result, this.est.parseElement(this.est.parseStructuredText(s)));

        s = "- list item\n\n\n";
        assertEquals(result, this.est.parseElement(this.est
                .parseStructuredText(s)));

        s = "\n- list item\n\n\n";
        assertEquals(result, this.est.parseElement(this.est
                .parseStructuredText(s)));

        s = "\n\n\n\n- list item\n\n\n\n\n";
        assertEquals(result, this.est.parseElement(this.est
                .parseStructuredText(s)));

    }

    @Test
    public void complexStructure() {
        try {
            String complex = "[sub:subscript] og [super:superscript]\\\n"
                    + "escaped newline\n*fet \\* stjerne* og _kursiv \\_ "
                    + "underscore_";
            Element parsed = this.est.parseStructuredText(complex);
            String reparsed = this.est.parseElement(parsed);
            assertEquals(complex, reparsed);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void subWithEscapedQuote() {
        Element parent = new Element("parent");
        try {
            this.est.parseSub("[sub:\\]]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("]", parent.getChildText("sub"));
    }

    @Test
    public void escapeText() {
        String s = "\\*bold\\*";
        String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            s2 = this.est.parseElement(e);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, s2);
    }

    // Roundtrip tests for testing escape

    @Test
    public void boldText() {
        String s = "*boldtext\\_test* \\\\* bla *\"bold_\\*text\" \\** \\* bla * bla _";
        String sback = "*boldtext\\_test* \\\\* bla *\"bold_\\*text\" \\** \\* bla \\* bla \\_";
        String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            s2 = this.est.parseElement(e);
        } catch (Exception e) {
            // e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(sback, s2);
    }

    @Test
    public void italicText() {
        String s = "Bla \\_bla _\"italic*text\" \\__ \\_ bla _ bla";
        String sback = "Bla \\_bla _\"italic*text\" \\__ \\_ bla \\_ bla";
        String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            s2 = this.est.parseElement(e);
        } catch (Exception e) {
            // e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(sback, s2);
    }

    @Test
    public void subText() {
        String s = "[sub:subtext\\]_]";
        String sback = "[sub:subtext\\]_]";
        String sreparsed = null;
        try {
            Element e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            sreparsed = this.est.parseElement(e);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals(sback, sreparsed);
    }

    @Test
    public void paragraphText() {
        String s = "Paragraphtext * _bla_ [ bla \n\\ -";
        String sback = "Paragraphtext \\* _bla_ \\[ bla \n\\ -";
        String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            s2 = this.est.parseElement(e);
        } catch (Exception e) {
            // e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(sback, s2);
    }

    @Test
    public void listText() {
        String s = "- Bla * _bla_ [ bla \n\\ -";
        String sback = "- Bla \\* _bla_ \\[ bla \n\\ -";
        String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            s2 = this.est.parseElement(e);
        } catch (Exception e) {
            // e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(sback, s2);
    }

    /**
     * Link tests
     */
    @Test
    public void linkText() {
        String s = "Bla \\\"bla \"link*_text\":bla_bla bla_bla";
        String sback = "Bla \\\"bla \"link*_text\":bla_bla bla\\_bla";
        String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            s2 = this.est.parseElement(e);
        } catch (Exception e) {
            // e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(sback, s2);
    }

    @Test
    public void escapingInsideLink() {
        String s = "normal link \"uio\":http://www.uio.no escaping in link \"\"bla\\\":bla\":http://www.vg.no end";
        String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            s2 = this.est.parseElement(e);
            // est.dumpXML(e, System.out);
        } catch (Exception e) {
            // e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, s2);
    }

    @Test
    public void linkWithQuotationMarks() {
        String s = "link: dette er \"ikke lenke\", men \"\\\"det\\\":te er\\\"\":http://vg.no også bla";
        String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            s2 = this.est.parseElement(e);
            // est.dumpXML(e, System.out);
        } catch (Exception e) {
            // e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, s2);
    }

    @Test
    public void emptyLinks() {
        String s = "\"\":www.uio.no \"uio\": end";
        String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            s2 = this.est.parseElement(e);
            // est.dumpXML(e, System.out);
        } catch (Exception e) {
            // e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, s2);
    }

    /**
     * Reference tests
     */
    @Test
    public void normalReferences() {
        String s1 = "reference-test 1 [attribute:reference]";
        String s1parsed = null;
        String s2 = "reference-test 2 [without_attribute]";
        String s2parsed = null;
        try {
            Element e;
            e = this.est.parseStructuredText(s1);
            s1parsed = this.est.parseElement(e);
            e = this.est.parseStructuredText(s2);
            s2parsed = this.est.parseElement(e);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s1, s1parsed);
        assertEquals(s2, s2parsed);
    }

    @Test
    public void escapedReferences() {
        String s = "reference-test with escape [attribute\\:reference] [attr:ref\\]]";
        String sParsed = null;
        try {
            Element e;
            e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            sParsed = this.est.parseElement(e);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, sParsed);
    }

    @Test
    public void emptyAttribute() {
        String s = "reference-test empty attribute [:reference] [attr\\:ref]";
        String sParsed = null;
        try {
            Element e;
            e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            sParsed = this.est.parseElement(e);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, sParsed);
    }

    /**
     * List items tests
     */
    @Test
    public void normalList() {
        String s = "- listitem 1 \n- listitem 2";
        String sParsed = null;
        try {
            Element e;
            e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            sParsed = this.est.parseElement(e);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, sParsed);
    }

    @Test
    public void normalNumlist() {
        String s = "# numlistitem 1 \n# numlistitem 2";
        String sParsed = null;
        try {
            Element e;
            e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            sParsed = this.est.parseElement(e);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, sParsed);
    }

    @Test
    public void escapedList() {
        String s = "normal text \n\n\\- escaped BAJS- \\- -BAJS listitem 1 \n\\- escaped listitem 2\n\n normal text again";
        String sParsed = null;
        try {
            Element e;
            e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            sParsed = this.est.parseElement(e);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, sParsed);
    }

    @Test
    public void escapedNumlist() {
        String s = "normal text \n\n\\# escaped numlistitem 1 \n\\# escaped numlistitem 2\n\n normal text again";
        String sParsed = null;
        try {
            Element e;
            e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            sParsed = this.est.parseElement(e);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, sParsed);
    }

    /**
     * Newline tests
     */

    @Test
    public void newLine() {
        String s = "la\nla\nla";
        Element parsed = this.est.parseStructuredText(s);
        String reparsed = this.est.parseElement(parsed);
        assertEquals(s, reparsed);
    }

    /**
     * FIXME: Escaped newline is not yet fully implementet!
     */
    // Will fail when escapable newline is implemented
    @Test
    public void simpleEscapedNewline() {
        String s = "line\\\nnewline";
        Element e = this.est.parseStructuredText(s);
        // est.dumpXML(e, System.out);
        String reparsed = this.est.parseElement(e);
        assertEquals(s, reparsed);
    }

    // Will fail when escapable newline is implemented
    @Test
    public void escapedNewline() {
        String s = "text\\\n text on escaped newline \n text after normal newline";
        String sParsed = null;
        try {
            Element e;
            e = this.est.parseStructuredText(s);
            // est.dumpXML(e, System.out);
            sParsed = this.est.parseElement(e);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(s, sParsed);
    }

    /**
     * TODO: Feiler på escaping newline før listepunkt (som dermed egentlig ikke
     * skal bli listepunkt) *
     */
    @Test
    public void tryingToMessUpListsWithEscape() {
        String s = "test-text\n\n\\- escaped listitem \\\n- not a listitem \\\n- another line not listitem \nend of text";
        // List adds a new newline (since escaped newline is not supported)
        String sExpectedResult = "test-text\n\n\\- escaped listitem \\\n\n- not a listitem \\\n- another line not listitem \nend of text";
        String sParsed = null;
        try {
            Element e;
            e = this.est.parseStructuredText(s);
            sParsed = this.est.parseElement(e);
            // est.dumpXML(e, System.out);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(sExpectedResult, sParsed);
    }

    /**
     * Test of potential outofbounds.
     */
    @Test
    public void endsWithUnfinishedLink() {
        try {
            this.est.parseStructuredText("lala lala \"lala\":");
        } catch (StringIndexOutOfBoundsException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void superWithOneCharString() {
        Element parent = new Element("parent");
        try {
            this.est.parseSuper("[super:1]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("1", parent.getChildText("sup"));
    }

    @Test
    public void superWithEmptyString() {
        Element parent = new Element("parent");
        try {
            this.est.parseSuper("[super:]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("", parent.getChildText("sup"));
    }

    @Test
    public void subWithOneCharString() {
        Element parent = new Element("parent");
        try {
            this.est.parseSub("[sub:1]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("1", parent.getChildText("sub"));
    }

    @Test
    public void subWithEmptyString() {
        Element parent = new Element("parent");
        try {
            this.est.parseSub("[sub:]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("", parent.getChildText("sub"));
    }

    @Test
    public void linkWithSingleCharUrlAndDescription() {
        String s = "\"a\":b";
        Element parsed = this.est.parseStructuredText(s);
        String reparsed = this.est.parseElement(parsed);
        assertEquals(s, reparsed);
    }

    /**
     * Iterative complex tests
     */
    @Test
    public void iterativeParsingOfComplexStructure() {
        try {
            String complex = "[sub:subscript] og [super:superscript]\\\n"
                    + "escaped newline\n*fet \\* stjerne* og _kursiv \\_ "
                    + "underscore_ lenke: \"her\\\":er lenketekst\":~ _kursiv_";
            // first iteration
            Element parsed = this.est.parseStructuredText(complex);
            String reparsed = this.est.parseElement(parsed);
            // second iteration
            parsed = this.est.parseStructuredText(reparsed);
            reparsed = this.est.parseElement(parsed);
            // third iteration
            parsed = this.est.parseStructuredText(reparsed);
            reparsed = this.est.parseElement(parsed);
            // est.dumpXML(parsed, System.out);
            assertEquals(complex, reparsed);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
