/*  Iframe resizing when only one iframe
 */
$(document).ready(function()
	{
	  $(window).load(function() {
	    var previewIframeMinHeight = 350;
	    var previewIframeMaxHeight = 20000;
	
	    var previewIframe = $("iframe#previewIframe")[0];
	    if (previewIframe) {
	      var newHeight = previewIframeMinHeight;
	      var dataHeight = previewIframe.contentWindow.document.body.offsetHeight + 45;
	      if (!isNaN(dataHeight) && (dataHeight > previewIframeMinHeight) && (dataHeight <= previewIframeMaxHeight)) {
	        newHeight = dataHeight
	      }
	      previewIframe.style.height = newHeight + "px";
	    }
	  });
	}
);
