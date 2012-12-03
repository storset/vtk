/*  
 *  Iframe resizing for cross domain (view for media files)
 *
 *  Based loosely on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  - Receives minimum height from top (available v.space in window)
 *  - Pass iframe height to parent with postMessage if loaded and if larger than minimum height (otherwise unchanged command)
 *  - Timeouts after ca. 6s and sends unchanged command if iframe is not loaded yet (checking it before/independent of communication)
 *  - Should work as before with regard to previewIframe (served from the view domain)
 *  - Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage
 */
 
var MIN_HEIGHT = 0;
var MIN_HEIGHT_FALLBACK = 450;
var CAN_LOG = (typeof console !== "undefined" && console.log);

var crossDocComLink = new CrossDocComLink();
crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
  switch(cmdParams[0]) {
    case "min-height":
      MIN_HEIGHT = (cmdParams.length === 2) ? cmdParams[1] : MIN_HEIGHT_FALLBACK;
      logMe("MIN HEIGHT RECEIVED: " + MIN_HEIGHT);
      break;
    default:
  }
});
 
$(document).ready(function () {
  $(window).load(function (e) {
    logMe("IFRAME LOADED");
    
    $("a").attr("target", "_top");
    
    var setHeight = MIN_HEIGHT_FALLBACK;
    var runTimes = 0;
    var waitForIframeLoaded = setTimeout(function() {
      runTimes++;
    
      if(MIN_HEIGHT) {
        setHeight = MIN_HEIGHT;
        var computedHeight = document.body.offsetHeight;
        if(computedHeight > setHeight) {
          setHeight = computedHeight;
          logMe("TRY TO SEND PREVIEW HEIGHT: " + setHeight);
          crossDocComLink.postCmdToParent("preview-height|" + (setHeight + 45));
        } else { // Computed height is less than or below minimum height
          logMe("TRY TO SEND KEEP MIN HEIGHT CMD: " + setHeight);
          crossDocComLink.postCmdToParent("keep-min-height");
        }
        document.body.style.height = setHeight + "px"; 
      } else {
        if(runTimes <= 100) {
          setTimeout(arguments.callee, 15);
        } else {  // Timeout after ca. 1.5s (http://ejohn.org/blog/accuracy-of-javascript-time/)
          document.body.style.height = setHeight + "px";
          logMe("TIMED OUT - TRY TO SEND KEEP MIN HEIGHT CMD: " + setHeight);
          crossDocComLink.postCmdToParent("keep-min-height");
        }
      }
    }, 15);
  });
});

function logMe(msg) {
  if(CAN_LOG) {
    console.log(msg);
  }
}