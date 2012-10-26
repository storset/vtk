/* Dialogs and interface to jQuery UI */

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
      width: 208,
      height: 60
    });
  },
  openHtmlDialog: function openHtmlDialog(html, title) {
    this.openDialog("#dialog-html", {
      msg: html,
      title: title,
      hasHtml: true,
      width: 600,
      height: 395
    });
  },
  openMsgDialog: function openMsgDialog(msg, title) {
    this.openDialog("#dialog-message", {
      msg: msg,
      title: title,
      hasOk: true
    });
  },
  openConfirmDialog: function openConfirmDialog(msg, title, funcOkComplete, funcCancelComplete, funcOkCompleteOpts) {
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
  openDialog: function openDialog(selector, opts) {
    var elm = $(selector);
    if (!elm.length) {
      if (opts.title) {
        $("body").append("<div id='" + selector.substring(1) + "' title='" + opts.title + "'><div id='" + selector.substring(1) + "-content'>" + (opts.hasHtml ? "<p>" + opts.msg + "</p>" : opts.msg) + "</div></div>");
      } else {
        $("body").append("<div id='" + selector.substring(1) + "'><div id='" + selector.substring(1) + "-content'>" + (opts.hasHtml ? "<p>" + opts.msg + "</p>" : opts.msg) + "</div></div>");
      }
      elm = $(selector); // Re-query DOM after appending html
      var l10nButtons = {};
      if (opts.hasOk) {
        l10nButtons["Ok"] = function() {
	      $(this).dialog("close");
	      if(opts.funcOkComplete) opts.funcOkComplete(opts.funcOkCompleteOpts);
        };
      }
      if (opts.hasCancel) {
        var Cancel = (typeof cancelI18n != "undefined") ? cancelI18n : "Cancel";
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
                               $(".ui-dialog-titlebar-close", ctx).hide();
                               $(".ui-dialog-titlebar", ctx).addClass("closable");
                             };
                           }       
                         
      elm.dialog(dialogOpts);
    } else {
      if(opts.title) {
        elm.prev().find("#ui-dialog-title-" + selector.substring(1)).html(opts.title); 
      }
      elm.find(selector + "-content").html(opts.hasHtml ? "<p>" + opts.msg + "</p>" : opts.msg);
    }
    elm.dialog("open");
  }
};

/* ^ Dialogs and interface to jQuery UI */
