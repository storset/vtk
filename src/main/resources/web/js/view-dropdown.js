/*
 * View dropdown
 *
 */

if(typeof viewDropdown === "undefined") { // Avoid duplicate running code
  var viewDropdown = true;
  (function() {
    var doc = $(document);
    doc.ready(function() {
    
      /* Add missing id with random postfix to avoid duplicates */
      var addMissingId = function(elm, classPrefix) {
        if(elm[0].id) {
          var id = elm[0].id;
        } else {
          var id = classPrefix + Math.round((+new Date() + 1) * Math.random());
          elm.attr("id", id);
        }
        return id;
      };
      
      /* Dropdown ARIA states */
      var ariaDropdownState = function (link, wrp, isExpanded) {
        if(isExpanded) {
          var firstInteractiveElem = wrp.find("a, input[type='button'], input[type='submit']").filter(":first");
          if(firstInteractiveElem.length) firstInteractiveElem.focus();
        }
        wrp.attr({
          "aria-expanded": isExpanded,
          "aria-hidden": !isExpanded
        });
      };
      
      /* Dropdown click events handler */
      var toggledOpenClosable = function(e) {
        var link = $(this);
        if(link.parent().hasClass("vrtx-dropdown-component-toggled")) {
          link.toggleClass("active");
        }
        if(link.hasClass("vrtx-dropdown-close-link")) {
          var wrp = link.closest(".vrtx-dropdown-wrapper");
        } else {
          var wrp = link.next(".vrtx-dropdown-wrapper");
        }
        wrp.slideToggle("fast", function() {
          ariaDropdownState(link, wrp, wrp.is(":visible"));
        });
        e.stopPropagation();
        e.preventDefault();
      };
    
      /* Initialize dropdowns */
      var wrappers = $(".vrtx-dropdown-wrapper"),
          i = wrappers.length,
          addMissingIdFunc = addMissingId;
      while(i--) {
        var wrp = $(wrappers[i]);
        var link = wrp.prev();
     
        var idWrp = addMissingIdFunc(wrp, "vrtx-dropdown-wrapper-");
        var idLink = addMissingIdFunc(link, "vrtx-dropdown-link-");
        
        wrp.attr("aria-labelledby", idLink);
        link.attr({
          "aria-controls": idWrp,
          "aria-haspopup": "true"
        });
        
        ariaDropdownState(link, wrp, false); /* Invisible at init */
      }
      
      /* Listen for click events */
      doc.on("click", ".vrtx-dropdown-component a.vrtx-dropdown-link, .vrtx-dropdown-component a.vrtx-dropdown-close-link", toggledOpenClosable);
    });
  })();
}