$(document).ready(function() {
	
  $("a.vrtx-close-toolbox-send-share").click(function(e) {
    if (!$("#vrtx-send-share div").is(":hidden")) {
      $("#vrtx-send-share div").hide();
    }
    e.preventDefault();
  });

  $("a.vrtx-share-link").click(function(e) {
    if ($("#vrtx-send-share div").is(":hidden")) {
      $("#vrtx-send-share div").slideDown("fast");
    }
    e.preventDefault();
  });
  
});