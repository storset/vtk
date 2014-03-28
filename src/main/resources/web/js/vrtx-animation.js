/* 
 * VrtxAnimation
 *  
 *  API: http://api.jqueryui.com/accordion/
 *  
 *  * Requires Dejavu OOP library
 *
 *  Horizontal [rightIn() + leftOut()] - hides content and marginLeft it left (show) and then right (hide)
 *  Vertical [topDown() + bottomUp()] - height and padding down (show) and then up (hide)
 *                                    TODO: Same strategy as horizontal will make vertical smoother
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
      return null;
    })()
  },
  __opts: {},
  initialize: function(opts) {
    this.__opts = opts;
    var animation = this;
    animation.__opts.cssTransition = (function () {
      var props = {
        'WebkitTransform': '-webkit-transition',
        'MozTransform': '-moz-transition',
        'OTransform': '-o-transition',
        'msTransform': '-ms-transition',
        'transform': 'transition'
      };
      return props.hasOwnProperty(animation.$static.cssTransform) ? props[animation.$static.cssTransform] : null;
    })();
    animation.__opts.cssTransitionEnd = (function () {
      var props = {
        'WebkitTransform': 'webkitTransitionEnd',
        'MozTransform': 'transitionend',
        'OTransform': 'oTransitionEnd otransitionend',
        'msTransform': 'MSTransitionEnd',
        'transform': 'transitionend'
      };
      return props.hasOwnProperty(animation.$static.cssTransform) ? props[animation.$static.cssTransform] : null;
    })();
  },
  __prepareMove: function() {
    if(this.__opts.outerWrapperElem && !this.__opts.outerWrapperElem.hasClass("overflow-hidden")) {
      this.__opts.outerWrapperElem.addClass("overflow-hidden");
    }
    return [this.__opts.elem.outerWidth(true), this.__opts.elem.height()];
  },
  __afterMove: function(afterSp) {
    if(this.__opts.outerWrapperElem) this.__opts.outerWrapperElem.removeClass("overflow-hidden");
    if(this.__opts.after) this.__opts.after(this);
    if(this.__opts[afterSp]) this.__opts[afterSp](this);
  },
  __horizontalMove: function(dir) {
    var width = this.__prepareMove()[0];

    if(dir === "in") {
      this.__opts.elem.css("marginLeft", -width);
      var left = 0;
      var afterSp = "afterIn";
    } else {
      var left = -width;
      var afterSp = "afterOut";
    }

    var animation = this;
    if(animation.$static.cssTransform == null) {
      var easing = (dir === "in") ? "easeIn" : "easeOut";
      var speed = animation.__opts.animationSpeed || animation.$static.animationSpeed;
      animation.__opts.elem.animate({
        "marginLeft": left + "px"
      }, speed, animation.__opts[easing] || animation.$static[easing], function() {
        animation.__afterMove(afterSp);
      });
    } else {
      var easing = (dir === "in") ? "cubic-bezier(0.17, 0.04, 0.03, 0.94)" : "cubic-bezier(0.03, 0.94, 0.96, 0.83)";
      var speed = animation.__opts.animationSpeed || animation.$static.animationSpeed;
      var transition = animation.__opts.cssTransition;
      var wait = setTimeout(function() {
        animation.__opts.elem.css({
          transition: "all " + speed + "ms " + easing,
          "marginLeft": left
        });
        document.addEventListener(animation.__opts.cssTransitionEnd, function () {
          document.removeEventListener(animation.__opts.cssTransitionEnd, arguments.callee);
          animation.__afterMove(afterSp);
        }, false);
      }, 5);
    }
  },
  __verticalMove: function(dir) {
    var height = this.__prepareMove()[1];
    
    if(dir === "in") {
      var top = height;
      var afterSp = "afterIn";
    } else {
      var top = 0;
      var afterSp = "afterOut";
    }
    
    var animation = this;
    if(animation.$static.cssTransform == null) {
      var easing = (dir === "in") ? "easeIn" : "easeOut";
      var speed = animation.__opts.animationSpeed || animation.$static.animationSpeed;
      animation.__opts.elem[(dir === "in") ? "slideDown" : "slideUp"](
         speed, animation.__opts[easing] || animation.$static[easing], function() {
        animation.__afterMove(afterSp);
      });
    } else {
      var elm = animation.__opts.elem.is("tr") ? animation.__opts.elem.find('td > div')
                                               : animation.__opts.elem;
      if(dir === "in") {
        var easing = "cubic-bezier(0.17, 0.04, 0.03, 0.94)"; // http://cubic-bezier.com/#.17,.04,.03,.94
        animation.__opts.elem.show();
        elm.show();
        top = elm.height();
        paddingTop = elm.css("paddingTop");
        paddingBottom = elm.css("paddingBottom");
        elm.css("paddingTop", "0");
        elm.css("paddingBottom", "0");
        elm.css("height", "0");
      } else {
        var easing = "cubic-bezier(0.03, 0.94, 0.96, 0.83)"; // http://cubic-bezier.com/#.03,.94,.96,.83
        paddingTop = 0;
        paddingBottom = 0;
      }
      var speed = animation.__opts.animationSpeed || animation.$static.animationSpeed;
      var wait = setTimeout(function() {
        var transition = animation.__opts.cssTransition;
        elm.css({
          transition: "all " + speed + "ms " + easing,
          "overflow": "hidden",
          "height": top,
          "paddingTop": paddingTop,
          "paddingBottom": paddingBottom
        });
        document.addEventListener(animation.__opts.cssTransitionEnd, function () {
          document.removeEventListener(animation.__opts.cssTransitionEnd, arguments.callee);
          if(dir === "out") animation.__opts.elem.hide();
          animation.__afterMove(afterSp);
        }, false);
      }, 5);
    }
  },
  rightIn: function() {
    this.__horizontalMove("in");
  },
  leftOut: function() {
    this.__horizontalMove("out");
  },
  topDown: function() {
    this.__verticalMove("in");
  },
  bottomUp: function() {
    this.__verticalMove("out");
  },
  update: function(opts) {
    this.__opts = opts;
  },
  updateElem: function(elem) {
    this.__opts.elem = elem;
  }
});