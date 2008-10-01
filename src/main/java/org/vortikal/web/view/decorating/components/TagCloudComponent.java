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
package org.vortikal.web.view.decorating.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.DataReportManager;
import org.vortikal.repository.reporting.Pair;
import org.vortikal.repository.reporting.PropertyValueFrequencyQuery;
import org.vortikal.repository.reporting.PropertyValueFrequencyQueryResult;
import org.vortikal.repository.reporting.UriScope;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;

/**
 * Decorator component for tag cloud.
 * 
 * @author oyviste
 */
public class TagCloudComponent extends ViewRenderingDecoratorComponent implements InitializingBean {

    private static final String DESCRIPTION = "Renders a tag cloud as an alphabetically sorted list. Classes are put on "
            + "the elements for representing the magnitude of the individual tags in the cloud. "
            + "List elements will be assigned classes 'tag-magnitude-N', where "
            + "N represents the magnitude as a bounded positive integer number.";

    private static final String PARAMETER_TAG_LIMIT = "limit";
    private static final int PARAMETER_TAG_LIMIT_DEFAULT_VALUE = 20;
    private static final String PARAMETER_TAG_LIMIT_DESC = "Set limit on how many tags to include. Setting this to a low value will "
            + "show only the most popular tags. Default is: " + PARAMETER_TAG_LIMIT_DEFAULT_VALUE;

    private static final String PARAMETER_TAG_OCCURENCE_MIN = "tag-occurence-min";
    private static final int PARAMETER_TAG_OCCURENCE_MIN_DEFAULT_VALUE = 1;
    private static final String PARAMETER_TAG_OCCURENCE_MIN_DESC = "Limit tag cloud to include only tags with an occurence count higher than "
            + "or equal to this minimal value. This can be used to weed out tags "
            + "with for instance only one or two occurences within the scope. "
            + "The default value is 1. Increase this as needed, if your tag cloud "
            + "contains many undesirable small tags with only few occurences.";

    private static final String PARAMETER_SCOPE = "scope";
    private static final String PARAMETER_SCOPE_DESC = "Set the URI scope for the tag cloud. Relative URIs are allowed. "
            + "Only tags existing in the folder tree given by the URI will be "
            + "taken into consideration when generating the tag cloud. "
            + "The default value is the current directory and below.";

    private static final String PARAMETER_MAGNITUDE_MAX = "magnitude-max";
    private static final int PARAMETER_MAGNITUDE_MAX_DEFAULT_VALUE = 5;
    private static final String PARAMETER_MAGNITUDE_MAX_DESC = "Sets the maximum magnitude for a tags in the cloud (an integer number bigger than 1). "
            + "The tags with the highest occurence will be assigned the maximum magnitude value. "
            + "You can use this to adjust the granularity of the magnitude-scale. "
            + "Note that this number must be bigger than or equal to the minimum value (see next parameter). "
            + "Default value is: " + PARAMETER_MAGNITUDE_MAX_DEFAULT_VALUE;

    private static final String PARAMETER_MAGNITUDE_MIN = "magnitude-min";
    private static final int PARAMETER_MAGNITUDE_MIN_DEFAULT_VALUE = 1;
    private static final String PARAMETER_MAGNITUDE_MIN_DESC = "Sets the minimum magnitude for a tag in the tag cloud (an integer number bigger than 1). "
            + "The tags with the lowest occurence within the result set will be assigned the minimum magnitude value. "
            + "The result set can be restricted using the parameters '"
            + PARAMETER_SCOPE
            + "' and '"
            + PARAMETER_TAG_LIMIT + "'. " + "Default value is: " + PARAMETER_MAGNITUDE_MIN_DEFAULT_VALUE;

    private static final String PARAMETER_SERVICE_URL = "service-url";
    private static final String PARAMETER_SERVICE_URL_DESC = "Sets the service URL template to use when generating a link for a tag."
            + "The string '%v' will be replaced with the actual tag value for each tag "
            + "when the link is generated.";

    private static final Pattern URL_REPLACEMENT_VALUE_PATTERN = Pattern.compile("%v");

    private DataReportManager dataReportManager;
    private PropertyTypeDefinition keywordsPropDef = null;
    private String defaultURLPattern = null;


    protected String getDescriptionInternal() {
        return DESCRIPTION;
    }


    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();

        map.put(PARAMETER_SCOPE, PARAMETER_SCOPE_DESC);
        map.put(PARAMETER_TAG_LIMIT, PARAMETER_TAG_LIMIT_DESC);
        map.put(PARAMETER_TAG_OCCURENCE_MIN, PARAMETER_TAG_OCCURENCE_MIN_DESC);
        map.put(PARAMETER_MAGNITUDE_MAX, PARAMETER_MAGNITUDE_MAX_DESC);
        map.put(PARAMETER_MAGNITUDE_MIN, PARAMETER_MAGNITUDE_MIN_DESC);
        map.put(PARAMETER_SERVICE_URL, PARAMETER_SERVICE_URL_DESC + " Default value is: " + this.defaultURLPattern);

        return map;
    }


    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        super.processModel(model, request, response);

        Path scopeUri = RequestContext.getRequestContext().getCurrentCollection();
        String token = SecurityContext.getSecurityContext().getToken();

        if (request.getStringParameter(PARAMETER_SCOPE) != null) {
            String scopeUriParam = request.getStringParameter(PARAMETER_SCOPE);

            // Current collection is the default scope
            if (!(".".equals(scopeUriParam) || "./".equals(scopeUriParam))) {
                if (!scopeUriParam.startsWith("/")) {
                    Path requestURI = RequestContext.getRequestContext().getResourceURI();
                    scopeUriParam = requestURI.toString().substring(0, requestURI.toString().lastIndexOf("/") + 1)
                            + scopeUriParam;
                    scopeUriParam = URIUtil.expandPath(scopeUriParam);
                }
                scopeUri = Path.fromString(scopeUriParam);
            }
        }

        int magnitudeMin = PARAMETER_MAGNITUDE_MIN_DEFAULT_VALUE;
        int magnitudeMax = PARAMETER_MAGNITUDE_MAX_DEFAULT_VALUE;
        int limit = PARAMETER_TAG_LIMIT_DEFAULT_VALUE;
        int tagOccurenceMin = PARAMETER_TAG_OCCURENCE_MIN_DEFAULT_VALUE;
        String serviceUrl = request.getStringParameter(PARAMETER_SERVICE_URL);
        try {
            if (request.getStringParameter(PARAMETER_MAGNITUDE_MIN) != null) {
                magnitudeMin = Integer.parseInt(request.getStringParameter(PARAMETER_MAGNITUDE_MIN));
            }

            if (request.getStringParameter(PARAMETER_MAGNITUDE_MAX) != null) {
                magnitudeMax = Integer.parseInt(request.getStringParameter(PARAMETER_MAGNITUDE_MAX));
            }

            if (request.getStringParameter(PARAMETER_TAG_LIMIT) != null) {
                limit = Integer.parseInt(request.getStringParameter(PARAMETER_TAG_LIMIT));
            }

            if (request.getStringParameter(PARAMETER_TAG_OCCURENCE_MIN) != null) {
                tagOccurenceMin = Integer.parseInt(request.getStringParameter(PARAMETER_TAG_OCCURENCE_MIN));
            }

            if (tagOccurenceMin < 1) {
                throw new DecoratorComponentException("Parameter '" + PARAMETER_TAG_OCCURENCE_MIN
                        + "' must be a number larger than or equal to 1.");
            }

            if (limit <= 0) {
                throw new DecoratorComponentException("Parameter '" + PARAMETER_TAG_LIMIT
                        + "' cannot be zero or negative");
            }

            if (magnitudeMin < 1 || magnitudeMax < magnitudeMin) {
                throw new DecoratorComponentException("Value of parameter '" + PARAMETER_MAGNITUDE_MAX
                        + "' must be greater or equal to value of parameter '" + PARAMETER_MAGNITUDE_MIN
                        + "' and both parameters must be greater than zero.");
            }

        } catch (NumberFormatException nfe) {
            throw new DecoratorComponentException("An invalid numeric parameter value was supplied: "
                    + nfe.getMessage());
        }

        // Do data report query
        PropertyValueFrequencyQueryResult result = executeDataReportQuery(limit, tagOccurenceMin, scopeUri, token);

        // Generate list of tag elements
        List<TagElement> tagElements = generateTagElementList(result, magnitudeMax, magnitudeMin, serviceUrl);

        // Populate model
        model.put("tagElements", tagElements);
    }


    private PropertyValueFrequencyQueryResult executeDataReportQuery(int limit, int tagOccurenceMin, Path scopeUri,
            String token) throws DecoratorComponentException {

        PropertyValueFrequencyQuery query = new PropertyValueFrequencyQuery();
        query.setPropertyTypeDefinition(this.keywordsPropDef);
        query.setOrdering(PropertyValueFrequencyQuery.Ordering.DESCENDING_BY_FREQUENCY);
        query.setLimit(limit);
        query.setMinValueFrequency(tagOccurenceMin);

        try {
            query.setUriScope(new UriScope(scopeUri));
        } catch (IllegalArgumentException e) {
            throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_SCOPE
                    + "', must be a valid URI.");
        }

        try {
            return (PropertyValueFrequencyQueryResult) this.dataReportManager.executeReportQuery(query, token);
        } catch (DataReportException d) {
            throw new DecoratorComponentException("There was a problem with the data report query: " + d.getMessage());
        }
    }


    private List<TagElement> generateTagElementList(PropertyValueFrequencyQueryResult result, int magnitudeMax,
            int magnitudeMin, String serviceUrl) {

        List<Pair<Value, Integer>> freqList = result.getValueFrequencyList();

        // Makes a list with tagelements.
        List<TagElement> tagElements = new ArrayList<TagElement>(freqList.size());

        if (!freqList.isEmpty()) {

            int minFreq = freqList.get(freqList.size() - 1).second().intValue();
            int maxFreq = freqList.get(0).second().intValue();

            for (Pair<Value, Integer> pair : freqList) {
                String text = pair.first().getStringValue();
                String link = getUrl(text, serviceUrl);

                int magnitude = getNormalizedMagnitude(pair.second().intValue(), maxFreq, minFreq, magnitudeMin,
                        magnitudeMax);

                tagElements.add(new TagElement(magnitude, link, text));
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


    private String getUrl(String text, String serviceUrl) {
        if (serviceUrl == null) {
            if (this.defaultURLPattern == null) {
                return null;
            } 
            serviceUrl = this.defaultURLPattern;
        } 
        
        Matcher matcher = URL_REPLACEMENT_VALUE_PATTERN.matcher(serviceUrl);
        return matcher.replaceAll(text);
    }

    /**
     * Represents a tag element for view rendering.
     * 
     */
    public static final class TagElement implements Comparable<TagElement> {
        private int magnitude;
        private String linkUrl;
        private String text;


        public TagElement(int magnitude, String linkUrl, String text) {
            this.magnitude = magnitude;
            this.linkUrl = linkUrl;
            this.text = text;
        }


        public int getMagnitude() {
            return magnitude;
        }


        public String getLinkUrl() {
            return linkUrl;
        }


        public String getText() {
            return text;
        }


        // VTK-1107: Sets the text to compare to lowercase,
        // thus avoiding problem with sorting.
        public int compareTo(TagElement other) {
            return this.text.toLowerCase().compareTo(other.text.toLowerCase());
        }

    }


    public void setDefaultURLPattern(String defaultURLPattern) {
        this.defaultURLPattern = defaultURLPattern;
    }


    @Required
    public void setKeywordsPropDef(PropertyTypeDefinition keywordsPropDef) {
        this.keywordsPropDef = keywordsPropDef;
    }


    @Required
    public void setDataReportManager(DataReportManager manager) {
        this.dataReportManager = manager;
    }

}
