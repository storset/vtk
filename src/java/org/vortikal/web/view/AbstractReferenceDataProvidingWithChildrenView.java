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
package org.vortikal.web.view;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;
import org.vortikal.web.referencedataprovider.Provider;

public abstract class AbstractReferenceDataProvidingWithChildrenView
  extends AbstractReferenceDataProvidingView {

    /**
     * Gets the set of reference data providers. The list returned is the union
     * of the providers set on this view and all providers for the list of
     * views.
     */
    public Provider[] getReferenceDataProviders() {
        Set providers = new HashSet();
        if (super.getReferenceDataProviders() != null) {
            providers.addAll(Arrays.asList(super.getReferenceDataProviders()));

        }

        View[] viewList = getViews();

        for (int i = 0; i < viewList.length; i++) {
            if (viewList[i] instanceof ReferenceDataProviding) {
                Provider[] providerList = ((ReferenceDataProviding) viewList[i])
                        .getReferenceDataProviders();
                if (providerList != null && providerList.length > 0) {
                    providers.addAll(Arrays.asList(providerList));
                }
            }
        }
        return (Provider[]) providers.toArray(new Provider[0]);
    }

    protected abstract View[] getViews();

}
