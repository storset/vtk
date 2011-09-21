/*
 *  Vortex Admin enhancements inside iframe (Create dialog)
 *
 */

/*-------------------------------------------------------------------*\
    DOM is ready
    readyState === "complete" || "DOMContentLoaded"-event (++)
\*-------------------------------------------------------------------*/

$(document).ready(function () {   
  dropdown({selector: "ul.manage-create"});
  
  // Slide up when choose something in dropdown
  $(".dropdown-shortcut-menu li a").click(function() {
    $(".dropdown-shortcut-menu-container").slideUp(100, "swing");
  });
  $(".dropdown-shortcut-menu-container li a").click(function() {
    $(".dropdown-shortcut-menu-container").slideUp(100, "swing");    
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
    list.parent().append("<div class='dropdown-shortcut-menu-container'><ul>" + list.html() + "</ul></div>");
    list.find("li").not(".first").remove();
    list.find("li.first").append("<span id='dropdown-shortcut-menu-click-area'></span>");

    var shortcutMenu = list.siblings(".dropdown-shortcut-menu-container");
    shortcutMenu.find("li.first").remove();
    shortcutMenu.css("left", (list.width() - 24) + "px");

    list.find("li.first #dropdown-shortcut-menu-click-area").click(function (e) {
      shortcutMenu.slideToggle(100, "swing");
      e.stopPropagation();
      e.preventDefault();
    });

    list.find("li.first #dropdown-shortcut-menu-click-area").hover(function () {
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

/* ^ Vortex Admin enhancements inside iframe (Create dialog) */