/*  Based on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  Changed to only process specific frame and pass height to parent with postMessage.
 *  Should work as before with regard to the previewViewIframe (served from the view domain). 
 *  Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage.
 */
 
var crossDocComLink = new CrossDocComLink();
crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
  switch(cmdParams[0]) {
    case "min-height":
      var minHeight = (cmdParams.length === 2) ? cmdParams[1] : 350;
      resize($("iframe#previewViewIframe")[0], minHeight); 
      break;
    default:
  }
});
      
var IFRAME_LOADED = false;
$(document).ready(function () {
  var previewViewIframe = $("iframe#previewViewIframe");
  if (previewViewIframe.length) {
    if ($.browser.msie) {
      // Iframe load event not firing in IE8 / IE9 when page with iframe is inside another iframe
      // Setting the iframe src seems to fix the problem
      var previewViewIframeElm = previewViewIframe[0];
      var iSource = previewViewIframeElm.src;
      previewViewIframeElm.src = '';
      previewViewIframeElm.src = iSource;
    } 
    previewViewIframe.load(function() {
      IFRAME_LOADED = true; 
    });
  }
});

function resize(iframe, minHeight) {
  var setHeight = minHeight;
  try {
    var runTimes = 0;
    var waitForDocument = setTimeout(function() {
      if(typeof iframe.contentWindow !== "undefined" && typeof iframe.contentWindow.document !== "undefined" && IFRAME_LOADED) {
        var computedHeight = Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 45; 
        if (computedHeight > setHeight) {
          setHeight = computedHeight;
        }
        iframe.style.height = setHeight + "px";
        crossDocComLink.postCmdToParent("preview-height|" + setHeight);
      } else {
        runTimes++;
        if(runTimes < 400) { // Wait max 6s
          setTimeout(arguments.callee, 15);
        } else { // Otherwise just post back min height
          iframe.style.height = setHeight + "px";
          crossDocComLink.postCmdToParent("preview-height|" + setHeight);
        }
      }
    }, 15);
  } catch(e) {
    if(typeof console !== "undefined" && console.log) {
      console.log("Error in getting iframe height or posting to parent: " + e.message);
    }
  }
}