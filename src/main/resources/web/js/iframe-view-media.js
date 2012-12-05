/*  
 *  Iframe resizing for cross domain (view for media files)
 *
 *  Based loosely on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  - Should work as before with regard to previewIframe (served from the view domain)
 *  - Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage
 */
 
var crossDocComLink = new CrossDocComLink();
crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
  switch(cmdParams[0]) {
    case "admin-min-height":
      var setHeight = (cmdParams.length === 2) ? cmdParams[1] : 450;
      var computedHeight = document.body.offsetHeight + 45;
      if(computedHeight > setHeight) {
        setHeight = computedHeight;
        crossDocComLink.postCmdToParent("preview-height|" + setHeight);
      } else { // Computed height is less than or below minimum height
        crossDocComLink.postCmdToParent("preview-keep-min-height");
      }
      break;
    default:
  }
});

// Notify parent when loaded
$(document).ready(function () {
  $(window).load(function (e) {
    crossDocComLink.postCmdToParent("preview-loaded");
    $("a").attr("target", "_top");
  });
});