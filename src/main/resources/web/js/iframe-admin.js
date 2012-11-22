/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage.
 *
 */

var appContentHeight, appWrapperHeight, appFooterHeight, windowHeight, previewIframeMinHeight;
var surplusAnimationSpeed = 200;

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
        var surplus = appFooterHeight + 20 + 12;
        var h = $("#app-content").height();
        if(diff > surplus) {
          $("#app-content").animate({height: (h + surplus) + "px"}, surplusAnimationSpeed);
          $("#main, #contents, #preview-loading").animate({height: (newHeight + surplus) + "px"}, surplusAnimationSpeed, function() {
            $("#preview-loading").remove();
            $("#main, #contents, #app-content").removeAttr('style');
            previewIframe.style.height = newHeight + "px";
          });  
        } else { // TODO
          $("#preview-loading").remove();
          $("#main, #contents, #app-content").removeAttr('style');
          previewIframe.style.height = newHeight + "px";
        }
      }
        
      break;
    default:
  }
});

$(document).ready(function() {
  if($("#vrtx-preview").length) {
    appContentHeight = $("#app-content").height();
    appWrapperHeight = $("#app-head-wrapper").height();
    appFooterHeight = $("#app-footer").outerHeight(true);
    windowHeight = $(window).height();
    previewIframeMinHeight = (windowHeight - (appContentHeight + appWrapperHeight + appFooterHeight)) + 150; //+ iframe default height

    $("#main, #contents").css({ height: previewIframeMinHeight + "px" });
        $("#app-content").css({ height: ((appContentHeight + previewIframeMinHeight) - 150 - 38 - 12 - 4) + "px" }); // TODO
    $("#contents").append("<span id='preview-loading' />")
                         .css({ position: "relative" });
    $("#preview-loading").css({
      position: "absolute",
      top: 0,
      left: 0,
      backgroundColor: "#fff",
      display: "block",
      width: "100%",
      height: previewIframeMinHeight + "px"
    });
  }
});