/*
 *  VrtxAccordion - facade to jQuery UI accordions
 *  
 *  API: http://api.jqueryui.com/accordion/
 *  
 *  * Requires Dejavu OOP library
 *  
 *  TODO: add function for adding header populators in elem
 */

var VrtxAccordionInterface = dejavu.Interface.declare({
  $name: "VrtxAccordionInterface"
});

var VrtxAccordion = dejavu.Class.declare({
  $name: "VrtxAccordion",
  $implements: [VrtxAccordionInterface],
  $constants: {
    headerMultipleCheckClass: ".header-empty-check-and",
    headerSingleCheckClass: ".header-empty-check-or",
    headerPopulatorsClass: ".header-populators",
    headerPopulatorsFallbackClass: ".header-fallback-populator"
  },
  __opts: {},
  initialize: function (opts) {
    this.__opts = opts;
  },
  create: function() {
    var accordion = this;
    accordion.destroy(); // Destroy if already exists
    accordion.__opts.elem.accordion({
      header: accordion.__opts.headerSelector,
      heightStyle: "content",
      collapsible: true,
      active: accordion.__opts.activeElem ? accordion.__opts.activeElem : false,
      activate: function (e, ui) {
        if(accordion.__opts.onActivate) accordion.__opts.onActivate(e, ui, accordion);
      }
    });
  },
  destroy: function() {
    if(this.__opts.elem.hasClass("ui-accordion")) {
      this.__opts.elem.accordion("destroy");
    }
  },
  refresh: function() {
    this.__opts.elem.accordion("refresh");
  },
  closeActiveHidden: function() {
    var active = this.__opts.elem.find(".ui-state-active");
    if (active.length && active.filter(":hidden").length) {
      this.__opts.elem.accordion("option", "active", false);
    }
  },
  updateHeader: function(elem, isJson, init) {
    var tree = this;
    
    var getString = function(input) {
      var inputId = input.id;
      if (isCkEditor(inputId)) { // Check if CK
        str = getCkValue(inputId); // Get CK content
      } else {
        str = input.value; // Get input text
      }
      return str;
    };
    
    var findMultipleForContentMatch = function(elm) {
      var containers = elm.find(tree.$static.headerMultipleCheckClass);
      var i = containers.length;
      for(;i--;) {
        var inputs = $(containers[i]).find("input[type='text'], textarea");
        var j = inputs.length;
        for(;j--;) {
          if("" === getString(inputs[j])) { // All need to have content for match
            return false;
          }
        }
        return true;
      }
    };
    
    var findSingleForContentMatch = function(elm) {
      var inputs = elm.find(tree.$static.headerSingleCheckClass + " input[type='text'], " + tree.$static.headerSingleCheckClass + " textarea");
      var i = inputs.length;
      for(;i--;) {
        if("" !== getString(inputs[i])) { // One need to have content for match
          return true;
        }
      }
    };
    
    var noContentOrNoTitle = function() {
      var lang = (vrtxAdmin !== "undefined") ? vrtxAdmin.lang : $("body").attr("lang");
      if(findMultipleForContentMatch(elm) || findSingleForContentMatch(elm)) {
        return (lang !== "en") ? "Ingen tittel" : "No title"; 
      } else {
        return (lang !== "en") ? "Intet innhold" : "No content";  
      }
    };

    if (typeof elem.closest !== "function") elem = $(elem);
    var elm = isJson ? elem.closest(".vrtx-json-element") 
                     : elem.closest(".vrtx-grouped"); // XXX: extract
    if (elm.length) { // Header populators
      var str = "";
      var fields = elm.find(tree.$static.headerPopulatorsClass);
      if (!fields.length) return;
      for (var i = 0, len = fields.length; i < len; i++) {
        var val = fields[i].value;
        if (!val.length) continue;
        str += (str.length) ? ", " + val : val;
      }
      if (!str.length) { // Fallback header populator
        var field = elm.find(tree.$static.headerPopulatorsFallbackClass);
        if (field.length) {
          var fieldId = field.attr("id");
          if (isCkEditor(fieldId)) { // Check if CK
            str = getCkValue(fieldId); // Get CK content
          } else {
            str = field.val(); // Get input text
          }
          if (field.is("textarea")) { // Remove markup and tabs
            str = $.trim(str.replace(/(<([^>]+)>|[\t\r]+)/ig, ""));
          }
          if (typeof str !== "undefined") {
            if (str.length > 30) {
              str = str.substring(0, 30) + "...";
            } else if (!str.length) {
              str = noContentOrNoTitle();
            }
          }
        } else {
          str = noContentOrNoTitle();
        }
      }
      var header = elm.find("> .header");
      if (!header.length) {
        elm.prepend('<div class="header">' + str + '</div>');
      } else {
        if (!isJson && init) {
          header.data("origText", header.text());
        }
        header.html('<span class="ui-accordion-header-icon ui-icon ui-icon-triangle-1-e"></span>' + (!isJson ? header.data("origText") + " - " : "") + str);
      }
    }
  }
});