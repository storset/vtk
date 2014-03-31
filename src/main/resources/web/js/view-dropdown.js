/*
 * View dropdown
 *
 * FIXME: Turn off() first to prevent bug when JS is already included
 * TODO: use toggle.js
 *
 */
 
$(document).ready(function() {
  var ariaExpanded = function (elem, isExpanded) {
    elem.attr("aria-expanded", isExpanded ? "true" : "false");
    elem.attr("aria-hidden", isExpanded ? "false" : "true");
  };

  var wrappers = $(".vrtx-dropdown-wrapper"), i = wrappers.length;
  while(i--) {
    var wrp = $(wrappers[i]);
    var a = wrp.prev();
    var id = "vrtx-dropdown-wrapper-" + Math.round(+new Date() * Math.random());
    a.attr("aria-haspopup", "true");
    a.attr("aria-controls", id);
    wrp.attr("id", id);
    ariaExpanded(wrp, false);
  }

  $(document).off("click", ".vrtx-dropdown-component-toggled a.vrtx-dropdown-link")
              .on("click", ".vrtx-dropdown-component-toggled a.vrtx-dropdown-link", function(e) {
              
    var link = $(this);
    var wrapper = link.next(".vrtx-dropdown-wrapper");
    link.toggleClass("active");
    if(typeof document.documentMode !== "undefined" && document.documentMode <= 7) {
      wrapper.toggle(); // Because slide sets "overflow: hidden" causing IE7 css-bug
      ariaExpanded(wrp, wrapper.is(":visible"));
    } else {
      wrapper.slideToggle("fast", function() {
        ariaExpanded(wrapper, wrapper.is(":visible"));
      });
    }
    e.stopPropagation();
    e.preventDefault();
  });
  $(document).off("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-link")
              .on("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-link", function(e) {
    var link = $(this);
    var wrapper = link.next(".vrtx-dropdown-wrapper");
    wrapper.slideDown("fast", function() {
      ariaExpanded(wrapper, true);
    });
    e.stopPropagation();
    e.preventDefault();
  });
  $(document).off("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-close-link")
              .on("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-close-link", function(e) {
              
    $(this).closest(".vrtx-dropdown-wrapper").slideUp("fast", function() {
      ariaExpanded(wrapper, false);
    });
    e.stopPropagation();
    e.preventDefault();
  });
});