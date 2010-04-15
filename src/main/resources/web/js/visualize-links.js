function visualizeDeadLink(that, e) {

	var filteredURL = filterURL($(that).attr('href') ? $(that).attr('href') : "");

	if (filteredURL != "") {
		    $.ajax({
				type : "HEAD",
				url : filteredURL,
				complete : function(xhr, textStatus) {
				    if(xhr.status == "404") { 
					  $(that).append(" - BRUTT (404)").css("color", "red"); //internal service error or service unavailable
				    }	
				},
                error : function (xhr, ajaxOptions, thrownError){
                    //$(that).append(" - FEILET").css("color", "red");
                }
			});
	}
}

// Remove "?vrtx=" URL and extract URL in anchor
function filterURL(rawURL) {

	if ((rawURL.indexOf("?") != -1) || (rawURL.indexOf("&") != -1)) {
		return ""; //do nothing
	}
	if (rawURL.indexOf("#") != -1) { // anchor
		if (rawURL.indexOf("http://") != -1) {
		   return rawURL.split("#", 1);
		} else {
		   return "";	
		}
	} else {
		return rawURL;
	}

}