/*  
 *  Iframe resizing for cross domain (admin)
 *
 *  Not essential functionality. Only works in browsers which support postMessage (>IE7).
 *
 *  - Sends available window v.space to iframe (minimum height)
 *  - Receives computed height from inner iframes or unchanged command
 *  - Shows loading overlay while rendering/loading
 *  - Animates changed height visible in window
 *
 */
 
(function ($) {

  var isPreviewMode,
      body,
      contents,
      appFooterHeight,
      previewIframeMinHeight,
      previewLoading,
      surplusAnimationSpeed = 200,
      postback = false,
      completed = false,
      totalRenderingCalculationTime = +new Date();

  var crossDocComLink = new CrossDocComLink();
  crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
    postback = true;
    var previewIframe = $("iframe#previewIframe")[0];
    switch(cmdParams[0]) {
      case "preview-height":
        vrtxAdmin.log({ msg: "PREVIEW HEIGHT RECEIVED" });
        
        var dataHeight = (cmdParams.length === 2) ? cmdParams[1] : 0;
        var newHeight = Math.min(Math.max(dataHeight, previewIframeMinHeight), 20000); // Keep height between available window pixels and 20000 pixels
        
        if(!vrtxAdmin.isIE8) {
          var diff = newHeight - previewIframeMinHeight;
          var surplus = appFooterHeight + 13 + 20; // +contentBottomMargin+border+contentBottomPadding+border
          var animatedPixels = (diff > surplus) ? surplus : newHeight;
          previewLoading.animate({height: (previewIframeMinHeight + animatedPixels) + "px"}, surplusAnimationSpeed);
          contents.animate({height: (previewIframeMinHeight + animatedPixels) + "px"}, surplusAnimationSpeed, function() {
            previewLoadingComplete(previewIframe, newHeight, previewLoading, contents);
          });
        } else {
          previewLoadingComplete(previewIframe, newHeight, previewLoading, contents);
        }
        break;
      case "keep-min-height":
        vrtxAdmin.log({ msg: "KEEP MIN HEIGHT CMD RECEIVED" });
        previewLoadingComplete(previewIframe, previewIframeMinHeight, previewLoading, contents);
        break;
      default:
    }
  });

  function previewLoadingComplete(previewIframe, newHeight, previewLoading, contents) {
    if(!completed) {
      completed = true;
      previewIframe.style.height = newHeight + "px";
      if(!vrtxAdmin.isIE8) {
        previewLoading.find("#preview-loading-inner").remove();
        previewLoading.fadeOut(surplusAnimationSpeed, function() {
          contents.removeAttr('style');
          previewLoading.remove();
          vrtxAdmin.log({ msg: "PREVIEW RENDERING AND ANIMATION TOOK: " + ((new Date() - totalRenderingCalculationTime)/1000) + "s"});
        });
      } else {
        contents.removeAttr('style');
        previewLoading.remove();
        vrtxAdmin.log({ msg: "PREVIEW RENDERING AND ANIMATION TOOK: " + ((new Date() - totalRenderingCalculationTime)/1000) + "s"});
      }
    }
  }

  $(document).ready(function() {
    isPreviewMode = $("#vrtx-preview").length;
    if(isPreviewMode) {
      body = $("body");
      contents = body.find("#contents");
      var appContentHeight = body.find("#app-content").height();
      var appHeadWrapperHeight = body.find("#app-head-wrapper").outerHeight(true);
      appFooterHeight = body.find("#app-footer").outerHeight(true);
      var windowHeight = $(window).outerHeight(true);

      previewIframeMinHeight = (windowHeight - (appContentHeight + appHeadWrapperHeight + appFooterHeight)) + 150; // + iframe default height
   
      contents.append("<span id='preview-loading'><span id='preview-loading-inner'><span>" + previewLoadingMsg + "...</span></span></span>")
              .css({ position: "relative",
                     height: (previewIframeMinHeight + 2) + "px" });
      
      
      previewLoading = contents.find("#preview-loading");
      var previewLoadingHeightCSS = { height: previewIframeMinHeight + "px" };  
      previewLoading.css(previewLoadingHeightCSS); 
      previewLoading.find("#preview-loading-inner")
                    .css(previewLoadingHeightCSS);
    }
  });
  
  $(window).load(function() {
    if(isPreviewMode) {
      vrtxAdmin.log({ msg: "TRY TO SEND MIN HEIGHT" });
      
      var previewIframe = $("iframe#previewIframe")[0];
      crossDocComLink.postCmdToIframe(previewIframe, "min-height|" + previewIframeMinHeight);
      
      /* TODO: this should not be necessary - but keep it for robustness
      var runTimes = 0;
      var waitForResponse = setTimeout(function() {
        runTimes++;
        if(!postback) {
          if(runTimes <= 333) { 
            setTimeout(arguments.callee, 15);
          } else { // Timeout after ca. 5s (http://ejohn.org/blog/accuracy-of-javascript-time/)
            vrtxAdmin.log({ msg: "WAITED TOO LONG FOR RESPONSE FROM IFRAME - USE MIN HEIGHT" });
            previewLoadingComplete(previewIframe, previewIframeMinHeight, previewLoading, contents);
          }
        }
      }, 15);
      */
    }
  });

}(jQuery));