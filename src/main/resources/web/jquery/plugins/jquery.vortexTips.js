/*
 *  vortexTips plugin
 *  
 *  Based loosely on tinyTips v1.1 by Mike Merritt (se license.txt)
 *  Modified by �yvind Hatland (USIT)
 *  
 *  Changes
 *  -------
 *
 *  * Delegate mouseover/mouseleave to affect added nodes dynamically
 *  * Independent/multiple tips in different contexts (by appendTo)
 *  * Configure different speeds for fadeIn, fadeOutPreDelay and fadeOut
 *  * Changed positioning 'algorithm'
 *  * Caching
 *
 */
(function ($) {
  $.fn.vortexTips = function (subSelector, appendTo, containerWidth, animInSpeed, animOutPreDelay, animOutSpeed, xOffset, yOffset, autoWidth) {

    var html = "<span class='tip " + appendTo.substring(1) + "'>&nbsp;</span>";
    var tip;
    var tipText;
    var fadeOutTimer;

    $(this).delegate(subSelector, "mouseover mouseleave", function (e) {
      if (e.type == "mouseover") {
        clearTimeout(fadeOutTimer); // remove fadeOutTimer
        $(appendTo).append(html);
        tip = $(".tip." + appendTo.substring(1));
        tip.hide();
        var link = $(this);
        var linkParent = link.parent();
        var classes = linkParent.attr("class") + " " + linkParent.parent().attr("class");
        if(typeof classes !== "undefined") {
          tip.addClass(classes);
        }
        var title = tipText = link.attr('title');
        tip.html(title);
        link.attr('title', '');
        var pos = link.position();
        var nPos = pos;
        nPos.top = pos.top + yOffset;
        if(autoWidth) {
          tip.css('position', 'absolute').css('z-index', '1000').css('width', pos.left + link.width() + xOffset + 'px');
          nPos.left = 0;
        } else {
          nPos.left = pos.left + link.width() + xOffset;
          tip.css('position', 'absolute').css('z-index', '1000').css('width', containerWidth + 'px');        
        }
        tip.css(nPos).fadeIn(animInSpeed);
      } else if (e.type == "mouseleave") {
        $(this).attr('title', tipText);
        fadeOutTimer = setTimeout(function () {
          tip.fadeOut(animOutSpeed, function () {
            $(this).remove();
          });
        }, animOutPreDelay);
      }
    });
  }
})(jQuery);