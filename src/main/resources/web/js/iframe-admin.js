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
      surplusReduce,
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
          var surplus = (appFooterHeight + 13 + 20) - surplusReduce; // +contentBottomMargin+border+contentBottomPadding+border
          if(surplus <= 0) { // If surplus have been swallowed by minimum height
            previewLoadingComplete(previewIframe, newHeight, previewLoading, contents);
          } else {
            var animatedPixels = (diff > surplus) ? (previewIframeMinHeight + surplus) : newHeight;
            previewLoading.animate({height: animatedPixels + "px"}, surplusAnimationSpeed);
            contents.animate({height: (animatedPixels + extras) + "px"}, surplusAnimationSpeed, function() {
              previewLoadingComplete(previewIframe, newHeight, previewLoading, contents);
            });
          }
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
      
      $(document).on("click", "#preview-mode a", function(e) {
        var previewIframe = $("iframe#previewIframe")[0];
        if(!$("#previewIframeWrapper").hasClass("mobile")) {
          $("#previewIframeWrapper").css("height", $("#previewIframe").height());
          crossDocComLink.postCmdToIframe(previewIframe, "update-height|" + 494);
        } else {
          $("#previewIframeWrapper").css("height", "auto");
          crossDocComLink.postCmdToIframe(previewIframe, "restore-height");
        }
      
        $("#previewIframeWrapper").toggleClass("mobile");

        var notLink = $("#preview-mode span");
        
        notLink.parent().removeClass("active-mode");
        $(this).parent().addClass("active-mode");
        
        notLink.replaceWith("<a id='" + notLink.attr("id") + "' href='javascript:void(0);'>" + notLink.text() + "</span");
        $(this).replaceWith("<span id='" + this.id + "'>" + $(this).text() + "</span>")
      });
   
      $(document).on("click", "#preview-actions-print", function(e) {
        // Print iframe
        alert("Implement.");
        e.preventDefault();
        e.stopPropagation();
      });
          
      $(document).on("click", "#preview-actions-fullscreen-toggle", function(e) {
        body.toggleClass('fullscreen-toggle-open');
        if(body.hasClass('fullscreen-toggle-open')) {
          alert("Implement.");
          $(this).text(fullscreenToggleClose);
        } else {
          $(this).text(fullscreenToggleOpen);
        }
        e.preventDefault();
        e.stopPropagation();
      });	

      contents = body.find("#contents");
      var appContentHeight = body.find("#app-content").height();
      var appHeadWrapperHeight = body.find("#app-head-wrapper").outerHeight(true);
      appFooterHeight = body.find("#app-footer").outerHeight(true);
      var windowHeight = $(window).outerHeight(true);

      var msg = $(".tabMessage-big");
      if(msg.length) {
        extras = msg.outerHeight(true);
      }
      var absMinHeight = 150;
      var availWinHeight = (windowHeight - (appContentHeight + appHeadWrapperHeight + appFooterHeight)) + absMinHeight; // + iframe height
      if(availWinHeight < absMinHeight) {
        surplusReduce = absMinHeight - availWinHeight;
        previewIframeMinHeight = absMinHeight;
      } else {
        previewIframeMinHeight = availWinHeight;
      }
      
      contents.append("<span id='preview-loading'><span id='preview-loading-inner'><span>" + previewLoadingMsg + "...</span></span></span>")
              .css({ position: "relative",
                     height: (previewIframeMinHeight + extras + 2) + "px" });
      
      previewLoading = contents.find("#preview-loading");
      previewLoading.css({
        height: previewIframeMinHeight + "px",
        top: extras + "px"
      });
      
      previewLoading.find("#preview-loading-inner").css({
        height: availWinHeight + "px"
      });
    }
  });

}(jQuery));