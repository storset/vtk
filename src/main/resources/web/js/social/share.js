$(document).ready(function() {
  $("a.vrtx-close-toolbox-send-share").click(function(e) {
    $("#vrtx-send-share div:visible").hide();
    e.preventDefault();
  });
  $("a.vrtx-share-link").click(function(e) {
    $("#vrtx-send-share div:hidden").slideDown("fast");
    e.preventDefault();
  });
});