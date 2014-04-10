/*
 * View dropdown
 *
 */

if(typeof viewDropdown === "undefined") {

var viewDropdown = true;
var doc = $(document);
doc.ready(function() {

  var ariaDropdownState = function (link, menu, isExpanded) {
    if(isExpanded) {
      var firstInteractiveElem = menu.find("a, input[type='button'], input[type='submit']").filter(":first");
      if(firstInteractiveElem.length) firstInteractiveElem.focus();
    }
    menu.attr("aria-expanded", isExpanded ? "true" : "false");
    menu.attr("aria-hidden", isExpanded ? "false" : "true");
  };

  var wrappers = $(".vrtx-dropdown-wrapper"), i = wrappers.length;
  while(i--) {
    var wrp = $(wrappers[i]);
    var a = wrp.prev();

    if(wrp[0].id) {
      var idWrp = wrp[0].id; 
    } else {
      var idWrp = "vrtx-dropdown-wrapper-" + Math.round(+new Date() * Math.random());
      wrp.attr("id", idWrp);
    }
    if(a[0].id) {
      var idLink = a[0].id;
    } else {
      var idLink = "vrtx-dropdown-link-" + Math.round((+new Date() + 1) * Math.random());
      a.attr("id", idLink);
    }

    a.attr("aria-haspopup", "true");
    a.attr("aria-controls", idWrp);
    wrp.attr("aria-labelledby", idLink);
    ariaDropdownState(a, wrp, false);
  }

  doc.on("click", ".vrtx-dropdown-component-toggled a.vrtx-dropdown-link", function(e) {
    var a = $(this);
    var wrp = a.next(".vrtx-dropdown-wrapper");
    a.toggleClass("active");
    if(typeof document.documentMode !== "undefined" && document.documentMode <= 7) {
      wrp.toggle(); // Because slide sets "overflow: hidden" causing IE7 css-bug
      ariaDropdownState(a, wrp, wrp.is(":visible"));
    } else {
      wrp.slideToggle("fast", function() {
        ariaDropdownState(a, wrp, wrp.is(":visible"));
      });
    }
    e.stopPropagation();
    e.preventDefault();
  });
  doc.on("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-link", function(e) {
    var a = $(this);
    var wrp = a.next(".vrtx-dropdown-wrapper");
    wrp.slideDown("fast", function() {
      ariaDropdownState(a, wrp, true);
    });
    e.stopPropagation();
    e.preventDefault();
  });
  doc.on("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-close-link", function(e) {
    var a = $(this);
    var wrp = a.closest(".vrtx-dropdown-wrapper");      
    wrp.slideUp("fast", function() {
      ariaDropdownState(a, wrp, false);
    });
    e.stopPropagation();
    e.preventDefault();
  });
});

}