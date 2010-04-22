/*  Need to use postMessage for iframe resizing since cross domain is typical case now.  
 *  Not essential functionality. Only works in browsers which support postMessage
 */
$(document).ready(function()
	{
	
		hasPostMessage = window['postMessage'] && (!($.browser.opera && $.browser.version < 9.65))
	    
		function receiveMessage(event)
		{
			var vrtxViewOrigin = event.origin; // TODO: TEMP
			
			var previewIframeMinHeight = 350;
			var previewIframeMaxHeight = 20000;
			
		    if (vrtxViewOrigin && (event.origin == vrtxViewOrigin)) {
				previewIframe = $("iframe#previewIframe")[0]
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
			window.addEventListener("message", receiveMessage, false);
		}		
	}
);
