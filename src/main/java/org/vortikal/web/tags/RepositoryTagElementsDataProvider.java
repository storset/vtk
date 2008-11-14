package org.vortikal.web.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.Pair;
import org.vortikal.repository.reporting.PropertyValueFrequencyQueryResult;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.reporting.TagsReportingComponent;

public class RepositoryTagElementsDataProvider {

    private static final Pattern URL_REPLACEMENT_VALUE_PATTERN = Pattern.compile("%v");

    private TagsReportingComponent tagsReporter;
    
    @Required
    public void setTagsReporter(TagsReportingComponent tagsReporter) {
        this.tagsReporter = tagsReporter;
    }

    public List<TagElement> getTagElements(Path scopeUri, String token,
            int magnitudeMin, int magnitudeMax, int limit, int tagOccurenceMin,
            String serviceUrl) throws DataReportException, IllegalArgumentException {
        // Do data report query

        PropertyValueFrequencyQueryResult result = null;

        result = this.tagsReporter.getTags(scopeUri, limit, tagOccurenceMin, token);
        
        
        // Generate list of tag elements
        List<TagElement> tagElements = generateTagElementList(result, magnitudeMax, magnitudeMin, serviceUrl);
        return tagElements;
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
            return null;
        } 
        
        Matcher matcher = URL_REPLACEMENT_VALUE_PATTERN.matcher(serviceUrl);

        return matcher.replaceAll(text);
    }


}
