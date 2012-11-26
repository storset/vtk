/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage.
 *
 */
 
(function ($) {

  var appContentHeight, appWrapperHeight,
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
          var newHeight = previewIframeMinHeight;
          var previewIframeMaxHeight = 20000;
          var dataHeight = (cmdParams.length === 2) ? cmdParams[1] : 0;
          if (dataHeight > previewIframeMinHeight) {
            if (dataHeight <= previewIframeMaxHeight) {
              newHeight = dataHeight;
            } else {
            newHeight = previewIframeMaxHeight;
            }
          }
          var diff = newHeight - previewIframeMinHeight;
          var surplus = appFooterHeight + 20 + 12; // TODO: Avoid hardcoded padding/margins
          var appContentHeight = appContent.height();
          if(diff > surplus) {
            // TODO: need to take into account travelling distance
            appContent.animate({height: (appContentHeight + surplus) + "px"}, surplusAnimationSpeed);
            contents.animate({height: (newHeight + surplus) + "px"}, surplusAnimationSpeed);
            previewLoading.animate({height: (newHeight + surplus) + "px"}, surplusAnimationSpeed);
            main.animate({height: (newHeight + surplus) + "px"}, surplusAnimationSpeed, function() {
              previewLoadingComplete(previewIframe, newHeight, previewLoading, appContent, main, contents);
            });  
          } else {
            previewLoadingComplete(previewIframe, newHeight, previewLoading, appContent, main, contents);
          }
        }
        
        break;
      default:
    }
  });

  function previewLoadingComplete(previewIframe, newHeight, previewLoading, appContent, main, contents) {
    previewIframe.style.height = newHeight + "px";
    previewLoading.fadeOut(surplusAnimationSpeed, function() {
      previewLoading.remove();
      appContent.removeAttr('style');
      main.removeAttr('style');
      contents.removeAttr('style');
    });
  }

  $(document).ready(function() {
    if($("#vrtx-preview").length) {
      var body = $("body");
      appContent = body.find("#app-content");
      main = appContent.find("#main");
      contents = main.find("#contents");
    
      appContentHeight = appContent.height();
      appWrapperHeight = body.find("#app-head-wrapper").height();
      appFooterHeight = body.find("#app-footer").outerHeight(true);
      windowHeight = $(window).height();
      previewIframeMinHeight = (windowHeight - (appContentHeight + appWrapperHeight + appFooterHeight)) + 150; // TODO: Avoid hardcoded padding/margins
   
      appContent.css({ height: ((appContentHeight + previewIframeMinHeight) - 150 - 38 - 12 - 4) + "px" }); // TODO: Avoid hardcoded padding/margins
            main.css({ height: previewIframeMinHeight + "px" });
        contents.append("<span id='preview-loading'><span>" + previewLoadingMsg + "...</span></span>")
                .css({ position: "relative",
                     height: previewIframeMinHeight + "px" });
                     
      previewLoading = contents.find("#preview-loading");
      previewLoading.css({ height: previewIframeMinHeight + "px" });
    }
  });

}(jQuery));