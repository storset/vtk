package org.vortikal.web.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.Pair;
import org.vortikal.repository.reporting.PropertyValueFrequencyQueryResult;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.RequestContext;
import org.vortikal.web.reporting.TagsReportingComponent;

public class RepositoryTagElementsDataProvider {

    private TagsReportingComponent tagsReporter;
    
    private boolean servesRoot = true;
    
    @Required
    public void setTagsReporter(TagsReportingComponent tagsReporter) {
        this.tagsReporter = tagsReporter;
    }

    @Required // to be removed..
    public void setServesRoot(boolean servesRoot) {
        this.servesRoot = servesRoot;
    }

    public List<TagElement> getTagElements(Path scopeUri, String token,
            int magnitudeMin, int magnitudeMax, int limit, int tagOccurenceMin) throws DataReportException, IllegalArgumentException {
        // Do data report query

        PropertyValueFrequencyQueryResult result = null;

        result = this.tagsReporter.getTags(scopeUri, limit, tagOccurenceMin, token);
        
        
        // Generate list of tag elements
        List<TagElement> tagElements = generateTagElementList(scopeUri, result, magnitudeMax, magnitudeMin);
        return tagElements;
    }

    private List<TagElement> generateTagElementList(Path scopeUri, 
            PropertyValueFrequencyQueryResult result, int magnitudeMax, int magnitudeMin) {

        List<Pair<Value, Integer>> freqList = result.getValueFrequencyList();

        // Makes a list with tagelements.
        List<TagElement> tagElements = new ArrayList<TagElement>(freqList.size());

        if (!freqList.isEmpty()) {

            int minFreq = freqList.get(freqList.size() - 1).second().intValue();
            int maxFreq = freqList.get(0).second().intValue();

            for (Pair<Value, Integer> pair : freqList) {
                String tagName = pair.first().getStringValue();
                String link = getUrl(tagName, scopeUri);

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


    private String getUrl(String tagName, Path scopeUri) {
        if (scopeUri.isRoot()) {
            if (!this.servesRoot) {
                return RequestContext.getRequestContext().getCurrentCollection() + "/?vrtx=tags&tag=" + tagName + "&scope=" + Path.ROOT;
            } 
            return "/?vrtx=tags&tag=" + tagName;
        }
        
        return scopeUri + "/?vrtx=tags&tag=" + tagName; 
    }


}
