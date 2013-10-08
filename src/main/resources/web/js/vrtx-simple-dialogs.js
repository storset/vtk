/*
 *  Dialogs and interface to jQuery UI
 */

var VrtxSimpleDialogInterface = dejavu.Interface.declare({
  $name: "VortexSimpleDialogInterface"
});

var AbstractVrtxSimpleDialog = dejavu.AbstractClass.declare({
  $name: "AbstractVortexSimpleDialog",      // Meta-attribute useful for debugging
  $implements: [VrtxSimpleDialogInterface],
  initialize: function(opts) {              // Constructor
      this.__addDOM(opts)
      var l10nButtons = {};
      if (opts.hasOk) {
        var btnTextOk = opts.btnTextOk || "Ok";
        l10nButtons[btnTextOk] = function() {
          $(this).dialog("close");
          if(opts.funcOkComplete) opts.funcOkComplete(opts.funcOkCompleteOpts);
        };
      }
      if (opts.hasCancel) {
        var Cancel = opts.btnTextCancel || ((typeof cancelI18n != "undefined") ? cancelI18n : "Cancel");
        if(/^\(/.test(Cancel)) {
          opts.cancelIsNotAButton = true;
        }
        l10nButtons[Cancel] = function() {
          $(this).dialog("close");
          if(opts.funcCancelComplete) opts.funcCancelComplete();
        };
      }
      var dialogOpts =     { selector: opts.selector,
                             modal: true,                        
                             autoOpen: false,
                             resizable: false,
                             buttons: l10nButtons };
      if (opts.width)      { dialogOpts.width = opts.width; }
      if (opts.height)     { dialogOpts.height = opts.height; }
      if (opts.unclosable) { dialogOpts.closeOnEscape = false;   // TODO: used only for loading dialog yet
                             dialogOpts.open = function(e, ui) { 
                               var ctx = $(this).parent();
                               ctx.find(".ui-dialog-titlebar-close").hide();
                               ctx.find(".ui-dialog-titlebar").addClass("closable");
                               if(opts.funcOnOpen) opts.funcOnOpen();
                             };
                           } else {
                             if(opts.funcOnOpen || opts.cancelIsNotAButton) {
                               dialogOpts.open = function(e, ui) {
                                 if(opts.funcOnOpen) {
                                   opts.funcOnOpen();
                                 }
                                 if(opts.cancelIsNotAButton) {
                                   var ctx = $(this).parent();
                                   ctx.find(".ui-dialog-buttonpane button:last-child span").unwrap().addClass("cancel-is-not-a-button");
                                 }
                               };
                             }
                           }
      this.opts = dialogOpts;
  },
  opts: {},
  __addDOM: function(opts) {
    $(".vrtx-dialog").remove();
    if (opts.title) {
      $("body").append("<div class='vrtx-dialog' id='" + opts.selector.substring(1) + "' title='" + opts.title + "'><div id='" + opts.selector.substring(1) + "-content'>" + (!opts.hasHtml ? "<p>" + opts.msg + "</p>" : opts.msg) + "</div></div>");
    }
    $("body").append("<div class='vrtx-dialog' id='" + opts.selector.substring(1) + "'><div id='" + opts.selector.substring(1) + "-content'>" + (!opts.hasHtml ? "<p>" + opts.msg + "</p>" : opts.msg) + "</div></div>");
    $(".vrtx-dialog").hide();
  },
  open: function () {
    this.opts.elm = $(this.opts.selector);
    this.opts.elm.dialog(this.opts);
    this.opts.elm.dialog("open");
  },
  destroyDialog: function () {
    this.opts.elm.dialog("destroy");
    this.opts.elm.remove();
  },
  closeDialog: function () {
    this.opts.elm.dialog("close");
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
      btnTextOk: (opts.btnTextOk) ? opts.btnTextOk : "Ok",
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