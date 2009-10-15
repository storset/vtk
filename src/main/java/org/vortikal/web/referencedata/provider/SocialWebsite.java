package org.vortikal.web.referencedata.provider;

public interface SocialWebsite {

    public void generateLink(String url, String title, String description, String name);


    public String getLink();


    public void setLink(String link);


    public String getName();


    public void setName(String name);

}
