/*
 *  VrtxStickyBar
 *  
 *  * Requires Dejavu OOP library
 */

var VrtxStickyBarInterface = dejavu.Interface.declare({
  $name: "VrtxStickyBarInterface",
  enable: function () {},
  disable: function () {},
  destroy: function () {}     
});

var VrtxStickyBar = dejavu.Class.declare({
  $name: "VrtxStickyBar",
  $implements: [VrtxStickyBarInterface],
  __opts: {},
  initialize: function(opts) {
      this.__opts = opts;

  },
  enable: function () {
  },
  disable: function () {
  },
  destroy: function () {
  }        
});