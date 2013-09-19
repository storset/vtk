/*
 *  Dialogs and interface to jQuery UI
 *  XXX: This should not be a singleton/module but function/class as it is used for different dialogs..
 */


var vrtxSimpleDialogs = {
  closeDialog: function (classOrId) {
    $(classOrId).dialog("close"); 
  },
  openLoadingDialog: function (title) {
    this.openDialog("#dialog-loading", {
      msg: "<img src='/vrtx/__vrtx/static-resources/themes/default/images/loadingAnimation.gif' alt='Loading icon' />",
      title: title,
      hasHtml: true,
      unclosable: true,
      width: 208
    });
  },
  openHtmlDialog: function (name, html, title, width, height, funcOkComplete, funcOkCompleteOpts, btnTextOk, btnTextCancel, funcOnOpen) {
    this.openDialog("#dialog-html-" + name, {
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
  },
  openMsgDialog: function (msg, title) {
    this.openDialog("#dialog-message", {
      msg: msg,
      title: title,
      hasOk: true
    });
  },
  openConfirmDialog: function (msg, title, funcOkComplete, funcCancelComplete, funcOkCompleteOpts) {
    this.openDialog("#dialog-confirm", { 
      msg: msg,
      title: title,
      hasOk: true,
      hasCancel: true,
      funcOkComplete: funcOkComplete,
      funcOkCompleteOpts: funcOkCompleteOpts,
      funcCancelComplete: funcCancelComplete
    });
  },
  openDialog: function (selector, opts) {
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
                             if(opts.funcOnOpen) {
                               dialogOpts.open = function(e, ui) { 
                                 opts.funcOnOpen();
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
  }
};

/* ^ Dialogs and interface to jQuery UI */
