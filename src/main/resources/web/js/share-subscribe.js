/* Share and subscribe box */

$(document).ready(function() {
  $("body").on("click", "a#vrtx-share-close-link, a#vrtx-subscribe-close-link", function(e) {
    $("#vrtx-share-wrapper:visible, #vrtx-subscribe-wrapper:visible").hide();
    e.preventDefault();
  });
  $("body").on("click", "a#vrtx-share-link", function(e) {
    $("#vrtx-share-wrapper:hidden, #vrtx-subscribe-wrapper:hidden").slideDown("fast");
    e.preventDefault();
  });
});