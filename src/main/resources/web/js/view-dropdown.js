/* 
 * View dropdown
 *
 * Turn off() first to prevent bug when JS is already included
 *
 */
 
$(document).ready(function() {
  $(document).off("click", ".vrtx-dropdown-component-toggled a.vrtx-dropdown-link")
              .on("click", ".vrtx-dropdown-component-toggled a.vrtx-dropdown-link", function(e) {
    if($.browser.msie && $.browser.version <= 7) {
      $(this).toggleClass("active").next(".vrtx-dropdown-wrapper").toggle(); // Because slide sets "overflow: hidden" causing IE7-bug
    } else {
      $(this).toggleClass("active").next(".vrtx-dropdown-wrapper").slideToggle("fast");
    }
    e.stopPropagation();
    e.preventDefault();
  });
  $(document).off("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-link")
              .on("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-link", function(e) {
    $(this).next(".vrtx-dropdown-wrapper").slideDown("fast");
    e.stopPropagation();
    e.preventDefault();
  });
  $(document).off("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-close-link")
              .on("click", ".vrtx-dropdown-component-not-toggled a.vrtx-dropdown-close-link", function(e) {
    $(this).closest(".vrtx-dropdown-wrapper").slideUp("fast");
    e.stopPropagation();
    e.preventDefault();
  });
});