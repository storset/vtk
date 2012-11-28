/*  Iframe resizing for cross domain (view for media files)
 *
 *  Based loosely on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  - Pass iframe height to parent with postMessage if larger than minimum height (otherwise unchanged command)
 *  - Receives minimum height from top (available v.space in window)
 *  - Timeouts after ca. 6s and sends unchanged command if iframe is not loaded yet (detects it before/independent of communication)
 *  - Should work as before with regard to the previewViewIframe (served from the view domain)
 *  - Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage
 */
 
var crossDocComLink = new CrossDocComLink();
crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
  switch(cmdParams[0]) {
    case "min-height":
      var minHeight = (cmdParams.length === 2) ? cmdParams[1] : 0;
      resize(minHeight); 
      break;
    default:
  }
});
 
var IFRAME_LOADED = false;
$(document).ready(function () {
  $(window).load(function (e) {
    IFRAME_LOADED = true;                
    for (var i = 0, links = $("a"), len = links.length; i < len; i++) {
      $(links[i]).attr("target", "_top");
    }
  });
});

function resize(minHeight) {
  var setHeight = minHeight;
  var runTimes = 0;
  var waitForIframeLoaded = setTimeout(function() {
    if(IFRAME_LOADED) {
      var computedHeight = document.body.offsetHeight;
      if(computedHeight > setHeight) {
        setHeight = computedHeight;
        crossDocComLink.postCmdToParent("preview-height|" + (setHeight + 45));
      } else { // Computed height is less than or below minimum height
        crossDocComLink.postCmdToParent("keep-min-height");
      }
      document.body.style.height = setHeight + "px"; 
    } else {
      runTimes++;
      if(runTimes < 400) {
        setTimeout(arguments.callee, 15);
      } else {  // Timeout after ca. 6s (http://ejohn.org/blog/accuracy-of-javascript-time/)
        document.body.style.height = setHeight + "px";
        crossDocComLink.postCmdToParent("keep-min-height");
      }
    }
  }, 15); 
}