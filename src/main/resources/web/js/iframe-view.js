/*  Iframe resizing for cross domain (view except media files)
 *
 *  Based loosely on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  - Receives minimum height from top (available v.space in window)
 *  - Pass inner iframe height to parent with postMessage if larger than minimum height (otherwise unchanged command)
 *  - Timeouts after ca. 6s and sends unchanged command if inner iframe is not loaded yet (checking it before/independent of communication)
 *  - Also sends unchanged command on error getting height
 *  - Should work as before with regard to the previewViewIframe (served from the view domain)
 *  - Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage
 */
 
var crossDocComLink = new CrossDocComLink();
crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
  switch(cmdParams[0]) {
    case "min-height":
      var minHeight = (cmdParams.length === 2) ? cmdParams[1] : 0;
      resize($("iframe#previewViewIframe")[0], minHeight); 
      break;
    default:
  }
});
      
var INNER_IFRAME_LOADED = false;
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
      
    } 
    previewViewIframe.load(function() {
      INNER_IFRAME_LOADED = true; 
    });
  }
});

function resize(iframe, minHeight) {
  var setHeight = minHeight;
  try {
    var runTimes = 0;
    var waitForIframeLoaded = setTimeout(function() {
      if(typeof iframe.contentWindow !== "undefined" && typeof iframe.contentWindow.document !== "undefined" && INNER_IFRAME_LOADED) {
        var computedHeight = Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 45;
        if(computedHeight > setHeight) {
          setHeight = computedHeight;
          crossDocComLink.postCmdToParent("preview-height|" + setHeight);
        } else { // Computed height is less than or below minimum height
          crossDocComLink.postCmdToParent("keep-min-height");
        }
        iframe.style.height = setHeight + "px";
      } else {
        runTimes++;
        if(runTimes < 400) {
          setTimeout(arguments.callee, 15);
        } else {  // Timeout after ca. 6s (http://ejohn.org/blog/accuracy-of-javascript-time/)
          iframe.style.height = setHeight + "px";
          crossDocComLink.postCmdToParent("keep-min-height");
        }
      }
    }, 15); 
  } catch(e) { // Error
    if(typeof console !== "undefined" && console.log) {
      console.log("Error in getting iframe height: " + e.message); // implied that we always can post to parent as parent initiate with a post to iframe
    }
    iframe.style.height = setHeight + "px";
    crossDocComLink.postCmdToParent("keep-min-height");
  }
}