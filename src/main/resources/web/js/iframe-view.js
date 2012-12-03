/*  
 *  Iframe resizing for cross domain (view except media files)
 *
 *  Based loosely on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  - Receives minimum height from top (available v.space in window)
 *  - Pass inner iframe height to parent with postMessage if loaded and if larger than minimum height (otherwise unchanged command)
 *  - Timeouts after ca. 6s and sends unchanged command if inner iframe is not loaded yet (checking it before/independent of communication)
 *  - Also sends unchanged command on error getting height
 *  - Should work as before with regard to the previewViewIframe (served from the view domain)
 *  - Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage
 */
 
var MIN_HEIGHT = 0;
var MIN_HEIGHT_FALLBACK = 550;
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
  var previewViewIframe = $("iframe#previewViewIframe");
  if (previewViewIframe.length) {
    if ($.browser.msie) {
      // Iframe load event not firing in IE8 / IE9 when page with iframe is inside another iframe
      // Setting the iframe src seems to fix the problem
      var previewViewIframeElm = previewViewIframe[0];
      
      // TODO: make sure .load() does not fire twice (need to test more presicely which IEs where load does not fire at init)
      var iSource = previewViewIframeElm.src;
      previewViewIframeElm.src = '';
      previewViewIframeElm.src = iSource;
      
      logMe("INNER IFRAME UPDATED SRC");
    } 
    previewViewIframe.load(function() {
      logMe("INNER IFRAME LOADED");
    
      var setHeight = MIN_HEIGHT_FALLBACK;
      var runTimes = 0;
      var waitForIframeLoaded = setTimeout(function() {
        try {
          runTimes++;
        
          var iframe = previewViewIframe[0];
          if(typeof iframe.contentWindow !== "undefined" && typeof iframe.contentWindow.document !== "undefined" && MIN_HEIGHT) {
            setHeight = MIN_HEIGHT;
            logMe("TIMER STOPPED AFTER " + runTimes + " RUNS");
            var computedHeight = Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 45;
            if(computedHeight > setHeight) {
              setHeight = computedHeight;
              logMe("TRY TO SEND PREVIEW HEIGHT");
              crossDocComLink.postCmdToParent("preview-height|" + setHeight);
            } else { // Computed height is less than or below minimum height
              logMe("TRY TO SEND MIN HEIGHT");
              crossDocComLink.postCmdToParent("keep-min-height");
            }
            iframe.style.height = setHeight + "px";
          } else {
            if(runTimes <= 400) {
              setTimeout(arguments.callee, 15);
            } else {  // Timeout after ca. 6s (http://ejohn.org/blog/accuracy-of-javascript-time/)
              iframe.style.height = setHeight + "px";
              logMe("TIMED OUT - TRY TO SEND KEEP MIN HEIGHT CMD");
              crossDocComLink.postCmdToParent("keep-min-height");
            }
          }
        } catch(e) { // Error
          logMe("ERROR: " + e.message);
          iframe.style.height = setHeight + "px";
          logMe("TRY TO SEND KEEP MIN HEIGHT CMD");
          crossDocComLink.postCmdToParent("keep-min-height");
        }
      }, 15);
    });
  } else {
    logMe("IFRAME 404 - TRY TO SEND KEEP MIN HEIGHT CMD");
    crossDocComLink.postCmdToParent("keep-min-height");
  }
});

function logMe(msg) {
  if(CAN_LOG) {
    console.log(msg);
  }
}