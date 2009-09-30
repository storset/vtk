function share() {
  if ($("#send-share div").is(":hidden")) {
    $("#send-share div").slideDown("fast");
  } else {
    $("#send-share div").hide();
  }
}