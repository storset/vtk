/*
 *  Dialogs facade to jQuery UI
 *
 *  * Uses Dejavu OOP
 *  * Lazy-loads jQuery UI (if not defined) on open
 *
 */

var VrtxSimpleDialogInterface = dejavu.Interface.declare({
  $name: "VortexSimpleDialogInterface",
  open: function() {},
  close: function() {},
  destroy: function() {}
});

var AbstractVrtxSimpleDialog = dejavu.AbstractClass.declare({
  $name: "AbstractVortexSimpleDialog",      // Meta-attribute useful for debugging
  $implements: [VrtxSimpleDialogInterface],
  initialize: function(opts) {              // Constructor
      this.__opts = opts;
      this.__addDOM();
      var dialogOpts =     { modal: true,                        
                             autoOpen: false,
                             resizable: false,
                             buttons: this.__generateOkCancel() };
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
                               if(dialog.__opts.funcOnOpen) dialog .__opts.funcOnOpen();
                               if(dialog.__opts.cancelIsNotAButton) {
                                 ctx.find(".ui-dialog-buttonpane button:last-child span").unwrap().addClass("cancel-is-not-a-button");
                               }
                             };
      this.__dialogOpts = dialogOpts;
  },
  __opts: {},
  __dialogOpts: {},
  __generateOkCancel: function() {
    var buttons = {};
    if (this.__opts.hasOk) {
      var ok = this.__opts.btnTextOk || "Ok";
      var dialog = this;
      buttons[ok] = function() {
        $(this).dialog("close");
        if(dialog.__opts.funcOkComplete) dialog.__opts.funcOkComplete(dialog.__opts.funcOkCompleteOpts);
      };
    }
    if (this.__opts.hasCancel) {
      var cancel = this.__opts.btnTextCancel || ((typeof cancelI18n != "undefined") ? cancelI18n : "Cancel");
      if(/^\(/.test(cancel)) {
        this.__opts.cancelIsNotAButton = true;
      }
      var dialog = this;
      buttons[cancel] = function() {
        $(this).dialog("close");
        if(dialog.__opts.funcCancelComplete) dialog.__opts.funcCancelComplete();
      };
    }
    return buttons;
  },
  __addDOM: function() {
    $(".vrtx-dialog").remove();
    if (this.__opts.title) {
      $("body").append("<div class='vrtx-dialog' id='" + this.__opts.selector.substring(1) + "' title='" + this.__opts.title + "'><div id='" + this.__opts.selector.substring(1) + "-content'>" + (!this.__opts.hasHtml ? "<p>" + this.__opts.msg + "</p>" : this.__opts.msg) + "</div></div>");
    }
    $("body").append("<div class='vrtx-dialog' id='" + this.__opts.selector.substring(1) + "'><div id='" + this.__opts.selector.substring(1) + "-content'>" + (!this.__opts.hasHtml ? "<p>" + this.__opts.msg + "</p>" : this.__opts.msg) + "</div></div>");
    $(".vrtx-dialog").hide();
  },
  open: function () {
    var dialog = this;
    var futureUiUrl = "/vrtx/__vrtx/static-resources/jquery/plugins/ui/jquery-ui-1.10.3.custom/js/jquery-ui-1.10.3.custom.min.js";
    var futureUi = (typeof $.ui === "undefined") ? $.getScript(futureUiUrl) : $.Deferred().resolve();
    $.when(futureUi).done(function() {
      dialog.__opts.elm = $(dialog.__opts.selector);
      dialog.__opts.elm.dialog(dialog.__dialogOpts);
      dialog.__opts.elm.dialog("open");
    });
  },
  close: function () {
    this.__opts.elm.dialog("close");
  },
  destroy: function () {
    this.__opts.elm.dialog("destroy");
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
      hasHtml: opts.true,
      width: opts.width,
      height: opts.height,
      funcOkComplete: opts.funcOkComplete,
      funcOkCompleteOpts: opts.funcOkCompleteOpts,
      hasOk: opts.btnTextOk,
      hasCancel: opts.btnTextCancel,
      btnTextOk: opts.btnTextOk,
      btnTextCancel: opts.btnTextCancel,
      funcOnOpen: opts.funcOnOpen
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
      hasOk: true,
      hasCancel: true,
      funcOkComplete: opts.funcOkComplete,
      funcOkCompleteOpts: opts.funcOkCompleteOpts,
      funcCancelComplete: opts.funcCancelComplete
    });
  }
});

/* ^ Dialogs and interface to jQuery UI */