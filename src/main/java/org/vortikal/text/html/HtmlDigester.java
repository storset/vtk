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

public class HtmlDigester {

    private static final int DEFAULT_TRUNCATE_LIMIT = 1000;
    // HORIZONTAL ELLIPSIS (&#8230;) instead?
    private static final String DEFAUL_TAIL = "...";

    private int limit = DEFAULT_TRUNCATE_LIMIT;

    public String truncateHtml(String html) {
        return this.truncateHtml(html, this.limit, DEFAUL_TAIL);
    }

    public String truncateHtml(String html, int limit) {
        return this.truncateHtml(html, limit, DEFAUL_TAIL);
    }

    /**
     * Truncate supplied html to within the given limit. Append tail to
     * truncated result and use JTidy to close removed tags.
     */
    public String truncateHtml(String html, int limit, String tail) {

        // XXX implement

        if (html == null) {
            throw new IllegalArgumentException("Must supply html to truncate");
        }

        // 1. Trim html and remove excessive whitespaces and linebreaks
        // -> then check length
        if (html.length() <= limit) {
            return html;
        }

        // 2. Truncate html and add tail
        // -> check truncated for invalid end tags (cut of in middle of tag)
        // JTidy struggles with those
        // -> check end of truncated for match with tail and handle

        // 3. Run through JTidy to get valid html

        // 4. Check limit and retry if length of valid html exceeds limit
        // -> attempt only up to a certain criterea (count?)
        // -> configuration of JTidy? Default or overridable? e.g.
        // printBodyOnly?

        return null;
    }

    public void setLimit(int limit) {
        if (limit < 1) {
            return;
        }
        this.limit = limit;
    }

}
