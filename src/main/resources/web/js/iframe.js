/*  This code was found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" and 
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)  
 */
$(document).ready(function()
	{
		// Set specific variable to represent all iframe tags.
		var iFrames = document.getElementsByTagName('iframe');

		// Resize heights.
		function iResize()
		{
			// Iterate through all iframes in the page.
			for (var i = 0, j = iFrames.length; i < j; i++)
			{
				// Set inline style to equal the body height of the iframed content,
				// when body content is at least 350px heigh
				if((iFrames[i].contentWindow.document.body.offsetHeight + 35) > 350){
					iFrames[i].style.height = (iFrames[i].contentWindow.document.body.offsetHeight + 35) + 'px';
				}else{
					iFrames[i].style.height = "350px"; 
				}
			}
		}

		// Check if browser is Safari or Opera.
		if ($.browser.safari || $.browser.opera)
		{
			// Start timer when loaded.
			$('iframe').load(function()
				{
					setTimeout(iResize, 0);
				}
			);

			// Safari and Opera need a kick-start.
			for (var i = 0, j = iFrames.length; i < j; i++)
			{
				var iSource = iFrames[i].src;
				iFrames[i].src = '';
				iFrames[i].src = iSource;
			}
		}
		else
		{
			// For other good browsers.
			$('iframe').load(function()
				{
					// Set inline style to equal the body height of the iframed content,
					// when body content is at least 350px heigh
					if((this.contentWindow.document.body.offsetHeight + 35) > 350){ 
						this.style.height = (this.contentWindow.document.body.offsetHeight + 35) + 'px';
					}else{
						this.style.height = "350px";
					}
						
				}
			);
		}
	}
);
