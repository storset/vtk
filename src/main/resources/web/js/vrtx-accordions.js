/*
 *  VrtxAccordion - facade to jQuery UI accordions
 *  
 *  API: http://api.jqueryui.com/accordion/
 *  
 *  TODO: generalize more function updateHeader()
 *  TODO: add function for adding header populators in elem
 */

var VrtxAccordionInterface = dejavu.Interface.declare({
  $name: "VrtxAccordionInterface"
});

var VrtxAccordion = dejavu.Class.declare({
  $name: "VrtxAccordion",
  $implements: [VrtxAccordionInterface],
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
    if(accordion.__opts.elem.hasClass("ui-accordion")) {
      accordion.__opts.elem.accordion("destroy");
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
    
    var emptyCheckAND = function(elm) { // XXX: Make more general - assumption inputs
      var checkAND = elm.find(".header-empty-check-and");
      var i = checkAND.length;
      if(i > 0) {
        for(;i--;) {
          var inputs = $(checkAND[i]).find("input[type='text']");
          var j = inputs.length;
          var allOfThem = true;
          for(;j--;) {
            if(inputs[j].value === "") {
              allOfThem = false;
              break;
            }
          }
          if(allOfThem) { // Find 1 with all values - return !empty
            return false;
          }
        }
      }
      return true;
    };
    
    var emptyCheckOR = function(elm) { // XXX: Make more general - assumption CK and single
      var checkOR = elm.find(".header-empty-check-or textarea");
      var i = checkOR.length;
      if(i > 0) {
        var oneOfThem = false;
        for(;i--;) {
          var inputId = checkOR[i].id;
          var str = "";
          if (isCkEditor(inputId)) { // Check if CK
            str = getCkValue(inputId); // Get CK content
          }
          if(str !== "") {
            oneOfThem = true;
            break;
          }
        }
        if(oneOfThem) { // Find 1 with one value - return !empty
          return false;
        }
      }
      return true;
    };
    
    var noContentOrNoTitle = function() {
      var lang = (vrtxAdmin !== "undefined") ? vrtxAdmin.lang : $("body").attr("lang");
      if(!emptyCheckAND(elm) || !emptyCheckOR(elm)) {
        return (lang !== "en") ? "Ingen tittel" : "No title"; 
      } else {
        return (lang !== "en") ? "Intet innhold" : "No content";  
      }
    };

    if (typeof elem.closest !== "function") elem = $(elem);
    var elm = isJson ? elem.closest(".vrtx-json-element") : elem.closest(".vrtx-grouped");
    if (elm.length) { // Prime header populators
      var str = "";
      var fields = elm.find(".header-populators");
      if (!fields.length) return;
      for (var i = 0, len = fields.length; i < len; i++) {
        var val = fields[i].value;
        if (!val.length) continue;
        str += (str.length) ? ", " + val : val;
      }
      if (!str.length) { // Fallback header populator
        var field = elm.find(".header-fallback-populator");
        if (field.length) {
          var fieldId = field.attr("id");
          if (isCkEditor(fieldId)) { // Check if CK
            str = getCkValue(fieldId); // Get CK content
          } else {
            str = field.val();
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