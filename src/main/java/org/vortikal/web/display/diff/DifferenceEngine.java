/* Copyright (c) 2014, University of Oslo, Norway
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
package org.vortikal.web.display.diff;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.diff.DaisyDiff;
import org.outerj.daisy.diff.helper.NekoHtmlParser;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.InputSource;

/*
 * Compute a new HTML text with mark up hints to show differences between two input texts.
 * Use the DaisyDiff library for implementation.
 * 
 * PS: Pardon the pun in the class name.
 */
public class DifferenceEngine {
    private static Log logger = LogFactory.getLog(DifferenceEngine.class);
    private SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    private SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    private String encoding = null;
    private Locale locale = Locale.getDefault();
    private String prefix = "diff"; // will be part of span tag id's in result
    private boolean useNeko = true;

    public String diff(String contentA, String contentB) throws Exception {
        return diff(
            new InputSource(new StringReader(contentA)),
            new InputSource(new StringReader(contentB))
            );
    }

    public String diff(InputSource sourceA, InputSource sourceB) throws Exception {
        StringWriter result = new StringWriter();
        TransformerHandler resultTokenHandler = createResultHandler();
        resultTokenHandler.setResult(new StreamResult(result));

        HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(resultTokenHandler, prefix);
        HTMLDiffer differ = new HTMLDiffer(output);
        TextNodeComparator comparatorA = createContentSource(sourceA, locale);
        TextNodeComparator comparatorB = createContentSource(sourceB, locale);
        differ.diff(comparatorA, comparatorB);

        return result.toString();
    }

    private TransformerHandler createResultHandler()
            throws TransformerConfigurationException {
        TransformerHandler result = transformerFactory.newTransformerHandler();
        result.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        result.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        result.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
        if (encoding != null && encoding != "") {
            result.getTransformer().setOutputProperty(OutputKeys.ENCODING, encoding);
        }
        return result;
    }
    
    private TextNodeComparator createContentSource(InputSource inputSource, Locale locale) throws Exception {
        DomTreeBuilder saxHandler = new DomTreeBuilder();
        if (useNeko) {
            //use CyberNeko (HTML) instead of SAXParser directly. (Neko will clean up any messy HTML).
            NekoHtmlParser parser = new NekoHtmlParser();
            parser.parse(inputSource, saxHandler);
            TextNodeComparator comparator = new TextNodeComparator(saxHandler, locale);
            return comparator;
        } else {
            SAXParser saxParser = parserFactory.newSAXParser();
            saxParser.parse(inputSource, saxHandler);
            TextNodeComparator comparator = new TextNodeComparator(saxHandler, locale);
            return comparator;
        }
    }

    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return the useNeko
     */
    public boolean isUseNeko() {
        return useNeko;
    }

    /**
     * @param useNeko the useNeko to set
     */
    public void setUseNeko(boolean useNeko) {
        this.useNeko = useNeko;
    }
    
    /*
     * Example from DaisyDiff command line application.
     */
    @SuppressWarnings("unused")
    static private String diffHtml(String lang, String contentA, String contentB) throws Exception {
        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
        TransformerHandler handler = tf.newTransformerHandler();
        StreamResult streamResult = new StreamResult(new StringWriter());
        handler.setResult(streamResult);
        InputSource inputSourceA = new InputSource(new StringReader(contentA));
        InputSource inputSourceB = new InputSource(new StringReader(contentB));

        DaisyDiff.diffHTML(inputSourceA, inputSourceB, handler, "diff", new Locale(lang));
        return streamResult.getWriter().toString();
    }
}
