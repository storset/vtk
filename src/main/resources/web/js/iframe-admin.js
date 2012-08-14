/*  Need to use postMessage for iframe resizing since cross domain is typical case now.
 *  Not essential functionality. Only works in browsers which support postMessage
 *
 */
 
 $(document).ready(function() {
  var sslComLink = new SSLComLink();
  sslComLink.setUpReceiveDataHandler({
    cmd: function(c, that, source) {
      switch(c) {
        case "create-dropdown-full-size":
          var previewCreateIframe = $("#create-iframe");
          if (previewCreateIframe) {
            var originalWidth = 150; // Hack for width when english i18n
            if ($("#locale-selection li.active").hasClass("en")) {
              originalWidth = 162;
            }
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
              that.postCmdAndNum("create-dropdown-move-dropdown", { top: Math.ceil(previewCreateIframePosTop),
                                                                   left: Math.ceil(previewCreateIframePosLeft)}, source);
            }
          }
            
          break;
        case "create-dropdown-original-size":
          var previewCreateIframe = $("#create-iframe");
          if (previewCreateIframe) {
            var originalWidth = 150; // Hack for width when english i18n
            if ($("#locale-selection li.active").hasClass("en")) {
              originalWidth = 162;
            }
            previewCreateIframe.css({
              "height": 40 + "px",
              "width": originalWidth + "px"
            });
            previewCreateIframe.removeClass("iframe-fullscreen");
            $("#global-menu-create").css("zIndex", "99");
          }  
            
          break;
        case "create-dropdown-collapsed":
          var previewCreateIframe = $("#create-iframe");
          if (previewCreateIframe) {
            previewCreateIframe.css("height", 40 + "px");
          }
            
          break;
        case "create-dropdown-expanded":
          var previewCreateIframe = $("#create-iframe");
          if (previewCreateIframe) {
            previewCreateIframe.css("height", 135 + "px");
          }
            
          break;
        default:
      }
    },
    cmdNum: function(c, n, that, source) {
      switch(c) {
        case "preview-height":
          var previewIframe = $("iframe#previewIframe")[0];
          if (previewIframe) {
            var previewIframeMinHeight = 350;
            var previewIframeMaxHeight = 20000;
            var newHeight = previewIframeMinHeight;
            var dataHeight = n;
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
    }
  });
});