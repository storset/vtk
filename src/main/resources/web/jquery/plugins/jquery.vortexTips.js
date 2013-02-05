/*
 *  vortexTips plugin
 *  
 *  Based loosely on tinyTips v1.1 by Mike Merritt (se license.txt)
 *  Modified by �yvind Hatland (USIT)
 *  
 *  Changes
 *  -------
 *
 *  * Delegate mouseover/mouseleave to affect added nodes dynamically (http://stackoverflow.com/questions/3367769/using-delegate-with-hover)
 *  * Independent/multiple tips in different contexts (by appendTo)
 *  * Configure different speeds for fadeIn, fadeOutPreDelay and fadeOut
 *  * Changed positioning 'algorithm'
 *  * Caching
 *
 */
(function ($) {
  $.fn.vortexTips = function (subSelector, opts) {
	opts.animInSpeed = opts.animInSpeed || 300;
	opts.animOutSpeed = opts.animOutSpeed || 300;
	  
    var html = '<span class="tip ' + opts.appendTo.substring(1) + '">&nbsp;</span>';
    if (opts.extra) {
      var extraHtml = '<span class="tipextra ' + opts.appendTo.substring(1) + '">&nbsp;</span>';
    }
    var tip;
    var tipExtra;
    var tipText;
    var fadeOutTimer;

    $(this).on("mouseenter mouseleave", subSelector, function (e) {
      if (e.type == "mouseenter") {
        var link = $(this);
        if (typeof linkTriggeredMouseEnter !== "undefined" && linkTriggeredMouseEnter) {
          linkTriggeredMouseEnter.attr('title', linkTriggeredMouseEnterTipText);
          linkTriggeredMouseEnter;
          linkTriggeredMouseEnterTipText;
        }
        if (typeof link.attr("href") === "undefined" && !link.is("abbr")) {
          link = link.find("a");
        }
        clearTimeout(fadeOutTimer);
        if (tip) {
          tip.remove();
        }
        if (tipExtra) {
          tipExtra.remove();
        }
        $(opts.appendTo).append(html);
        tip = $('.tip.' + opts.appendTo.substring(1));
        tip.hide();
        if (opts.extra) {
         $(opts.appendTo).append(extraHtml);
         tipExtra = $('.tipextra.' + opts.appendTo.substring(1));
         tipExtra.hide();
        }
        var linkParent = link.parent();
        var classes = linkParent.attr("class") + " " + linkParent.parent().attr("class");
        if(typeof classes !== "undefined") {
          tip.addClass(classes);
        }
        var title = tipText = link.attr('title');
        tip.html(title);
        link.attr('title', '');
        var nPos = link.position();
        nPos.top = nPos.top + opts.yOffset;
        var left = nPos.left + link.width() + opts.xOffset;
        if (opts.extra) {
          var ePos = link.position();
          ePos.top = ePos.top + opts.yOffset;
          tipExtra.css('position', 'absolute').css('z-index', '-1').css('width', '99%');
          ePos.left = 0;
        }
        if (opts.autoWidth) {
          tip.css('position', 'absolute').css('z-index', '1000').css('width', left + 'px');
          nPos.left = 0;
        } else {
          nPos.left = left;
          tip.css('position', 'absolute').css('z-index', '1000').css('width', opts.containerWidth + 'px');        
        }
        tip.css(nPos).fadeIn(opts.animInSpeed);
        if (opts.extra) {
          tipExtra.css(ePos).fadeIn(opts.animInSpeed);
        }
      } else if (e.type == "mouseleave") {
        var link = $(this);
        if (typeof link.attr("href") === "undefined" && !link.is("abbr")) {
          link = link.find("a");
        }
        link.attr('title', tipText);
        fadeOutTimer = setTimeout(function () {
          if (opts.extra) {
            tipExtra.fadeOut(opts.animOutSpeed, function () {
              $(this).remove();
            });
          }
          tip.fadeOut(opts.animOutSpeed, function () {
            $(this).remove();
          });
        }, opts.animOutPreDelay);
      }
      e.stopPropagation();
    });
  }
})(jQuery);
