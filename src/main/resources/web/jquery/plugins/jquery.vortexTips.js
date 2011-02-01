/* 
 *  vortexTips plugin
 *  
 *  Based loosely tinyTips v1.1 by Mike Merritt (se license.txt)
 *  Modified by Øyvind Hatland (USIT)
 *  
 *  Changes
 *  -------
 *  
 *  * Delegate mouseover/mouseleave to affect added nodes
 *  * Simpler parameters
 *  * Changed positioning 'algorithm'
 *  * Different animationspeeds for fadeIn / fadeOut
 *  * Caching of link-element
 *  
 */

(function($){  
	$.fn.vortexTips = function (subSelector) {
		
		var html = "<span class='tip'>&nbsp;</span>";
		var animInSpeed = 300;
		var animOutSpeed = 5000;
		var tip;
		var tipText;
		
		$(this).delegate(subSelector + " a", "mouseover mouseleave", function(e) {
		  if(e.type == "mouseover") {
		    $("#contents").append(html);
			tip = $(".tip"); tip.hide();
			var link = $(this);
		    var title = link.attr('title');
		    tip.html(title);
		    tipText = link.attr('title');
		    link.attr('title', '');
			var yOffset = tip.height() / 2;
			var xOffset = link.width() + 20;
			var pos = link.offset();
			var nPos = pos;
			nPos.top = (pos.top - yOffset);
			nPos.left = (pos.left + xOffset);
			tip.css('position', 'absolute').css('z-index', '1000').css('width', '400px');
			tip.css(nPos).fadeIn(animInSpeed);
		  } else if (e.type == "mouseleave") {
			$(this).attr('title', tipText);
			tip.fadeOut(animOutSpeed, function() {
			  $(this).remove();
			});
		  }
		});
   }
})(jQuery);