/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage
 */
$(document).ready(function () {

  var hasPostMessage = window['postMessage'] && (!($.browser.opera && $.browser.version < 9.65))

  function receiveMessage(event) {
      var vrtxViewOrigin = event.origin; // TODO: TEMP
      var previewIframeMinHeight = 350;
      var previewIframeMaxHeight = 20000;

      if (vrtxViewOrigin && (event.origin == vrtxViewOrigin)) {
        var previewIframe = $("iframe#previewIframe")[0]
        if (previewIframe) {
          var newHeight = previewIframeMinHeight;
          var dataHeight = parseInt(event.data, 10);
          if (!isNaN(dataHeight) && (dataHeight > previewIframeMinHeight) && (dataHeight <= previewIframeMaxHeight)) {
            newHeight = dataHeight
          }
          previewIframe.style.height = newHeight + "px";
        }
      }
      }
      
      
      
  if (hasPostMessage) {
    if (window.addEventListener) {
      window.addEventListener("message", receiveMessage, false);
    } else if (window.attachEvent) {
      // Internet Explorer
      window.attachEvent("onmessage", receiveMessage);
    }
  }
});