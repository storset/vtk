package org.vortikal.web.tags;

public class TagElement extends Tag implements Comparable<TagElement> {

    private int magnitude;
    private String linkUrl;
    private int occurences;


    public TagElement(int magnitude, String linkUrl, String text,int occurences) {
        super(text);
        this.magnitude = magnitude;
        this.linkUrl = linkUrl;
        this.occurences = occurences;
    }

	public int getMagnitude() {
        return magnitude;
    }


    public String getLinkUrl() {
        return linkUrl;
    }


    // VTK-1107: Sets the text to compare to lowercase,
    // thus avoiding problem with sorting.
    public int compareTo(TagElement other) {
        return this.getText().toLowerCase().compareTo(other.getText().toLowerCase());
    }

	public int getOccurences() {
		return occurences;
	}

}
