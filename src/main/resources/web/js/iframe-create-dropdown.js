/*
 *  Create iframe
 *
 */
 

var sslComLink = new SSLComLink();
sslComLink.setUpReceiveDataHandler({
  cmd: function(c, source) {
    switch(c) {
      case "create-dropdown-collapsed":
        $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing", function() {
          sslComLink.postCmd("create-dropdown-collapsed", source);
        });
        break;
      default:
    }
  },
  cmdNums: function(c, n, source) {
    switch(c) {
      case "create-dropdown-move-dropdown":
        try {
          $("ul.manage-create").css({
            "position": "absolute", 
            "top": n.top + "px",
            "left": n.left + "px"
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

$(document).ready(function () {   
  dropdown({selector: "ul.manage-create"});

  // Slide up when choose something in dropdown
  $(".dropdown-shortcut-menu li a").click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing");
  });
  $(".dropdown-shortcut-menu-container li a").click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing");
  });

  $(document).click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing", function() {
      sslComLink.postCmdToParent("create-dropdown-collapsed");
    });
  });
  
  $("a.thickbox").click(function() { 
    sslComLink.postCmdToParent("create-dropdown-full-size");
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
        sslComLink.postCmdToParent("create-dropdown-expanded");
      }
      shortcutMenu.slideToggle(100, "swing", function() {
        if(isVisible) {
          sslComLink.postCmdToParent("create-dropdown-collapsed");
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

/* ^ Create iframe */