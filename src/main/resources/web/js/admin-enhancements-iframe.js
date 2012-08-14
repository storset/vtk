/*
 *  Vortex Admin enhancements inside iframe (Create dialog)
 *
 */

/*-------------------------------------------------------------------*\
    DOM is ready
    readyState === "complete" || "DOMContentLoaded"-event (++)
\*-------------------------------------------------------------------*/

var sslComLink;

$(document).ready(function () {   
  dropdown({selector: "ul.manage-create"});

  // Slide up when choose something in dropdown
  $(".dropdown-shortcut-menu li a").click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing");
  });
  $(".dropdown-shortcut-menu-container li a").click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing");
  });
  
  sslComLink = new SSLComLink();
  sslComLink.setUpReceiveDataHandler({
    cmd: function(c, that, source) {
      switch(c) {
        case "collapsedsize":
          $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing", function() {
            that.postCmd("collapsedsize", source);
          });
          break;
        default:
      }
    },
    cmdNums: function(c, n, that, source) {
      switch(c) {
        case "move-dropdown":
          try {
            var createDropdownOriginalTop = n.top;  
            var createDropdownOriginalLeft = n.left;
            $("ul.manage-create").css({
              "position": "absolute", 
              "top": createDropdownOriginalTop + "px",
              "left": createDropdownOriginalLeft + "px"
            });
          } catch(e){
            if(typeof console !== "undefined" && console.log) {
              console.log("Error parsing original position for create-iframe: " + e.message);
            }
          }
          break;
        default:
      }
    }
  });
  
  $(document).click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing", function() {
      sslComLink.postCmdToParent("collapsedsize");
    });
  });
  
  $(".thickbox").click(function() { 
    sslComLink.postCmdToParent("fullsize");
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
      if(!isVisible) {
        sslComLink.postCmdToParent("expandedsize");
      }
      shortcutMenu.slideToggle(100, "swing", function() {
        if(isVisible) {
          sslComLink.postCmdToParent("collapsedsize");
        }
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

/* ^ Vortex Admin enhancements inside iframe (Create dialog) */