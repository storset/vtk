/* Share and subscribe box */

$(document).ready(function() {
  $("body").on("click", "a.vrtx-share-subscribe-close-link", function(e) {
    $("#vrtx-share-subscribe-wrapper:visible").hide();
    e.preventDefault();
  });
  $("body").on("click", "a#vrtx-share-subscibe-link", function(e) {
    $("#vrtx-share-subscribe-wrapper:hidden").slideDown("fast");
    e.preventDefault();
  });
});