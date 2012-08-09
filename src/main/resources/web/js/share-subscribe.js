/* Share and subscribe box */

$(document).ready(function() {
  $("body").on("click", "a.vrtx-close-toolbox-send-share, a#vrtx-close-subscribe", function(e) {
    $("#vrtx-send-share div:visible, #vrtx-subscribe-wrapper div:visible").hide();
    e.preventDefault();
  });
  $("body").on("click", "a.vrtx-share-link, a#vrtx-subscribe-link", function(e) {
    $("#vrtx-send-share div:hidden, #vrtx-subscribe-wrapper div:hidden").slideDown("fast");
    e.preventDefault();
  });
});