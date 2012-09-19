/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage.
 *
 */

var crossDocComLink = new CrossDocComLink();
crossDocComLink.setUpReceiveDataHandler(function(cmdParams, source) {
  switch(cmdParams[0]) {
    case "preview-height":
      var previewIframe = $("iframe#previewIframe")[0];
      if (previewIframe) {
        var previewIframeMinHeight = 350;
        var previewIframeMaxHeight = 20000;
        var newHeight = previewIframeMinHeight;
        var dataHeight = (cmdParams.length === 2) ? cmdParams[1] : 700;
        if (dataHeight > previewIframeMinHeight) {
          if (dataHeight <= previewIframeMaxHeight) {
            newHeight = dataHeight;
          } else {
            newHeight = previewIframeMaxHeight;
          }
        }
        previewIframe.style.height = newHeight + "px";
      }
        
      break;
    default:
  }
});