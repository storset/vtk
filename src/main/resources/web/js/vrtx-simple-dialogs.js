/*
 *  VrtxSimpleDialog - facade to jQuery UI dialogs
 *  
 *  API: http://api.jqueryui.com/dialog/
 *
 *  * Requires Dejavu OOP library
 *  * Requires but Lazy-loads jQuery UI library (if not defined) on open
 *  * Lazy-loads Tree and Datepicker libraries (if not defined) on open if:
 *     - requiresTree: true
 *     - requiresDatepicker: true
 */

var VrtxSimpleDialogInterface = dejavu.Interface.declare({
  $name: "VrtxSimpleDialogInterface",
  open: function() {},
  close: function() {},
  destroy: function() {}
});

var AbstractVrtxSimpleDialog = dejavu.AbstractClass.declare({
  $name: "AbstractVrtxSimpleDialog",        // Meta-attribute useful for debugging
  $implements: [VrtxSimpleDialogInterface],
  __opts: {},
  __dialogOpts: {},
  initialize: function(opts) {              // Constructor
      this.__opts = opts;
      this.__addDOM();
      var dialogOpts =     { modal: true,                        
                             autoOpen: false,
                             resizable: false,
                             buttons: this.__generateButtons() };
      if (opts.width)      { dialogOpts.width = opts.width; }
      if (opts.height)     { dialogOpts.height = opts.height; }
      if (opts.unclosable) { dialogOpts.closeOnEscape = false; }
                             var dialog = this;
                             dialogOpts.open = function(e, ui) {
                               var ctx = $(this).parent();
                               if (dialog.__opts.unclosable) {
                                 ctx.find(".ui-dialog-titlebar-close").hide();
                                 ctx.find(".ui-dialog-titlebar").addClass("closable");
                               }
                               if(dialog.__opts.onOpen) dialog.__opts.onOpen();
                               if(dialog.__opts.cancelIsNotAButton) {
                                 ctx.find(".ui-dialog-buttonpane button:last-child span").unwrap().addClass("cancel-is-not-a-button");
                               }
                               var inputs = ctx.find("textarea, input[type='text'], select").filter(":visible");
                               if(inputs.length) {
                                 inputs.filter(":first")[0].focus();
                               } else {
                                 input = ctx.find(".ui-dialog-buttonpane, .vrtx-focus-button, .vrtx-button, .vrtx-button-small").filter(":visible").filter(":first");
                                 if(input.length) {
                                   $("<a style='outline: none;' tabindex='-1' />").insertBefore(input)[0].focus();
                                 }
                               }
                             };
      this.__dialogOpts = dialogOpts;
  },
  __generateButtons: function() {
    var buttons = {};
    if (this.__opts.hasOk) {
      var ok = this.__opts.btnTextOk || "Ok";
      var dialog = this;
      buttons[ok] = function() {
        $(this).dialog("close");
        if(dialog.__opts.onOk) dialog.__opts.onOk(dialog.__opts.onOkOpts);
      };
    }
    if(this.__opts.extraBtns) {
      for(var i = 0, len = this.__opts.extraBtns.length; i < len; i++) {
        var extraBtn = this.__opts.extraBtns[i];
        buttons[extraBtn.btnText] = function() {
          $(this).dialog("close");
          if(extraBtn.onOk) extraBtn.onOk(extraBtn.onOkOpts);
        };
      }
    }
    if (this.__opts.hasCancel) {
      var cancel = this.__opts.btnTextCancel || ((typeof cancelI18n != "undefined") ? cancelI18n : "Cancel");
      if(/^\(/.test(cancel)) {
        this.__opts.cancelIsNotAButton = true;
      }
      var dialog = this;
      buttons[cancel] = function() {
        $(this).dialog("close");
        if(dialog.__opts.onCancel) dialog.__opts.onCancel();
      };
    }
    return buttons;
  },
  __addDOM: function() {
    $(".vrtx-dialog").remove();
    var html = "<div class='vrtx-dialog' id='" + this.__opts.selector.substring(1) + "'";
    if (this.__opts.title) {
      html += " title='" + this.__opts.title + "'";
    }
    $("body").append(html + "><div id='" + this.__opts.selector.substring(1) + "-content'>" + (!this.__opts.hasHtml ? "<p>" + this.__opts.msg + "</p>" : this.__opts.msg) + "</div></div>");
    $(".vrtx-dialog").hide();
  },
  open: function () {
    var dialog = this;
    
    // TODO: rootUrl and jQueryUiVersion should be retrieved from Vortex config/properties somehow
    var rootUrl = "/vrtx/__vrtx/static-resources";
    var jQueryUiVersion = "1.10.4";
    
    var futureUi = $.Deferred();
    if (typeof $.ui === "undefined") {
      $.getScript(rootUrl + "/jquery/plugins/ui/jquery-ui-" + jQueryUiVersion + ".custom/js/jquery-ui-" + jQueryUiVersion + ".custom.min.js", function () {
        futureUi.resolve();
      });
    } else {
      futureUi.resolve();
    }
    $.when(futureUi).done(function() {
      dialog.__opts.elm = $(dialog.__opts.selector);
      dialog.__opts.elm.dialog(dialog.__dialogOpts);
      dialog.__opts.elm.dialog("open");
    });
  },
  close: function () {
    $(".ui-dialog-content").filter(":visible").dialog("close");
  },
  destroy: function () {
    $(".ui-dialog-content").filter(":visible").dialog("destroy");
    this.__opts.elm.remove();
  }        
});

var VrtxLoadingDialog = dejavu.Class.declare({
  $name: "VrtxLoadingDialog",
  $extends: AbstractVrtxSimpleDialog,
  initialize: function (opts) {
    this.$super({
      selector: "#dialog-loading",
      msg: "<img src='/vrtx/__vrtx/static-resources/themes/default/images/loadingAnimation.gif' alt='Loading icon' />",
      title: opts.title,
      hasHtml: true,
      unclosable: true,
      width: 208
    });
  }
});

var VrtxHtmlDialog = dejavu.Class.declare({
  $name: "VrtxHtmlDialog",
  $extends: AbstractVrtxSimpleDialog,
  initialize: function (opts) {
    this.$super({
      selector: "#dialog-html-" + opts.name,
      msg: opts.html,
      title: opts.title,
      hasHtml: true,
      width: opts.width,
      height: opts.height,
      onOk: opts.onOk,
      onOkOpts: opts.onOkOpts,
      hasOk: opts.btnTextOk,
      hasCancel: opts.btnTextCancel,
      btnTextOk: opts.btnTextOk,
      btnTextCancel: opts.btnTextCancel,
      requiresDatepicker: opts.requiresDatepicker,
      onOpen: opts.onOpen
    });
  }
});

var VrtxMsgDialog = dejavu.Class.declare({
  $name: "VrtxMsgDialog",
  $extends: AbstractVrtxSimpleDialog,
  initialize: function (opts) {
    this.$super({
      selector: "#dialog-message",
      msg: opts.msg,
      title: opts.title,
      width: opts.width,
      height: opts.height,
      hasOk: true
    });
  }
});

var VrtxConfirmDialog = dejavu.Class.declare({
  $name: "VrtxConfirmDialog",
  $extends: AbstractVrtxSimpleDialog,
  initialize: function (opts) {
    this.$super({ 
      selector: "#dialog-confirm",
      msg: opts.msg,
      title: opts.title,
      width: opts.width,
      height: opts.height,
      hasOk: true,
      hasCancel: true,
      btnTextOk: opts.btnTextOk,
      btnTextCancel: opts.btnTextCancel,
      onOk: opts.onOk,
      onOkOpts: opts.onOkOpts,
      onCancel: opts.onCancel,
      extraBtns: opts.extraBtns
    });
  }
});