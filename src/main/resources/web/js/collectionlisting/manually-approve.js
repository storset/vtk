/**
 * JS for handling manually approved resources
 *
 * TODO: cleaner interfaces and easier to understand code (probably refactor more human-readable methods)
 *
 */

var lastVal = "";

$(window).load(function() {
  var manuallyApproveFolders = $("#resource\\.manually-approve-from");
  if(manuallyApproveFolders.length) {
    lastVal = manuallyApproveFolders.val()
  }
});

$(document).ready(function() {

    // Retrieve initial resources
    var manuallyApproveFoldersTxt = $("#resource\\.manually-approve-from");
    var aggregatedFoldersTxt = $("#resource\\.aggregation");
    
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
    }

    // Refresh when folders to approve from are changed
    $("#manually-approve-refresh").click(function(e) {
      if(manuallyApproveFoldersTxt.length) {
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
    $("#manually-approve-container").delegate("input", "click", function(e) {
      var textfield = $("#resource\\.manually-approved-resources");
      var value = textfield.val();
      var uri = $(this).val();
      if ($(this).is(":checked")) {
        if (value.length) {
          value += ", " + uri;
        } else {
          value = uri;
        }
      } else {
        if (value.indexOf(uri) == 0) { // not first
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

  var getUri = serviceUri + "/?vrtx=admin&service=manually-approve-resources";
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
    url :getUri,
    dataType :"json",
    success : function(data) {
      if (data != null && data.length > 0) {
        $("#manually-approve-container:hidden").removeClass("hidden");
        generateManuallyApprovedContainer(data);
      } else {
        $("#manually-approve-container").addClass("hidden");
      }
    },
    error : function(xhr, textStatus) {
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
 * TODO: i18n
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
      "<span id='approve-spinner'>Genererer side <span id='approve-spinner-generated-pages'>" + pages + "</span> av "
          + totalPages + "...</span>");

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
        html += "<a href='#page-" + (pages - 1) + "' class='prev' id='page-" + (pages - 1) + "'>Forrige " + prPage
            + "</a>";
      }
      html += "</div>";
      $("#manually-approve-container").append(html);
      $("#approve-spinner").remove();
      if (len > prPage) {
        $("#manually-approve-container #approve-page-" + pages).hide();
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
    html += "<td><input type='checkbox' checked='checked' value='" + resource.uri + "'/>";
  } else {
    html += "<td><input type='checkbox' value='" + resource.uri + "'/>";
  }
  html += "<a class='approve-link' href='" + resource.uri + "' title='" + resource.title + "'>" + resource.title
      + "</a></td>" + "<td>" + resource.source + "</td><td class='approve-published'>" + resource.published
      + "</td></tr>";
  return html;
}

function generateTableEndAndPageInfo(pages, prPage, len, lastRow) {
  var last = lastRow ? len : pages * prPage;
  return "</tbody></table><span class='approve-info'>Viser " + (((pages - 1) * prPage) + 1) + "-" + last + " av " + len
      + "</span>";
}

function generateNavAndEndPage(i, html, prPage, remainder, pages, totalPages) {
  var nextPrPage = pages < totalPages || remainder == 0 ? prPage : remainder;
  var html = "<a href='#page-" + pages + "' class='next' id='page-" + pages + "'>Neste " + nextPrPage + "</a>";
  if (i > prPage) {
    var prevPage = pages - 2;
    html += "<a href='#page-" + prevPage + "' class='prev' id='page-" + prevPage + "'>Forrige " + prPage + "</a>";
  }
  html += "</div>";
  return html;
}

function generateStartPageAndTableHead(pages) {
  return "<div id='approve-page-"
      + pages
      + "'><table><thead><tr><th>Tittel</th><th>Kilde</th><th id='approve-published'>Publisert</th></tr></thead><tbody>";
}

/* ^ HTML generation functions */

/* ^ JS for handling manually approved resources */