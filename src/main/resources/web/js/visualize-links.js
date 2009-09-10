// Visualize dead links
function visualizeDeadLink(that, doExternalLink, e) {

	var filteredURL = filterURL($(that).attr('href') ? $(that).attr('href') : "");

	if (filteredURL != "") {

		var cssRedBlinkingLink = {
			'color' :'red',
			'text-decoration' : 'blink'
		}
		
		var cssRedLink = {
			'color' :'red'
		}

		var cssGreenBlinkingLink = {
			'color' :'green',
			'text-decoration' : 'blink'
		}
		
		var cssGreenLink = {
			'color' :'green'
		}
		
		// Internal link
		if (e.hostname && e.hostname == location.hostname) {
			$.ajax( {
				type :"HEAD",
				url :filteredURL,
				complete : function(xhr, textStatus) {
					if (xhr.status == "404") {
						$(that).append(" - BROKEN (404)").css(cssRedBlinkingLink); //broken
					} else if(xhr.status == "500" || xhr.status == "503") { 
						$(that).append(" - SERVICE DOWN / UNAVAILABLE").css(cssRedBlinkingLink); //internal service error or service unavailable
					} else if (xhr.status == "401" || xhr.status == "403") {
						$(that).append(" - RESTRICTED / FORBIDDEN").css(cssRedLink); //unauthorized or forbidden (Opera) - visRestrictedResources
					} else if(xhr.status == "301") {
						$(that).append(" - MOVED PERMANENTLY").css(cssGreenLink); //redirects
					}
				}
			});
		}

		// External link
		// This is optional / experimental as it is doing external JSON requests
		if ((e.hostname && e.hostname !== location.hostname) && doExternalLink) {
			var url = "http://json-head.appspot.com/?url="
					+ encodeURI(filteredURL) + "&callback=?";
			$.getJSON(url, function(json) {
				// returns nothing in JSON-object if 404
					if (typeof json.status_code == "undefined") {
						$(that).append(" - BROKEN (404) - EXT").css(cssDeadLink);
					}
				});
		}

	}
}

// Remove "?vrtx=" URL and extract URL in anchor
function filterURL(rawURL) {

	if (rawURL.indexOf("?vrtx=") != -1) {
		return "";
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

	// Can also be written like this:
	// return rawURL.indexOf("?vrtx=") != -1 ? "" : rawURL.indexOf("#") != -1
	// ? rawURL.indexOf("http://") != -1 ? rawURL.split("#", 1) : "" : rawURL;

}