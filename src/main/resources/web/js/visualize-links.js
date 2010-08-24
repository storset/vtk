// AJAX request against link-check action
// that returns links seperated by new line
// Return array of dead links
function visualizeDeadLinkInit() {
	
	var deadLinks = [];
	
	var LINK_CHECK_URL = "?vrtx=admin&action=link-check";
	var URL = $(location).attr('href');
	
	if ((URL.indexOf("?") != -1)) {
		URL = URL.split("?")[0];
	}
	
	URL = URL + LINK_CHECK_URL;
	
	$.ajax({
	  type : "GET",
	  url : URL,
	  dataType : "text",
	  complete : function(msg) { 
        deadLinks = msg.responseText.split("\n");
        
		$("iframe").contents().find("body")
          .filter(function() {
            return this.id.match(/^(?!vrtx-[\S]+-listing|vrtx-collection)[\S]+/);
          })
          .find("#main")
          .not("#left-main")
            .find("a").each(function(i, e){
              visualizeDeadLink(this, deadLinks);
            });
        
	  },
      error : function (xhr, ajaxOptions, thrownError){}
	});
	
	return deadLinks;
}

// Check if current link is in array of dead links
function visualizeDeadLink(that, deadLinks) {
	
	var TARGET_URL = $(that).attr("href");
	
	var deadLinksLength = deadLinks.length;
	for(var i = 0; i < deadLinksLength; i++) {
	  if(TARGET_URL == deadLinks[i]) {
	    $(that).append(" - 404");  
	    $(that).css("color", "red"); 
	  }
	}
	
}