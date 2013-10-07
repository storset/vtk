/*
 *  Dialogs and interface to jQuery UI
 */

var VrtxSimpleDialogInterface = dejavu.Interface.declare({
  $name: "VortexSimpleDialogInterface"
});

var AbstractVrtxSimpleDialog = dejavu.AbstractClass.declare({
  $name: "AbstractVortexSimpleDialog", // Meta-attribute useful for debugging
  $implements: [VrtxSimpleDialogInterface],
  initialize: function() {     // Constructor
  },
  _openDialog: function (selector, opts) {
    var elm = $(selector);
    if (!elm.length) {
      if (opts.title) {
        $("body").append("<div id='" + selector.substring(1) + "' title='" + opts.title + "'><div id='" + selector.substring(1) + "-content'>" + (!opts.hasHtml ? "<p>" + opts.msg + "</p>" : opts.msg) + "</div></div>");
      } else {
        $("body").append("<div id='" + selector.substring(1) + "'><div id='" + selector.substring(1) + "-content'>" + (!opts.hasHtml ? "<p>" + opts.msg + "</p>" : opts.msg) + "</div></div>");
      }
      elm = $(selector); // Re-query DOM after appending html
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
      var dialogOpts =     { modal: true,                        // Defaults
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
      elm.dialog(dialogOpts);
    } else {
      if(opts.title) {
        elm.prev().find(".ui-dialog-title").html(opts.title); 
      }
      elm.find(selector + "-content").html(!opts.hasHtml ? "<p>" + opts.msg + "</p>" : opts.msg);
    }
    elm.dialog("open");
  },
  destroyDialog: function (classOrId) {
    var elem = $(classOrId);
    elem.dialog("destroy");
    elem.remove();
  },
  closeDialog: function (classOrId) {
    $(classOrId).dialog("close"); 
  }          
});

var VrtxLoadingDialog = dejavu.Class.declare({
  $name: "VrtxLoadingDialog",
  $extends: AbstractVrtxSimpleDialog,
  initialize: function () {
    this.$super();
  },
  open: function (title) {
    this._openDialog("#dialog-loading", {
      msg: "<img src='/vrtx/__vrtx/static-resources/themes/default/images/loadingAnimation.gif' alt='Loading icon' />",
      title: title,
      hasHtml: true,
      unclosable: true,
      width: 208
    });
  }
});

var VrtxHtmlDialog = dejavu.Class.declare({
  $name: "VrtxHtmlDialog",
  $extends: AbstractVrtxSimpleDialog,
  initialize: function () {
    this.$super();
  },
  open: function (name, html, title, width, height, funcOkComplete, funcOkCompleteOpts, btnTextOk, btnTextCancel, funcOnOpen) {
    this._openDialog("#dialog-html-" + name, {
      msg: html,
      title: title,
      hasHtml: true,
      width: width,
      height: height,
      funcOkComplete: (typeof funcOkComplete !== "undefined") ? funcOkComplete : null,
      funcOkCompleteOpts: (typeof funcOkCompleteOpts !== "undefined") ? funcOkCompleteOpts : null,
      hasOk: (typeof btnTextOk !== "undefined"),
      hasCancel: (typeof btnTextCancel !== "undefined"),
      btnTextOk: (typeof btnTextOk !== "undefined") ? btnTextOk : "Ok",
      btnTextCancel: (typeof btnTextCancel !== "undefined") ? btnTextCancel : null,
      funcOnOpen: (typeof funcOnOpen !== "undefined") ? funcOnOpen : null
    });
  }
});

var VrtxMsgDialog = dejavu.Class.declare({
  $name: "VrtxMsgDialog",
  $extends: AbstractVrtxSimpleDialog,
  initialize: function () {
    this.$super();
  },
  open: function (msg, title) {
    this._openDialog("#dialog-message", {
      msg: msg,
      title: title,
      hasOk: true
    });
  }
});

var VrtxConfirmDialog = dejavu.Class.declare({
  $name: "VrtxConfirmDialog",
  $extends: AbstractVrtxSimpleDialog,
  initialize: function () {
    this.$super();
  },
  open: function (msg, title, funcOkComplete, funcCancelComplete, funcOkCompleteOpts) {
    this.destroyDialog("#dialog-confirm");
    this._openDialog("#dialog-confirm", { 
      msg: msg,
      title: title,
      hasOk: true,
      hasCancel: true,
      funcOkComplete: funcOkComplete,
      funcOkCompleteOpts: funcOkCompleteOpts,
      funcCancelComplete: funcCancelComplete
    });
  }
});

/* ^ Dialogs and interface to jQuery UI */