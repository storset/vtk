/*
 *  vortexTips plugin
 *  
 *  Based loosely on tinyTips v1.1 by Mike Merritt (se license.txt)
 *  Modified heavily by Øyvind Hatland (USIT)
 *
 */
(function ($) {
  $.fn.vortexTips = function (subSelector, opts) {
	opts.animInSpeed = opts.animInSpeed || 300;
	opts.animOutSpeed = opts.animOutSpeed || 300;
	opts.expandHoverToTipBox = opts.expandHoverToTipBox || false;
	opts.enterOpens = opts.enterOpens || false;
	opts.autoWidth = opts.autoWidth || false;
	opts.extra = opts.extra || false;
	
	var keyTriggersOpen = opts.keySpaceTriggersOpen ? 32 : 13;

    var html = '<span class="tip ' + opts.appendTo.substring(1) + '">&nbsp;</span>';
    if (opts.extra) {
      var extraHtml = '<span class="tipextra ' + opts.appendTo.substring(1) + '">&nbsp;</span>';
    }
    var tip;
    var tipExtra;
    var tipText;
    var fadeOutTimer;
    var waitForKeyUp;
    var toggleOn = false;
    var hoverTip = false;

    var openCloseTooltip = function(e) {
      if (e.type == "mouseenter" || (opts.enterOpens ? (((e.which && e.which == keyTriggersOpen) || (e.keyCode && e.keyCode == keyTriggersOpen)) && !toggleOn) : (e.type == "focusin"))) {
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
          var tipWidth = left;
          nPos.left = 0;
        } else {
          var tipWidth = opts.containerWidth;
          nPos.left = left;
        }
        tip.css({'position': 'absolute', 'z-index': '1000', 'width': tipWidth + 'px'})
           .css(nPos).fadeIn(opts.animInSpeed, function() {
          var buttons = $(this).find(".vrtx-button, .vrtx-button-small").filter(":visible");
          if(buttons.length) {
            $("<a class='tip-focusable' style='display: inline-block; outline: none;' tabindex='-1' />").insertBefore(buttons.filter(":first")).focus();
          }
        });
        if (opts.extra) {
          tipExtra.css(ePos).fadeIn(opts.animInSpeed);
        }
        if(opts.enterOpens && e.type == "keypress") {
          tip.on("focusout keypress", function (e) {
            if(hoverTip) return;
            if(e.type == "focusout") {
              waitForKeyUp = window.setTimeout(function() {
                var e2 = jQuery.Event("keypress");
                e2.which = keyTriggersOpen;
                link.trigger(e2);
              }, 200);
            } else {
              if(waitForKeyUp) {
                window.clearTimeout(waitForKeyUp)
              }
            }
          });
        }
        e.stopPropagation();
      } else if (e.type == "mouseleave" || (opts.enterOpens ? (((e.which && e.which == keyTriggersOpen) || (e.keyCode && e.keyCode == keyTriggersOpen)) && toggleOn) : (e.type == "focusout"))) {
        var link = $(this);
        if(opts.expandHoverToTipBox) {
          tip.on("mouseenter" + (opts.enterOpens ? "" : " focusin"), function (e) {
            hoverTip = true;
          });
          tip.on("mouseleave" + (opts.enterOpens ? "" : " focusout"), function (e) {
            hoverTip = false;
            tip.off("mouseenter" + (opts.enterOpens ? "" : " focusin"));
            tip.off("mouseleave" + (opts.enterOpens ? "" : " focusout"));
            link.trigger("mouseleave");
          });
        }
        if(!(opts.expandHoverToTipBox && hoverTip)) {
          toggleOn = false;
          if (typeof link.attr("href") === "undefined" && !link.is("abbr")) {
            link = link.find("a");
          }
          link.attr('title', tipText);
          if(e.type == "keypress") {
            link.focus();
          }
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
          }, opts.expandHoverToTipBox ? 350 : 0);
          e.stopPropagation();
        }
      }
    };
    
    $(this).on("mouseenter mouseleave" + (opts.enterOpens ? " keypress" : " focusin focusout"), subSelector, openCloseTooltip);
  }
})(jQuery);
