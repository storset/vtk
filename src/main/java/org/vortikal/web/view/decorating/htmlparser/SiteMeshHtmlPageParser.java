/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating.htmlparser;


import com.opensymphony.module.sitemesh.html.BlockExtractingRule;
import com.opensymphony.module.sitemesh.html.HTMLProcessor;
import com.opensymphony.module.sitemesh.html.State;
import com.opensymphony.module.sitemesh.html.Tag;
import com.opensymphony.module.sitemesh.html.rules.PageBuilder;
import com.opensymphony.module.sitemesh.html.util.CharArray;
import com.opensymphony.module.sitemesh.parser.TokenizedHTMLPage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.Purifier;
import org.cyberneko.html.filters.Writer;
import org.cyberneko.html.parsers.DOMParser;
import org.cyberneko.html.parsers.SAXParser;
import org.vortikal.web.view.decorating.HtmlPage;
import org.vortikal.web.view.decorating.HtmlPageParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;


public class SiteMeshHtmlPageParser implements HtmlPageParser {

    private char[] tmpNormalize(char[] content) throws Exception {
        String filterProp = "http://cyberneko.org/html/properties/filters";
        //ByteArrayOutputStream out = new ByteArrayOutputStream();
        //XMLDocumentFilter writer = new Writer(out, "utf-8");
        //Purifier purifier = new Purifier();
        DOMParser parser = new DOMParser();
        //XMLParserConfiguration parser = new HTMLConfiguration();
        //parser.setProperty(filterProp, new XMLDocumentFilter[]{purifier, writer});

        parser.setFeature("http://cyberneko.org/html/features/balance-tags", true);
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        parser.setFeature("http://cyberneko.org/html/features/override-namespaces", true);
        parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", true);
        parser.setProperty("http://cyberneko.org/html/properties/namespaces-uri",
                           "http://www.w3.org/1999/xhtml");
        InputSource input = new InputSource(new ByteArrayInputStream(new String(content).getBytes("utf-8")));
        parser.parse(input);
        Document doc = parser.getDocument();
        
        StringWriter sw = new StringWriter();
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.transform(new DOMSource(doc),new StreamResult(sw));
        String outputXHTML = sw.toString();
        return outputXHTML.toCharArray();
    }
    

    public HtmlPage parse(char[] content) throws Exception {

        content = tmpNormalize(content);        

        System.out.println("__content: " + new String(content));

        //ByteArrayInputStream in = );
        //parser.parse()
        //parser.parse(java.io.InputStream, java.lang.String, java.lang.String, org.python.core.CompilerFlags)

        CharArray head = new CharArray(64);
        CharArray body = new CharArray(4096);
        TokenizedHTMLPage page = new TokenizedHTMLPage(content, body, head);
        HTMLProcessor processor = new HTMLProcessor(content, body);
        State html = processor.defaultState();
        TagExtractor extractor = new TagExtractor();
        html.addRule(extractor);
        //html.addRule(new LinkExtractingRule(page));
        processor.process();
        return extractor.getPage();
    }

}
