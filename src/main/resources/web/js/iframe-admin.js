/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage
 *
 *  Updated with cross-browser postMessage -- which means using hash communication from:
 *    http://benalman.com/code/projects/jquery-postmessage/examples/iframe/
 *    see src: https://raw.github.com/cowboy/jquery-postmessage/master/jquery.ba-postmessage.js
 */
$(document).ready(function () {
  $.receiveMessage(function(e) {
    var previewIframeMinHeight = 350;
    var previewIframeMaxHeight = 20000;
    var previewIframe = $("iframe#previewIframe")[0]
    if (previewIframe) {
      var newHeight = previewIframeMinHeight;
      var recievedData = e.data;
      if(!(recievedData.indexOf) || (recievedData.indexOf("height") == -1)) {
        var dataHeight = parseInt(recievedData, 10); // recieved with postMessage
      } else {
        var dataHeight = Number(recievedData.replace( /.*height=(\d+)(?:&|$)/, '$1' ) );  // recieved via hash
      }
      if (!$.isNaN(dataHeight) && (dataHeight > previewIframeMinHeight) && (dataHeight <= previewIframeMaxHeight)) {
        newHeight = dataHeight
      }
      previewIframe.style.height = newHeight + "px";
    }
    var previewIframe = $("#create-iframe")[0];
    if (previewIframe) {
      var recievedData = e.data;
      if(recievedData.indexOf && recievedData.indexOf("fullsize") != -1) {
        previewIframe.style.height = document.body.clientHeight + "px";
        previewIframe.style.width = document.body.clientWidth + "px";
      }
    }
  }); // TODO: here we can add where we only want to receive from, e.g. }, "<domain>");
});