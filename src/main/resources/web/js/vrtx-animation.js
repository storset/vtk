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
    easeOut: !/msie (8|9.)/.test(navigator.userAgent.toLowerCase()) ? "easeOutQuad" : "linear"
  },
  __opts: {},
  initialize: function(opts) {
    this.__opts = opts;
  },
  __prepareHorizontalMove: function() {
    if(this.__opts.outerWrapperElem && !this.__opts.outerWrapperElem.hasClass("overflow-hidden")) {
      this.__opts.outerWrapperElem.addClass("overflow-hidden");
    }
    return this.__opts.elem.outerWidth(true);
  },
  __horizontalMove: function(left, easing, afterSp) {
    var animation = this;
    animation.__opts.elem.animate({
      "marginLeft": left + "px"
    }, animation.__opts.animationSpeed || animation.$static.animationSpeed, this.__opts[easing] || this.$static[easing], function() {
      if(animation.__opts.outerWrapperElem) animation.__opts.outerWrapperElem.removeClass("overflow-hidden");
      if(animation.__opts.after) animation.__opts.after(animation);
      if(animation.__opts[afterSp]) animation.__opts[afterSp](animation);
    });
  },
  __verticalMove: function(fn, easing, afterSp) {
    var animation = this;
    animation.__opts.elem[fn](
        animation.__opts.animationSpeed || animation.$static.animationSpeed, 
        animation.__opts[easing] || animation.$static[easing], function() {
      if(animation.__opts.after) animation.__opts.after(animation);
      if(animation.__opts[afterSp]) animation.__opts[afterSp](animation);
    });
  },
  rightIn: function() {
    var width = this.__prepareHorizontalMove();
    this.__opts.elem.css("marginLeft", -width);
    this.__horizontalMove(0, "easeIn", "afterIn");
  },
  leftOut: function() {
    var width = this.__prepareHorizontalMove();
    this.__horizontalMove(-width, "easeOut", "afterOut");
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