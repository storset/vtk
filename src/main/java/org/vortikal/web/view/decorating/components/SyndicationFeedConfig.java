package org.vortikal.web.view.decorating.components;

public class SyndicationFeedConfig {

    boolean feedTitle = true;
    boolean feedDescription = false;
    boolean itemDescription = false;
    int maxMsgs = 10;
    boolean publishedDate = true;
    boolean bottomLinkToAllMessages = true;
    boolean sortByTitle = false;
    private String format = "dd.MM.yyyy HH:mm";
    
    public boolean isBottomLinkToAllMessages() {
        return bottomLinkToAllMessages;
    }
    public void setBottomLinkToAllMessages(boolean bottomLinkToAllMessages) {
        this.bottomLinkToAllMessages = bottomLinkToAllMessages;
    }
    public boolean isFeedDescription() {
        return feedDescription;
    }
    public void setFeedDescription(boolean feedDescription) {
        this.feedDescription = feedDescription;
    }
    public boolean isFeedTitle() {
        return feedTitle;
    }
    public void setFeedTitle(boolean feedTitle) {
        this.feedTitle = feedTitle;
    }
    public boolean isItemDescription() {
        return itemDescription;
    }
    public void setItemDescription(boolean itemDescription) {
        this.itemDescription = itemDescription;
    }
    public int getMaxMsgs() {
        return maxMsgs;
    }
    public void setMaxMsgs(int maxMsgs) {
        this.maxMsgs = maxMsgs;
    }
    public boolean isPublishedDate() {
        return publishedDate;
    }
    public void setPublishedDate(boolean publishedDate) {
        this.publishedDate = publishedDate;
    }
    public boolean isSortByTitle() {
        return sortByTitle;
    }
    public void setSortByTitle(boolean sortByTitle) {
        this.sortByTitle = sortByTitle;
    }
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }

}
