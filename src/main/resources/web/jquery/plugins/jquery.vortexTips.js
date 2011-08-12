/*
 *  vortexTips plugin
 *  
 *  Based loosely on tinyTips v1.1 by Mike Merritt (se license.txt)
 *  Modified by �yvind Hatland (USIT)
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
(function ($) {
  $.fn.vortexTips = function (subSelector, appendTo, containerWidth, animInSpeed, animOutPreDelay, animOutSpeed) {

    var html = "<span class='tip'>&nbsp;</span>";
    var tip;
    var tipText;
    var fadeOutTimer;

    $(this).delegate(subSelector, "mouseover mouseleave", function (e) {
      if (e.type == "mouseover") {
        clearTimeout(fadeOutTimer); // remove fadeOutTimer
        $(appendTo).append(html);
        tip = $(".tip");
        tip.hide();
        var link = $(this);
        var classes = link.parent().attr("class") + " " + link.parent().parent().attr("class");
        if(typeof classes !== "undefined") {
          tip.addClass(classes);
        }
        var title = link.attr('title');
        tip.html(title);
        tipText = link.attr('title');
        link.attr('title', '');
        var yOffset = tip.height() / 2;
        var xOffset = link.width();
        var pos = link.position();
        var nPos = pos;
        nPos.top = (pos.top) + yOffset;
        nPos.left = (pos.left + xOffset + 30);
        tip.css('position', 'absolute').css('z-index', '1000').css('width', containerWidth + 'px');
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