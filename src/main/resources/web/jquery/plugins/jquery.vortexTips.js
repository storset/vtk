/*
 *  vortexTips plugin
 *  
 *  Based loosely on tinyTips v1.1 by Mike Merritt (se license.txt)
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
(function ($) {
  $.fn.vortexTips = function (subSelector) {

    var html = "<span class='tip'>&nbsp;</span>";
    var animInSpeed = 300;
    var animOutPreDelay = 4000;
    var animOutSpeed = 3000;
    var tip;
    var tipText;
    var fadeOutTimer;

    $(this).delegate(subSelector + " a", "mouseover mouseleave", function (e) {
      if (e.type == "mouseover") {
        clearTimeout(fadeOutTimer); // remove fadeOutTimer
        $("#contents").append(html);
        tip = $(".tip");
        tip.hide();
        var link = $(this);
        var classes = link.parent().attr("class") + " " + link.parent().parent().attr("class");
        tip.addClass(classes);
        var title = link.attr('title');
        tip.html(title);
        tipText = link.attr('title');
        link.attr('title', '');
        var yOffset = tip.height() / 2;
        var xOffset = link.width();
        var pos = link.offset();
        var nPos = pos;
        nPos.top = (pos.top - 230);
        nPos.left = (pos.left + xOffset) - 230;
        tip.css('position', 'absolute').css('z-index', '1000').css('width', '400px');
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