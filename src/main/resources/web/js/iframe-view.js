/*  
 *  Iframe resizing for cross domain (view except media files)
 *
 *  Based loosely on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  - Should work as before with regard to the previewViewIframe (served from the view domain)
 *  - Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage
 */
 
if(window !== top) {
  var crossDocComLink = new CrossDocComLink();
  crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
    switch(cmdParams[0]) {
      case "admin-min-height":
        var setHeight = (cmdParams.length === 2) ? cmdParams[1] : 450;
      
        var previewViewIframe = $("iframe#previewViewIframe");
        var iframe = previewViewIframe[0];
        try {
          if(typeof iframe.contentWindow !== "undefined" && typeof iframe.contentWindow.document !== "undefined" && typeof iframe.contentWindow.document.body !== "undefined") {
            var computedHeight = Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 45;
            if(computedHeight > setHeight) {
              setHeight = computedHeight;
              crossDocComLink.postCmdToParent("preview-height|" + setHeight);
            } else { // Computed height is less than or below minimum height
              crossDocComLink.postCmdToParent("preview-keep-min-height");
            }
          } else {
            crossDocComLink.postCmdToParent("preview-keep-min-height");
          }
          iframe.style.height = (setHeight - ($.browser.msie ? 4 : 0)) + "px";
        } catch(e) { // Error
          if(typeof console !== "undefined" && console.log) {
            console.log("Error finding preview height: " + e.message);
          }
          iframe.style.height = (setHeight - ($.browser.msie ? 4 : 0)) + "px";
          crossDocComLink.postCmdToParent("preview-keep-min-height");
        }
        break;
      default:
    }
  });
}

// Notify parent when loaded
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
      if(window !== top) {
        crossDocComLink.postCmdToParent("preview-loaded");
      } else {
        var previewViewIframeElm = previewViewIframe[0];
        try {
          if(typeof previewViewIframeElm.contentWindow !== "undefined" && typeof previewViewIframeElm.contentWindow.document !== "undefined" && typeof previewViewIframeElm.contentWindow.document.body !== "undefined") {
            previewViewIframeElm.style.height = (Math.ceil(previewViewIframeElm.contentWindow.document.body.offsetHeight) + 45) + "px";
          } else {
            previewViewIframeElm.style.height = ($(window).outerHeight(true) - $("h1").outerHeight(true) - 40) + "px";
          }
        } catch(e) { // Error
          if(typeof console !== "undefined" && console.log) {
            console.log("Error finding preview height: " + e.message);
          }
          previewViewIframeElm.style.height = ($(window).outerHeight(true) - $("h1").outerHeight(true) - 40) + "px";
        }
      }
    });
  } else {
    crossDocComLink.postCmdToParent("preview-keep-min-height");
  }
});