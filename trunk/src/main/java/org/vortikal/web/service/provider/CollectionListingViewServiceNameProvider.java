/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.service.provider;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.web.display.listing.ListingPager;

public class CollectionListingViewServiceNameProvider implements ServiceNameProvider {

    public String getLocalizedName(Resource resource, HttpServletRequest request) {

        String page = request.getParameter(ListingPager.USER_DISPLAY_PAGE);
        if (StringUtils.isBlank(page)) {
            return null;
        }
        try {
            int p = Integer.parseInt(page);
            if (p < 2) {
                return null;
            }
        } catch (NumberFormatException nfe) {
            return null;
        }

        Path uri = resource.getURI();
        RequestContext rc = new RequestContext(request);

        if (Path.ROOT.equals(uri)) {
            return rc.getMessage("viewCollectionListing.serviceTitle.root", new Object[] { page });
        }

        return rc.getMessage("viewCollectionListing.serviceTitle", new Object[] { resource.getTitle(), page });
    }

}
