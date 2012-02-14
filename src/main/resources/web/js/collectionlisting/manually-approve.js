/**
 * JS for handling manually approved resources
 *
 * TODO: cleaner interfaces and easier to understand code (probably refactor more human-readable methods)
 *
 */

var lastVal = "", manuallyApproveFoldersTxt, aggregatedFoldersTxt, approvedOnly = false;

$(window).load(function() {
  // Retrieve initial resources
  manuallyApproveFoldersTxt = $("#resource\\.manually-approve-from");
  aggregatedFoldersTxt = $("#resource\\.aggregation");
  
  if(manuallyApproveFoldersTxt.length) {
    var folders, aggregatedFolders;
    var value = manuallyApproveFoldersTxt.val();
    lastVal = $.trim(value);
    folders = lastVal.split(",");
    if(aggregatedFoldersTxt.length) {
      aggregatedFolders = $.trim(aggregatedFoldersTxt.val());
      aggregatedFolders = aggregatedFolders.split(",");
    }
    retrieveResources(".", folders, aggregatedFolders);
    
    
    var html = '<ul id="vrtx-manually-approve-tab-menu">'
               + '<li class="active active-first"><span>' + approveShowAll + '</span></li>'
               + '<li class="last"><a href="javascript:void(0);">' + approveShowApprovedOnly + '</a></li>'
             + '</ul>';
    $(html).insertAfter("#manually-approve-container-title");
  }
});

$(document).ready(function() {

    $("#app-content").delegate("#vrtx-manually-approve-tab-menu a", "click", function(e) {
      var parent = $(this).parent();
      $(this).replaceWith("<span>" + $(this).html() + "</span>");
      if(parent.hasClass("last")) {
        approvedOnly = true;
        parent.attr("class", "active active-last");
        var parentPrev = parent.prev();
        parentPrev.attr("class", "first");
        parentPrevSpan = parentPrev.find("span");
        parentPrevSpan.replaceWith('<a href="javascript:void(0);">' + parentPrevSpan.html() + "</a>");
      } else {
        approvedOnly = false;
        parent.attr("class", "active active-first");
        parentNext = parent.next();
        parentNext.attr("class", "last");
        parentNextSpan = parentNext.find("span");
        parentNextSpan.replaceWith('<a href="javascript:void(0);">' + parentNextSpan.html() + "</a>");     
      }
      $("#manually-approve-refresh").trigger("click");
      e.stopPropagation();
      e.preventDefault();
    });

    $("#manually-approve-refresh").click(function(e) {
      if(manuallyApproveFoldersTxt && manuallyApproveFoldersTxt.length) {
        var folders, aggregatedFolders;
        
        formatDocumentsData();
      
        var value = manuallyApproveFoldersTxt.val();
        lastVal = $.trim(value);
        folders = lastVal.split(",");
        if(aggregatedFoldersTxt.length) {
          aggregatedFolders = $.trim(aggregatedFoldersTxt.val());
          aggregatedFolders = aggregatedFolders.split(",");
        }

        retrieveResources(".", folders, aggregatedFolders);  
      }
      e.stopPropagation();
      e.preventDefault();
    });

    // Add / remove manually approved uri's
    $("#manually-approve-container").delegate("td.checkbox input", "change", function(e) {
      var textfield = $("#resource\\.manually-approved-resources");
      var value = textfield.val();
      var uri = $(this).val();
      if (this.checked) {
        if (value.length) {
          value += ", " + uri;
        } else {
          value = uri;
        }
      } else {
        if (value.indexOf(uri) == 0) {
          value = value.replace(uri, "");
        } else {
          value = value.replace(", " + uri, "");
        }
      }
      textfield.val(value);
    });
    
    // Paging - next
    $("#manually-approve-container").delegate(".next", "click", function(e) {
      var that = $(this).parent();
      var next = that.next();
      if (next.attr("id") && next.attr("id").indexOf("approve-page") != -1) {
        $(that).hide();
        $(next).show();
      }
      return false;
    });

    // Paging - previous
    $("#manually-approve-container").delegate(".prev", "click", function(e) {
      var that = $(this).parent();
      var prev = that.prev();
      if (prev.attr("id") && prev.attr("id").indexOf("approve-page") != -1) {
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

  if(approvedOnly) {
    var getUri = serviceUri + "/?vrtx=admin&service=manually-approve-resources&approved-only";
  } else {
    var getUri = serviceUri + "/?vrtx=admin&service=manually-approve-resources";
  }
  
  if (folders) {
    for (var i = 0, len = folders.length; i < len; i++) {
      getUri += "&folders=" + $.trim(folders[i]);
    }
  }
  if (aggregatedFolders) {
    for (i = 0, len = aggregatedFolders.length; i < len; i++) {
      getUri += "&aggregate=" + $.trim(aggregatedFolders[i]);
    }
  }

  $.ajax( {
    url: getUri + "&no-cache=" + (+new Date()),
    dataType: "json",
    cache: false,
    success: function(data) {
      if (data != null && data.length > 0) {
        $("#manually-approve-container:hidden").removeClass("hidden");
        generateManuallyApprovedContainer(data);
        // TODO !spageti && !run twice
        if (requestFromEditor()) {
          storeInitPropValues();
        }
      } else {
        $("#manually-approve-container").addClass("hidden");
      }
    },
    error: function(xhr, textStatus) {
      var errMsg = "<span class='manually-approve-from-ajax-error'>";
      if (xhr.readyState == 4 && xhr.status == 200) {
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
 * First page synchronous (if more than one page) Rest of pages asynchrounous
 * adding each to DOM when complete
 * 
 * @param resources as JSON array
 * 
 */

function generateManuallyApprovedContainer(resources) {

  // Initial setup
  var pages = 1, prPage = 15, len = resources.length, remainder = len % prPage, moreThanOnePage = len > prPage, totalPages = len > prPage ? (parseInt(len
      / prPage) + 1)
      : 1;

  // Function pointers
  var generateTableRowFunc = generateTableRow;
  var generateTableEndAndPageInfoFunc = generateTableEndAndPageInfo;
  var generateNavAndEndPageFunc = generateNavAndEndPage;
  var generateStartPageAndTableHeadFunc = generateStartPageAndTableHead;

  var i = 0;

  var html = generateStartPageAndTableHead(pages);
  // If more than one page
  if (moreThanOnePage) {
    for (; i < prPage; i++) { // Generate first page synchronous
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
  $("#manually-approve-container-title").append(
      "<span id='approve-spinner'>" + approveGeneratingPage + " <span id='approve-spinner-generated-pages'>"
      + pages + "</span> " + approveOf + " " + totalPages + "...</span>");
  // Generate rest of pages asynchronous
  setTimeout( function() {
    html += generateTableRowFunc(resources[i], i);
    if ((i + 1) % prPage == 0) {
      html += generateTableEndAndPageInfoFunc(pages, prPage, len, false);
      pages++;
      if (i < len - 1) {
        html += generateNavAndEndPageFunc(i, html, prPage, remainder, pages, totalPages);
        $("#manually-approve-container").append(html);
        if (moreThanOnePage) {
          $("#manually-approve-container #approve-page-" + (pages - 1)).hide();
        }
        html = generateStartPageAndTableHeadFunc(pages);
      }
      $("#approve-spinner-generated-pages").html(pages);
    }
    i++;
    if (i < len) {
      setTimeout(arguments.callee, 1);
    } else {
      if (remainder != 0) {
        html += generateTableEndAndPageInfoFunc(pages, prPage, len, true);
      } else {
        pages--;
      }
      if (len > prPage) {
        html += "<a href='#page-" + (pages - 1) + "' class='prev' id='page-" + (pages - 1) + "'>" 
              + approvePrev + " " + prPage + "</a>";
      }
      html += "</div>";
      $("#manually-approve-container").append(html);
      initializeCheckUncheckAll();
      $("#approve-spinner").remove();
      if (len > prPage) {
        $("#manually-approve-container #approve-page-" + pages).hide();
      }
      // TODO !spageti && !run twice
     if (requestFromEditor()) {
       storeInitPropValues();
     }
    }
  }, 1);

}

/* HTML generation functions */

function generateTableRow(resource, i) {
  if (i & 1) { // faster than i % 2
    var html = "<tr class='even'>";
  } else {
    var html = "<tr>";
  }
  if (resource.approved) {
    html += "<td class='checkbox'><input type='checkbox' checked='checked' value='" + resource.uri + "'/></td>";
  } else {
    html += "<td class='checkbox'><input type='checkbox' value='" + resource.uri + "'/></td>";
  }
  html += "<td><a class='approve-link' target='_blank' href='" + resource.uri + "' title='" + resource.title + "'>" + resource.title
      + "</a></td>" + "<td>" + resource.source + "</td><td class='approve-published'>" + resource.published
      + "</td></tr>";
  return html;
}

function generateTableEndAndPageInfo(pages, prPage, len, lastRow) {
  var last = lastRow ? len : pages * prPage;
  return "</tbody></table><span class='approve-info'>" + approveShowing + " " + (((pages - 1) * prPage) + 1)
         + "-" + last + " " + approveOf + " " + len + "</span>";
}

function generateNavAndEndPage(i, html, prPage, remainder, pages, totalPages) {
  var nextPrPage = pages < totalPages || remainder == 0 ? prPage : remainder;
  var html = "<a href='#page-" + pages + "' class='next' id='page-" + pages + "'>" 
             + approveNext + " " + nextPrPage + "</a>";
  if (i > prPage) {
    var prevPage = pages - 2;
    html += "<a href='#page-" + prevPage + "' class='prev' id='page-" + prevPage + "'>"
          + approvePrev + " " + prPage + "</a>";
  }
  html += "</div>";
  return html;
}

function generateStartPageAndTableHead(pages) {
  return "<div id='approve-page-"
      + pages
      + "'><table><thead><tr><th id='approve-checkbox' class='checkbox'></th><th id='approve-title'>" + approveTableTitle + "</th><th id='approve-src'>" + approveTableSrc + "</th><th id='approve-published'>" + approveTablePublished + "</th></tr></thead><tbody>";
}

/* ^ HTML generation functions */

/* ^ JS for handling manually approved resources */