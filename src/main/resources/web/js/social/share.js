$(document).ready(function() {
  $("body").on("click", "a.vrtx-close-toolbox-send-share", function(e) {
    $("#vrtx-send-share div:visible").hide();
    e.preventDefault();
  });
  $("body").on("click", "a.vrtx-share-link", function(e) {
    $("#vrtx-send-share div:hidden").slideDown("fast");
    e.preventDefault();
  });
});