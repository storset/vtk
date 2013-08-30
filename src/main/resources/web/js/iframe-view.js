/*
 *  Iframe resizing for cross domain (view except media files)
 *
 *  Based loosely on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  - Should work as before with regard to the previewViewIframe (served from the view domain)
 *  - Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage
 */
if (window != top) { // Obs IE bug: http://stackoverflow.com/questions/4850978/ie-bug-window-top-false
  var crossDocComLink = new CrossDocComLink();
  
  // Mobile preview
  var originalHeight = 0;
  var originalZoom = 0;
  var supportedProp = (function () {
    var propArray = ['transform', 'MozTransform', 'WebkitTransform', 'msTransform', 'OTransform'];
    var root = document.documentElement;
    for (var i = 0, len = propArray.length; i < len; i++) {
      if (propArray[i] in root.style){
        return propArray[i];
      }
    }
  })();

  crossDocComLink.setUpReceiveDataHandler(function (cmdParams, source) {
    switch (cmdParams[0]) {
      case "admin-min-height":
        var setHeight = (cmdParams.length === 2) ? cmdParams[1] : 450;

        var previewViewIframe = $("iframe#previewViewIframe");
        var iframe = previewViewIframe[0];
        try {
          if (typeof iframe.contentWindow !== "undefined" && typeof iframe.contentWindow.document !== "undefined" && typeof iframe.contentWindow.document.body !== "undefined") {
            var computedHeight = Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 45;
            if (computedHeight > setHeight) {
              setHeight = computedHeight;
              crossDocComLink.postCmdToParent("preview-height|" + setHeight);
            } else { // Computed height is less than or below minimum height
              crossDocComLink.postCmdToParent("preview-keep-min-height");
            }
          } else {
            crossDocComLink.postCmdToParent("preview-keep-min-height");
          }
          setHeight = (setHeight - ($.browser.msie ? 4 : 0));
          originalHeight = setHeight;
          iframe.style.height = setHeight + "px";
        } catch (e) { // Error
          if (typeof console !== "undefined" && console.log) {
            console.log("Error finding preview height: " + e.message);
          }
          setHeight = (setHeight - ($.browser.msie ? 4 : 0));
          originalHeight = setHeight;
          iframe.style.height = setHeight + "px";
          crossDocComLink.postCmdToParent("preview-keep-min-height");
        }
        break;
        
      /* Mobile preview */
        
      case "update-height-vertical":
        var previewViewIframeWrp = $("#previewViewIframeWrapper");
        var previewViewIframe = $("iframe#previewViewIframe");
        
        // Restore zoom
        previewViewIframeWrp.css(supportedProp, "");
        originalZoom = 0;
        
        var viewportMetaTag = previewViewIframe.contents().find("meta[name='viewport']");
        if(viewportMetaTag.attr("content").indexOf("width=device-width") === -1) {
          previewViewIframeWrp.addClass("mobile-none-responsive");
          previewViewIframeWrp.removeClass("mobile-none-responsive-horizontal");
        } else {
          try {
            var iframe = previewViewIframe[0];
            if (typeof iframe.contentWindow !== "undefined" && typeof iframe.contentWindow.document !== "undefined" && typeof iframe.contentWindow.document.body !== "undefined") {
              var computedHeight = Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 45;
              iframe.style.height = computedHeight + "px";
            }
          } catch (e) {}
        }
        break;
      case "update-height-horizontal":
        var previewViewIframeWrp = $("#previewViewIframeWrapper");
        var previewViewIframe = $("iframe#previewViewIframe");
        
        // Restore zoom
        previewViewIframeWrp.css(supportedProp, "");
        originalZoom = 0;
        
        var viewportMetaTag = previewViewIframe.contents().find("meta[name='viewport']");
        if(viewportMetaTag.attr("content").indexOf("width=device-width") === -1) {
          previewViewIframeWrp.addClass("mobile-none-responsive-horizontal");
          previewViewIframeWrp.removeClass("mobile-none-responsive");
        } else {
          try {
            var iframe = previewViewIframe[0];
            if (typeof iframe.contentWindow !== "undefined" && typeof iframe.contentWindow.document !== "undefined" && typeof iframe.contentWindow.document.body !== "undefined") {
              var computedHeight = Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 45;
              iframe.style.height = computedHeight + "px";
            }
          } catch (e) {}
        }
        break;
      case "restore-height":
        var previewViewIframeWrp = $("#previewViewIframeWrapper");
        var previewViewIframe = $("iframe#previewViewIframe");
        previewViewIframeWrp.removeClass("mobile-none-responsive");
        previewViewIframeWrp.removeClass("mobile-none-responsive-horizontal");
        // Restore zoom
        previewViewIframe.css(supportedProp, "");
        originalZoom = 0;
        var iframe = previewViewIframe[0];
        iframe.style.height = originalHeight + "px";
        break;
        
      /* BETA functionality for mobile preview */
        
      case "zoom-in":
        var previewViewIframeWrp = $("#previewViewIframeWrapper");
        var zoom = parseFloat(previewViewIframeWrp.css(supportedProp).match(/[0-9]*[.][0-9]+/)[0], 10);
        if(originalZoom === 0) originalZoom = zoom;
        zoom = zoom + 0.05;
        previewViewIframeWrp.css(supportedProp, "scale(" + zoom + ")");
        break;
      case "zoom-out":
        var previewViewIframeWrp = $("#previewViewIframeWrapper");
        var zoom = parseFloat(previewViewIframeWrp.css(supportedProp).match(/[0-9]*[.][0-9]+/)[0], 10);
        if(originalZoom === 0) originalZoom = zoom;
        zoom = zoom - 0.05;
        if(zoom >= originalZoom) {
          previewViewIframeWrp.css(supportedProp, "scale(" + zoom + ")");
        }
        break;
      case "restore-zoom":
        var previewViewIframeWrp = $("#previewViewIframeWrapper");
        previewViewIframeWrp.css(supportedProp, "");
        originalZoom = 0;
        break;
              
      /* Print preview */
        
      case "print":
        var previewViewIframe = $("iframe#previewViewIframe");
        var iframe = previewViewIframe[0];
        var ifWin = iframe.contentWindow || iframe;
        iframe.focus();
        ifWin.print(); 
        break;    
        
      default:
    }
  });
}

(function () {
  var waitMaxForPreviewLoaded = 10000, // 10s
    waitMaxForPreviewLoadedTimer,
    sentPreviewLoaded = false,
    previewViewIframe;
  
  $(document).ready(function () {
    previewViewIframe = $("iframe#previewViewIframe");
    if (previewViewIframe.length) {
      waitMaxForPreviewLoadedTimer = setTimeout(function () {
        sendPreviewLoaded();
      }, waitMaxForPreviewLoaded);
      if ($.browser.msie) {
        // Iframe load event not firing in IE8 / IE9 when page with iframe is inside another iframe
        // Setting the iframe src seems to fix the problem
        var previewViewIframeElm = previewViewIframe[0];

        // TODO: make sure .load() does not fire twice (need to test more presicely which IEs where load does not fire at init)
        var iSource = previewViewIframeElm.src;
        previewViewIframeElm.src = '';
        previewViewIframeElm.src = iSource;
      }
      previewViewIframe.load(function () {
        sendPreviewLoaded();
      });
    } else {
      crossDocComLink.postCmdToParent("preview-keep-min-height");
    }
  });

  function sendPreviewLoaded() {
    if (!sentPreviewLoaded) {
      if (window != top) { // Obs IE bug: http://stackoverflow.com/questions/4850978/ie-bug-window-top-false
        crossDocComLink.postCmdToParent("preview-loaded");
      } else {
        var previewViewIframeElm = previewViewIframe[0];
        var winHeight = 0;
        try {
          if (typeof previewViewIframeElm.contentWindow !== "undefined" && typeof previewViewIframeElm.contentWindow.document !== "undefined" && typeof previewViewIframeElm.contentWindow.document.body !== "undefined") {
            var previewHeight = (Math.ceil(previewViewIframeElm.contentWindow.document.body.offsetHeight) + 45);
            previewViewIframeElm.style.height = previewHeight + "px";
          } else {
            winHeight = ($(window).outerHeight(true) - $("h1").outerHeight(true) - 40);
            previewViewIframeElm.style.height = winHeight + "px";
          }
        } catch (e) { // Error
          if (typeof console !== "undefined" && console.log) {
            console.log("Error finding preview height: " + e.message);
          }
          winHeight = ($(window).outerHeight(true) - $("h1").outerHeight(true) - 40);
          previewViewIframeElm.style.height = winHeight + "px";
        }
      }
      sentPreviewLoaded = true;
    }
  }
})();
