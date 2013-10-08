/*
 *  Dialogs facade to jQuery UI
 *
 *  * Uses Dejavu OOP library
 *  * Lazy-loads jQuery UI library (if not defined) on open
 *  * Lazy-loads Tree and Datepicker libraries (if not defined) on open if:
 *     - requiresTree: true
 *     - requiresDatepicker: true
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
    var html = "<div class='vrtx-dialog' id='" + this.__opts.selector.substring(1) + "'";
    if (this.__opts.title) {
      html += " title='" + this.__opts.title + "'";
    }
    $("body").append(html + "><div id='" + this.__opts.selector.substring(1) + "-content'>" + (!this.__opts.hasHtml ? "<p>" + this.__opts.msg + "</p>" : this.__opts.msg) + "</div></div>");
    $(".vrtx-dialog").hide();
  },
  open: function () {
    var dialog = this;
    
    // TODO: these should be retrieved from Vortex config/properties somehow
    var futureRootUrl = "/vrtx/__vrtx/static-resources";
    var jQueryUiVersion = "1.10.3";
    
    var futureUi = $.Deferred();
    if (typeof $.ui === "undefined") {
      $.getScript("/jquery/plugins/ui/jquery-ui-" + jQueryUiVersion + ".custom/js/jquery-ui-" + jQueryUiVersion + ".custom.min.js", function () {
        futureUi.resolve();
      });
    } else {
      futureUi.resolve();
    }
    var futureTree = $.Deferred();
    if (typeof $.fn.treeview !== "function" && dialog.__opts.requiresTree) {
      $.getScript(location.protocol + "//" + location.host + futureRootUrl + "/jquery/plugins/jquery.treeview.js", function () {
        $.getScript(location.protocol + "//" + location.host + futureRootUrl + "/jquery/plugins/jquery.treeview.async.js", function () {
          $.getScript(location.protocol + "//" + location.host + futureRootUrl + "/jquery/plugins/jquery.scrollTo.min.js", function () {
            futureTree.resolve();
          });
        });
      });
    } else {
      futureTree.resolve();
    }
    var futureDatepicker = $.Deferred();
    if (typeof initDatePicker !== "function" && dialog.__opts.requiresDatepicker) {
      $.getScript(futureRootUrl + "/js/datepicker/datepicker-admin.js", function() {
        $.getScript(futureRootUrl + "/jquery/plugins/ui/jquery-ui-" + jQueryUiVersion + ".custom/js/jquery.ui.datepicker-no.js", function() {
          $.getScript(futureRootUrl + "/jquery/plugins/ui/jquery-ui-" + jQueryUiVersion + ".custom/js/jquery.ui.datepicker-nn.js", function() {
            futureDatepicker.resolve(); 
          });
        });
      });
    } else {
      futureDatepicker.resolve(); 
    }
    $.when(futureUi, futureTree, futureDatepicker).done(function() {
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
      requiresTree: opts.requiresTree,
      requiresDatepicker: opts.requiresDatepicker,
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