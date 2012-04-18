/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.text.html;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import org.w3c.tidy.Tidy;

/**
 * XXX Should also handle inputstreams and doms
 */
public class HtmlDigester {

    private static final int DEFAULT_TRUNCATE_LIMIT = 1000;
    // HORIZONTAL ELLIPSIS (&#8230;) instead?
    private static final String DEFAUL_TAIL = "...";

    private int limit = DEFAULT_TRUNCATE_LIMIT;

    /**
     * Compress the html, remove whitespaces, linebreaks and comments
     */
    public String compress(String html) {

        // XXX Any good library for this? HtmlCleaner etc?

        String compressed = html.replaceAll("\\r|\\n", "");
        compressed = compressed.trim().replaceAll("\\s+", " ");
        return compressed;
    }

    public String truncateHtml(String html) {
        return this.truncateHtml(html, this.limit, DEFAUL_TAIL, true);
    }

    /**
     * Truncate supplied html to within the given limit. Append tail to
     * truncated result and use JTidy to close removed tags.
     */
    public String truncateHtml(String html, int limit, String tail, boolean compress) {

        if (html == null) {
            throw new IllegalArgumentException("Must supply html to truncate");
        }

        // XXX Handle values for limit, also check tail to make sure it does not
        // violate limits when added

        if (compress) {
            String compressed = this.compress(html);
            if (compressed.length() <= limit) {
                return compressed;
            }
            html = compressed;
        }

        // Leave room for the tail -> what about the closing tags that will be
        // added by jTidy? Consider that here? 'Cuz this isn't right. We already
        // know we'll be exceeding the limit once truncated html is sanitized by
        // JTidy.
        int truncationLimit = limit - tail.length();
        String truncated = html.substring(0, truncationLimit);

        // If truncated ends with a tag, valid or not, remove it before adding
        // the tail. XXX Any good regex solution for this instead?
        if (truncated.endsWith(">") || (truncated.lastIndexOf("<") > truncated.lastIndexOf(">"))) {
            truncated = truncated.substring(0, truncated.lastIndexOf("<"));

            // XXX Perhaps we should keep the end tag if it's valid and append
            // it back on after tail has been added...

        }

        // Add the tail
        truncated = truncated.concat(tail);

        String sanitizedTruncated = this.sanitizeTruncated(truncated, compress);

        // XXX Check result for limit violation and retry with new limit. (how
        // to calculate new limit?)

        return sanitizedTruncated;

    }

    private String sanitizeTruncated(String truncated, boolean compress) {

        // XXX Setup/configuration? Add tags without attributes... e.g. <li
        // style...>? Print body only?
        Tidy tidy = new Tidy();
        tidy.setPrintBodyOnly(true);
        tidy.setMakeClean(true);
        tidy.setInputEncoding("utf-8");
        tidy.setOutputEncoding("utf-8");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(truncated.getBytes());
        tidy.parseDOM(new BufferedInputStream(bais), baos);

        String sanitized = null;
        try {
            sanitized = new String(baos.toByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            // XXX log?
            sanitized = new String(baos.toByteArray());
        }

        if (compress) {
            sanitized = this.compress(sanitized);
        }

        return sanitized;
    }

    public void setLimit(int limit) {
        if (limit < 1) {
            return;
        }
        this.limit = limit;
    }

}
