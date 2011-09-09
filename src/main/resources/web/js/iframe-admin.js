/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage
 *
 *  Updated with cross-browser postMessage -- which means using hash communication from:
 *    http://benalman.com/code/projects/jquery-postmessage/examples/iframe/
 *    see src: https://raw.github.com/cowboy/jquery-postmessage/master/jquery.ba-postmessage.js
 */
 
$(document).ready(function () {
    $.receiveMessage(function(e) {
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
        var originalWidth = 150;
        if ($(".localeSelection li.active").hasClass("en")) {
          originalWidth = 162;
        }
      
        // Fullsize
        if(recievedData.indexOf && recievedData.indexOf("fullsize") != -1) {
          var winHeight = $(window).height();
          var winWidth = $(window).width();
          
          // Get original iframe position
          previewCreateIframePos = previewCreateIframe.offset();
          previewCreateIframePosTop = previewCreateIframePos.top;
          previewCreateIframePosLeft = previewCreateIframePos.left;

          previewCreateIframe.css({
              "height": winHeight + "px", 
              "width": winWidth  + "px"
            });
          previewCreateIframe.addClass("iframe-fullscreen");
          $(".dropdown-shortcut-menu-container").css("display", "none");
          $("#global-menu-create").css({"zIndex": "999999", "width": originalWidth + "px"});
          
          // Post back to iframe original iframe position
          var hasPostMessage = previewCreateIframe[0].contentWindow['postMessage'] && (!($.browser.opera && $.browser.version < 9.65));
          var vrtxAdminOrigin = "*"; // TODO: TEMP Need real origin of adm
          if(hasPostMessage) {
            previewCreateIframe[0].contentWindow.postMessage("top=" + previewCreateIframePosTop 
                                                           + "left=" + previewCreateIframePosLeft, vrtxAdminOrigin);
          }
        }
        // Back to normal again
        if(recievedData.indexOf && recievedData.indexOf("originalsize") != -1) {
          previewCreateIframe.css({
              "height": 135 + "px", // TODO: generalize
              "width": originalWidth + "px"
            });
          previewCreateIframe.removeClass("iframe-fullscreen");
          $("#global-menu-create").css("zIndex", "99");    
        } 
      }
    
    }); // TODO: here we can add where we only want to receive from, e.g. }, "<domain>");
});