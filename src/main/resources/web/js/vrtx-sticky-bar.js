/*
 *  VrtxStickyBar
 *  
 *  * Requires Dejavu OOP library
 */

var VrtxStickyBarInterface = dejavu.Interface.declare({
  $name: "VrtxStickyBarInterface",
  destroy: function () {}     
});

var VrtxStickyBar = dejavu.Class.declare({
  $name: "VrtxStickyBar",
  $implements: [VrtxStickyBarInterface],
  __opts: {},
  initialize: function(opts) {
    this.__opts = opts;
    var wrapperId = opts.wrapperId;
    var stickyClass = opts.stickyClass;
    var contents = $(opts.contentsId);
    var main = $(opts.outerContentsId);
    var extraWidth = opts.extraWidth || 0;
    var stickFn = opts.stick || null;
    var unstickFn = opts.unstick || null;
      
    var wrapper = $(wrapperId);
    var thisWindow = $(window);
    var ua = navigator.userAgent.toLowerCase();
    if (wrapper.length && !/iphone/.test(ua)) { // Turn off for iPhone. 
      if (navigator.appName == "Microsoft Internet Explorer" && /msie 8/.test(ua)) { // Shadow below in IE8
        wrapper.append("<span class='sticky-bg-ie8-below'></span>");
      }

      var wrapperPos = wrapper.offset();
      if(opts.isBottomSticky) {
        wrapper.addClass("sticky-bottom");
      }
      var shouldStick = function() {
        if(opts.isBottomSticky) {
          return (thisWindow.scrollTop() + thisWindow.height()) <= (wrapperPos.top - 1 + wrapper.height());
        } else {
          return thisWindow.scrollTop() >= wrapperPos.top + 1;
        }
      };
      
      // Scroll and resize
      thisWindow.on("scroll", function () {
        if (shouldStick()) {
          if (!wrapper.hasClass(stickyClass)) {
            wrapper.addClass(stickyClass);
            contents.css("paddingTop", wrapper.outerHeight(true) + "px");
            if(stickFn != null) {
              stickFn();
            }
          }
          wrapper.css("width", (main.outerWidth(true) - 2 + extraWidth) + "px");
        } else {
          if (wrapper.hasClass(stickyClass)) {
            wrapper.removeClass(stickyClass);
            wrapper.css("width", "auto");
            contents.css("paddingTop", "0px");
            if(unstickFn != null) {
              unstickFn();
            }
          }
        }
      });
      thisWindow.on("resize", function () {
        if (shouldStick()) {
          wrapper.css("width", (main.outerWidth(true) - 2 + extraWidth) + "px");
        }
      });
      
    }
  },
  destroy: function () {
    var thisWindow = $(window);
    thisWindow.off("scroll");
    thisWindow.off("resize");
    var wrapper = $(this.__opts.wrapperId);
    if (wrapper.hasClass(this.__opts.stickyClass)) {
      wrapper.removeClass(this.__opts.stickyClass);
      if (wrapper.hasClass("sticky-bottom")) {
        wrapper.removeClass("sticky-bottom");
      }
      wrapper.css("width", "auto");
      $(this.__opts.contentsId).css("paddingTop", "0px");
    }
  }        
});