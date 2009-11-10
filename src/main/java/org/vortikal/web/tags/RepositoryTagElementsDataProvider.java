/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.Pair;
import org.vortikal.repository.reporting.PropertyValueFrequencyQueryResult;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.RequestContext;
import org.vortikal.web.reporting.TagsReportingComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class RepositoryTagElementsDataProvider {

    private TagsReportingComponent tagsReporter;
    private Service tagService;

    private boolean servesRoot = true;

    @Required
    public void setTagsReporter(TagsReportingComponent tagsReporter) {
        this.tagsReporter = tagsReporter;
    }

    @Required
    // to be removed..
    public void setServesRoot(boolean servesRoot) {
        this.servesRoot = servesRoot;
    }

    @Required
    public void setTagService(Service tagService) {
        this.tagService = tagService;
    }

    public List<TagElement> getTagElements(Path scopeUri, List<ResourceTypeDefinition> resourceTypeDefs, String token,
            int magnitudeMin, int magnitudeMax, int limit, int tagOccurenceMin) throws DataReportException,
            IllegalArgumentException {
        return getTagElementsInternal(scopeUri, resourceTypeDefs, token, magnitudeMin, magnitudeMax, limit,
                tagOccurenceMin);
    }

    public List<TagElement> getTagElements(Path scopeUri, String token, int magnitudeMin, int magnitudeMax, int limit,
            int tagOccurenceMin) throws DataReportException, IllegalArgumentException {
        return getTagElementsInternal(scopeUri, null, token, magnitudeMin, magnitudeMax, limit, tagOccurenceMin);
    }

    private List<TagElement> getTagElementsInternal(Path scopeUri, List<ResourceTypeDefinition> resourceTypeDefs,
            String token, int magnitudeMin, int magnitudeMax, int limit, int tagOccurenceMin)
            throws DataReportException, IllegalArgumentException {

        // Do data report query
        PropertyValueFrequencyQueryResult result = this.tagsReporter.getTags(scopeUri, resourceTypeDefs, limit,
                tagOccurenceMin, token);

        // Generate list of tag elements
        List<TagElement> tagElements = generateTagElementList(scopeUri, result, magnitudeMax, magnitudeMin);

        return tagElements;
    }

    private List<TagElement> generateTagElementList(Path scopeUri, PropertyValueFrequencyQueryResult result,
            int magnitudeMax, int magnitudeMin) {

        List<Pair<Value, Integer>> freqList = result.getValueFrequencyList();

        // Makes a list with tagelements.
        List<TagElement> tagElements = new ArrayList<TagElement>(freqList.size());

        if (!freqList.isEmpty()) {

            int minFreq = freqList.get(freqList.size() - 1).second().intValue();
            int maxFreq = freqList.get(0).second().intValue();

            for (Pair<Value, Integer> pair : freqList) {
                String tagName = pair.first().getStringValue();
                URL link = getUrl(tagName, scopeUri);

                int magnitude = getNormalizedMagnitude(pair.second().intValue(), maxFreq, minFreq, magnitudeMin,
                        magnitudeMax);

                tagElements.add(new TagElement(magnitude, link, tagName, pair.second().intValue()));
            }

            // Sort alphabetically
            // XXX: locale-dependent sorting ?
            Collections.sort(tagElements);
        }

        return tagElements;
    }

    private int getNormalizedMagnitude(int frequency, int maxFreq, int minFreq, int magnitudeMin, int magnitudeMax) {
        if (maxFreq == minFreq || magnitudeMin == magnitudeMax) {
            return (magnitudeMin + magnitudeMax) / 2;
        }

        int maxLeveled = maxFreq - minFreq;
        int frequencyLeveled = frequency - minFreq;
        float magnitude = (float) frequencyLeveled / (float) maxLeveled;

        return (int) Math.round(magnitude * (magnitudeMax - magnitudeMin) + magnitudeMin);
    }

    private URL getUrl(String tagName, Path scopeUri) {
        URL url = this.tagService.constructURL(scopeUri);
        if (scopeUri.isRoot() && !this.servesRoot) {
            scopeUri = RequestContext.getRequestContext().getCurrentCollection();
            url = this.tagService.constructURL(scopeUri);
            url.addParameter("scope", Path.ROOT.toString());
        }
        url.addParameter("tag", tagName);
        return url;
    }
}
