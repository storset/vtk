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
package org.vortikal.web.decorating.components;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

import org.vortikal.util.cache.ContentCacheLoader;
import org.vortikal.web.decorating.components.AggregatedFeedsComponent.MissingPublishedDateException;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class AggregatedFeedsComponentTest extends TestCase {

    @SuppressWarnings("unchecked")
    public void testSorting() throws Exception {
        List<SyndEntry> entries = new FeedLoader().load("feed1.xml").getEntries();

        AggregatedFeedsComponent component = new AggregatedFeedsComponent();
        component.sort(entries);

        assertEquals("Newest", entries.get(0).getTitle());
        assertEquals("Oldest", entries.get(1).getTitle());
    }
  
    @SuppressWarnings("unchecked")
    public void testMissingPubDate() throws Exception {
        List<SyndEntry> entries = new FeedLoader().load("missingPubDate.xml").getEntries();

        AggregatedFeedsComponent component = new AggregatedFeedsComponent();
        try {
            component.sort(entries);
            fail("Should fail on missing published date");
        } catch (MissingPublishedDateException e) {
            assertEquals("Missing", e.getEntry().getTitle());
        }
    }
    
    private class FeedLoader implements ContentCacheLoader<String, SyndFeed> {

        public SyndFeed load(String identifier) throws Exception {
            return getFeed(getClass().getResourceAsStream(identifier));
        }

        
        private SyndFeed getFeed(InputStream stream) throws IOException, IllegalArgumentException, FeedException {
            XmlReader xmlReader = new XmlReader(stream);
            SyndFeedInput input = new SyndFeedInput();
            return input.build(xmlReader);
        }
    }

    
    
//  private void run(String[] urlArray) {
//
//      List<SyndEntry> entries = new ArrayList<SyndEntry>();
//      Map<SyndEntry, SyndFeed> feedMapping = new HashMap<SyndEntry, SyndFeed>();
//      
//      ContentCache cache = new ContentCache();
//      cache.setName("lala");
//      cache.setCacheLoader(new FeedLoader());
//      cache.afterPropertiesSet();
//      AggregatedFeedsComponent component = new AggregatedFeedsComponent();
//      component.setContentCache(cache);
//      component.parseFeeds(null, entries, feedMapping, urlArray);
//      component.sort(entries);
//      write(entries, feedMapping);
//  }

//  private void write(List<SyndEntry> entries, Map<SyndEntry, SyndFeed> feedMapping) {
//      for (SyndEntry entry : entries) {
//          System.out.println(entry.getPublishedDate() + " " + feedMapping.get(entry).getTitle());
//      }
//      
//  }
  

}
