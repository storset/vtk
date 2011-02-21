/**
 * JS for handling manually approved resources
 */

function toggleManuallyApprovedContainer(resources, manuallyApproved) {
  // Toggle visibility of container for list of resources to manually approve.
  // "resources" is complete list (array) of resources. If empty, hide
  // container, else display.
  // "manuallyApproved" contains list of resources already manually
  // approved. Use to mark resources.

  // TODO: i18n ++

  var pages = 1, prPage = 10, len = resources.length;
  var total = len > prPage ? (parseInt(len / prPage) + 1) : 1;

  var html = "<div id='approve-page-" + pages + "'><h3>Manually approve resources - page " + pages + "/" + total + "</h3><ul>";

  for(var i = 0; i < len; i++) {
    if(resources[i].approved) {
      html += "<li><input type='checkbox' checked='checked' />";
    } else {
      html += "<li><input type='checkbox' />";
    }
    html += "<a href='" + resources[i].uri + "'>" + resources[i].title + "</a></li>";
    if((i+1) % prPage == 0) {
      pages++;
      html += "</ul>";
      if(i < len-1) {
        if(i > prPage) {
          html += "<a href='#page-" + (pages - 1) + "' class='prev' id='page-" + (pages - 1) + "'>Prev</a>";
        }
        html += "<a href='#page-" + pages + "' class='next' id='page-" + pages + "'>Next</a>"
              + "</div><div id='approve-page-" + pages + "'>"
              + "<h3>Manually approve resources - page " + pages + "/" + total + "</h3>";
      }
      html += "<ul>";
    }
  }
  html += "</ul><a href='#page-" + (pages - 1) + "' class='prev' id='page-" + (pages - 1) + "'>Prev</a></div>"
        + "<div id='manually-approve-save-cancel'><input type='submit' id='manually-approve-save' value='Save' />"
        + "<a href='#' id='manually-approve-cancel'>Cancel</a></div>";

  $("#manually-approve-container").html(html).slideDown("fast");
  $("#manually-approve-container div").not("#approve-page-1").not("#manually-approve-save-cancel").hide();

}

function retrieveResources(folders, resourceType) {
  // Retrieve and return array of resources for folders to manually approve from.
  // Needs Vortex-service.

  // Dummy JSON
  return = [
        {
          "title": "artikkel.html",
          "uri": "/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        }
    ];
}

$(document).ready(function() {

    // TODO: get folders from textfield
    var resources = retrieveResources("/om/", "resource");

    // TODO: registrate click() for a button instead of focus() on textfield
    $("#resource\\.manually-approve-from").focus(function() {
      toggleManuallyApprovedContainer(resources, "");
    });

    // Action - Save
    $("#manually-approve-container").delegate("#manually-approve-save", "click", function() {
    	// TODO: implement save..

    	$(this).parent().parent().slideUp("fast");
        return false;
    });

    // Action - Cancel
    $("#manually-approve-container").delegate("#manually-approve-cancel", "click", function() {
    	$(this).parent().parent().slideUp("fast");
        return false;
    });

    // Paging - Next
    $("#manually-approve-container").delegate(".next", "click", function() {
      var that = $(this).parent();
      var next = that.next();
      if(next) {
         $(that).hide();
         $(next).show();
      }
      return false;
    });

    // Paging - Previous
    $("#manually-approve-container").delegate(".prev", "click", function() {
      var that = $(this).parent();
      var prev = that.prev();
      if(prev) {
         $(that).hide();
         $(prev).show();
      }
      return false;
    });
 });