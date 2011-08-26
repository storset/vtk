/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage
 *
 *  Updated with cross-browser postMessage: http://benalman.com/code/projects/jquery-postmessage/examples/iframe/
 */
$(document).ready(function () {
  $.receiveMessage(function(e) {
      var previewIframeMinHeight = 350;
      var previewIframeMaxHeight = 20000;
      var previewIframe = $("iframe#previewIframe")[0]
      if (previewIframe) {
        var newHeight = previewIframeMinHeight;
        var dataHeight = Number(e.data.replace( /.*height=(\d+)(?:&|$)/, '$1' ) );
        if (!$.isNaN(dataHeight) && (dataHeight > previewIframeMinHeight) && (dataHeight <= previewIframeMaxHeight)) {
          newHeight = dataHeight
        }
        previewIframe.style.height = newHeight + "px";
      }
  });
});