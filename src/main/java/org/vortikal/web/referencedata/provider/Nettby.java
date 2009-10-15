package org.vortikal.web.referencedata.provider;

public class Nettby implements SocialWebsite {

    private String link;
    private String name;


    public void generateLink(String url, String title, String description, String name) {
        String thelink = "http://www.nettby.no/user/edit_link.php?name=" + title + "&amp;url=" + url
                + "&amp;description=" + description;
        this.link = thelink;
        this.name = name;
    }


    public String getLink() {
        return link;
    }


    public void setLink(String link) {
        this.link = link;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }

}