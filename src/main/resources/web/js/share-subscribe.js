/* Share and subscribe box 
 *
 * TODO: this is not optimal, and the animation should be on wrapper..
 *
 */

$(document).ready(function() {
  $("body").on("click", "a#vrtx-share-close-link", function(e) {
    $("#vrtx-share-wrapper div").hide();
    e.preventDefault();
  });
  $("body").on("click", "a#vrtx-subscribe-close-link", function(e) {
    $("#vrtx-subscribe-wrapper div").hide();
    e.preventDefault();
  });
  $("body").on("click", "a#vrtx-share-link", function(e) {
    $("#vrtx-share-wrapper div:hidden").slideDown("fast");
    e.preventDefault();
  });  
  $("body").on("click", "a#vrtx-subscribe-link", function(e) {
    $("#vrtx-subscribe-wrapper div:hidden").slideDown("fast");
    e.preventDefault();
  });
});