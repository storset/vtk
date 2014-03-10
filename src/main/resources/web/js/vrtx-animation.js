/* 
 * VrtxAnimation
 *  
 *  API: http://api.jqueryui.com/accordion/
 *  
 *  * Requires Dejavu OOP library
 */
 
var VrtxAnimationInterface = dejavu.Interface.declare({
  $name: "VrtxAnimationInterface",
  __opts: {},
  __prepareHorizontalMove: function() {},
  __horizontalMove: function() {},
  update: function(opts) {},
  updateElem: function(elem) {},
  rightIn: function() {},
  leftOut: function() {},
  topDown: function() {},
  bottomUp: function() {}
});
 
var VrtxAnimation = dejavu.Class.declare({
  $name: "VrtxAnimation",
  $implements: [VrtxAnimationInterface],
  $constants: {
    // TODO: remove vrtxAdmin dependency
    animationSpeed: (typeof vrtxAdmin !== "undefined" && vrtxAdmin.isMobileWebkitDevice) ? 0 : 200,
    easeIn: (typeof vrtxAdmin !== "undefined" && !(vrtxAdmin.isIE && vrtxAdmin.browserVersion < 10) && !vrtxAdmin.isMobileWebkitDevice) ? "easeInQuad" : "linear",
    easeOut: (typeof vrtxAdmin !== "undefined" && !(vrtxAdmin.isIE && vrtxAdmin.browserVersion < 10) && !vrtxAdmin.isMobileWebkitDevice) ? "easeOutQuad" : "linear"
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
  __horizontalMove: function(left, easing) {
    var animation = this;
    animation.__opts.elem.animate({
      "marginLeft": left + "px"
    }, animation.__opts.animationSpeed || animation.$static.animationSpeed, easing, function() {
      if(animation.__opts.outerWrapperElem) animation.__opts.outerWrapperElem.removeClass("overflow-hidden");
      if(animation.__opts.after) animation.__opts.after(animation);
      // TODO: closures pr. direction if needed also for horizontal animation
      if(animation.__opts.afterIn) animation.__opts.afterIn(animation);
      if(animation.__opts.afterOut) animation.__opts.afterOut(animation);
    });
  },
  update: function(opts) {
    this.__opts = opts;
  },
  updateElem: function(elem) {
    this.__opts.elem = elem;
  },
  rightIn: function() {
    var width = this.__prepareHorizontalMove();
    this.__opts.elem.css("marginLeft", -width);
    this.__horizontalMove(0, this.__opts.easeIn || this.$static.easeIn);
  },
  leftOut: function() {
    var width = this.__prepareHorizontalMove();
    this.__horizontalMove(-width, this.__opts.easeOut || this.$static.easeOut);
  },
  topDown: function() {
    var animation = this;
    animation.__opts.elem.slideDown(
        animation.__opts.animationSpeed || animation.$static.animationSpeed,
        animation.__opts.easeIn || animation.$static.easeIn, function() {
      if(animation.__opts.after) animation.__opts.after(animation);
      if(animation.__opts.afterIn) animation.__opts.afterIn(animation);
    });
  },
  bottomUp: function() {
    var animation = this;
    animation.__opts.elem.slideUp(
        animation.__opts.animationSpeed || animation.$static.animationSpeed, 
        animation.__opts.easeOut || animation.$static.easeOut, function() {
      if(animation.__opts.after) animation.__opts.after(animation);
      if(animation.__opts.afterOut) animation.__opts.afterOut(animation);
    });
  }
});