/*
 *  Vortex Admin enhancements inside iframe (Create dialog)
 *
 */

/*-------------------------------------------------------------------*\
    DOM is ready
    readyState === "complete" || "DOMContentLoaded"-event (++)
\*-------------------------------------------------------------------*/

var hasPostMessage = window['postMessage'] && (!($.browser.opera && $.browser.version < 9.65));
var vrtxAdminOrigin = "*"; // TODO: TEMP Need real origin of adm

$(document).ready(function () {   
  dropdown({selector: "ul.manage-create"});
  
  // Slide up when choose something in dropdown
  $(".dropdown-shortcut-menu li a").click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing", function() {
      notifyCollapsedDropdown(true);    
    });
  });
  $(".dropdown-shortcut-menu-container li a").click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing", function() {
       notifyCollapsedDropdown(true);   
    });  
  });
  $(document).click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing", function() {
      notifyCollapsedDropdown(true);
    });
  });
});


/*-------------------------------------------------------------------*\
    Dropdown (TODO: not dupe the one from admin-enhancements.js)
\*-------------------------------------------------------------------*/

function dropdown(options) {
  var list = $(options.selector);
  if (!list.length) return;

  var numOfListElements = list.find("li").size();

  if (!options.proceedCondition || (options.proceedCondition && options.proceedCondition(numOfListElements))) {
    list.addClass("dropdown-shortcut-menu");
    
    // Move listelements except .first into container
    var listParent = list.parent();
    listParent.append("<div class='dropdown-shortcut-menu-container'><ul>" + list.html() + "</ul></div>");
    
    var startDropdown = options.start != null ? ":nth-child(-n+" + options.start + ")" : ".first";
    var dropdownClickArea = options.start != null ? ":nth-child(3)" : ".first";
    
    list.find("li").not(startDropdown).remove();
    list.find("li" + dropdownClickArea).append("<span id='dropdown-shortcut-menu-click-area'></span>");
 
    var shortcutMenu = listParent.find(".dropdown-shortcut-menu-container");
    shortcutMenu.find("li" + startDropdown).remove();
    shortcutMenu.css("left", (list.width()+5) + "px");
    
    list.find("li" + dropdownClickArea).addClass("dropdown-init");
    
    list.find("li.dropdown-init #dropdown-shortcut-menu-click-area").click(function (e) { 
      var isVisible = shortcutMenu.is(":visible"); 
      notifyExpandedDropdown(isVisible);
      shortcutMenu.slideToggle(100, "swing", function() {
        notifyCollapsedDropdown(isVisible);
      });
      e.stopPropagation();
      e.preventDefault();
    });

    list.find("li.dropdown-init #dropdown-shortcut-menu-click-area").hover(function () {
      var $this = $(this);
      $this.parent().toggleClass('unhover');
      $this.prev().toggleClass('hover');
    }, function () {
      var $this = $(this);
      $this.parent().toggleClass('unhover');
      $this.prev().toggleClass('hover');
    });
  }
}

function notifyExpandedDropdown(isVisible) {
  if(parent && !isVisible) {
    if(hasPostMessage) {
      parent.postMessage("expandedsize", vrtxAdminOrigin);
    } else { // use the hash stuff in plugin from jQuery "Cowboy"
      var parentUrl = decodeURIComponent(document.location.hash.replace(/^#/, ''));
      $.postMessage({expandedsize: true}, parentUrl, parent);
    }
  }
}

function notifyCollapsedDropdown(isVisible) {
  if(parent && isVisible) {
    if(hasPostMessage) {
      parent.postMessage("collapsedsize", vrtxAdminOrigin);
    } else { // use the hash stuff in plugin from jQuery "Cowboy"
      var parentUrl = decodeURIComponent(document.location.hash.replace(/^#/, ''));
      $.postMessage({collapsedsize: true}, parentUrl, parent);
    }
  }
}

/* ^ Vortex Admin enhancements inside iframe (Create dialog) */