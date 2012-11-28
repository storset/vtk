/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage.
 *
 *  TODO: the animation could be improved/simplified with less "overflow: hidden" on wrappers/containers
 *
 */
 
(function ($) {

  var isPreviewMode, body,
      appContentHeight, appHeadWrapperHeight,
      appFooterHeight, windowHeight,
      previewIframeMinHeight, appContent,
      main, contents, previewLoading,
      surplusAnimationSpeed = 200;

  var crossDocComLink = new CrossDocComLink();
  crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
    switch(cmdParams[0]) {
      case "preview-height":
        var previewIframe = $("iframe#previewIframe")[0];
        if (previewIframe) {
          var dataHeight = (cmdParams.length === 2) ? cmdParams[1] : 0;
          
          var newHeight = Math.min(Math.max(dataHeight, previewIframeMinHeight), 20000) - 1;
          var diff = newHeight - previewIframeMinHeight;
          var surplus = body.find("#app-head-wrapper").outerHeight(true);
          
          if(diff > 0 && diff > surplus) {
            previewLoading.animate({height: (previewIframeMinHeight + surplus) + "px"}, surplusAnimationSpeed);
            contents.animate({height: (previewIframeMinHeight + surplus) + "px"}, surplusAnimationSpeed, function() {
              previewLoadingComplete(previewIframe, newHeight, previewLoading, contents);
            });  
          } else if(diff > 0 && diff < surplus) {
            previewLoading.animate({height: newHeight + "px"}, surplusAnimationSpeed);
            contents.animate({height: newHeight + "px"}, surplusAnimationSpeed, function() {
              previewLoadingComplete(previewIframe, newHeight, previewLoading, contents);
            });  
          } else {
            previewLoadingComplete(previewIframe, newHeight, previewLoading, contents);
          }
        }
        
        break;
      default:
    }
  });

  function previewLoadingComplete(previewIframe, newHeight, previewLoading, contents) {
    previewIframe.style.height = newHeight + "px";
    previewLoading.fadeOut(surplusAnimationSpeed, function() {
      previewLoading.remove();
      contents.removeAttr('style');
    });
  }

  $(document).ready(function() {
    isPreviewMode = $("#vrtx-preview").length;
    if(isPreviewMode) {
      body = $("body");
      appContent = body.find("#app-content");
      main = appContent.find("#main");
      contents = main.find("#contents");
    
      appContentHeight = appContent.height();
      appHeadWrapperHeight = body.find("#app-head-wrapper").outerHeight(true);
      appFooterHeight = body.find("#app-footer").outerHeight(true);
      windowHeight = $(window).outerHeight(true);

      previewIframeMinHeight = (windowHeight - (appContentHeight + appHeadWrapperHeight + appFooterHeight)) + 150; // + iframe default height
   
      contents.append("<span id='preview-loading'><span>" + previewLoadingMsg + "...</span></span>")
              .css({ position: "relative",
                     height: previewIframeMinHeight + "px" });
                     
      previewLoading = contents.find("#preview-loading");
      previewLoading.css({ height: previewIframeMinHeight + "px" });
    }
  });
  
  $(window).load(function() {
    if(isPreviewMode) {
      crossDocComLink.postCmdToIframe($("iframe#previewIframe")[0], "min-height|" + previewIframeMinHeight);
    }
  });

}(jQuery));