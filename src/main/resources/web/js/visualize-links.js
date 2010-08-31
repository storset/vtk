// AJAX request against link-check action
// that returns links seperated by new line
// Return array of dead links
function visualizeDeadLinks(resourceUrl) {

  var deadLinks = [];
  var LINK_CHECK_URL = resourceUrl + "?vrtx=link-check";

  $.ajax( {
    type : "GET",
    url : LINK_CHECK_URL,
    dataType : "text",
    complete : function(msg) {

      deadLinks = msg.responseText.split("\n");

      $("iframe").contents().find("body").filter( function() {
        return this.id.match(/^(?!vrtx-[\S]+-listing|vrtx-collection)[\S]+/);
      }).find("#main").not("#left-main").find("a").each( function(i, e) {
        visualizeDeadLink(this, deadLinks);
      });

      if (msg.status != 200) {
        $("iframe").contents().find("body").append(msg.status + " " + msg.statusText);
      }

    },
    error : function(xhr, ajaxOptions, thrownError) {
      $("iframe").contents().find("body").append(" " + thrownError + " ");
    }
  });
}

// Check if current link is in array of dead links
function visualizeDeadLink(that, deadLinks) {

  var TARGET_URL = $(that).attr("href");

  var deadLinksLength = deadLinks.length;
  for ( var i = 0; i < deadLinksLength; i++) {
    if (TARGET_URL == deadLinks[i]) {
      $(that).append(" - 404").css("color", "red");
    }
  }

}