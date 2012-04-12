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

      // What is the recieved message..
      var isMsgCreateIframeFullSize = recievedData.indexOf && (recievedData.indexOf("fullsize") != -1);
      var isMsgCreateIframeOriginalSize = recievedData.indexOf && (recievedData.indexOf("originalsize") != -1);
      var isMsgCreateIframeCollapsedSize = recievedData.indexOf && (recievedData.indexOf("collapsedsize") != -1);
      var isMsgCreateIframeExpandedSize = recievedData.indexOf && (recievedData.indexOf("expandedsize") != -1);
      var isMsgPreviewIframeInnerHeight = !isMsgCreateIframeFullSize && 
                                          !isMsgCreateIframeOriginalSize && 
                                          !isMsgCreateIframeCollapsedSize &&
                                          !isMsgCreateIframeExpandedSize;

      if(isMsgPreviewIframeInnerHeight) {
        var previewIframe = $("iframe#previewIframe")[0];
        if (previewIframe) {
          var previewIframeMinHeight = 350;
          var previewIframeMaxHeight = 20000;
          var newHeight = previewIframeMinHeight;
          if(recievedData.indexOf) { // Recieved via hash
            var dataHeight = Number(recievedData.replace(/.*height=(\d+)(?:&|$)/, '$1' ));
          } else { // Recieved with postMessage
            var dataHeight = parseInt(recievedData, 10);
          }
          if (!isNaN(dataHeight) && (dataHeight > previewIframeMinHeight)) {
            if (dataHeight <= previewIframeMaxHeight) {
              newHeight = dataHeight;
            } else {
              newHeight = previewIframeMaxHeight;
            }
          }
          previewIframe.style.height = newHeight + "px";
        }
      } else {
        var previewCreateIframe = $("#create-iframe");
        if (previewCreateIframe) {
          var originalWidth = 150; // Hack for width when english i18n
          if ($("#locale-selection li.active").hasClass("en")) {
            originalWidth = 162;
          }
          if(isMsgCreateIframeFullSize) {
            var winHeight = window.innerHeight ? window.innerHeight : $(window).height();
            var winWidth = $(window).width();
          
            previewCreateIframePos = previewCreateIframe.offset(); // Get original iframe position
            previewCreateIframePosTop = previewCreateIframePos.top;
            previewCreateIframePosLeft = previewCreateIframePos.left;

            previewCreateIframe.css({ // Go fullsize
              "height": winHeight + "px",
              "width": winWidth + "px"
            });
            previewCreateIframe.addClass("iframe-fullscreen");
            $(".dropdown-shortcut-menu-container").css("display", "none");
            $("#global-menu-create").css({"zIndex": "999999", "width": originalWidth + "px"});
          
            // Post back to iframe the original iframe offset position
            var isPreviewCreateIframePosValid = !isNaN(previewCreateIframePosTop) && !isNaN(previewCreateIframePosLeft);
            if(isPreviewCreateIframePosValid) {
              var hasPostMessage = previewCreateIframe[0].contentWindow['postMessage'] && (!($.browser.opera && $.browser.version < 9.65));
              var vrtxAdminOrigin = "*"; // TODO: TEMP Need real origin of adm
              if(hasPostMessage) {
                previewCreateIframe[0].contentWindow.postMessage("top=" + Math.ceil(previewCreateIframePosTop)
                                                               + "left=" + Math.ceil(previewCreateIframePosLeft), vrtxAdminOrigin);
              }
            }
          } else if(isMsgCreateIframeOriginalSize) {
            previewCreateIframe.css({
              "height": 40 + "px",
              "width": originalWidth + "px"
            });
            previewCreateIframe.removeClass("iframe-fullscreen");
            $("#global-menu-create").css("zIndex", "99");
          } else if(isMsgCreateIframeCollapsedSize) {
            previewCreateIframe.css("height", 40 + "px");
          } else if(isMsgCreateIframeExpandedSize) {
            previewCreateIframe.css("height", 135 + "px");
          }
        }
      }
    }); // TODO: here we can add where we only want to receive from, e.g. }, "<domain>");
});