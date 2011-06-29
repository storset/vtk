package org.vortikal.web.display.listing;

import org.vortikal.web.service.URL;

public class ListingPagingLink {
	
	/* Can be number of page, prev or next. */
	private String title;
	
	private URL url;
	
	/* Current page */
	private boolean marked;
	
	public ListingPagingLink(String title, URL url, boolean marked) {
		this.title = title;
		this.url = url;
		this.marked = marked;
	} 
	
	public String getTitle() {
		return title;
	}


	public URL getUrl() {
		return url;
	}


	public boolean isMarked() {
		return marked;
	}
}