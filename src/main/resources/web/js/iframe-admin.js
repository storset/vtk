/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage
 *
 *  Updated with cross-browser postMessage -- which means using hash communication from:
 *    http://benalman.com/code/projects/jquery-postmessage/examples/iframe/
 *    see src: https://raw.github.com/cowboy/jquery-postmessage/master/jquery.ba-postmessage.js
 */
 
if(typeof hasReceiveMessageHandler === "undefined") {
  var hasReceiveMessageHandler = false; 
}
 
$(document).ready(function () {
  if (typeof hasReceiveMessageHandler === "undefined" || !hasReceiveMessageHandler) { // not handler in iframe-view.js available
    $.receiveMessage(function(e) {
    
      hasReceiveMessageHandler = true;
     
      var recievedData = e.data;
  
      // Preview iframe
      var previewIframeMinHeight = 350;
      var previewIframeMaxHeight = 20000;
      var previewIframe = $("iframe#previewIframe")[0]
      if (previewIframe) {
        var newHeight = previewIframeMinHeight;
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
    
      // Create tree iframe
      var previewCreateIframe = $("#create-iframe");
      if (previewCreateIframe) {
        // Fullsize
        if(recievedData.indexOf && recievedData.indexOf("fullsize") != -1) {
          previewCreateIframe.css({"height": $(window).height() + "px", 
                                    "width": $(window).width()  + "px"});
          previewCreateIframe.addClass("iframe-fullscreen");
          previewCreateIframe.contents().find("ul.manage-create").hide(0);
          previewCreateIframe.contents().find(".dropdown-shortcut-menu-container").hide(0);
        }
        // Back to normal again
        if(recievedData.indexOf && recievedData.indexOf("originalsize") != -1) {
          previewCreateIframe.css({"height": 100 + "px", 
                                   "width": 180 + "px"});
          previewCreateIframe.removeClass("iframe-fullscreen");
          previewCreateIframe.contents().find("ul.manage-create").show(0);
        } 
      }
    
    }); // TODO: here we can add where we only want to receive from, e.g. }, "<domain>");
  }
});