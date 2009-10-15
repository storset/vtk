package org.vortikal.web.referencedata.provider;

public class Facebook implements SocialWebsite {

    private String link;
    private String name;


    public void generateLink(String url, String title, String description, String name) {
        this.link = "http://www.facebook.com/share.php?u=" + url;
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