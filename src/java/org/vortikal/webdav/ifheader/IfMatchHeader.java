/*  Copyright 2006 University of Oslo, Norway
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.vortikal.webdav.ifheader;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Resource;

public class IfMatchHeader {

    protected static Log logger = LogFactory.getLog(IfMatchHeader.class);

    /**
     * The string representation of the header value
     */
    private final String headerValue;

    public IfMatchHeader(final HttpServletRequest request) {
        super();
        this.headerValue = request.getHeader("If-Match");
        logger.debug("if-match-header: " + this.headerValue);
        // stateEntryList = parse();
    }

    /**
     * Assume resource exists and is not null
     * 
     * @param request
     * @param resource
     * @return
     */
    public boolean matches(final Resource resource) {
        boolean match;
        if (this.headerValue == null) {
            match = true;
        } else if (this.headerValue.equals("*")) {
            match = true;
        } else {
            // TODO: Implement support for multiple etags in the header
            match = this.headerValue.equals(resource.getEtag());
        }
        logger.debug("match: " + match);
        return match;
    }

}
