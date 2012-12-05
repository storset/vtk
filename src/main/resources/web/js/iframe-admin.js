/*  
 *  Iframe resizing for cross domain (admin)
 *
 *  Not essential functionality. Only works in browsers which support postMessage (>IE7).
 *
 *  - Sends available window v.space to iframe (minimum height) after receives msg about iframe is loaded
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
      extras = 0,
      previewIframeMinHeight,
      previewLoading,
      surplusAnimationSpeed = 200;

  var crossDocComLink = new CrossDocComLink();
  crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
    postback = true;
    var previewIframe = $("iframe#previewIframe")[0];
    switch(cmdParams[0]) {
      case "preview-loaded":
        crossDocComLink.postCmdToIframe(previewIframe, "admin-min-height|" + previewIframeMinHeight);
        break;
      case "preview-height":
        var dataHeight = (cmdParams.length === 2) ? cmdParams[1] : 0;
        var newHeight = Math.min(Math.max(dataHeight, previewIframeMinHeight), 20000); // Keep height between available window pixels and 20000 pixels
        
        if(!vrtxAdmin.isIE8) {
          var diff = newHeight - previewIframeMinHeight;
          var surplus = appFooterHeight + 13 + 20; // +contentBottomMargin+border+contentBottomPadding+border
          var animatedPixels = (diff > surplus) ? (previewIframeMinHeight + surplus) : newHeight;

          previewLoading.animate({height: animatedPixels + "px"}, surplusAnimationSpeed);
          contents.animate({height: (animatedPixels + extras) + "px"}, surplusAnimationSpeed, function() {
            previewLoadingComplete(previewIframe, newHeight, previewLoading, contents);
          });
        } else {
          previewLoadingComplete(previewIframe, newHeight, previewLoading, contents);
        }
        break;
      case "preview-keep-min-height":
        previewLoadingComplete(previewIframe, previewIframeMinHeight, previewLoading, contents);
        break;
      default:
    }
  });

  // Remove preview-loading overlay and set height
  function previewLoadingComplete(previewIframe, newHeight, previewLoading, contents) {
    previewIframe.style.height = newHeight + "px";
    if(!vrtxAdmin.isIE8) {
      previewLoading.find("#preview-loading-inner").remove();
      previewLoading.fadeOut(surplusAnimationSpeed, function() {
        contents.removeAttr('style');
        previewLoading.remove();
      });
    } else {
      contents.removeAttr('style');
      previewLoading.remove();
    }
  }

  // Find min-height
  $(document).ready(function() {
    isPreviewMode = $("#vrtx-preview").length;
    if(isPreviewMode) {
      body = $("body");
      contents = body.find("#contents");
      var appContentHeight = body.find("#app-content").height();
      var appHeadWrapperHeight = body.find("#app-head-wrapper").outerHeight(true);
      appFooterHeight = body.find("#app-footer").outerHeight(true);
      var windowHeight = $(window).outerHeight(true);

      var msg = $(".tabMessage-big");
      if(msg.length) {
        extras = msg.outerHeight(true);
      }

      previewIframeMinHeight = (windowHeight - (appContentHeight + appHeadWrapperHeight + appFooterHeight)) + 150; // + iframe default height
   
      contents.append("<span id='preview-loading'><span id='preview-loading-inner'><span>" + previewLoadingMsg + "...</span></span></span>")
              .css({ position: "relative",
                     height: (previewIframeMinHeight + extras + 2) + "px" });
      
      
      previewLoading = contents.find("#preview-loading");
      previewLoading.css({
        height: previewIframeMinHeight + "px",
        top: extras + "px",
        left: 0
      }); 
      previewLoading.find("#preview-loading-inner")
                    .css({height: previewIframeMinHeight + "px"});
    }
  });

}(jQuery));