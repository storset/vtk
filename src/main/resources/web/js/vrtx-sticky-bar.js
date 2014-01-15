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
    var extraWidth = opts.extraWidth || 0;
    var contents = $(opts.contentsId);
    var main = $(opts.outerContentsId);
      
    var wrapper = $(wrapperId);
    var thisWindow = $(window);
    var ua = navigator.userAgent.toLowerCase();
    if (wrapper.length && !/iphone/.test(ua)) { // Turn off for iPhone. 
      var wrapperPos = wrapper.offset();
      if (navigator.appName == "Microsoft Internet Explorer" && /msie 8/.test(ua)) { // Shadow below in IE8
        wrapper.append("<span class='sticky-bg-ie8-below'></span>");
      }
      thisWindow.on("scroll", function () {
        if (thisWindow.scrollTop() >= wrapperPos.top + 1) {
          if (!wrapper.hasClass(stickyClass)) {
            wrapper.addClass(stickyClass);
            contents.css("paddingTop", wrapper.outerHeight(true) + "px");
          }
          wrapper.css("width", (main.outerWidth(true) - 2 + extraWidth) + "px");
        } else {
          if (wrapper.hasClass(stickyClass)) {
            wrapper.removeClass(stickyClass);
            wrapper.css("width", "auto");
            contents.css("paddingTop", "0px");
          }
        }
      });
      thisWindow.on("resize", function () {
        if (thisWindow.scrollTop() >= wrapperPos.top + 1) {
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
      wrapper.css("width", "auto");
      $(this.__opts.contentsId).css("paddingTop", "0px");
    }
  }        
});