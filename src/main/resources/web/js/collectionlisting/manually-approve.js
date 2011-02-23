/**
 * JS for handling manually approved resources
 */

function toggleManuallyApprovedContainer(resources) {
  // Toggle visibility of container for list of resources to manually approve.
  // "resources" is complete list (array) of resources. If empty, hide
  // container, else display. Use to mark resources.

  // TODO: i18n ++
  // TODO: refactor duplicate code
	
  //var startTime = new Date();
	
  var pages = 1, prPage = 25, len = resources.length, remainder = len % prPage;
  var totalPages = len > prPage ? (parseInt(len / prPage) + 1) : 1;

  var html = "<div id='approve-page-" + pages + "'>"
           + "<table><thead><tr><th>Tittel</th><th>Uri</th><th>Publisert</th></thead></tr><tbody>";
  
  var i = 0;
  
  if(len > prPage) { // If more than one page
    for(; i < prPage; i++) { // Generate first page synchronous
      if(resources[i].approved) {
	    html += "<tr><td><input type='checkbox' checked='checked' />";
	  } else {
	    html += "<tr><td><input type='checkbox' />";
	  }
	  html += "<a href='" + resources[i].uri + "'>" + resources[i].title + "</a></td>"
	        + "<td class='uri'>" + resources[i].uri + "</td><td>" + resources[i].published + "</td></tr>";
	  if((i+1) % prPage == 0) {
	    html += "</tbody></table>"
	       	  + "<span class='approve-info'>Viser " + (((pages-1) * prPage)+1) + "-" + (pages * prPage) + " av " + len + "</span>";
	    pages++;
	    if(i < len-1) {
	      if(i > prPage) {
	        html += "<a href='#page-" + (pages-2) + "' class='prev' id='page-" + (pages-2) + "'>Forrige " + prPage + "</a>";
	      }
	      var nextPrPage = pages < totalPages || remainder == 0 ? prPage : remainder;
	      html += "<a href='#page-" + pages + "' class='next' id='page-" + pages + "'>Neste " + nextPrPage + "</a>"
	            + "</div>";
	    }
      }  	    
    }
    $("#manually-approve-container").html(html);
    html = "<div id='approve-page-" + pages + "'>"
         + "<table><thead><tr><th>Tittel</th><th>Uri</th><th>Publisert</th></tr></thead><tbody>";
  }
  
  $("#manually-approve-container").prepend("<span id='approve-spinner'><img src='/vrtx/__vrtx/static-resources/themes/default/icons/tabmenu-spinner.gif' alt='spinner' />"
                                      + "&nbsp;Genererer side <span id='approve-row'>" + pages + "</span> av " + totalPages + "...</span>");
  
  setTimeout(function() { // Generate rest of pages asynchronous
    if(resources[i].approved) {
      html += "<tr><td><input type='checkbox' checked='checked' />";
    } else {
      html += "<tr><td><input type='checkbox' />";
    }
    html += "<a href='" + resources[i].uri + "'>" + resources[i].title + "</a></td>"
          + "<td class='uri'>" + resources[i].uri + "</td><td>" + resources[i].published + "</td></tr>";
    if((i+1) % prPage == 0) {
      html += "</tbody></table>"
    	    + "<span class='approve-info'>Viser " + (((pages-1) * prPage)+1) + "-" + (pages * prPage) + " av " + len + "</span>";
      pages++;
      if(i < len-1) {
        if(i > prPage) {
          html += "<a href='#page-" + (pages-2) + "' class='prev' id='page-" + (pages-2) + "'>Forrige " + prPage + "</a>";
        }
        var nextPrPage = pages < totalPages || remainder == 0 ? prPage : remainder;
        html += "<a href='#page-" + pages + "' class='next' id='page-" + pages + "'>Neste " + nextPrPage + "</a>"
              + "</div><div id='approve-page-" + pages + "'>"
              + "<table><thead><tr><th>Tittel</th><th>Uri</th><th>Publisert</th></tr></thead><tbody>";
      }
      $("#approve-row").html(pages);
    }
    i++;
    if(i < len) {
      setTimeout(arguments.callee, 1);	
    } else {
      if(remainder != 0) {
        html += "</tbody></table><span class='approve-info'>Viser " + (((pages-1) * prPage)+1) + "-" + len + " av " + len + "</span>";
      }
      if(len > prPage) {
        html += "<a href='#page-" + (pages-1) + "' class='prev' id='page-" + (pages-1) + "'>Forrige " + prPage + "</a>";
      }
      html += "</div>";
      $("#manually-approve-container").append(html)
        //.prepend("Tid: " + (new Date() - startTime) + "ms")
        .find("#approve-spinner").remove();
      $("#manually-approve-container div").not("#approve-page-1").hide();
    }
  }, 1);
}

function retrieveResources(serviceUri, folders) {
  // Retrieve and return array of resources for folders to manually approve from.
  // Needs Vortex-service.
  
  var getUri = serviceUri + "/?vrtx=manually-approve-resources";
  if(folders != null) {
	for(var i = 0, len = folders.length; i < len; i++) {
	  getUri += "&folders=" + $.trim(folders[i]);  
	}
  }
	
  $.ajax({
    url: getUri,
	dataType: "json",
	success: function(data){
	  if(data != null && data.length > 0) {
	    toggleManuallyApprovedContainer(data);
	  }
	}
  });
}

$(document).ready(function() {

    retrieveResources(".", null);

    // Refresh table
    $("#manually-approve-refresh").click(function(e) {
      var folders = $("#resource\\.manually-approve-from").val().split(",");
      retrieveResources(".", folders);
      return false; 
    });
    
    // Add / remove uri's
    $("#manually-approve-container").delegate("input", "click", function(e) {
      var textfield = $("#resource\\.manually-approved-resources");
      var val = textfield.val();
      var uri = $(this).parent().parent().find("td.uri").text();
      if($(this).attr("checked")) {
    	if(val.length) {
          val += ", " + uri;
    	} else {
    	  val = uri;	
    	}
      } else {
    	if(val.indexOf(uri) == 0) { // not first
    	  val = val.replace(uri, ""); 
    	} else {
    	  val = val.replace(", " + uri, ""); 
    	}
      }
      textfield.val(val);
    });

    // Paging - Next
    $("#manually-approve-container").delegate(".next", "click", function() {
      var that = $(this).parent();
      var next = that.next();
      if(next.attr("id") && next.attr("id").indexOf("approve-page") != -1) {
         $(that).hide();
         $(next).show();
      }
      return false;
    });

    // Paging - Previous
    $("#manually-approve-container").delegate(".prev", "click", function() {
      var that = $(this).parent();
      var prev = that.prev();
      if(prev.attr("id") && prev.attr("id").indexOf("approve-page") != -1) {
         $(that).hide();
         $(prev).show();
      }
      return false;
    });
 });