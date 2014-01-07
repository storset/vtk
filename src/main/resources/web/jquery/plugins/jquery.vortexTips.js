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
 *  * Configure different speeds for fadeIn and fadeOut
 *  * Possible to expand hover area to tip-box
 *  * Changed positioning 'algorithm'
 *  * Caching
 *
 */
(function ($) {
  $.fn.vortexTips = function (subSelector, opts) {
	opts.animInSpeed = opts.animInSpeed || 300;
	opts.animOutSpeed = opts.animOutSpeed || 300;
	opts.expandHoverToTipBox = opts.expandHoverToTipBox || false;
	opts.autoWidth = opts.autoWidth || false;
	opts.extra = opts.extra || false;
	  
    var html = '<span class="tip ' + opts.appendTo.substring(1) + '">&nbsp;</span>';
    if (opts.extra) {
      var extraHtml = '<span class="tipextra ' + opts.appendTo.substring(1) + '">&nbsp;</span>';
    }
    var tip;
    var tipExtra;
    var tipText;
    var fadeOutTimer;
    var toggleOn = false;
    var hoverTip = false;
    
    $(this).on("mouseenter mouseleave keyup", subSelector, function (e) {
      if (e.type == "mouseenter" || (((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) && !toggleOn)) {
        toggleOn = true;
        
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
        tip.css(nPos).fadeIn(opts.animInSpeed, function() {
          var button = $(this).find(".vrtx-button, .vrtx-button-small");
          if(button.length && button.is(":visible")) button.filter(":first")[0].focus();
        });
        if (opts.extra) {
          tipExtra.css(ePos).fadeIn(opts.animInSpeed);
        }
        e.stopPropagation();
      } else if (e.type == "mouseleave" || (((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) && toggleOn)) {
        var link = $(this);
        if(opts.expandHoverToTipBox) {
          tip.on("mouseenter", function (e) {
            hoverTip = true;
          });
          tip.on("mouseleave", function (e) {
            hoverTip = false;
            tip.off("mouseenter");
            tip.off("mouseleave");
            link.trigger("mouseleave");
          });
        }
        if(!(opts.expandHoverToTipBox && hoverTip)) {
          toggleOn = false;
          if (typeof link.attr("href") === "undefined" && !link.is("abbr")) {
            link = link.find("a");
          }
          link.attr('title', tipText);
          fadeOutTimer = setTimeout(function () {
            if(!(opts.expandHoverToTipBox && hoverTip)) {
              if (opts.extra) {
                tipExtra.fadeOut(opts.animOutSpeed, function () {
                  $(this).remove();
                });
              }
              tip.fadeOut(opts.animOutSpeed, function () {
                $(this).remove();
              });
            }
          }, opts.expandHoverToTipBox ? 250 : 0);
          e.stopPropagation();
        }
      }
    });
  }
})(jQuery);
