/*
 * Created on 27.apr.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.vortikal.web.view;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

/**
 * @author Kristian
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RssView implements View {

    /* (non-Javadoc)
     * @see org.springframework.web.servlet.View#render(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(Map model, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // TODO Auto-generated method stub
        
        PrintWriter writer = resp.getWriter();
        writer.print("test");
        writer.close();
        
        /*
        // Genererer og skriver ut RSS-fila
        try {
            SyndFeed feed = new SyndFeedImpl();
            feed.setFeedType("rss_1.0");
            feed.setPublishedDate(new NSTimestamp());
            feed.setTitle(cleanAndCodeUTF8(lokalKonto.getDelNavn()));
            feed.setLink(cleanAndCodeUTF8(avisUrl));
            feed.setDescription(cleanAndCodeUTF8(lokalKonto.getDelNavn()
                + " - nettavis på universitetet i Oslo"));

            NSArray artiklerTilRSS =
Artikkel.nyesteArtiklerForKonto(lokalKonto, 15, getEC());
            Artikkel enArtikkel;
            List syndEntries = new ArrayList();
            SyndEntry syndEntry;
            SyndContent syndDescription;
            String description;

            for (int i = 0; i < artiklerTilRSS.count(); i++) {
                enArtikkel = (Artikkel) artiklerTilRSS.objectAtIndex(i);
                syndEntry = new SyndEntryImpl();

syndEntry.setTitle(cleanAndCodeUTF8(localStripHTML(enArtikkel.getKorttittel())));

syndEntry.setLink(cleanAndCodeUTF8(enArtikkel.fullUrlTilArtikkel(context())));
                syndEntry.setPublishedDate(enArtikkel.getPublisertDato());
                syndDescription = new SyndContentImpl();
                syndDescription.setType("text/plain");
                description =
cleanAndCodeUTF8(localStripHTML(enArtikkel.getPresentasjon()));
                // Maks lovlige lengde for description er 500 tegn.
                if (description != null && description.length() > 500) {
                    syndDescription.setValue(description.substring(0, 500));
                } else {
                    syndDescription.setValue(description);
                }
                syndEntry.setDescription(syndDescription);
                syndEntries.add(syndEntry);
            }

            feed.setEntries(syndEntries);

            FileWriter fileWriter = new FileWriter(filnavn);
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, fileWriter);
            fileWriter.close();

            //Endrer rettighetene slik at filen er lesbar for alle
            String chmod = Application.systemUtil.pathOfProgram("chmod")
+ " go+r " + filnavn;
            Runtime.getRuntime().exec(chmod);
        } catch (Exception exception) {
            // Hvis noe går galt, dropper jeg rett og slett å skrive fila. Det er enklt å få gjort dette senere likevel
            System.out.println("Exception ved skriving av RSS-fil!!");
            System.out.println("Filnavn: " + filnavn);
            System.out.println("Exception:\n" + exception.toString());
        }
    }


    
    // Erstatter CP1252-spesialtegn og deretter koder til UTF-8.
    private String cleanAndCodeUTF8(String s) throws
UnsupportedEncodingException {
        String iso_8859_1 = no.uio.util.TextUtil.cp1252ToISO_8859_1(s);
        return new String(iso_8859_1.getBytes("UTF-8"));
    }
        
  */      
    }

}
