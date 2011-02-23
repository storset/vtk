/**
 * JS for handling manually approved resources
 */

$(document).ready(function() {

	// Retrieve initial resources
    retrieveResources(".", null);

    // Refresh when folders to approve from are changed
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

    // Paging - next
    $("#manually-approve-container").delegate(".next", "click", function() {
      var that = $(this).parent();
      var next = that.next();
      if(next.attr("id") && next.attr("id").indexOf("approve-page") != -1) {
         $(that).hide();
         $(next).show();
      }
      return false;
    });

    // Paging - previous
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

/**
 * Retrieves resources as JSON array for folders to manually approve from
 * 
 * @param serviceUri as string
 * @param folders as array
 * 
 */

function retrieveResources(serviceUri, folders) {

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
	    generateManuallyApprovedContainer(data);
	  }
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
  var pages = 1, prPage = 25, len = resources.length,
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
      html += generateTableRowFunc(resources[i]);
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
  $("#manually-approve-container").prepend("<span id='approve-spinner'><img src='/vrtx/__vrtx/static-resources/themes/default/icons/tabmenu-spinner.gif' alt='spinner' />"
                                      + "&nbsp;Genererer side <span id='approve-spinner-generated-pages'>" + pages + "</span> av " + totalPages + "...</span>");
  
  // Generate rest of pages asynchronous
  setTimeout(function() { 
	html += generateTableRowFunc(resources[i]);
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
      }
      if(len > prPage) {
        html += "<a href='#page-" + (pages-1) + "' class='prev' id='page-" + (pages-1) + "'>Forrige " + prPage + "</a>";
      }
      html += "</div>";
      $("#manually-approve-container").append(html)
        .find("#approve-spinner").remove();
      if(len > prPage) { 
        $("#manually-approve-container #approve-page-" + pages).hide();
      }
    }
  }, 1);
  
}

/* HTML generation functions */

function generateTableRow(resource) {
  var html = "";
  if(resource.approved) {
    html += "<tr><td><input type='checkbox' checked='checked' />";
  } else {
	html += "<tr><td><input type='checkbox' />";
  }
  html += "<a href='" + resource.uri + "'>" + resource.title + "</a></td>"
	    + "<td class='uri'>" + resource.uri + "</td><td>" + resource.published + "</td></tr>";
  return html;
}

function generateTableEndAndPageInfo(pages, prPage, len, lastRow) {
  var last = lastRow ? len : pages * prPage;	
  return "</tbody></table><span class='approve-info'>Viser " + (((pages-1) * prPage)+1) + "-" + last + " av " + len + "</span>";
}

function generateNavAndEndPage(i, html, prPage, remainder, pages, totalPages) {
  var html = "";
  if(i > prPage) {
    var prevPage = pages - 2;
    html += "<a href='#page-" + prevPage + "' class='prev' id='page-" + prevPage + "'>Forrige " + prPage + "</a>";
  }
  var nextPrPage = pages < totalPages || remainder == 0 ? prPage : remainder;
  html += "<a href='#page-" + pages + "' class='next' id='page-" + pages + "'>Neste " + nextPrPage + "</a></div>";
  return html; 
}

function generateStartPageAndTableHead(pages) {
  return "<div id='approve-page-" + pages + "'><table><thead><tr><th>Tittel</th><th>Uri</th><th>Publisert</th></tr></thead><tbody>";
}

/* ^ HTML generation functions */