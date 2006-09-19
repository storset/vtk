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


import org.jdom.Element;

import junit.framework.TestCase;

public class EvenStructuredTextTestCase extends TestCase {

    private EvenStructuredText est; 
    
    protected void setUp() throws Exception {
        super.setUp();
        this.est = new EvenStructuredText();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test of potential outofbounds.
     */
    public void testEndsWithUnfinishedLink() {
        try {
            this.est.parseStructuredText("lala lala \"lala\":");
        } catch (StringIndexOutOfBoundsException e) {
            fail(e.getMessage());
        }
    }
    
    public void testSuperWithOneCharString() {
        Element parent = new Element("parent");
        try {
            this.est.parseSuper("[super:1]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("1", parent.getChildText("sup"));
    }

    public void testSuperWithEmptyString() {
        Element parent = new Element("parent");
        try {
            this.est.parseSuper("[super:]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("", parent.getChildText("sup"));
    }

    public void testSubWithOneCharString() {
        Element parent = new Element("parent");
        try {
            this.est.parseSub("[sub:1]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("1", parent.getChildText("sub"));
    }

    public void testSubWithEmptyString() {
        Element parent = new Element("parent");
        try {
            this.est.parseSub("[sub:]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("", parent.getChildText("sub"));
    }

    public void testNewLine() {
        String s = "la\nla\nla";
        Element parsed = this.est.parseStructuredText(s);
        String reparsed = this.est.parseElement(parsed);
        assertEquals(s, reparsed);
    }

//    public void testListsWithSingleNewLine() {
//        String s = "\n# ol";
//        Element parsed = this.est.parseStructuredText(s);
//        System.out.println(parsed.getChildren());
//        String reparsed = this.est.parseElement(parsed);
//        assertEquals(s, reparsed);
//    }

    public void testComplexStructure() {
        try {
            String complex = "[sub:subscript] og [super:superscript]\\\n" +
            "escaped newline\n*fet \\* stjerne* og _kursiv \\_ " +
            "underscore_";
            Element parsed = this.est.parseStructuredText(complex);
            String reparsed = this.est.parseElement(parsed);
            assertEquals(complex, reparsed);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testSubWithEscapedQuote() {
        Element parent = new Element("parent");
        try {
            this.est.parseSub("[sub:\\]]", 0, parent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("]", parent.getChildText("sub"));
    }


    
 
    public void testEscapeText() {
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
    
    public void testBoldText() {
        String s = "*boldtext\\_test* \\\\* bla *\"bold_\\*text\" \\** \\* bla * bla _";
    	String sback = "*boldtext\\_test* \\\\* bla *\"bold_\\*text\" \\** \\* bla \\* bla \\_";
    	String s2 = null;
        try {
            Element e = this.est.parseStructuredText(s);
            //est.dumpXML(e, System.out);
            s2 = this.est.parseElement(e);
        } catch (Exception e) {
//        	e.printStackTrace();
        	fail(e.getMessage());
        }
    	assertEquals(sback, s2);
    }

public void testItalicText() {
	String s = "Bla \\_bla _\"italic*text\" \\__ \\_ bla _ bla";
	String sback = "Bla \\_bla _\"italic*text\" \\__ \\_ bla \\_ bla";
	String s2 = null;
    try {
    		Element e = this.est.parseStructuredText(s);
                //est.dumpXML(e, System.out);
    		s2 = this.est.parseElement(e);
    } catch (Exception e) {
//    	e.printStackTrace();
    	fail(e.getMessage());
    }
	assertEquals(sback, s2);	
}

public void testSubText() {
        String s = "[sub:subtext\\]_]";
        String sback = "[sub:subtext\\]_]";
        String sreparsed = null;
    try {
        Element e = this.est.parseStructuredText(s);
        //est.dumpXML(e, System.out);
        sreparsed = this.est.parseElement(e);
    } catch (Exception e) {
        fail(e.getMessage());
    }
    assertEquals(sback, sreparsed);
}

public void testLinkText() {
	String s = "Bla \\\"bla \"link*_text\":bla_bla bla_bla";
	String sback = "Bla \\\"bla \"link*_text\":bla_bla bla\\_bla";
	String s2 = null;
    try {
    		Element e = this.est.parseStructuredText(s);
                //est.dumpXML(e, System.out);
    		s2 = this.est.parseElement(e);
    } catch (Exception e) {
//    	e.printStackTrace();
    	fail(e.getMessage());
    }
	assertEquals(sback, s2);
}

public void testParagraphText() {
        String s = "Paragraphtext * _bla_ [ bla \n\\ -";
	String sback = "Paragraphtext \\* _bla_ \\[ bla \n\\ -";
	String s2 = null;
    try {
    		Element e = this.est.parseStructuredText(s);
                //est.dumpXML(e, System.out);
    		s2 = this.est.parseElement(e);
    } catch (Exception e) {
//    	e.printStackTrace();
    	fail(e.getMessage());
    }
	assertEquals(sback, s2);
}

public void testListText() {
	String s = "- Bla * _bla_ [ bla \n\\ -";
	String sback = "- Bla \\* _bla_ \\[ bla \n\\ -";
	String s2 = null;
    try {
    		Element e = this.est.parseStructuredText(s);
    		s2 = this.est.parseElement(e);
    } catch (Exception e) {
//    	e.printStackTrace();
    	fail(e.getMessage());
    }
	assertEquals(sback, s2);
}

/**
 * Reference tests
 */
public void testNormalReferences() {
    String s1 = "reference-test 1 [attributt:referanse]";
    String s1parsed = null;
    String s2 = "reference-test 2 [uten_attributt]";
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

public void testEscapedReferences() {
    String s = "reference-test med escape [attributt\\:referanse] [attr:ref\\]]";
    String sParsed = null;
    try {
        Element e;
        e = this.est.parseStructuredText(s);
        //est.dumpXML(e, System.out);
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
public void testNormalList() {
    String s = "- listitem 1 \n- listitem 2";
    String sParsed = null;
    try {
        Element e;
        e = this.est.parseStructuredText(s);
        //est.dumpXML(e, System.out);
        sParsed = this.est.parseElement(e);
    } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
    }
        assertEquals(s, sParsed);
}

public void testNormalNumlist() {
    String s = "# numlistitem 1 \n# numlistitem 2";
    String sParsed = null;
    try {
        Element e;
        e = this.est.parseStructuredText(s);
        //est.dumpXML(e, System.out);
        sParsed = this.est.parseElement(e);
    } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
    }
        assertEquals(s, sParsed);
}


public void testEscapedList() {
    String s = "normal text \n\n\\- escaped BAJS- \\- -BAJS listitem 1 \n\\- escaped listitem 2\n\n normal text again";
    String sParsed = null;
    try {
        Element e;
        e = this.est.parseStructuredText(s);
        //est.dumpXML(e, System.out);
        sParsed = this.est.parseElement(e);
    } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
    }
        assertEquals(s, sParsed );
}

public void testEscapedNumlist() {
    String s = "normal text \n\n\\# escaped numlistitem 1 \n\\# escaped numlistitem 2\n\n normal text again";
    String sParsed = null;
    try {
        Element e;
        e = this.est.parseStructuredText(s);
        //est.dumpXML(e, System.out);
        sParsed = this.est.parseElement(e);
    } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
    }
        assertEquals(s, sParsed);
}


/**
 * TODO:
 * Feiler på escaping newline før listepunkt (som dermed egentlig ikke skal bli listepunkt) *
 */
public void testTryingToMessUpListsWithEscape() {
    String s = "test-text\n\n\\- escaped listitem \\\n- not a listitem \\\n- not a listitem \nend of text";
    String sParsed = null;
    try {
        Element e;
        e = this.est.parseStructuredText(s);
        est.dumpXML(e, System.out);
        sParsed = this.est.parseElement(e);
    } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
    }
        assertEquals(s, sParsed);
}


/**
 * Newline
 */
public void testEscapedNewline() {
    String s = "text\\\n text on escaped newline \n text after normal newline";
    String sParsed = null;
    try {
        Element e;
        e = this.est.parseStructuredText(s);
        //est.dumpXML(e, System.out);
        sParsed = this.est.parseElement(e);
    } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
    }
        assertEquals(s, sParsed);
}

}