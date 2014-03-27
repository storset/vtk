/* 
 * VrtxAnimation
 *  
 *  API: http://api.jqueryui.com/accordion/
 *  
 *  * Requires Dejavu OOP library
 *
 *  TODO: Progressively support CSS3 animations (requires Modernizr)
 *
 */
 
var VrtxAnimationInterface = dejavu.Interface.declare({
  $name: "VrtxAnimationInterface",
  __opts: {},
  __prepareHorizontalMove: function() {},
  __horizontalMove: function(left, easing, afterSp) {},
  __verticalMove: function(fn, easing, afterSp) {},
  rightIn: function() {},
  leftOut: function() {},
  topDown: function() {},
  bottomUp: function() {},
  update: function(opts) {},
  updateElem: function(elem) {}
});
 
var VrtxAnimation = dejavu.Class.declare({
  $name: "VrtxAnimation",
  $implements: [VrtxAnimationInterface],
  $constants: {
    animationSpeed: /(iphone|ipad|android)/.test(navigator.userAgent.toLowerCase()) ? 0 : 200,
    easeIn: !/msie (8|9.)/.test(navigator.userAgent.toLowerCase()) ? "easeInQuad" : "linear",
    easeOut: !/msie (8|9.)/.test(navigator.userAgent.toLowerCase()) ? "easeOutQuad" : "linear",
    cssTransform: (function () {
      var propArray = ['transform', 'MozTransform', 'WebkitTransform', 'msTransform', 'OTransform'];
      var root = document.documentElement;
      for (var i = 0, len = propArray.length; i < len; i++) {
        if (propArray[i] in root.style){
          return propArray[i];
        }
      }
    })()
  },
  __opts: {},
  initialize: function(opts) {
    this.__opts = opts;
  },
  __prepareMove: function() {
    if(this.__opts.outerWrapperElem && !this.__opts.outerWrapperElem.hasClass("overflow-hidden")) {
      this.__opts.outerWrapperElem.addClass("overflow-hidden");
    }
    return [this.__opts.elem.outerWidth(true), this.__opts.elem.outerHeight(true)];
  },
  __afterMove: function(afterSp) {
    if(this.__opts.outerWrapperElem) this.__opts.outerWrapperElem.removeClass("overflow-hidden");
    if(this.__opts.after) this.__opts.after(this);
    if(this.__opts[afterSp]) this.__opts[afterSp](this);
  },
  __horizontalMove: function(fn, easing, afterSp) {
    var width = this.__prepareMove()[0];
    var left = (fn === "slideIn") ? 0 : -width;
    if(fn === "slideIn") {
      this.__opts.elem.css("marginLeft", -width);
    }
    
    var animation = this;
    animation.__opts.elem.animate({
      "marginLeft": left + "px"
    }, animation.__opts.animationSpeed || animation.$static.animationSpeed,
      animation.__opts[easing] || animation.$static[easing], function() {
      animation.__afterMove(afterSp);
    });
  },
  __verticalMove: function(fn, easing, afterSp) {
    var height = this.__prepareMove()[1];
    
    var animation = this;
    animation.__opts.elem[fn](
        animation.__opts.animationSpeed || animation.$static.animationSpeed, 
        animation.__opts[easing] || animation.$static[easing], function() {
      animation.__afterMove(afterSp);
    });
  },
  rightIn: function() {
    this.__horizontalMove("slideIn", "easeIn", "afterIn");
  },
  leftOut: function() {
    this.__horizontalMove("slideOut", "easeOut", "afterOut");
  },
  topDown: function() {
    this.__verticalMove("slideDown", "easeIn", "afterIn");
  },
  bottomUp: function() {
    this.__verticalMove("slideUp", "easeOut", "afterOut");
  },
  update: function(opts) {
    this.__opts = opts;
  },
  updateElem: function(elem) {
    this.__opts.elem = elem;
  }
});