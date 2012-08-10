/* Share and subscribe box */

$(document).ready(function() {
  $("body").on("click", "a.vrtx-close-toolbox-send-share, a#vrtx-close-subscribe", function(e) {
    $("#vrtx-send-share:visible, #vrtx-subscribe-wrapper:visible").hide();
    e.preventDefault();
  });
  $("body").on("click", "a.vrtx-share-link, a#vrtx-subscribe-link", function(e) {
    $("#vrtx-send-share:hidden, #vrtx-subscribe-wrapper:hidden").slideDown("fast");
    e.preventDefault();
  });
});