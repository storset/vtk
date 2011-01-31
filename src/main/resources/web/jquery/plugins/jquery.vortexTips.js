/* 
 *  vortexTips plugin
 *  
 *  Based heavily on tinyTips v1.1 by Mike Merritt (se license.txt)
 *  Modified by Øyvind Hatland (USIT)
 *  
 *  Changes
 *  -------
 *  
 *  * Delegate mouseover/mouseleave to affect added nodes
 *  * Simpler and little different parameters
 *  * Different animationspeeds for fadeIn / fadeOut
 *  
 */

(function($){  
	$.fn.vortexTips = function (subSelector, leftOffset, topOffset) {
		
		var html = "<span class='tip'>&nbsp;</span>";
		var animInSpeed = 300;
		var animOutSpeed = 5000;
		var tip;
		var tipText;
		
		$(this).delegate(subSelector + " a", "mouseover mouseleave", function(e) {
		  if(e.type == "mouseover") {
		    $("#contents").append(html);
			tip = $(".tip"); tip.hide();
		    var title = $(this).attr('title');
		    tip.html(title);
		    tipText = $(this).attr('title');
			$(this).attr('title', '');
			var yOffset = tip.height() + 2;
			var xOffset = (tip.width() / 2) - ($(this).width() / 2);
			var pos = $(this).offset();
			var nPos = pos;
			nPos.top = (pos.top - yOffset) + topOffset;
			nPos.left = (pos.left - xOffset) + leftOffset;
			tip.css('position', 'absolute').css('z-index', '1000');
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