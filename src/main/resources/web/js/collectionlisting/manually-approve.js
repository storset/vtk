/**
 * JS for handling manually approved resources
 */

var lastVal = "";

$(window).ready(function() {
  lastVal = $("#resource\\.manually-approve-from").val();
});

$(document).ready(function() {

	// Retrieve initial resources
	var val = $("#resource\\.manually-approve-from").val(); lastVal = val;
    var folders = val.split(",");
    var val2 = $("#resource\\.aggregation").val();
    var aggregatedFolders = val2.split(",");
    retrieveResources(".", folders, aggregatedFolders);

    // Refresh when folders to approve from are changed
    $("#manually-approve-refresh").click(function(e) {
      var val = $("#resource\\.manually-approve-from").val(); lastVal = val;
      var folders = val.split(",");
      var val2 = $("#resource\\.aggregation").val();
      var aggregatedFolders = val2.split(",");
      retrieveResources(".", folders, aggregatedFolders);
      $(this).hide();
      return false; 
    });
    
    // Add / remove manually approved uri's
    $("#manually-approve-container").delegate("input", "click", function(e) {
      var textfield = $("#resource\\.manually-approved-resources");
      var val = textfield.val();
      var uri = $(this).parent().parent().find("a.approve-link").attr("href");
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
    
    // Show refresh button only if textfield has changed
    $("#resource\\.manually-approve-from").keyup(function(e) {
      var val = $("#resource\\.manually-approve-from").val();
      if(val != lastVal) {
        $("#manually-approve-refresh").show();   
      } else {
    	$("#manually-approve-refresh:visible").hide();     
      }
    });

    // Paging - next
    $("#manually-approve-container").delegate(".next", "click", function(e) {
      var that = $(this).parent();
      var next = that.next();
      if(next.attr("id") && next.attr("id").indexOf("approve-page") != -1) {
         $(that).hide();
         $(next).show();
      }
      return false;
    });

    // Paging - previous
    $("#manually-approve-container").delegate(".prev", "click", function(e) {
      var that = $(this).parent();
      var prev = that.prev();
      if(prev.attr("id") && prev.attr("id").indexOf("approve-page") != -1) {
         $(that).hide();
         $(prev).show();
      }
      return false;
    });
 });

/**
 * Retrieves resources as JSON array for folders to manually approve from
 * 
 * @param serviceUri as string
 * @param folders as array
 * 
 */

function retrieveResources(serviceUri, folders, aggregatedFolders) {

  var getUri = serviceUri + "/?vrtx=admin&service=manually-approve-resources";
  if(folders != null) {
	for(var i = 0, len = folders.length; i < len; i++) {
	  getUri += "&folders=" + $.trim(folders[i]);  
	}
  }
  if(aggregatedFolders != null) {
	for(i = 0, len = aggregatedFolders.length; i < len; i++) {
	  getUri += "&aggregate=" + $.trim(aggregatedFolders[i]);  
	}
  }

  $.ajax({
    url: getUri,
	dataType: "json",
	success: function(data) {
	  if(data != null && data.length > 0) {
	    generateManuallyApprovedContainer(data);
	  }
	},
	error: function(xhr, textStatus) {
	  var errMsg = "<span class='manually-approve-from-ajax-error'>";
      if(xhr.readyState == 4 && xhr.status == 200) {
    	errMsg += "The service is not active.";
      } else {
    	errMsg += "The service returned " + xhr.status + " and failed to retrieve resources.";      	  
      }
      errMsg += "</span>";
      $("#manually-approve-container").html(errMsg);
	}
  });
  
}

/**
 * Generate tables with resources
 * 
 * First page synchronous (if more than one page)
 * Rest of pages asynchrounous adding each to DOM when complete
 * 
 * @param resources as JSON array
 * 
 * TODO: i18n
 * 
 */

function generateManuallyApprovedContainer(resources) {

  // Initial setup
  var pages = 1, prPage = 15, len = resources.length,
      remainder = len % prPage, moreThanOnePage = len > prPage,
      totalPages = len > prPage ? (parseInt(len / prPage) + 1) : 1;
      
  // Function pointers
  var generateTableRowFunc = generateTableRow;
  var generateTableEndAndPageInfoFunc = generateTableEndAndPageInfo;
  var generateNavAndEndPageFunc = generateNavAndEndPage; 
  var generateStartPageAndTableHeadFunc = generateStartPageAndTableHead;
  
  var i = 0;

  var html = generateStartPageAndTableHead(pages);
  //If more than one page
  if(moreThanOnePage) { 
    for(; i < prPage; i++) { // Generate first page synchronous
      html += generateTableRowFunc(resources[i], i);
    }
    html += generateTableEndAndPageInfoFunc(pages, prPage, len, false);
    pages++;
    html += generateNavAndEndPageFunc(i, html, prPage, remainder, pages, totalPages);
    $("#manually-approve-container").html(html);
    html = generateStartPageAndTableHeadFunc(pages);
  } else {
	$("#manually-approve-container").html(""); // clear if only one page
  }
  
  // Add spinner
  $("#manually-approve-container-title").append("<span id='approve-spinner'>Genererer side <span id='approve-spinner-generated-pages'>" + pages + "</span> av " + totalPages + "...</span>");
  
  // Generate rest of pages asynchronous
  setTimeout(function() { 
	html += generateTableRowFunc(resources[i], i);
    if((i+1) % prPage == 0) {
      html += generateTableEndAndPageInfoFunc(pages, prPage, len, false);
      pages++;
      if(i < len-1) {
    	html += generateNavAndEndPageFunc(i, html, prPage, remainder, pages, totalPages);
        $("#manually-approve-container").append(html);
        if(moreThanOnePage) { 
          $("#manually-approve-container #approve-page-" + (pages-1)).hide();
        }
        html = generateStartPageAndTableHeadFunc(pages);
      }
      $("#approve-spinner-generated-pages").html(pages);
    }
    i++;
    if(i < len) {
      setTimeout(arguments.callee, 1);	
    } else {
      if(remainder != 0) {
        html += generateTableEndAndPageInfoFunc(pages, prPage, len, true);
      } else {
    	pages--;  
      }
      if(len > prPage) {
        html += "<a href='#page-" + (pages-1) + "' class='prev' id='page-" + (pages-1) + "'>Forrige " + prPage + "</a>";
      }
      html += "</div>";
      $("#manually-approve-container").append(html);
      $("#approve-spinner").remove();
      if(len > prPage) {
        $("#manually-approve-container #approve-page-" + pages).hide();
      }
    }
  }, 1);
  
}

/* HTML generation functions */

function generateTableRow(resource, i) {
  if(i & 1) {
    var html = "<tr class='even'>";
  } else {
	var html = "<tr>";	  
  }
  if(resource.approved) {
    html += "<td><input type='checkbox' checked='checked' />";
  } else {
	html += "<td><input type='checkbox' />";
  }
  html += "<a class='approve-link' href='" + resource.uri + "' title='" + resource.uri + "'>" + resource.title + "</a></td>"
	    + "<td>" + resource.source + "</td><td class='approve-published'>" + resource.published + "</td></tr>";
  return html;
}

function generateTableEndAndPageInfo(pages, prPage, len, lastRow) {
  var last = lastRow ? len : pages * prPage;	
  return "</tbody></table><span class='approve-info'>Viser " + (((pages-1) * prPage)+1) + "-" + last + " av " + len + "</span>";
}

function generateNavAndEndPage(i, html, prPage, remainder, pages, totalPages) {
  var nextPrPage = pages < totalPages || remainder == 0 ? prPage : remainder;
  var html = "<a href='#page-" + pages + "' class='next' id='page-" + pages + "'>Neste " + nextPrPage + "</a>";
  if(i > prPage) {
    var prevPage = pages - 2;
    html += "<a href='#page-" + prevPage + "' class='prev' id='page-" + prevPage + "'>Forrige " + prPage + "</a>";
  }
  html += "</div>";
  return html; 
}

function generateStartPageAndTableHead(pages) {
  return "<div id='approve-page-" + pages + "'><table><thead><tr><th>Tittel</th><th>Kilde</th><th id='approve-published'>Publisert</th></tr></thead><tbody>";
}

/* ^ HTML generation functions */