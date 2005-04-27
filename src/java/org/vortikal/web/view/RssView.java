/*
 * Created on 27.apr.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.vortikal.web.view;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * @author Kristian
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RssView implements View {

    /**
     * Map 'model' contains a list of TavleIndexHolder objects containing the data used for the RSS feed 
     * @see org.springframework.web.servlet.View#render(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(Map model, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // TODO Auto-generated method stub
        
        PrintWriter writer = resp.getWriter();
        
        //String s = (String) model.get("test");
        //writer.print(s);
        //writer.print( model.get("documents"));
        //writer.close();
    
        // Genererer og skriver ut RSS-fila
        try {
            SyndFeed feed = new SyndFeedImpl();
         
            // Set format and feed header info (title, link, description)
            feed.setFeedType("rss_1.0");
            feed.setTitle("NoticeboardRSS");
            feed.setLink("http://www.uio.no/NoticeBoard");
            feed.setDescription("RSS feed for UiO-tavler");
            
            // Created and add list of entries  
            // (Each entry is set with a title, link, published date and a description) 
            // ( -> Description can be plain text or HTML) 
            List rssEntries = new ArrayList();
            SyndEntry entry;
            SyndContent description;

            /*
     entry = new SyndEntryImpl();
     entry.setTitle("ROME v1.0");
     entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome01");
     entry.setPublishedDate(DATE_PARSER.parse("2004-06-08"));
     description = new SyndContentImpl();
     description.setType("text/plain"); // or ("text/html")
     description.setValue("Initial release of ROME");
     entry.setDescription(description);
     entries.add(entry);
             */
            
            
            // Add list with entries to the SyndFeed bean.
            feed.setEntries(rssEntries);
            
            /*
            // To write a syndication feed XML document using ROME:
            SyndFeed feed = ...;
            Writer writer = ...;

            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed,writer);
            writer.close();
            */

            // DUMMY ENTRY:
            Date testDate = new Date();
            testDate.setYear(2005);
            testDate.setMonth(4);
            testDate.setDate(6);
            // TEST !!!
            entry = new SyndEntryImpl();
            entry.setTitle("TestEntry 0.1");
            entry.setLink("http://www.itavisen.no");
            entry.setPublishedDate(testDate);
            description = new SyndContentImpl();
            description.setType("text/plain");
            description.setValue("Initial release of ROME");
            entry.setDescription(description);
            
            rssEntries.add(entry);

            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed,writer);
            
            writer.close();

       
        } catch (Exception e) {}
    } // end of render()

    /**
     * Method to replace CP1252-special characters and re-code to UTF-8
     * @param  string
     * @return UTF-8 encoded string
     */
    /*
    private String cleanAndCodeUTF8(String s) throws Exception {
        String iso_8859_1 = no.uio.util.TextUtil.cp1252ToISO_8859_1(s);
        return new String(iso_8859_1.getBytes("UTF-8"));
    }*/
        
} // end class RssView
