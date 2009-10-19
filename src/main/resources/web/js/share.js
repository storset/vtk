function share() {
  if ($("#vrtx-send-share div").is(":hidden")) {
    $("#vrtx-send-share div").slideDown("fast");
  } else {
    $("#vrtx-send-share div").hide();
  }
}