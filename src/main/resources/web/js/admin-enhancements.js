/*
 *  Vortex Admin enhancements
 *
 *  Note(s):
 *
 *  Iterating-method for jQuery elements based on: http://jsperf.com/loop-through-jquery-elements/2 (among multiple tests)
 *   -> Backwards for-loop is in most cases even faster but not always desirable
 *
 *  TODO: i18n/more specific AJAX error messages
 *  TODO: CPU usage in ready() vs. wait for it in load()
 *  TODO: Add more functions as prototype to vrtxAdmin (with maybe some exceptions)
 *  TODO: JSDoc functions
 *  TODO: Better/revisit architecture for Async code regarding Deferred/Promise 
 *        (http://net.tutsplus.com/tutorials/javascript-ajax/wrangle-async-tasks-with-jquery-promises/)
 *  TODO: Better seperation of business logic, events, Async/AJAX and DOM interaction (and write tests for easier refactoring)
 *  TODO: Remove caching of this when local reference inside function (http://stackoverflow.com/questions/12662118/is-this-cached-locally)
 *
 *  ToC: 
 *
 *  1.  Config
 *  2.  DOM is fully loaded
 *  3.  DOM is ready
 *  4.  General interaction / dialogs
 *  5.  Dropdowns
 *  6.  Create service
 *  7.  File upload service
 *  8.  Collectionlisting
 *  9.  Editor
 *  10. Permissions
 *  11. Reports
 *  12. Versioning
 *  13. Async functions
 *  14. Async helper functions and AJAX server façade
 *  15. CK browse server integration
 *  16. Utils
 *  17. Override JavaScript / jQuery
 *
 */
 
/*-------------------------------------------------------------------*\
    1. Config
\*-------------------------------------------------------------------*/
 
var startLoadTime = +new Date();

/**
 * Creates an instance of VrtxAdmin
 * @constructor
 */
function VrtxAdmin() {

  // Class-like singleton pattern (p.145 JavaScript Patterns)
  var instance; // cached instance
  VrtxAdmin = function VrtxAdmin() { // rewrite constructor
    return instance;
  };
  VrtxAdmin.prototype = this; // carry over properties
  instance = new VrtxAdmin(); // instance
  instance.constructor = VrtxAdmin; // reset construction pointer

  /** Cache jQuery instance internally
    * @type object */
  this._$ = $;

  // Browser info: used for progressive enhancement and performance scaling based on knowledge of current JS-engine
  this.ua = navigator.userAgent.toLowerCase();
  this.isIE = this._$.browser.msie;
  this.browserVersion = this._$.browser.version;
  this.isIE8 = this.isIE && this.browserVersion <= 8;
  this.isIE7 = this.isIE && this.browserVersion <= 7;
  this.isIE6 = this.isIE && this.browserVersion <= 6;
  this.isIE5OrHigher = this.isIE && this.browserVersion >= 5;
  this.isIETridentInComp = this.isIE7 && /trident/.test(this.ua);
  this.isOpera = this._$.browser.opera;
  this.isSafari = this._$.browser.safari;
  this.isIPhone = /iphone/.test(this.ua);
  this.isIPad = /ipad/.test(this.ua);
  this.isAndroid = /android/.test(this.ua); // http://www.gtrifonov.com/2011/04/15/google-android-user-agent-strings-2/
  this.isMobileWebkitDevice = (this.isIPhone || this.isIPad || this.isAndroid);
  this.isWin = ((this.ua.indexOf("win") != -1) || (this.ua.indexOf("16bit") != -1));
  this.supportsFileList = window.FileList;
  this.animateTableRows = !this.isIE;
  
  /** Language extracted from cookie */
  this.lang = readCookie("vrtx.manage.language", "no");
  
  this.hasConsole = typeof console !== "undefined";
  this.hasConsoleLog = this.hasConsole && console.log;
  this.hasConsoleError = this.hasConsole && console.error;

  // Autocomplete parameters
  this.permissionsAutocompleteParams = { minChars: 4, selectFirst: false,
                                         max: 30, delay: 800, minWidth: 180, adjustForParentWidth: 15 };                              
  this.usernameAutocompleteParams =    { minChars: 2, selectFirst: false,
                                         max: 30, delay: 500, multiple: false, minWidth: 180, adjustForParentWidth: 15 };                                       
  this.tagAutocompleteParams =         { minChars: 1, minWidth: 180, adjustForParentWidth: 15 };
     
  // Transitions
  this.transitionSpeed = this.isMobileWebkitDevice ? 0 : 200; // same as 'fast'
  this.transitionCustomPermissionSpeed = this.isMobileWebkitDevice ? 0 : 200; // same as 'fast'
  this.transitionPropSpeed = this.isMobileWebkitDevice ? 0 : 100;
  this.transitionDropdownSpeed = this.isMobileWebkitDevice ? 0 : 100;
  this.transitionEasingSlideDown = (!(this.isIE && this.browserVersion < 10) && !this.isMobileWebkitDevice) ?  "easeOutQuad" : "linear";
  this.transitionEasingSlideUp = (!(this.sIE && this.browserVersion < 10) && !this.isMobileWebkitDevice) ?  "easeInQuad" : "linear";
  
  // Application logic
  this.editorSaveButtonName = "";
  this.asyncEditorSavedDeferred;
  this.asyncGetFormsInProgress = 0;
  this.asyncGetStatInProgress = false;
  this.createResourceReplaceTitle = true;
  this.createDocumentFileName = "";
  this.trashcanCheckedFiles = 0;
  
  /* TODO: Need some rewrite of data structure to use 1 instead of 3 variables */
  this.multipleCommaSeperatedInputFieldNames = [];
  this.multipleCommaSeperatedInputFieldCounter = [];
  this.multipleCommaSeperatedInputFieldLength = [];
  this.multipleCommaSeperatedInputFieldTemplates = [];
  this.multipleCommaSeperatedInputFieldDeferred;
  
  this.breadcrumbsLastPosLeft = -999;
  this.resizeDebouncedWait = true;
  this.reloadFromServer = false; // changed by funcProceedCondition and used by funcComplete in completeFormAsync for admin-permissions
  this.ignoreAjaxErrors = false;
  this._$.ajaxSetup({
    timeout: 300000 // 5min
  });
  this.runReadyLoad = true;
  this.bodyId = "";
  
  return instance;
};

var vrtxAdmin = new VrtxAdmin();

/*-------------------------------------------------------------------*\
    2. DOM is fully loaded ("load"-event) 
\*-------------------------------------------------------------------*/
                                            
vrtxAdmin._$(window).load(function() {
  var vrtxAdm = vrtxAdmin;
  
  if(vrtxAdm.runReadyLoad === false) return; // XXX: return if should not run load() code
  
  vrtxAdm.initAdaptiveBreadcrumbs();
  
  vrtxAdm.log({msg: "Window.load() in " + (+new Date - startLoadTime) + "ms."});
});


/*-------------------------------------------------------------------*\
    3. DOM is ready
       readyState === "complete" || "DOMContentLoaded"-event (++)
\*-------------------------------------------------------------------*/

vrtxAdmin._$(document).ready(function () {
  var startReadyTime = +new Date(),
      vrtxAdm = vrtxAdmin;

  vrtxAdm.cacheDOMNodesForReuse();
  
  vrtxAdm.cachedBody.addClass("js");
  
  if(vrtxAdm.runReadyLoad === false) return; // XXX: return if should not run all of ready() code
  
  vrtxAdm.initFunctionalityDocReady();

  vrtxAdm.log({
    msg: "Document.ready() in " + (+new Date - startReadyTime) + "ms."
  });
});

VrtxAdmin.prototype.cacheDOMNodesForReuse = function cacheDOMNodesForReuse() {
  var vrtxAdm = this, _$ = vrtxAdm._$;
  
  vrtxAdm.cachedBody = _$("body");
  vrtxAdm.cachedAppContent = vrtxAdm.cachedBody.find("#app-content");
  vrtxAdm.cachedContent = vrtxAdm.cachedAppContent.find("#contents");
  vrtxAdm.cachedDirectoryListing = _$("#directory-listing");
  vrtxAdm.cachedActiveTab = vrtxAdm.cachedAppContent.find("#active-tab");
};

// TODO: these function needs a lot of seperation of concerns
VrtxAdmin.prototype.initFunctionalityDocReady = function initFunctionalityDocReady() {
  var vrtxAdm = this, _$ = vrtxAdm._$;
  
  var bodyId = vrtxAdm.cachedBody.attr("id");
  vrtxAdm.bodyId = (typeof bodyId !== "undefined") ? bodyId : "";

  // Remove active tab if it has no children
  if (!vrtxAdm.cachedActiveTab.find(" > *").length) {
    vrtxAdm.cachedActiveTab.remove();
  }

  // Remove active tab-message if it is empty
  var activeTabMsg = vrtxAdm.cachedActiveTab.find(" > .tabMessage");
  if (!activeTabMsg.text().length) {
    activeTabMsg.remove();
  }

  // Buttons into links
  vrtxAdm.logoutButtonAsLink();

  // Move down resource menu when long title
  var titleSplits = _$("h1 .title-split");
  var resourceMenuLeft = _$("#resourceMenuLeft");
  var titleSplitsLength = titleSplits.length;
  if (resourceMenuLeft.length) {
    if (titleSplitsLength == 2) {
      resourceMenuLeft.css("marginTop", "-22px");
    } else if (titleSplitsLength >= 3) {
      resourceMenuLeft.css("marginTop", "0px");
    }
  }
  
  vrtxAdm.initDropdowns();

  // Ignore all AJAX errors on tab change
  _$("#app-tabs, #vrtx-breadcrumb-wrapper").on("click", "a", function (e) {
    vrtxAdm.ignoreAjaxErrors = true;
  });

  // Tooltips
  $("#title-container").vortexTips("abbr", "#title-container", 200, 300, 250, 300, 20, 0, false, false);
  $("#main").vortexTips("abbr.resource-prop-info", ".vrtx-admin-form", 200, 300, 250, 300, 20, -30, false, false);

  // Resource menus
  var resourceMenuLeftServices = ["renameService", "deleteResourceService", "manage\\.createArchiveService", "manage\\.expandArchiveService"];
  for (var i = resourceMenuLeftServices.length; i--;) {
    vrtxAdm.getFormAsync({
      selector: "#title-container a#" + resourceMenuLeftServices[i],
      selectorClass: "globalmenu",
      insertAfterOrReplaceClass: "ul#resourceMenuLeft",
      nodeType: "div",
      simultanSliding: true
    });
    vrtxAdm.completeFormAsync({
      selector: "form#" + resourceMenuLeftServices[i] + "-form input[type=submit]"
    });
  }
  var resourceMenuRightServices = ["vrtx-unpublish-document", "vrtx-publish-document"];
  for (i = resourceMenuRightServices.length; i--;) {
    var publishUnpublishService = resourceMenuRightServices[i];

    // Ajax save before publish if editing
    var isSavingBeforePublish = publishUnpublishService === "vrtx-publish-document" && (bodyId === "vrtx-editor" || bodyId === "vrtx-edit-plaintext");
    
    vrtxAdm.getFormAsync({
      selector: "#title-container a#" + publishUnpublishService,
      selectorClass: "globalmenu",
      insertAfterOrReplaceClass: "ul#resourceMenuLeft",
      secondaryInsertAfterOrReplaceClass: "ul#resourceMenuRight",
      nodeType: "div",
      simultanSliding: true,
      funcComplete: (isSavingBeforePublish ? function(p) {
        if(vrtxAdm.lang === "en") {
          $("#vrtx-publish-document-form h3").text("Are you sure you want to save and publish?");
        } else {
          $("#vrtx-publish-document-form h3").text("Er du sikker på at du vil lagre og publisere?");
        }
      } : null)
    });
    vrtxAdm.completeFormAsync({
      selector: "form#" + publishUnpublishService + "-form input[type=submit]",
      updateSelectors: ["#resourceMenuRight", "#publishing-status", "#publishing-publish-date", "#publishing-unpublish-date"],
      funcComplete: (isSavingBeforePublish ? 
        function(link) { // Save async
          vrtxAdm.completeFormAsyncPost({  // Publish async
            updateSelectors: ["#resourceMenuRight"],
            link: link,
            form: $("#vrtx-publish-document-form"),
            funcComplete: function() { // Save and unlock to view regulary
              _$("#vrtx-save-view-shortcut").trigger("click");
            }
          });
          return false;
        } : null
      ),
      post: (bodyId !== "vrtx-preview" && !isSavingBeforePublish)
    });
  }
  // Unlock
  vrtxAdm.getFormAsync({
    selector: "#title-container a#manage\\.unlockFormService",
    selectorClass: "globalmenu",
    insertAfterOrReplaceClass: "#resource-title > ul:last",
    nodeType: "div",
    simultanSliding: true
  });
  vrtxAdm.completeFormAsync({
    selector: "form#manage\\.unlockFormService-form input[type=submit]"
  });
  
  vrtxAdm.completeFormAsync({
    selector: "li.manage\\.unlockFormService form[name=unlockForm]",
    updateSelectors: ["#resourceMenuRight", "#contents"],
    post: (bodyId !== "vrtx-editor" && bodyId !== "vrtx-edit-plaintext" && bodyId !== "vrtx-manage-collectionlisting" && bodyId !== "")
  });
  
  // Create folder chooser in global menu
  // TODO: generalize dialog jQuery UI function with AJAX markup/text
  $(document).on("click", "#global-menu-create a", function (e) {
    var link = this;
    var id = link.id + "-content";
    var dialogManageCreate = $("#" + id);
    if (!dialogManageCreate.length) {
      vrtxAdm.serverFacade.getHtml(link.href, {
        success: function (results, status, resp) {
          _$("body").append("<div id='" + id + "'>" + _$(results).find("#vrtx-manage-create-content").html() + "</div>");
          dialogManageCreate = $("#" + id);
          dialogManageCreate.hide();
          // Lazy-load JS-dependency chain (cached). TODO: general API for this and error msg
          $.cachedScript(location.protocol + '//' + location.host + '/vrtx/__vrtx/static-resources/jquery/plugins/jquery.treeview.js')
            .done(function (script, textStatus) {
            $.cachedScript(location.protocol + '//' + location.host + '/vrtx/__vrtx/static-resources/jquery/plugins/jquery.treeview.async.js')
              .done(function (script, textStatus) {
              $.cachedScript(location.protocol + '//' + location.host + '/vrtx/__vrtx/static-resources/jquery/plugins/jquery.scrollTo.min.js')
                .done(function (script, textStatus) {
                vrtxSimpleDialogs.openHtmlDialog("global-menu-create", dialogManageCreate.html(), link.title, 600, 395);
                initializeTree();
              }).fail(function (jqxhr, settings, exception) { vrtxAdmin.displayErrorMsg(exception + ": jquery.scrollTo.min.js"); });
            }).fail(function (jqxhr, settings, exception) { vrtxAdmin.displayErrorMsg(exception + ": jquery.treeview.async.js"); });
          }).fail(function (jqxhr, settings, exception) { vrtxAdmin.displayErrorMsg(exception + ": jquery.treeview.js"); });
        }
      });
    } else {
      vrtxSimpleDialogs.openHtmlDialog("global-menu-create", dialogManageCreate.html(), link.title, 600, 395);
      initializeTree();
    }
    e.stopPropagation();
    e.preventDefault();
  });
  
  // Send to approval
  // TODO: generalize dialog jQuery UI function with AJAX markup/text
  // XXX: HTML content should set height (not hardcoded)
  _$(document).on("click", "#vrtx-send-to-approval, #vrtx-send-to-approval-global", function (e) {
    var link = this;
    var id = link.id + "-content";
    var dialogManageCreate = $("#" + id);
    if (!dialogManageCreate.length) {
      vrtxAdm.serverFacade.getHtml(link.href, {
        success: function (results, status, resp) {
          _$("body").append("<div id='" + id + "'>" + _$(results).find("#contents").html() + "</div>");
          dialogManageCreate = $("#" + id);
          dialogManageCreate.hide();
          var hasEmailFrom = dialogManageCreate.find("#emailFrom").length;
          vrtxSimpleDialogs.openHtmlDialog("send-approval", dialogManageCreate.html(), link.title, 410, (hasEmailFrom ? 620 : 545));
          var dialog = $(".ui-dialog");
          if(dialog.find("#emailTo").val().length > 0) {
            if(hasEmailFrom) {
              dialog.find("#emailFrom")[0].focus();
            } else {
              dialog.find("#yourCommentTxtArea")[0].focus();
            } 
          }
        }
      });
    } else {
      var hasEmailFrom = dialogManageCreate.find("#emailFrom").length;
      vrtxSimpleDialogs.openHtmlDialog("send-approval", dialogManageCreate.html(), link.title, 410, (hasEmailFrom ? 620 : 545));
      var dialog = $(".ui-dialog");
      if(dialog.find("#emailTo").val().length > 0) {
        if(hasEmailFrom) {
          dialog.find("#emailFrom")[0].focus();
        } else {
          dialog.find("#yourCommentTxtArea")[0].focus();
        }
      }
    }
    e.stopPropagation();
    e.preventDefault();
  });
  _$(document).on("click", "#dialog-html-send-approval-content .vrtx-focus-button", function(e) {
    var btn = $(this);
    var form = btn.closest("form");
    var url = form.attr("action");
    var dataString = form.serialize();
    vrtxAdm.serverFacade.postHtml(url, dataString, {
      success: function (results, status, resp) {
        var formParent = form.parent();
        formParent.html(_$(results).find("#contents").html());
        var successWrapper = formParent.find("#email-approval-success");
        if(successWrapper.length) {  // Save async if sent mail
          successWrapper.trigger("click");
          setTimeout(function() {
            _$("#vrtx-save-view-shortcut").trigger("click");
          }, 250);
        }
      }
    });
    e.stopPropagation();
    e.preventDefault();
  });

  // Interactions initialization
  createInteraction(bodyId, vrtxAdm, _$);
  vrtxAdm.collectionListingInteraction();
  editorInteraction(bodyId, vrtxAdm, _$);
  reportsInteraction(bodyId, vrtxAdm, _$);
  versioningInteraction(bodyId, vrtxAdm, _$);
  
  // Ajax initialization / listeners
  
  switch (bodyId) {
    case "vrtx-manage-collectionlisting":
      var tabMenuServices = ["fileUploadService", "createDocumentService", "createCollectionService"];
      var speedCreationServices = vrtxAdm.isIE8 ? 0 : 350;
      for (i = tabMenuServices.length; i--;) {
        if (tabMenuServices[i] == "createCollectionService") {
          vrtxAdm.getFormAsync({
            selector: "ul#tabMenuRight a#" + tabMenuServices[i],
            selectorClass: "vrtx-admin-form",
            insertAfterOrReplaceClass: "#active-tab ul#tabMenuRight",
            nodeType: "div",
            funcComplete: function (p) {
              createFuncComplete();
            },
            simultanSliding: true,
            transitionSpeed: speedCreationServices
          });
          vrtxAdm.completeFormAsync({
            selector: "form#" + tabMenuServices[i] + "-form input[type=submit]",
            updateSelectors: ["#contents"],
            errorContainer: "errorContainer",
            errorContainerInsertAfter: "> ul",
            funcComplete: vrtxAdm.updateCollectionListingInteraction,
            post: true,
            transitionSpeed: speedCreationServices
          });
        } else { // Half-async for file upload and create document
          if (tabMenuServices[i] == "createDocumentService") {
            vrtxAdm.getFormAsync({
              selector: "ul#tabMenuRight a#" + tabMenuServices[i],
              selectorClass: "vrtx-admin-form",
              insertAfterOrReplaceClass: "#active-tab ul#tabMenuRight",
              nodeType: "div",
              funcComplete: function (p) {
                createFuncComplete();
              },
              simultanSliding: true,
              transitionSpeed: speedCreationServices
            });
            vrtxAdm.completeFormAsync({
              selector: "form#" + tabMenuServices[i] + "-form input[type=submit]",
              transitionSpeed: speedCreationServices
            });
          } else {
            if (vrtxAdm.isIPhone || vrtxAdm.isIPad) { // TODO: feature detection
              $("ul#tabMenuRight li." + tabMenuServices[i]).remove();
            } else {
              vrtxAdm.getFormAsync({
                selector: "ul#tabMenuRight a#" + tabMenuServices[i],
                selectorClass: "vrtx-admin-form",
                insertAfterOrReplaceClass: "#active-tab ul#tabMenuRight",
                nodeType: "div",
                funcComplete: function (p) {
                  vrtxAdm.initFileUpload()
                },
                simultanSliding: true
              });
              vrtxAdm.completeFormAsync({
                selector: "form#" + tabMenuServices[i] + "-form input[type=submit]"
              });
              vrtxAdm.initFileUpload(); // when error message
            }
          }
        }
      }

      tabMenuServices = ["collectionListing\\.action\\.move-resources", "collectionListing\\.action\\.copy-resources"];
      resourceMenuServices = ["moveToSelectedFolderService", "copyToSelectedFolderService"];
      // TODO: This map/lookup-obj is a little hacky..
      tabMenuServicesInjectMap = {
        "collectionListing.action.move-resources": "moveToSelectedFolderService",
          "collectionListing.action.copy-resources": "copyToSelectedFolderService"
      };
      for (i = tabMenuServices.length; i--;) {
        vrtxAdm.cachedContent.on("click", "input#" + tabMenuServices[i], function (e) {
          var input = _$(this);
          var form = input.closest("form");
          var url = form.attr("action");
          var li = "li." + tabMenuServicesInjectMap[input.attr("id")];
          var dataString = form.serialize() + "&" + input.attr("name") + "=" + input.val();
          vrtxAdm.serverFacade.postHtml(url, dataString, {
            success: function (results, status, resp) {
              var resourceMenuRight = $("#resourceMenuRight");
              var copyMoveExists = "";
              for (var key in tabMenuServicesInjectMap) {
                var copyMove = resourceMenuRight.find("li." + tabMenuServicesInjectMap[key]);
                if (copyMove.length) {
                  copyMoveExists = copyMove;
                  break;
                }
              }
              var results = _$(results);
              if (copyMoveExists !== "") { // Reverse the belt and roll out updated baggage :)
                baggageBeltAnimFx(copyMoveExists, {
                  reverse: true,
                  complete: function () {
                    copyMoveExists.remove();
                    resourceMenuRight.html(results.find("#resourceMenuRight").html());
                    vrtxAdm.displayInfoMsg(results.find(".infomessage").html());
                    var resourceTitle = resourceMenuRight.closest("#resource-title");
                    if (resourceTitle.hasClass("compact")) { // Instant compact => expanded
                      resourceTitle.removeClass("compact");
                    }
                    baggageBeltAnimFx(resourceMenuRight.find(li));
                  }
                });
              } else {
                resourceMenuRight.html(results.find("#resourceMenuRight").html());
                vrtxAdm.displayInfoMsg(results.find(".infomessage").html());
                var resourceTitle = resourceMenuRight.closest("#resource-title");
                if (resourceTitle.hasClass("compact")) { // Instant compact => expanded
                  resourceTitle.removeClass("compact");
                }
                baggageBeltAnimFx(resourceMenuRight.find(li));
              }
            }
          });
          e.stopPropagation();
          e.preventDefault();
        });
      }

      for (i = resourceMenuServices.length; i--;) {
        vrtxAdm.cachedAppContent.on("click", "#resourceMenuRight li." + resourceMenuServices[i] + " button", function (e) {
          var button = _$(this);
          var form = button.closest("form");
          var url = form.attr("action");
          var li = form.closest("li");
          var dataString = form.serialize() + "&" + button.attr("name") + "=" + button.val();
          vrtxAdm.serverFacade.postHtml(url, dataString, {
            success: function (results, status, resp) {
              baggageBeltAnimFx(li, {
                reverse: true,
                complete: function () {
                  var result = _$(results);
                  vrtxAdm.displayErrorMsg(result.find(".errormessage").html());
                  vrtxAdm.cachedContent.html(_$(results).find("#contents").html());
                  vrtxAdm.updateCollectionListingInteraction();
                  li.remove();
                  var resourceTitle = _$(results).find("#resource-title");
                  if (resourceTitle.hasClass("compact")) { // Instant compact => expanded
                    $("#resource-title").addClass("compact");
                  }
                }
              });
            }
          });
          e.stopPropagation();
          e.preventDefault();
        });
      }
      /*
      vrtxAdm.cachedContent.on("click", "input#collectionListing\\.action\\.delete-resources", function (e) {
        var input = _$(this);
        var form = input.closest("form");
        var url = form.attr("action");
        var dataString = form.serialize() + "&" + input.attr("name") + "=" + input.val();
        vrtxAdm.serverFacade.postHtml(url, dataString, {
          success: function (results, status, resp) {
            var result = _$(results);
            vrtxAdm.displayErrorMsg(result.find(".errormessage").html());
            vrtxAdm.cachedContent.html(result.find("#contents").html());
            vrtxAdm.updateCollectionListingInteraction();
          }
        });
        e.stopPropagation();
        e.preventDefault();
      });
      */
      break;
    case "vrtx-trash-can":
      vrtxAdm.cachedContent.on("click", "input.deleteResourcePermanent", function (e) {
        if (vrtxAdm.trashcanCheckedFiles >= (vrtxAdm.cachedContent.find("tbody tr").length - 1)) return; // Redirect if empty trash can
        vrtxAdm.trashcanCheckedFiles = 0;
        var input = _$(this);
        var form = input.closest("form");
        var url = form.attr("action");
        var dataString = form.serialize() + "&" + input.attr("name");
        vrtxAdm.serverFacade.postHtml(url, dataString, {
          success: function (results, status, resp) {
            var result = _$(results);
            vrtxAdm.displayErrorMsg(result.find(".errormessage").html());
            vrtxAdm.cachedContent.html(result.find("#contents").html());
            vrtxAdm.updateCollectionListingInteraction();
          }
        });
        e.stopPropagation();
        e.preventDefault();
      });
      break;
    case "vrtx-permissions":
      var privilegiesPermissions = ["read", "read-write", "all"];
      for (i = privilegiesPermissions.length; i--;) {
        vrtxAdm.getFormAsync({
          selector: "div.permissions-" + privilegiesPermissions[i] + "-wrapper a.full-ajax",
          selectorClass: "expandedForm-" + privilegiesPermissions[i],
          insertAfterOrReplaceClass: "div.permissions-" + privilegiesPermissions[i] + "-wrapper",
          isReplacing: true,
          nodeType: "div",
          funcComplete: initPermissionForm,
          simultanSliding: false,
          transitionSpeed: 0,
          transitionEasingSlideDown: "linear",
          transitionEasingSlideUp: "linear"
        });
        vrtxAdm.completeFormAsync({
          selector: "div.permissions-" + privilegiesPermissions[i] + "-wrapper .submitButtons input",
          isReplacing: true,
          updateSelectors: [".permissions-" + privilegiesPermissions[i] + "-wrapper",
                            "#resourceMenuRight"],
          errorContainer: "errorContainer",
          errorContainerInsertAfter: ".groups-wrapper",
          funcProceedCondition: checkStillAdmin,
          funcComplete: function () {
            if (vrtxAdm.reloadFromServer) {
              location.reload(true);
            }
          },
          post: true,
          transitionSpeed: 0,
          transitionEasingSlideDown: "linear",
          transitionEasingSlideUp: "linear"
        });
      }

      var privilegiesPermissionsInTable = ["add-comment", "read-processed", "read-write-unpublished"];
      for (i = privilegiesPermissionsInTable.length; i--;) {
        vrtxAdm.getFormAsync({
          selector: ".privilegeTable tr." + privilegiesPermissionsInTable[i] + " a.full-ajax",
          selectorClass: privilegiesPermissionsInTable[i],
          insertAfterOrReplaceClass: "tr." + privilegiesPermissionsInTable[i],
          isReplacing: true,
          nodeType: "tr",
          funcComplete: initPermissionForm,
          simultanSliding: true
        });
        vrtxAdm.completeFormAsync({
          selector: "tr." + privilegiesPermissionsInTable[i] + " .submitButtons input",
          isReplacing: true,
          updateSelectors: ["tr." + privilegiesPermissionsInTable[i],
                            "#resourceMenuRight"],
          errorContainer: "errorContainer",
          errorContainerInsertAfter: ".groups-wrapper",
          post: true
        });
      }

      // Remove/add permissions
      vrtxAdm.removePermissionAsync("input.removePermission", ".principalList");
      vrtxAdm.addPermissionAsync("span.addGroup", ".principalList", ".groups-wrapper", "errorContainer");
      vrtxAdm.addPermissionAsync("span.addUser", ".principalList", ".users-wrapper", "errorContainer");

      var SUBMIT_SET_INHERITED_PERMISSIONS = false;
      $(document).on("click", "#permissions\\.toggleInheritance\\.submit", function (e) {
        if (!SUBMIT_SET_INHERITED_PERMISSIONS) {
          vrtxSimpleDialogs.openConfirmDialog(confirmSetInheritedPermissionsMsg, confirmSetInheritedPermissionsTitle, function () {
            SUBMIT_SET_INHERITED_PERMISSIONS = true;
            $("#permissions\\.toggleInheritance\\.submit").trigger("click");
          }, null, null);
          e.stopPropagation();
          e.preventDefault();
        } else {
          e.stopPropagation();
        }
      });

      break;
    case "vrtx-publishing":
      // TODO: generalize dialog jQuery UI function with AJAX markup/text
      $(document).on("click", "a.publishing-status-link", function (e) {
        var dialogTemplate = $("#vrtx-dialog-template-content");
        if (!dialogTemplate.length) {
          vrtxAdm.serverFacade.getHtml(this.href, {
            success: function (results, status, resp) {
              _$("body").append(_$(results).find("#vrtx-dialog-template-content").parent().html());
              dialogTemplate = $("#vrtx-dialog-template-content");
              dialogTemplate.hide();

              vrtxSimpleDialogs.openConfirmDialog("", dialogTemplate.find(".vrtx-confirm-publish-msg").html(), function () {
                dialogTemplate.find(".vrtx-focus-button input").trigger("click");
              }, null, null);
            }
          });
        } else {
          vrtxSimpleDialogs.openConfirmDialog("", dialogTemplate.find(".vrtx-confirm-publish-msg").html(), function () {
            dialogTemplate.find(".vrtx-focus-button input").trigger("click");
          }, null, null);
        }
        e.stopPropagation();
        e.preventDefault();
      });

      break;
    case "vrtx-about":
      vrtxAdm.zebraTables(".resourceInfo");

      if (!vrtxAdmin.isIE7) { // Turn of tmp. in IE7
        var propsAbout = ["contentLocale", "commentsEnabled", "userTitle", "keywords", "description",
                        "verifiedDate", "authorName", "authorEmail", "authorURL", "collection-type",
                        "contentType", "userSpecifiedCharacterEncoding", "plaintext-edit", "xhtml10-type",
                        "obsoleted", "editorial-contacts"];
        for (i = propsAbout.length; i--;) {
          vrtxAdm.getFormAsync({
            selector: ".prop-" + propsAbout[i] + " a.vrtx-button-small",
            selectorClass: "expandedForm-prop-" + propsAbout[i],
            insertAfterOrReplaceClass: "tr.prop-" + propsAbout[i],
            isReplacing: true,
            nodeType: "tr",
            simultanSliding: true
          });
          vrtxAdm.completeFormAsync({
            selector: ".prop-" + propsAbout[i] + " form input[type=submit]",
            isReplacing: true
          });
        }
      }

      var SUBMIT_TAKE_OWNERSHIP = false;
      $(document).on("submit", "#vrtx-admin-ownership-form", function (e) {
        if (!SUBMIT_TAKE_OWNERSHIP) {
          vrtxSimpleDialogs.openConfirmDialog(confirmTakeOwnershipMsg, confirmTakeOwnershipTitle, function () {
            SUBMIT_TAKE_OWNERSHIP = true;
            $("#vrtx-admin-ownership-form").submit();
          }, null, null);
          e.stopPropagation();
          e.preventDefault();
        } else {
          e.stopPropagation();
        }
      });

      // Urchin stats
      vrtxAdm.cachedBody.on("click", "#vrtx-resource-visit-tab-menu a", function (e) {
        if (vrtxAdm.asyncGetStatInProgress) {
          return false;
        }
        vrtxAdm.asyncGetStatInProgress = true;

        var liElm = $(this).parent();
        if (liElm.hasClass("first")) {
          liElm.removeClass("first").addClass("active").addClass("active-first");
          liElm.next().removeClass("active").removeClass("active-last").addClass("last");
        } else {
          liElm.removeClass("last").addClass("active").addClass("active-last");
          liElm.prev().removeClass("active").removeClass("active-first").addClass("first");
        }

        _$("#vrtx-resource-visit-wrapper").append("<span id='urchin-loading'></span>");
        _$("#vrtx-resource-visit-chart-stats-info").remove();
        vrtxAdm.serverFacade.getHtml(this.href, {
          success: function (results, status, resp) {
            _$("#urchin-loading").remove();
            _$("#vrtx-resource-visit").append("<div id='vrtx-resource-visit-chart-stats-info'>" + _$(results).find("#vrtx-resource-visit-chart-stats-info").html() + "</div>");
            vrtxAdm.asyncGetStatInProgress = false;
          }
        });
        e.stopPropagation();
        e.preventDefault();
      });
      break;
    default:
      // noop
      break;
  }

  // Show message in IE6, IE7 and IETrident in compability mode
  if (vrtxAdm.isIE7 || vrtxAdm.isIETridentInComp) {
    var message = vrtxAdm.cachedAppContent.find(" > .message");
    if (message.length) {
      message.html(outdatedBrowserText);
    } else {
      vrtxAdm.cachedAppContent.prepend("<div class='infomessage'>" + outdatedBrowserText + "</div>");
    }
  }
};

/*-------------------------------------------------------------------*\
    4. General interaction / dialogs
\*-------------------------------------------------------------------*/

function interceptEnterKey(idOrClass) {
  vrtxAdmin.cachedAppContent.delegate("form input" + idOrClass, "keypress", function (e) {
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      e.preventDefault(); // cancel the default browser click
    }
  });
}

function interceptEnterKeyAndReroute(txt, btn) {
  vrtxAdmin.cachedAppContent.delegate(txt, "keypress", function (e) {
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      if ($(this).hasClass("blockSubmit")) { // submit/rerouting can be blocked elsewhere on textfield
        $(this).removeClass("blockSubmit");
        e.preventDefault();
      } else {
        $(btn).click(); // click the associated button
        e.preventDefault();
      }
    }
  });
}

function baggageBeltAnimFx(elm, opts) {
  var opts = (typeof opts === "object") ? opts : {};
  var width = elm.outerWidth(true);
  if(opts.reverse) {
    anim = -width;
    easing = vrtxAdmin.transitionEasingSlideUp;
  } else {
    elm.css("marginLeft", -width);
    anim = 0;
    easing = vrtxAdmin.transitionEasingSlideDown;
  }
  if(opts.complete) {
    elm.animate({"marginLeft": anim + "px"}, vrtxAdmin.transitionSpeed, easing, opts.complete);
  } else {
    elm.animate({"marginLeft": anim + "px"}, vrtxAdmin.transitionSpeed, easing);
  }
}

VrtxAdmin.prototype.mapShortcut = function mapShortcut(selectors, reroutedSelector) {
  this.cachedAppContent.on("click", selectors, function(e) {
    $(reroutedSelector).click();
    e.stopPropagation();
    e.preventDefault();
  });
};

VrtxAdmin.prototype.logoutButtonAsLink = function logoutButtonAsLink() {
  var _$ = this._$;

  var btn = _$('input#logoutAction');
  if (!btn.length) return;
  btn.hide();
  btn.after('&nbsp;<a id=\"logoutAction.link\" name=\"logoutAction\" href="javascript:void(0);">'
          + btn.attr('value') + '</a>');
  _$("#app-head-wrapper").on("click", '#logoutAction\\.link', function (e) {
    btn.click();
    e.stopPropagation();
    e.preventDefault();
  });
};

/* Create dialog tree view */

function initializeTree() {
  var dialog = $(".ui-dialog:visible");
  var treeElem = dialog.find(".tree-create");
  var treeTrav = dialog.find("#vrtx-create-tree-folders").hide().text().split(",");
  var treeType = dialog.find("#vrtx-create-tree-type").hide().text();
  var timestamp = 1 - new Date();
  var pathNum = 0;
  treeElem.treeview({
    animated: "fast",
    url: location.protocol + '//' + location.host + location.pathname + "?vrtx=admin&service=" + treeType + "-from-drop-down&uri=&ts=" + timestamp,
    service: treeType + "-from-drop-down",
    dataLoaded: function () { // AJAX success
      var last = false;
      if (pathNum == (treeTrav.length - 1)) {
        last = true;
      }
      traverseNode(treeElem, treeTrav[pathNum++], last);
    }
  })

  treeElem.on("click", "a", function (e) { // Don't want click on links
    e.preventDefault();
  });
  
  dialog.on("click", ".tip a", function (e) { // Override jQuery UI prevention
    location.href = this.href;
  });

  // Params: class, appendTo, containerWidth, in-, pre-, outdelay, xOffset, yOffset, autoWidth
  treeElem.vortexTips("li span.folder", ".vrtx-create-tree", 80, 300, 4000, 300, 10, - 8, false, true);
}

function treeCreateScrollToCallback(link) {
  linkTriggeredMouseEnter = link;
  linkTriggeredMouseEnterTipText = linkTriggeredMouseEnter.attr('title');
  link.parent().trigger("mouseenter");
}

function traverseNode(treeElem, treeTravNode, lastNode) {
  var checkNodeAvailable = setInterval(function () {
    var link = treeElem.find("a[href$='" + treeTravNode + "']");
    if (link.length) {
      clearInterval(checkNodeAvailable);
      var hit = link.closest("li").find("> .hitarea");
      hit.click();
      if (lastNode) { // If last: scroll to node
        treeElem.css("background", "none");
        var scrollToLink = (link.position().top - 145);
        scrollToLink = scrollToLink < 0 ? 0 : scrollToLink;
        treeElem.fadeIn(200, function () {
          $(".ui-dialog:visible .ui-dialog-content").scrollTo(scrollToLink, 250, {
            easing: "swing",
            queue: true,
            axis: 'y',
            complete: treeCreateScrollToCallback(link)
          });
        });
      }
    }
  }, 20);
}

/* ^ Create dialog tree view */


/*-------------------------------------------------------------------*\
    5. Dropdowns XXX: etc.
\*-------------------------------------------------------------------*/

/**
 * Initialize dropdowns
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.initDropdowns = function initDropdowns() {
  this.dropdownPlain("#locale-selection");
  this.dropdown({
    selector: "#resource-title ul#resourceMenuLeft",
    proceedCondition: function (numOfListElements) {
      return numOfListElements > 1;
    },
    calcTop: true
  });
  this.dropdown({
    selector: "ul.manage-create"
  });
  var vrtxAdm = this;
  this.cachedBody.on("click", ".dropdown-shortcut-menu li a, .dropdown-shortcut-menu-container li a", function () {
    vrtxAdm.closeDropdowns();
  });
  this.cachedBody.on("click", document, function (e) {
    vrtxAdm.closeDropdowns();
    vrtxAdm.hideTips();
  });
};

/**
 * Dropdown with links
 *
 * @this {VrtxAdmin}
 * @param {string} selector The selector for container
 */
VrtxAdmin.prototype.dropdownPlain = function dropdownPlain(selector) {
  var vrtxAdm = this, _$ = vrtxAdm._$;
  
  var languageMenu = _$(selector + " ul");
  if (!languageMenu.length) return;

  var parent = languageMenu.parent();
  parent.addClass("js-on");

  // Remove ':' and replace <span> with <a>
  var header = parent.find(selector + "-header");
  var headerText = header.text();
  // outerHtml
  header.replaceWith("<a href='javascript:void(0);' id='" + selector.substring(1) + "-header'>"
                     + headerText.substring(0, headerText.length - 1) + "</a>");

  languageMenu.addClass("dropdown-shortcut-menu-container");

  vrtxAdm.cachedBody.on("click", selector + "-header", function (e) {
    _$(".dropdown-shortcut-menu-container:visible").slideUp(vrtxAdm.transitionDropdownSpeed, "swing");
    _$(this).next(".dropdown-shortcut-menu-container").not(":visible").slideDown(vrtxAdm.transitionDropdownSpeed, "swing");
    e.stopPropagation();
    e.preventDefault();
  });
};

/**
 * Dropdown with button-row
 *
 * @this {VrtxAdmin}
 * @param {object} options Configuration
 * @param {string} options.selector The selector for the container (list)
 * @param {function} options.proceedCondition Callback function before proceeding that uses number of list elements as parameter
 * @param {number} options.start Specify a starting point otherwise first is used
 * @param {boolean} options.calcTop Wheter or not to calculate absolute top position
 */
VrtxAdmin.prototype.dropdown = function dropdown(options) {
  var vrtxAdm = this, _$ = vrtxAdm._$;

  var list = _$(options.selector);
  if (!list.length) return;

  var numOfListElements = list.find("li").size();

  if (!options.proceedCondition || (options.proceedCondition && options.proceedCondition(numOfListElements))) {
    list.addClass("dropdown-shortcut-menu");
    
    // Move listelements except .first into container
    var listParent = list.parent();
    listParent.append("<div class='dropdown-shortcut-menu-container'><ul>" + list.html() + "</ul></div>");
    
    var startDropdown = options.start != null ? ":nth-child(-n+" + options.start + ")" : ".first";
    var dropdownClickArea = options.start != null ? ":nth-child(3)" : ".first";
    
    list.find("li").not(startDropdown).remove();
    list.find("li" + dropdownClickArea).append("<span id='dropdown-shortcut-menu-click-area'></span>");
 
    var shortcutMenu = listParent.find(".dropdown-shortcut-menu-container");
    shortcutMenu.find("li" + startDropdown).remove();
    if(options.calcTop) {
      shortcutMenu.css("top", (list.position().top + list.height() - (parseInt(list.css("marginTop")) * -1) + 1) + "px");  
    }
    shortcutMenu.css("left", (list.width()+5) + "px");
    
    list.find("li" + dropdownClickArea).addClass("dropdown-init");
    
    list.find("li.dropdown-init #dropdown-shortcut-menu-click-area").click(function (e) {
      _$(".dropdown-shortcut-menu-container:visible").slideUp(vrtxAdm.transitionDropdownSpeed, "swing");
      shortcutMenu.not(":visible").slideDown(vrtxAdm.transitionDropdownSpeed, "swing");   
      e.stopPropagation();
      e.preventDefault();
    });

    list.find("li.dropdown-init #dropdown-shortcut-menu-click-area").hover(function () {
      var area = _$(this);
      area.parent().toggleClass('unhover');
      area.prev().toggleClass('hover');
    });
  }
};

/**
 * Close all dropdowns (slide up)
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.closeDropdowns = function closeDropdowns() {
  this._$(".dropdown-shortcut-menu-container:visible").slideUp(this.transitionDropdownSpeed, "swing");
};

/**
 * Hide tips (fade out)
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.hideTips = function hideTips() {
  this._$(".tip:visible").fadeOut(this.transitionDropdownSpeed, "swing");
};

/**
 * Init adaptive breadcrumbs
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.initAdaptiveBreadcrumbs = function initAdaptiveBreadcrumbs() {
  var vrtxAdm = this, _$ = this._$;
  vrtxAdm.cachedBreadcrumbs = _$("#vrtx-breadcrumb > span");
  vrtxAdm.adaptiveBreadcrumbs();
}

/**
 * Update adaptive breadcrumbs
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.adaptiveBreadcrumbs = function adaptiveBreadcrumbs() {
  var _$ = this._$;
  var breadcrumbs = this.cachedBreadcrumbs, 
      i = breadcrumbs.length,
      runnedAtStart = false;
  while(i--) {
    var breadcrumb = _$(breadcrumbs[i]);
    var breadcrumbPos = breadcrumb.position();
    var breadcrumbPosTop = breadcrumbPos.top;
    var breadcrumbPosLeft = breadcrumbPos.left;
    if (!runnedAtStart) {
      if (vrtxAdmin.breadcrumbsLastPosLeft == breadcrumbPosLeft) {
        return;     
      } else {
        vrtxAdmin.breadcrumbsLastPosLeft = breadcrumbPosLeft;
      }
      runnedAtStart = true;
    }
    if (breadcrumbPosTop > 0 && breadcrumbPosLeft == 50) {
      if (!breadcrumb.hasClass("vrtx-breadcrumb-left")) {
        breadcrumb.addClass("vrtx-breadcrumb-left");
      }
      if (breadcrumb.hasClass("vrtx-breadcrumb-active")) {
        var prevBreadcrumb = breadcrumb.prev();
        if(prevBreadcrumb.hasClass("vrtx-breadcrumb-before-active")) {
          prevBreadcrumb.removeClass("vrtx-breadcrumb-before-active");
        }
      }
    } else {
      if (breadcrumb.hasClass("vrtx-breadcrumb-left")) {
        breadcrumb.removeClass("vrtx-breadcrumb-left");
      }
      if (breadcrumb.hasClass("vrtx-breadcrumb-active")) {
        var prevBreadcrumb = breadcrumb.prev();
        if(!prevBreadcrumb.hasClass("vrtx-breadcrumb-before-active")) {
          prevBreadcrumb.addClass("vrtx-breadcrumb-before-active");
        }
      }
    }
  }
};

/*-------------------------------------------------------------------*\
    6. Create service
    XXX: optimize more and needs more seperation
\*-------------------------------------------------------------------*/

function createInteraction(bodyId, vrtxAdm, _$) {  
  vrtxAdmin.cachedAppContent.on("click", "#vrtx-checkbox-is-index input", function(e) {
    isIndexFile($("#vrtx-textfield-file-name input").attr("name"), $(this).attr("name"));
    e.stopPropagation();
  });
  vrtxAdmin.cachedAppContent.on("click", ".radio-buttons input", function(e) {
    var focusedTextField = $(".vrtx-admin-form input[type='text']:visible:first");
    if(focusedTextField.length && !focusedTextField.val().length) { // Only focus when empty
      focusedTextField.focus();
    }
    e.stopPropagation();
  });    
}

function createFuncComplete() {
  var lastColTitle = "";
  var lastColName = "";
  var lastFileTitle = "";
  var lastFileName = "";
  
  $(document).on("keyup", "#vrtx-textfield-collection-title input", function(e) {
    var colTitle = $(this),
        colTitleVal = colTitle.val();
    if(colTitle.length && colTitleVal !== lastColTitle) {
      lastColTitle = colTitleVal;
      userTitleChange(colTitle.attr("name"), $("#vrtx-textfield-collection-name input").attr("name"), null);
    }
  });
  $(document).on("keyup", "#vrtx-textfield-collection-name input", function(e) {
    var colName = $(this),
        colNameVal = colName.val();
    if(colName.length && colName.is(":focus") && colNameVal !== lastColName) {
      lastColName = colNameVal;       
      disableReplaceTitle(colName.attr("name"));
    }
  });
  $(document).on("keyup", "#vrtx-textfield-file-title input", function(e) {
    var fileTitle = $(this),
        fileTitleVal = fileTitle.val();
    if(fileTitle.length && fileTitleVal !== lastFileTitle) {
      lastFileTitle = fileTitleVal;
      userTitleChange(fileTitle.attr("name"), $("#vrtx-textfield-file-name input").attr("name"), $("#vrtx-checkbox-is-index input").attr("name"));
    }
  });
  $(document).on("keyup", "#vrtx-textfield-file-name input", function(e) {
    var fileName = $(this),
        fileNameVal = fileName.val();
    if(fileName.length && fileName.is(":focus") && fileNameVal !== lastFileName) {
      lastFileName = fileNameVal;
      disableReplaceTitle(fileName.attr("name"));
    }
  });

  vrtxAdmin.createResourceReplaceTitle = true;
  
  // Fix margin left for radio descriptions because radio width variation on different OS-themes
  var radioDescriptions = $(".radioDescription");
  if(radioDescriptions.length) {
    var leftPos = $(".radio-buttons label:first").position().left;
    radioDescriptions.css("marginLeft", leftPos + "px");
  }
  
  $("#initChangeTemplate").trigger("click"); 
  
  var textFields = $(".vrtx-admin-form input[type='text']");
  textFields.attr("autocomplete", "off");
  textFields.attr("autocorrect", "off");
  
}

function changeTemplate(element, hasTitle) {
  var checked = $(".radio-buttons input:checked");
  if(checked.length) {
    var templateFile = checked.val();
    if(templateFile.indexOf(".") !== -1) {
      var fileType = $("#vrtx-textfield-file-type");
      if(fileType.length) {
        var fileTypeEnding = templateFile.split(".")[1];
        fileType.text("." + fileTypeEnding);
      }
    }
  }
  var idx = $("#isIndex");
  var isIndex = idx.length;
  if(isIndex && fileTypeEnding !== "html") {
    isIndex = false;
    idx.parent().hide();
    if(idx.is(":checked")) {
      idx.removeAttr("checked");
      isIndexFile($("#vrtx-textfield-file-name input").attr("name"), $("#vrtx-checkbox-is-index input").attr("name"));
    }
  } else if(isIndex) {
    idx.parent().show();
    isIndex = idx.is(":checked");
  }

  var name = $("#name");
  
  if(hasTitle) {
    $("#vrtx-div-file-title").show();
    var minWidth = vrtxAdmin.createResourceReplaceTitle ? 35 : 100;
    minWidth = isIndex ? 35 : minWidth;
  } else {
    $("#vrtx-div-file-title").hide();
    var minWidth = isIndex ? 35 : 100;
  }
  
  growField(name, name.val(), 5, minWidth, 530);
  
  if(vrtxAdmin.createResourceReplaceTitle) {
    $(".vrtx-admin-form").addClass("file-name-from-title");
  }
}

function isIndexFile(nameBind, indexBind) {
  var indexCheckbox = $("#" + indexBind);
  var nameField = $("#" + nameBind);
  if (indexCheckbox.is(":checked")) {
    vrtxAdmin.createDocumentFileName = nameField.val();
    nameField.val('index');
    growField(nameField, 'index', 5, 35, 530);

    nameField[0].disabled = true;
    $("#vrtx-textfield-file-type").addClass("disabled");
  } else {
    nameField[0].disabled = false;
    $("#vrtx-textfield-file-type").removeClass("disabled");

    nameField.val(vrtxAdmin.createDocumentFileName);
    growField(nameField, vrtxAdmin.createDocumentFileName, 5, (vrtxAdmin.createResourceReplaceTitle ? 35 : 100), 530);
  }
}

function userTitleChange(titleBind, nameBind, indexBind) {
  if (vrtxAdmin.createResourceReplaceTitle) {
    var titleField = $("#" + titleBind);
    var nameField = $("#" + nameBind);
    if(indexBind) {
      var indexCheckbox = $("#" + indexBind);
    }
    
    var nameFieldVal = replaceInvalidChar(titleField.val());
    
    if(!indexBind || !indexCheckbox.length || !indexCheckbox.is(":checked")) {
      if(nameFieldVal.length > 50) {
        nameFieldVal = nameFieldVal.substring(0, 50); 
      }
      nameField.val(nameFieldVal);
      growField(nameField, nameFieldVal, 5, 35, 530);
    } else {
      vrtxAdmin.createDocumentFileName = nameFieldVal;
    }
  }
}

function disableReplaceTitle(nameBind) {
  if (vrtxAdmin.createResourceReplaceTitle) {
    vrtxAdmin.createResourceReplaceTitle = false;
  }
  
  var nameField = $("#" + nameBind);
  
  var currentCaretPos = getCaretPos(nameField[0]);

  var nameFieldValBeforeReplacement = nameField.val();
  var nameFieldVal = replaceInvalidChar(nameFieldValBeforeReplacement);
  nameField.val(nameFieldVal);
  growField(nameField, nameFieldVal, 5, 100, 530);
  
  setCaretToPos(nameField[0], currentCaretPos - (nameFieldValBeforeReplacement.length - nameFieldVal.length));
  
  $(".file-name-from-title").removeClass("file-name-from-title");
}

function replaceInvalidChar(val) {
  val = val.toLowerCase();
  var replaceMap = {
    " ":     "-",
    "&":     "-",
    "'":     "-",
    "\"":    "-",
    "\\/":   "-",
    "\\\\":  "-",
    "æ":     "e",
    "ø":     "o",
    "å":     "a",
    ",":     "",
    "%":     "",
    "#":     "",
    "\\?":   ""
  };

  for (var key in replaceMap) {
    var replaceThisCharGlobally = new RegExp(key, "g");
    val = val.replace(replaceThisCharGlobally, replaceMap[key]);
  }

  return val;
}

// Taken from second comment (and jquery.autocomplete.js): 
// http://stackoverflow.com/questions/499126/jquery-set-cursor-position-in-text-area
function setCaretToPos(input, pos) {
  setSelectionRange(input, pos, pos);
}

function setSelectionRange(field, start, end) {
  if (field.createTextRange) {
    var selRange = field.createTextRange();
    selRange.collapse(true);
    selRange.moveStart("character", start);
    selRange.moveEnd("character", end);
    selRange.select();
  } else if (field.setSelectionRange) {
    field.setSelectionRange(start, end);
  } else {
    if (field.selectionStart) {
      field.selectionStart = start;
      field.selectionEnd = end;
    }
  }
  field.focus();
}

// Taken from fourth comment:
// http://stackoverflow.com/questions/4928586/get-caret-position-in-html-input
function getCaretPos(input) {
  if (input.setSelectionRange) {
    return input.selectionStart;
  } else if (document.selection && document.selection.createRange) {
    var range = document.selection.createRange();
    var bookmark = range.getBookmark();
    return bookmark.charCodeAt(2) - 2;
  }
}

// jQuery autoGrowInput plugin by James Padolsey
// Modified to simplified function ++ for more specific use / event-handling by USIT, 2012
// See related thread: http://stackoverflow.com/questions/931207/is-there-a-jquery-autogrow-plugin-for-text-fields
  
function growField(input, val, comfortZone, minWidth, maxWidth) {
  var testSubject = $('<tester/>').css({
        position: 'absolute',
        top: -9999,
        left: -9999,
        width: 'auto',
        fontSize: input.css('fontSize'),
        fontFamily: input.css('fontFamily'),
        fontWeight: input.css('fontWeight'),
        letterSpacing: input.css('letterSpacing'),
        whiteSpace: 'nowrap'
      });
  input.parent().find("tester").remove(); // Remove existing test-subjects
  testSubject.insertAfter(input);  
  testSubject.html(val);
 
  // vrtxAdmin.log({msg: val});

  var newWidth = Math.min(Math.max(testSubject.width() + comfortZone, minWidth), maxWidth),
      currentWidth = input.width();
 
  // vrtxAdmin.log({msg:currentWidth + " " + newWidth});

  if (newWidth !== currentWidth) {
    input.width(newWidth);
  }
}


/*-------------------------------------------------------------------*\
    7. File upload service
\*-------------------------------------------------------------------*/

/**
 * Initialize file upload
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.initFileUpload = function initFileUpload() {
  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;
  var form = _$("form[name=fileUploadService]");
  if(!form.length) return;
  var inputFile = form.find("#file");

  _$("<div class='vrtx-textfield vrtx-file-upload'><input id='fake-file' type='text' /><a class='vrtx-button vrtx-file-upload'><span>Browse...</span></a></div>'")
    .insertAfter(inputFile);

  inputFile.addClass("js-on").change(function(e) {
    var filePath = _$(this).val();
    filePath = filePath.substring(filePath.lastIndexOf("\\")+1);
    if (vrtxAdm.supportsFileList) {
      var files = this.files;
      if (files.length > 1) {
        var tailMsg = "files selected";
        if (typeof fileUploadMoreFilesTailMessage !== "undefined") { 
          tailMsg = fileUploadMoreFilesTailMessage;
        }
        filePath = files.length + " " + tailMsg;
      }
    }
    form.find("#fake-file").val(filePath);
  });

  inputFile.hover(function () {
    _$("a.vrtx-file-upload").addClass("hover");
  }, function () {
    _$("a.vrtx-file-upload").removeClass("hover");;
  });

  if (vrtxAdm.supportsReadOnly(document.getElementById("fake-file"))) {
    form.find("#fake-file").attr("readOnly", "readOnly");  
  }
  if (vrtxAdm.supportsMultipleAttribute(document.getElementById("file"))) {
    inputFile.attr("multiple", "multiple");
    if(typeof multipleFilesInfoText !== "undefined") {
      _$("<p id='vrtx-file-upload-info-text'>" + multipleFilesInfoText + "</p>").insertAfter(".vrtx-textfield");
    }
  }
};
 
/**
 * Check if browser supports 'multiple' attribute
 * Credits: http://miketaylr.com/code/input-type-attr.html (MIT license)
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.supportsMultipleAttribute = function supportsMultipleAttribute(inputfield) {
  return (!!(inputfield.multiple === false) && !!(inputfield.multiple !== "undefined"));
};

/**
 * Check if browser supports 'readOnly' attribute
 * Credits: http://miketaylr.com/code/input-type-attr.html (MIT license)
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.supportsReadOnly = function supportsReadOnly(inputfield) {
  return (!!(inputfield.readOnly === false) && !!(inputfield.readOnly !== "undefined"));
};


/*-------------------------------------------------------------------*\
    8. Collectionlisting
       TODO: dynamic event handlers for tab-menu links
\*-------------------------------------------------------------------*/

/**
 * Initialize collection listing interaction
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.collectionListingInteraction = function collectionListingInteraction() {
  var vrtxAdm = this, _$ = vrtxAdm._$;

  if(!vrtxAdm.cachedDirectoryListing.length) return;
  
  // TODO: generalize dialog jQuery UI function with AJAX markup/text
  $(document).on("click", "a.vrtx-copy-move-to-selected-folder-disclosed", function(e) {
    var dialogTemplate = $("#vrtx-dialog-template-copy-move-content");
    if(!dialogTemplate.length) {
      vrtxAdm.serverFacade.getHtml(this.href, {
        success: function (results, status, resp) {
          _$("body").append("<div id='vrtx-dialog-template-copy-move-content'>" + _$(results).find("#vrtx-dialog-template-content").html() + "</div>");
          dialogTemplate = $("#vrtx-dialog-template-copy-move-content");
          dialogTemplate.hide();
            
          vrtxSimpleDialogs.openConfirmDialog(dialogTemplate.find(".vrtx-confirm-copy-move-explanation").text(), dialogTemplate.find(".vrtx-confirm-copy-move-confirmation").text(), function() {
            dialogTemplate.find(".vrtx-focus-button button").trigger("click");
          }, null, null);
        }
      });
    } else {
      vrtxSimpleDialogs.openConfirmDialog(dialogTemplate.find(".vrtx-confirm-copy-move-explanation").text(), dialogTemplate.find(".vrtx-confirm-copy-move-confirmation").text(), function() {
        dialogTemplate.find(".vrtx-focus-button button").trigger("click");
      }, null, null);
    }
    e.stopPropagation();
    e.preventDefault();
  });

  if(typeof moveUncheckedMessage !== "undefined") {
    vrtxAdm.placeCopyMoveButtonInActiveTab({
      formName: "collectionListingForm",
      btnId: "collectionListing\\.action\\.move-resources",
      service: "moveResourcesService",
      msg: moveUncheckedMessage,
      title: moveTitle
    });
    vrtxAdm.placeCopyMoveButtonInActiveTab({
      formName: "collectionListingForm",
      btnId: "collectionListing\\.action\\.copy-resources",
      service: "copyResourcesService",
      msg: copyUncheckedMessage,
      title: copyTitle
    });
    vrtxAdm.placeDeleteButtonInActiveTab();
    
    vrtxAdm.placePublishButtonInActiveTab();
    vrtxAdm.placeUnpublishButtonInActiveTab();
    vrtxAdm.dropdownPlain("#collection-more-menu");
  }

  vrtxAdm.placeRecoverButtonInActiveTab();
  vrtxAdm.placeDeletePermanentButtonInActiveTab();
  vrtxAdm.initializeCheckUncheckAll();
}

/**
 * Update collection listing interaction
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.updateCollectionListingInteraction = function updateCollectionListingInteraction() {
  var vrtxAdm = vrtxAdmin;
  vrtxAdm.cachedContent = vrtxAdm.cachedAppContent.find("#contents");
  vrtxAdm.cachedDirectoryListing = vrtxAdm.cachedContent.find("#directory-listing");
  vrtxAdm.cachedDirectoryListing.find("th.checkbox").append("<input type='checkbox' name='checkUncheckAll' />");
  vrtxAdm.cachedContent.find("input[type=submit]").hide();
};

/**
 * Check / uncheck all initialization
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.initializeCheckUncheckAll = function initializeCheckUncheckAll() {
  var vrtxAdm = this, _$ = vrtxAdm._$;
  
  var tdCheckbox = vrtxAdm.cachedDirectoryListing.find("td.checkbox");
  if(tdCheckbox.length) {
    vrtxAdm.cachedDirectoryListing.find("th.checkbox").append("<input type='checkbox' name='checkUncheckAll' />");
    vrtxAdm.cachedAppContent.on("click", "th.checkbox input", function(e) {
      var trigger = this;
      var checkAll = trigger.checked;
      
      $(trigger).closest("table").find("tbody tr")
                .filter(function(idx) {
                  var name = "checked";
                  if(checkAll) {
                    $(this).filter(":not(." + name + ")").addClass(name)
                      .find("td.checkbox input").attr(name, true).change();
                  } else {
                    $(this).filter("." + name).removeClass(name)
                      .find("td.checkbox input").attr(name, false).change();
                  }
                })

      e.stopPropagation();
    });
    vrtxAdm.cachedAppContent.on("click", "td.checkbox input", function(e) {
      $(this).closest("tr").toggleClass("checked");
      e.stopPropagation();
    });
  }
};

/**
 * Places Copy or Move button in active tab as link and setup dialog
 *
 * @this {VrtxAdmin}
 * @param {object} options Configuration
 * @param {string} options.service The className for service
 * @param {string} options.btnId The id for button
 * @param {string} options.msg The dialog message
 * @param {string} options.title The dialog title
 */
VrtxAdmin.prototype.placeCopyMoveButtonInActiveTab = function placeCopyMoveButtonInActiveTab(options) {
  var vrtxAdm = this, _$ = vrtxAdm._$;
  
  var btn = vrtxAdm.cachedAppContent.find("#" + options.btnId);
  if (!btn.length) return;
  btn.hide();
  var li = vrtxAdm.cachedActiveTab.find("li." + options.service);
  li.html("<a id='" + options.service + "' href='javascript:void(0);'>" + btn.attr('title') + "</a>");
  vrtxAdm.cachedActiveTab.find("#" + options.service).click(function (e) {
    if (!vrtxAdm.cachedDirectoryListing.find("td input[type=checkbox]:checked").length) {
      vrtxSimpleDialogs.openMsgDialog(options.msg, options.title);
    } else {
      vrtxAdm.cachedAppContent.find("#" + options.btnId).click();
    }
    e.stopPropagation();
    e.preventDefault();
  });
};

/**
 * Places Delete button in active tab as link and setup dialog
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.placeDeleteButtonInActiveTab = function placeDeleteButtonInActiveTab() {
  var vrtxAdm = this, _$ = vrtxAdm._$;

  var btn = vrtxAdm.cachedAppContent.find('#collectionListing\\.action\\.delete-resources');
  if (!btn.length) return;
  btn.hide();
  var li = vrtxAdm.cachedActiveTab.find('li.deleteResourcesService');
- li.html('<a id="deleteResourceService" href="javascript:void(0);">' + btn.attr('title') + '</a>');
  
  vrtxAdm.cachedActiveTab.find('#deleteResourceService').click(function (e) {
    var boxes = vrtxAdm.cachedDirectoryListing.find('td input[type=checkbox]:checked');
    var boxesSize = boxes.length;
    if (!boxesSize) {
      vrtxSimpleDialogs.openMsgDialog(deleteUncheckedMessage, deleteTitle);
    } else {
      var list = vrtxAdm.buildFileList(boxes, boxesSize, false);
      vrtxSimpleDialogs.openConfirmDialog(confirmDelete.replace("(1)", boxesSize) + '<br />' + list, confirmDeleteTitle, function() {
        vrtxAdm.cachedAppContent.find('#collectionListing\\.action\\.delete-resources').click();
      }, null, null);
    }
    e.stopPropagation();
    e.preventDefault();
  });
};

/**
 * Places Publish button in active tab as link and setup dialog
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.placePublishButtonInActiveTab = function placeDeleteButtonInActiveTab() {
  if(typeof moreTitle === "undefined") return;
  var vrtxAdm = this, _$ = vrtxAdm._$;

  var btn = vrtxAdm.cachedAppContent.find('#collectionListing\\.action\\.publish-resources');
  if (!btn.length) return;
  btn.hide();
  var li = vrtxAdm.cachedActiveTab.find('li.publishResourcesService'); li.hide();
  var menu = li.closest("#tabMenuRight");
  var html = '<li class="more-menu">'
                 + '<div id="collection-more-menu">'
                   + '<span id="collection-more-menu-header">' + moreTitle + '</span>'
                   + '<ul><li><a id="publishTheResourcesService" href="javascript:void(0);">' + btn.attr('title') + '</a></li></ul>'
                 + '</div>'
               + '</li>';
  
  menu.append(html);
  $('#publishTheResourcesService').click(function (e) {
    var boxes = vrtxAdm.cachedDirectoryListing.find('td input[type=checkbox]:checked');
    var boxesSize = boxes.length;
    if (!boxesSize) {
      vrtxSimpleDialogs.openMsgDialog(publishUncheckedMessage, publishTitle);
    } else {
      var list = vrtxAdm.buildFileList(boxes, boxesSize, false);
      vrtxSimpleDialogs.openConfirmDialog(confirmPublish.replace("(1)", boxesSize) + '<br />' + list, confirmPublishTitle, function() {
        vrtxAdm.cachedAppContent.find('#collectionListing\\.action\\.publish-resources').click();
      }, null, null);
    }
    e.stopPropagation();
    e.preventDefault();
  });
};

/**
 * Places Unpublish button in active tab as link and setup dialog
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.placeUnpublishButtonInActiveTab = function placeDeleteButtonInActiveTab() {
  if(typeof moreTitle === "undefined") return;
  var vrtxAdm = this, _$ = vrtxAdm._$;

  var btn = vrtxAdm.cachedAppContent.find('#collectionListing\\.action\\.unpublish-resources');
  if (!btn.length) return;
  btn.hide();
  var li = vrtxAdm.cachedActiveTab.find('li.unpublishResourcesService'); li.hide();
  var menu = li.closest("#tabMenuRight")
  menu.find("#collection-more-menu ul").append('<li><a id="unpublishTheResourcesService" href="javascript:void(0);">' + btn.attr('title') + '</a></li>');
  $('#unpublishTheResourcesService').click(function (e) {
    var boxes = vrtxAdm.cachedDirectoryListing.find('td input[type=checkbox]:checked');
    var boxesSize = boxes.length;
    if (!boxesSize) {
      vrtxSimpleDialogs.openMsgDialog(unpublishUncheckedMessage, unpublishTitle);
    } else {
      var list = vrtxAdm.buildFileList(boxes, boxesSize, false);
      vrtxSimpleDialogs.openConfirmDialog(confirmUnpublish.replace("(1)", boxesSize) + '<br />' + list, confirmUnpublishTitle, function() {
        vrtxAdm.cachedAppContent.find('#collectionListing\\.action\\.unpublish-resources').click();
      }, null, null);
    }
    e.stopPropagation();
    e.preventDefault();
  });
};

/**
 * Places Recover button in active tab as link and setup dialog
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.placeRecoverButtonInActiveTab = function placeRecoverButtonInActiveTab() {
  var vrtxAdm = this, _$ = vrtxAdm._$;

  var btn = vrtxAdm.cachedAppContent.find('.recoverResource');
  if (!btn.length) return;
  btn.hide();
  vrtxAdm.cachedActiveTab.prepend('<ul class="list-menu" id="tabMenuRight"><li class="recoverResourceService">'
                                + '<a id="recoverResourceService" href="javascript:void(0);">' 
                                + btn.attr('value') + '</a></li></ul>');
  vrtxAdm.cachedActiveTab.find("#recoverResourceService").click(function (e) {
    var boxes = vrtxAdm.cachedDirectoryListing.find('td input[type=checkbox]:checked');
    var boxesSize = boxes.length;
    if (!boxesSize) {
      vrtxSimpleDialogs.openMsgDialog(recoverUncheckedMessage, recoverTitle);
    } else {
      vrtxAdm.trashcanCheckedFiles = boxesSize;
      vrtxAdm.cachedAppContent.find('.recoverResource').click();
    }
    e.stopPropagation();
    e.preventDefault();
  });
};

/**
 * Places Delete Permanent button in active tab as link and setup dialog
 *
 * @this {VrtxAdmin}
 */
VrtxAdmin.prototype.placeDeletePermanentButtonInActiveTab = function placeDeletePermanentButtonInActiveTab() {
  var vrtxAdm = this, _$ = vrtxAdm._$;
  
  var btn = vrtxAdm.cachedAppContent.find('.deleteResourcePermanent');
  if (!btn.length) return;
  btn.hide();
  vrtxAdm.cachedActiveTab.find("#tabMenuRight")
    .append('<li class="deleteResourcePermanentService"><a id="deleteResourcePermanentService" href="javascript:void(0);">' 
          + btn.attr('value') + '</a></li>');
  vrtxAdm.cachedActiveTab.find('#deleteResourcePermanentService').click(function (e) {
    var boxes = vrtxAdm.cachedDirectoryListing.find('td input[type=checkbox]:checked');
    var boxesSize = boxes.length;
    if (!boxesSize) {
      vrtxSimpleDialogs.openMsgDialog(deletePermanentlyUncheckedMessage, deletePermTitle);
    } else {
      vrtxAdm.trashcanCheckedFiles = boxesSize;
      var list = vrtxAdm.buildFileList(boxes, boxesSize, true);
      vrtxSimpleDialogs.openConfirmDialog(confirmDeletePermanently.replace("(1)", boxesSize) + '<br />' + list, confirmDeletePermTitle, function() {
        vrtxAdm.cachedContent.find('.deleteResourcePermanent').click();
      }, null, null);
    }
    e.stopPropagation();
    e.preventDefault();
  });
};

/**
 * Builds a file list with ten items based on name- or title-attribute
 *
 * @this {VrtxAdmin}
 * @param {array} boxes The items
 * @param {number} boxesSize The size of the boxes
 * @param {boolean} useTitle Whether to use title- instead of name-attribute
 * @return {string} The builded HTML
 */
VrtxAdmin.prototype.buildFileList = function buildFileList(boxes, boxesSize, useTitle) {
  var boxesSizeExceedsTen = boxesSize > 10;
  var boxesSizeTmp = boxesSizeExceedsTen ? 10 : boxesSize;
  
  var fileNameAttr = useTitle ? "title" : "name";
  
  var list = "<ul>";
  for (var i = 0; i < boxesSizeTmp; i++) {
    var name = boxes[i][fileNameAttr].split("/");
    list += "<li>" + name[name.length-1] + "</li>";
  }
  list += "</ul>"; 
  if (boxesSizeExceedsTen) {
    list += "... " + confirmAnd + " " + (boxesSize - 10) + " " + confirmMore;
  }
  return list;
};

/*-------------------------------------------------------------------*\
    9. Editor
\*-------------------------------------------------------------------*/

function editorInteraction(bodyId, vrtxAdm, _$) {
  if(_$("form#editor").length) { 
    // Dropdowns
    vrtxAdm.dropdownPlain("#editor-help-menu");
    vrtxAdm.dropdown({selector: "ul#editor-menu"});

    // Save shortcut and AJAX
    $(document).bind('keydown', 'ctrl+s', function(e) {
      ctrlSEventHandler(_$, e);
    });
    vrtxAdm.cachedAppContent.on("click", ".vrtx-focus-button:last input", function(e) {
      vrtxAdm.editorSaveButtonName = _$(this).attr("name");
      ajaxSave();
      $.when(vrtxAdm.asyncEditorSavedDeferred)
          .done(function() { // TODO: show status saved msg
            vrtxAdm.removeMsg("error");
          })
          .fail(function(err) {
            if(err !== "INIT") {
              vrtxAdmin.displayErrorMsg(err);
            }
          });
      e.stopPropagation();
      e.preventDefault();
    });
  }
}

function ajaxSave() {
  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;
  
  vrtxAdm.asyncEditorSavedDeferred = _$.Deferred();

  if(typeof CKEDITOR !== "undefined") { 
    for (instance in CKEDITOR.instances) {
      CKEDITOR.instances[instance].updateElement();
    }  
  }
  var startTime = new Date();       
    
  vrtxSimpleDialogs.openLoadingDialog(ajaxSaveText);  
      
  if(typeof vrtxImageEditor !== "undefined" && vrtxImageEditor.save) {
    vrtxImageEditor.save();
   }
   if(typeof performSave !== "undefined") {      
     var ok = performSave();
     if(!ok) {
       vrtxSimpleDialogs.closeDialog("#dialog-loading");
       vrtxAdm.asyncEditorSavedDeferred.rejectWith(this, ["INIT"]);
       return false;
     }
   }
   _$("#editor").ajaxSubmit({
     success: function() {
       var endTime = new Date() - startTime;
       var waitMinMs = 800;
       if(endTime >= waitMinMs) { // Wait minimum 0.8s
         vrtxSimpleDialogs.closeDialog("#dialog-loading");
         vrtxAdm.asyncEditorSavedDeferred.resolve();
       } else {
         setTimeout(function() {
           vrtxSimpleDialogs.closeDialog("#dialog-loading");
           vrtxAdm.asyncEditorSavedDeferred.resolve();
         }, Math.round(waitMinMs - endTime));
       }
     },
     error: function(xhr, statusText, errMsg) {
       vrtxSimpleDialogs.closeDialog("#dialog-loading");
       vrtxAdm.asyncEditorSavedDeferred.rejectWith(this, [errMsg]);
     }
  });
}

function ctrlSEventHandler(_$, e) {
  if(!_$("#dialog-loading:visible").length) {
    _$(".vrtx-focus-button:last input").click();
  }
  e.preventDefault();
  return false;
}

/* Multiple inputfields */

function loadMultipleInputFields(name, addName, removeName, moveUpName, moveDownName, browseName, isMovable, isBrowsable) { // TODO: simplify
  var inputField = $("." + name + " input[type=text]");
  var inputFieldVal = inputField.val();
  if (inputFieldVal == null) {
    return;
  }

  var formFields = inputFieldVal.split(",");

  vrtxAdmin.multipleCommaSeperatedInputFieldCounter[name] = 1; // 1-index
  vrtxAdmin.multipleCommaSeperatedInputFieldLength[name] = formFields.length;
  vrtxAdmin.multipleCommaSeperatedInputFieldNames.push(name);

  var size = inputField.attr("size");

  inputFieldParent = inputField.parent();
    
  var isDropdown = inputFieldParent.hasClass("vrtx-multiple-dropdown") ? true : false;
  isMovable = isDropdown ? false : isMovable; // Turn off move-functionality tmp. if dropdown (not needed yet and needs some flicking of code)

  var inputFieldParentParent = inputFieldParent.parent();
  inputFieldParentParent.addClass("vrtx-multipleinputfields");

  if(inputFieldParentParent.hasClass("vrtx-resource-ref-browse")) {
    isBrowsable = true;
    if(inputFieldParent.next().hasClass("vrtx-button")) {
      inputFieldParent.next().hide();
    } 
  }

  if (isBrowsable && (typeof browseBase === "undefined" 
                   || typeof browseBaseFolder === "undefined"
                   || typeof browseBasePath === "undefined")) {
    isBrowsable = false; 
  }

  inputField.hide();

  var appendHtml = $.mustache(vrtxAdmin.multipleCommaSeperatedInputFieldTemplates["add-button"], { name: name, removeName: removeName, moveUpName: moveUpName, 
                                                                                                   moveDownName: moveDownName, browseName: browseName,
                                                                                                   size: size, isBrowsable: isBrowsable, isMovable: isMovable,
                                                                                                   isDropdown: isDropdown, buttonText: addName });

  inputFieldParent.removeClass("vrtx-textfield").append(appendHtml);
    
  var addFormFieldFunc = addFormField;
  for (var i = 0; i < vrtxAdmin.multipleCommaSeperatedInputFieldLength[name]; i++) {
    addFormFieldFunc(name, $.trim(formFields[i]), removeName, moveUpName, moveDownName, browseName, size, isBrowsable, true, isMovable, isDropdown);
  }
      
  autocompleteUsernames(".vrtx-autocomplete-username");
}

function initMultipleInputFields() {
  vrtxAdmin.cachedAppContent.on("click", ".vrtx-multipleinputfield button.remove", function(e){
    removeFormField($(this));
    e.preventDefault();
    e.stopPropagation();
  });
  vrtxAdmin.cachedAppContent.on("click", ".vrtx-multipleinputfield button.moveup", function(e){
    moveUpFormField($(this));
    e.preventDefault();
    e.stopPropagation();
  });
  vrtxAdmin.cachedAppContent.on("click", ".vrtx-multipleinputfield button.movedown", function(e){
    moveDownFormField($(this));
    e.preventDefault();
    e.stopPropagation();
  });
  vrtxAdmin.cachedAppContent.on("click", ".vrtx-multipleinputfield button.browse-resource-ref", function(e){
    browseServer($(this).closest(".vrtx-multipleinputfield").find('input').attr('id'), browseBase, browseBaseFolder, browseBasePath, 'File');
    e.preventDefault();
    e.stopPropagation();
  });
  
  // Retrieve HTML templates
  vrtxAdmin.multipleCommaSeperatedInputFieldDeferred = $.Deferred();
  vrtxAdmin.multipleCommaSeperatedInputFieldTemplates = vrtxAdmin.retrieveHTMLTemplates("multiple-inputfields",
                                                                                        ["button", "add-button", "multiple-inputfield"],
                                                                                        vrtxAdmin.multipleCommaSeperatedInputFieldDeferred);
}

function addFormField(name, value, removeName, moveUpName, moveDownName, browseName, size, isBrowsable, init, isMovable, isDropdown) {
  if (value == null) value = "";

  var idstr = "vrtx-" + name + "-",
      i = vrtxAdmin.multipleCommaSeperatedInputFieldCounter[name],
      removeButton = "", moveUpButton = "", moveDownButton = "", browseButton = "";

  if (removeName) {
    removeButton = $.mustache(vrtxAdmin.multipleCommaSeperatedInputFieldTemplates["button"], { type: "remove", name: " " + name, 
                                                                                               idstr: idstr,   buttonText: removeName });
  }
  if (isMovable && moveUpName && i > 1) {
    moveUpButton = $.mustache(vrtxAdmin.multipleCommaSeperatedInputFieldTemplates["button"], { type: "moveup", name: "", 
                                                                                               idstr: idstr,   buttonText: "&uarr; " + moveUpName });
  }
  if (isMovable && moveDownName && i < vrtxAdmin.multipleCommaSeperatedInputFieldLength[name]) {
    moveDownButton = $.mustache(vrtxAdmin.multipleCommaSeperatedInputFieldTemplates["button"], { type: "movedown", name: "", 
                                                                                                 idstr: idstr,     buttonText: "&darr; " + moveDownName });
  }
  if(isBrowsable) {
    browseButton = $.mustache(vrtxAdmin.multipleCommaSeperatedInputFieldTemplates["button"], { type: "browse", name: "-resource-ref", 
                                                                                               idstr: idstr,   buttonText: browseName });
  }
    
  var html = $.mustache(vrtxAdmin.multipleCommaSeperatedInputFieldTemplates["multiple-inputfield"], { idstr: idstr, i: i, value: value, 
                                                                                                      size: size, browseButton: browseButton,
                                                                                                      removeButton: removeButton, moveUpButton: moveUpButton,
                                                                                                      moveDownButton: moveDownButton, isDropdown: isDropdown,
                                                                                                      dropdownArray: "dropdown" + name });

  $(html).insertBefore("#vrtx-" + name + "-add");
    
  if(!init) {
    if(vrtxAdmin.multipleCommaSeperatedInputFieldLength[name] > 0 && isMovable) {
      var fields = $("." + name + " div.vrtx-multipleinputfield");
      if(fields.eq(vrtxAdmin.multipleCommaSeperatedInputFieldLength[name] - 1).not("has:button.movedown")) {
        moveDownButton = $.mustache(vrtxAdmin.multipleCommaSeperatedInputFieldTemplates["button"], { type: "movedown", name: "", 
                                                                                                     idstr: idstr,     buttonText: "&darr; " + moveDownName });
        fields.eq(vrtxAdmin.multipleCommaSeperatedInputFieldLength[name] - 1).append(moveDownButton);
      }
    }
    vrtxAdmin.multipleCommaSeperatedInputFieldLength[name]++;
    autocompleteUsername(".vrtx-autocomplete-username", idstr + i);
  }

  vrtxAdmin.multipleCommaSeperatedInputFieldCounter[name]++;   
}

function removeFormField(input) {
  var name = input.attr("class").replace("remove ", "");
  input.closest(".vrtx-multipleinputfield").remove();

  vrtxAdmin.multipleCommaSeperatedInputFieldLength[name]--;
  vrtxAdmin.multipleCommaSeperatedInputFieldCounter[name]--;

  var fields = $("." + name + " div.vrtx-multipleinputfield");

  if(fields.eq(vrtxAdmin.multipleCommaSeperatedInputFieldLength[name] - 1).has("button.movedown")) {
    fields.eq(vrtxAdmin.multipleCommaSeperatedInputFieldLength[name] - 1).find("button.movedown").parent().remove();
  }
  if(fields.eq(0).has("button.moveup")) {
    fields.eq(0).find("button.moveup").parent().remove();
  }
}

function moveUpFormField(input) {
  var parent = input.closest(".vrtx-multipleinputfield");
  var thisInput = parent.find("input");
  var prevInput = parent.prev().find("input");
  var thisText = thisInput.val();
  var prevText = prevInput.val();
  thisInput.val(prevText);
  prevInput.val(thisText);
}

function moveDownFormField(input) {
  var parent = input.closest(".vrtx-multipleinputfield");
  var thisInput = parent.find("input");
  var nextInput = parent.next().find("input");
  var thisText = thisInput.val();
  var nextText = nextInput.val();
  thisInput.val(nextText);
  nextInput.val(thisText);
}

function saveMultipleInputFields() {
  var formatMultipleInputFieldsFunc = formatMultipleInputFields;
  for(var i = 0, len = vrtxAdmin.multipleCommaSeperatedInputFieldNames.length; i < len; i++){
    formatMultipleInputFields(vrtxAdmin.multipleCommaSeperatedInputFieldNames[i]);
  }
}

function formatMultipleInputFields(name) {
  var multipleTxt = $("." + name + " input[type=text]").filter(":hidden");;
  if (multipleTxt.val() == null) return;

  var allFields = $("input[type=text][id^='vrtx-" + name + "']");
  var isDropdown = false;
  if(!allFields.length) {
    allFields = $("select[id^='vrtx-" + name + "']");
    if(allFields.length) {
      isDropdown = true;
    } else {
      multipleTxt.val("");
      return;
    }
  }
  
  for (var i = 0, len = allFields.length, result = ""; i < len; i++) {
    result += isDropdown ? $.trim($(allFields[i]).find("option:selected").val()) : $.trim(allFields[i].value);
    if (i < (len-1)) {
      result += ",";
    }
  }
  
  multipleTxt.val(result);
}

/* Show and hide properties
 *
 * @param radioIds: Multiple id's for radiobuttons binding click events (Array)
 * @param conditionHide: Condition to be checked for hiding
 * @param conditionHideEqual: What it should equal
 * @param showHideProps: Multiple props / id's / classnames to show / hide (Array)
 */
function showHide(radioIds, conditionHide, conditionHideEqual, showHideProps) {
  var showHidePropertiesFunc = showHideProperties;
  showHidePropertiesFunc(true, conditionHide, conditionHideEqual, showHideProps); // Init
  for (var j = 0, len = radioIds.length; j < len; j++) {
    $(radioIds[j]).click(function () {
      showHidePropertiesFunc(false, conditionHide, conditionHideEqual, showHideProps);
    });
  }
}

function showHideProperties(init, conditionHide, conditionHideEqual, showHideProps) {
  for (var conditionHideVal = $(conditionHide).val(), showHidePropertyFunc = showHideProperty, 
       i = 0, len = showHideProps.length; i < len; i++) {
    showHidePropertyFunc(showHideProps[i], init, conditionHideVal == conditionHideEqual ? false : true);
  }
}

function showHideProperty(id, init, show) {
  init ? show ? $(id).show() 
              : $(id).hide()
       : show ? $(id).slideDown(vrtxAdmin.transitionPropSpeed, vrtxAdmin.transitionEasingSlideDown)
              : $(id).slideUp(vrtxAdmin.transitionPropSpeed, vrtxAdmin.transitionEasingSlideUp);
}


/*-------------------------------------------------------------------*\
    10. Permissions
\*-------------------------------------------------------------------*/

function initPermissionForm(selectorClass) {
  if (!$("." + selectorClass + " .aclEdit").length) return;
  toggleConfigCustomPermissions(selectorClass);
  interceptEnterKeyAndReroute("." + selectorClass + " .addUser input[type=text]", "." + selectorClass + " input.addUserButton");
  interceptEnterKeyAndReroute("." + selectorClass + " .addGroup input[type=text]", "." + selectorClass + " input.addGroupButton");
  initSimplifiedPermissionForm();
}

function initSimplifiedPermissionForm() {
  permissionsAutocomplete('userNames', 'userNames', vrtxAdmin.permissionsAutocompleteParams, false);
  splitAutocompleteSuggestion('userNames');
  permissionsAutocomplete('groupNames', 'groupNames', vrtxAdmin.permissionsAutocompleteParams, false);  
}

function toggleConfigCustomPermissions(selectorClass) {
  var customInput = $("." + selectorClass + " ul.shortcuts label[for=custom] input");
  if (!customInput.is(":checked") && customInput.length) {
    $("." + selectorClass).find(".principalList").addClass("hidden");
  }
  vrtxAdmin.cachedAppContent.delegate("." + selectorClass + " ul.shortcuts label[for=custom]", "click", function (e) {
    $(this).closest("form").find(".principalList.hidden").slideDown(vrtxAdmin.transitionCustomPermissionSpeed, vrtxAdmin.transitionEasingSlideDown, function() {
      $(this).removeClass("hidden");
    });
    e.stopPropagation(); 
  });
  vrtxAdmin.cachedAppContent.delegate("." + selectorClass + " ul.shortcuts label:not([for=custom])", "click", function (e) {
    $(this).closest("form").find(".principalList:not(.hidden)").slideUp(vrtxAdmin.transitionCustomPermissionSpeed, vrtxAdmin.transitionEasingSlideUp, function() {
      $(this).addClass("hidden");
    });
    e.stopPropagation(); 
  });
}

function checkStillAdmin(options) {
  var stillAdmin = options.form.find(".still-admin").text();
  vrtxAdmin.reloadFromServer = false;
  if(stillAdmin == "false") {
    vrtxAdmin.reloadFromServer = true;
    vrtxSimpleDialogs.openConfirmDialog(removeAdminPermissionsMsg, removeAdminPermissionsTitle, vrtxAdmin.completeFormAsyncPost, function() {
      vrtxAdmin.reloadFromServer = false;
    }, options);
  } else {
    vrtxAdmin.completeFormAsyncPost(options);
  }
}

function autocompleteUsernames(selector) {
  var _$ = vrtxAdmin._$;
  var autocompleteTextfields = _$(selector).find('.vrtx-textfield input');
  var i = autocompleteTextfields.length;
  while(i--) {
    permissionsAutocomplete(_$(autocompleteTextfields[i]).attr("id"), 'userNames', vrtxAdmin.usernameAutocompleteParams, true);
  }
}

function autocompleteUsername(selector, subselector) {
  var autocompleteTextfield = vrtxAdmin._$(selector).find('input#' + subselector);
  if(autocompleteTextfield.length) {
    permissionsAutocomplete(subselector, 'userNames', vrtxAdmin.usernameAutocompleteParams, true);
  }
}

function autocompleteTags(selector) {
  var _$ = vrtxAdmin._$;
  var autocompleteTextfields = _$(selector).find('.vrtx-textfield input');
  var i = autocompleteTextfields.length;
  while(i--) {
    setAutoComplete(_$(autocompleteTextfields[i]).attr("id"), 'tags', vrtxAdmin.tagAutocompleteParams);
  }
}


/*-------------------------------------------------------------------*\
    11. Reports
\*-------------------------------------------------------------------*/

function reportsInteraction(bodyId, vrtxAdm, _$) {
  vrtxAdm.cachedAppContent.on("click", "td.vrtx-report-broken-links-web-page a", function(e) {
    var openedWebpageWithBrokenLinks = openRegular(this.href, 1020, 800, "DisplayWebpageBrokenLinks");
    e.stopPropagation();
    e.preventDefault();
  });
  var brokenLinksFilters = _$("#vrtx-report-filters");
  if(brokenLinksFilters.length) {
    brokenLinksFilters.append("<a href='#' id='vrtx-report-filters-show-hide-advanced' onclick='javascript:void(0);'>" + filtersAdvancedShow + "...</a>");
    // TODO: Generate HTML with Mustache
    var html = "<div id='vrtx-report-filters-folders-include-exclude' class='solidExpandedForm'>"
               + "<h3>" + filtersAdvancedTitle + "</h3>"
               + "<div id='vrtx-report-filters-folders-exclude' class='report-filters-folders-exclude'><h4>" + filtersAdvancedExcludeTitle + "</h4>"
               + "<div class='vrtx-textfield'><input type='text' id='exclude-folders' size='25' /></div></div>"
               + "<div id='vrtx-report-filters-folders-include' class='report-filters-folders-include'><h4>" + filtersAdvancedIncludeTitle + "</h4>"
               + "<div class='vrtx-textfield'><input type='text' id='include-folders' size='25' /></div></div>"
               + "<a class='vrtx-button'><span>" + filtersAdvancedUpdate + "</span></a>"
            + "</div>";
    _$(html).insertAfter(brokenLinksFilters);

    var pairs = location.search.split(/\&/);
    var pairsLen = pairs.length;
    var includedFolders = "", excludedFolders = "", query = "", pair = "";
    for(var i = 0; i < pairsLen; i++) { // Add include folders
      if(pairs[i].match(/^include-path/g)) {
        pair = decodeURIComponent(pairs[i]);
        includedFolders += pair.split("=")[1] + ", ";
        query += "&" + pair;
      }
    }
    for(i = 0; i < pairsLen; i++) { // Add exclude folders
      if(pairs[i].match(/^exclude-path/g)) {
        pair = decodeURIComponent(pairs[i]);
        excludedFolders += pair.split("=")[1] + ", ";
        query += "&" + pair;
      }   
    }
    
    var filterLinks = _$("#vrtx-report-filters ul a");
    i = filterLinks.length;
    while(i--) {
      filterLinks[i].href = filterLinks[i].href + query;
    }
    
    // If any included or excluded folders show advanced settings
    if(includedFolders.length || excludedFolders.length) { 
      _$("#vrtx-report-filters-folders-include-exclude").slideToggle(0);
      _$("#vrtx-report-filters-show-hide-advanced").text(filtersAdvancedHide + "...");
    }
    
    _$("#include-folders").val(includedFolders.substring(0, includedFolders.lastIndexOf(",")));
    _$("#exclude-folders").val(excludedFolders.substring(0, excludedFolders.lastIndexOf(",")));
    
    vrtxAdm.cachedAppContent.on("click", "#vrtx-report-filters #vrtx-report-filters-show-hide-advanced", function(e) { // Show / hide advanced settings
      var container = _$("#vrtx-report-filters-folders-include-exclude");
      if(container.is(":visible")) {
        container.slideUp(vrtxAdm.transitionSpeed, vrtxAdmin.transitionEasingSlideUp, function() {
          _$("#vrtx-report-filters-show-hide-advanced").text(filtersAdvancedShow + "...");
        });
      } else {
        container.slideDown(vrtxAdm.transitionSpeed, vrtxAdmin.transitionEasingSlideDown, function() {
          _$("#vrtx-report-filters-show-hide-advanced").text(filtersAdvancedHide + "...");
        });
      }
      e.stopPropagation();
      e.preventDefault();
    });
    vrtxAdm.cachedAppContent.on("click", "#vrtx-report-filters-folders-include-exclude a.vrtx-button", function(e) { // Filter exclude and include folders
      saveMultipleInputFields(); // Multiple to comma-separated
      // Build query string
      var includeFolders = unique($("#include-folders").val().split(",")); // Get included folders and remove duplicates
      var excludeFolders = unique($("#exclude-folders").val().split(",")); // Get excluded folders and remove duplicates
      var includeFoldersLen = includeFolders.length, excludeFoldersLen = excludeFolders.length,
          includeQueryString = "", excludeQueryString = ""; 
      for(var i = 0; i < includeFoldersLen; i++) {
        var theIncludeFolder = $.trim(includeFolders[i]);
        if(theIncludeFolder.length) {
          includeQueryString += "&include-path=" + encodeURIComponent(theIncludeFolder);
        }     
      }
      for(i = 0; i < excludeFoldersLen; i++) {
        var theExcludeFolder = $.trim(excludeFolders[i]);
        if(theExcludeFolder.length) {
          excludeQueryString += "&exclude-path=" + encodeURIComponent(theExcludeFolder);
        }
      }
      // Update URL in address bar
      var thehref = location.href;
      var indexOfIncludeFolder = thehref.indexOf("&include-path"),
          indexOfExcludeFolder = thehref.indexOf("&exclude-path"),
          indexOfIncludeORExcludeFolder = (indexOfIncludeFolder !== -1) ? (indexOfExcludeFolder !== -1) 
                                                                        ? Math.min(indexOfIncludeFolder, indexOfExcludeFolder)
                                                                        : indexOfIncludeFolder : indexOfExcludeFolder;
      if(indexOfIncludeORExcludeFolder !== -1) {
        location.href = thehref.substring(0, indexOfIncludeORExcludeFolder) + includeQueryString + excludeQueryString;
      } else {
        location.href = thehref + includeQueryString + excludeQueryString;      
      }
      e.stopPropagation();
      e.preventDefault();
    });
  }
}


/*-------------------------------------------------------------------*\
    12. Versioning
\*-------------------------------------------------------------------*/

function versioningInteraction(bodyId, vrtxAdm, _$) {
  vrtxAdm.cachedAppContent.on("click", "a.vrtx-revision-view", function(e) {
    var openedRevision = openRegular(this.href, 1020, 800, "DisplayRevision");
    e.stopPropagation();
    e.preventDefault();
  });

  if(bodyId == "vrtx-revisions") {
    var contents = _$("#contents");
    
    // Delete revisions
    contents.on("click", ".vrtx-revisions-delete-form input[type=submit]", function(e) {
      var form = _$(this).closest("form")
      var url = form.attr("action");
      var dataString = form.serialize();
      vrtxAdm.serverFacade.postHtml(url, dataString, {
        success: function (results, status, resp) {
          var tr = form.closest("tr");
          if(vrtxAdm.animateTableRows) {
            tr.prepareTableRowForSliding().hide(0).slideDown(0, "linear");
          }
          // Check when multiple animations are complete; credits: http://tinyurl.com/83oodnp
          var animA = tr.find("td").animate({paddingTop: '0px', paddingBottom: '0px'}, 
                                             vrtxAdm.transitionDropdownSpeed, vrtxAdm.transitionEasingSlideUp, _$.noop);
          var animB = tr.slideUp(vrtxAdm.transitionDropdownSpeed, vrtxAdm.transitionEasingSlideUp, _$.noop);
          _$.when(animA, animB).done(function() {
            var result = _$(results);
            contents.html(result.find("#contents").html());
            _$("#app-tabs").html(result.find("#app-tabs").html());
          });
        }
      });
      e.stopPropagation();
      e.preventDefault();
    });
    
    // Restore revisions
    contents.on("click", ".vrtx-revisions-restore-form input[type=submit]", function(e) {
      var form = _$(this).closest("form")
      var url = form.attr("action");
      var dataString = form.serialize();
      _$("td.vrtx-revisions-buttons-column input").attr("disabled", "disabled"); // Lock buttons
      vrtxAdm.serverFacade.postHtml(url, dataString, {
        success: function (results, status, resp) {
          contents.html($(results).find("#contents").html());
          if(typeof versionsRestoredInfoMsg !== "undefined") {         
            var revisionNr = url.substring(url.lastIndexOf("=")+1, url.length);
            var versionsRestoredInfoMsgTmp = versionsRestoredInfoMsg.replace("X", revisionNr);
            vrtxAdm.displayInfoMsg(versionsRestoredInfoMsgTmp);
          }
          scroll(0,0);
        },
        error: function (xhr, textStatus) {
          _$("td.vrtx-revisions-buttons-column input").removeAttr("disabled"); // Unlock buttons
        }
      });
      e.stopPropagation();
      e.preventDefault();
    });
    
    // Make working copy into current version
    contents.on("click", "#vrtx-revisions-make-current-form input[type=submit]", function(e) {
      var form = _$(this).closest("form")
      var url = form.attr("action");
      var dataString = form.serialize();
      vrtxAdm.serverFacade.postHtml(url, dataString, {
        success: function (results, status, resp) {
          contents.html(_$(results).find("#contents").html());
          _$("#app-tabs").html(_$(results).find("#app-tabs").html());
          if(typeof versionsMadeCurrentInfoMsg !== "undefined") {
            vrtxAdm.displayInfoMsg(versionsMadeCurrentInfoMsg);
          }
        }
      });
      e.stopPropagation();
      e.preventDefault();
    });
  }
}


/*-------------------------------------------------------------------*\
    13. Async functions  
\*-------------------------------------------------------------------*/

/**
 * Retrieve a form async
 *
 * @this {VrtxAdmin}
 * @param {object} options Configuration
 * @param {string} options.selector Selector for links that should retrieve a form async
 * @param {string} options.selectorClass Selector for form
 * @param {string} options.insertAfterOrReplaceClass Where to put the form
 * @param {boolean} options.isReplacing Whether to replace instead of insert after
 * @param {string} options.nodeType Node type that should be replaced or inserted
 * @param {function} options.funcComplete Callback function to run on success
 * @param {boolean} options.simultanSliding Whether to slideUp existing form at the same time slideDown new form (only when there is an existing form)
 * @param {number} options.transitionSpeed Transition speed in ms
 * @param {string} options.transitionEasingSlideDown Transition easing algorithm for slideDown()
 * @param {string} options.transitionEasingSlideUp Transition easing algorithm for slideUp()
 * @return {boolean} Whether or not to proceed with regular link operation
 */
VrtxAdmin.prototype.getFormAsync = function getFormAsync(options) {
  var args = arguments,       // this function
      vrtxAdm = this,         // use prototypal hierarchy 
      _$ = vrtxAdm._$;
      
  vrtxAdm.cachedBody.dynClick(options.selector, function (e) {
    var url = _$(this).attr("href") || _$(this).closest("form").attr("action");
    
    if(vrtxAdm.asyncGetFormsInProgress) { // If there are any getFormAsync() in progress
      return false;
    }
    vrtxAdm.asyncGetFormsInProgress++;

    var selector = options.selector,
        selectorClass = options.selectorClass,
        simultanSliding = options.simultanSliding,
        transitionSpeed = ((options.transitionSpeed != null) ? options.transitionSpeed : vrtxAdm.transitionSpeed),
        transitionEasingSlideDown = options.transitionEasingSlideDown || vrtxAdm.transitionEasingSlideDown,
        transitionEasingSlideUp = options.transitionEasingSlideUp || vrtxAdm.transitionEasingSlideUp,
        modeUrl = location.href,
        fromModeToNotMode = false,
        existExpandedFormIsReplaced = false,
        expandedForm = $(".expandedForm"),
        existExpandedForm = expandedForm.length;

    // Make sure we get the mode markup (current page) if service is not mode
    // -- only if a expandedForm exists and is of the replaced kind..
    //
    if(existExpandedForm && expandedForm.hasClass("expandedFormIsReplaced")) {                      
      if(url.indexOf("&mode=") == -1 && modeUrl.indexOf("&mode=") != -1) {
        fromModeToNotMode = true; 
      }
      existExpandedFormIsReplaced = true;
    }
    
    vrtxAdmin.serverFacade.getHtml(url, {
      success: function (results, status, resp) {
        var form = _$(results).find("." + selectorClass).html();

        // If something went wrong
        if(!form) {
          vrtxAdm.error({args: args, msg: "retrieved form from " + url + " is null"});
          if(vrtxAdm.asyncGetFormsInProgress) {
            vrtxAdm.asyncGetFormsInProgress--;
          }
          return;    
        }
        // Another form is already open
        if(existExpandedForm) {
          // Get class for original markup
          var resultSelectorClasses = expandedForm.attr("class").split(" ");
          var resultSelectorClass = "";
          var ignoreClasses = {"even":"", "odd":"", "first":"", "last":""};
          for(var i = resultSelectorClasses.length; i--;) {
            var resultSelectorClassCache = resultSelectorClasses[i];
            if(resultSelectorClassCache && resultSelectorClassCache != ""
               && !(resultSelectorClassCache in ignoreClasses)) {
                 resultSelectorClass = "." + resultSelectorClasses[i];
                 break;
            }  
          }

          expandedForm.slideUp(transitionSpeed, transitionEasingSlideUp, function() {
            if(existExpandedFormIsReplaced) {
              if(fromModeToNotMode) { // When we need the 'mode=' HTML when requesting a 'not mode=' service
                vrtxAdmin.serverFacade.getHtml(modeUrl, {
                  success: function (results, status, resp) {
                    var succeededAddedOriginalMarkup = vrtxAdm.addOriginalMarkup(modeUrl, results, resultSelectorClass, expandedForm);
                    if(succeededAddedOriginalMarkup) {
                      vrtxAdm.addNewMarkup(options, selectorClass, transitionSpeed, transitionEasingSlideDown, transitionEasingSlideUp, form);
                    } else {
                      if(vrtxAdm.asyncGetFormsInProgress) {
                        vrtxAdm.asyncGetFormsInProgress--;
                      }
                    }
                  },
                  error: function(xhr, textStatus) {
                    if(vrtxAdm.asyncGetFormsInProgress) {
                      vrtxAdm.asyncGetFormsInProgress--;
                    }
                  }
                });
              } else {
                var succeededAddedOriginalMarkup = vrtxAdm.addOriginalMarkup(url, results, resultSelectorClass, expandedForm);
              }
            } else {
              var node = _$(this).parent().parent();
              if(node.is("tr") && vrtxAdm.animateTableRows) {  // Because 'this' can be tr > td > div
                node.remove();
              } else {
                _$(this).remove();            
              }
            }
            if(!simultanSliding && !fromModeToNotMode) {
              if(typeof succeededAddedOriginalMarkup !== "undefined" && !succeededAddedOriginalMarkup) {
                if(vrtxAdm.asyncGetFormsInProgress) {
                  vrtxAdm.asyncGetFormsInProgress--;
                }
              } else {
                vrtxAdm.addNewMarkup(options, selectorClass, transitionSpeed, transitionEasingSlideDown, transitionEasingSlideUp, form);
              }
            }
          });
        }
        if ((!existExpandedForm || simultanSliding) && !fromModeToNotMode) {
          vrtxAdm.addNewMarkup(options, selectorClass, transitionSpeed, transitionEasingSlideDown, transitionEasingSlideUp, form);
        }
      },
      error: function(xhr, textStatus) {
        if(vrtxAdm.asyncGetFormsInProgress) {
          vrtxAdm.asyncGetFormsInProgress--;
        }
      }
    });
    e.stopPropagation();
    e.preventDefault();
  });
};

/**
 * Add original form markup after async retrieve
 *
 * @this {VrtxAdmin}
 * @param {string} url The URL for original markup
 * @param {object} results The results
 * @param {string} resultSelectorClass Selector for original form markup
 * @param {object} expanded The expanded form
 * @return {boolean} Whether it succeeded or not
 */
VrtxAdmin.prototype.addOriginalMarkup = function addOriginalMarkup(url, results, resultSelectorClass, expanded) {
  var args = arguments,
      vrtxAdm = this;

  var resultHtml = vrtxAdm.outerHTML(results, resultSelectorClass);
  if(!resultHtml) { // If all went wrong
    vrtxAdm.error({args: args, msg: "trying to retrieve existing expandedForm from " + url + " returned null"});
    return false;
  }
  var node = expanded.parent().parent();
  if(node.is("tr") && vrtxAdm.animateTableRows) {  // Because 'this' can be tr > td > div
    node.replaceWith(resultHtml).show(0);
  } else {
    expanded.replaceWith(resultHtml).show(0);              
  }
  return true;
};

/**
 * Add new form markup after async retrieve
 *
 * @this {VrtxAdmin}
 * @param {object} options Configuration
 * @param {string} selectorClass The selector for form
 * @param {string} transitionSpeed Transition speed in ms
 * @param {string} transitionEasingSlideDown Transition easing algorithm for slideDown()
 * @param {string} transitionEasingSlideUp Transition easing algorithm for slideUp()
 * @param {object} form The form
 */
VrtxAdmin.prototype.addNewMarkup = function addNewMarkup(options, selectorClass, transitionSpeed, transitionEasingSlideDown, transitionEasingSlideUp, form) {
  var vrtxAdm = this,
      insertAfterOrReplaceClass = options.insertAfterOrReplaceClass,
      secondaryInsertAfterOrReplaceClass = options.secondaryInsertAfterOrReplaceClass,
      isReplacing = options.isReplacing || false,
      nodeType = options.nodeType,
      funcComplete = options.funcComplete,
      _$ = vrtxAdm._$;
   
  var inject = _$(insertAfterOrReplaceClass);
  if(!inject.length) {
    inject = _$(secondaryInsertAfterOrReplaceClass);
  }

  if (isReplacing) {
    var classes = inject.attr("class");
    inject.replaceWith(vrtxAdm.wrap(nodeType, "expandedForm expandedFormIsReplaced nodeType"
                                                         + nodeType + " " + selectorClass + " " + classes, form));
  } else {
    _$(vrtxAdm.wrap(nodeType, "expandedForm nodeType" + nodeType + " " + selectorClass, form))
      .insertAfter(inject);
  }
  if(funcComplete) {
    funcComplete(selectorClass);
  }
  if(vrtxAdm.asyncGetFormsInProgress) {
    vrtxAdm.asyncGetFormsInProgress--;
  }
  if(nodeType == "tr" && vrtxAdm.animateTableRows) {
    _$(nodeType + "." + selectorClass).prepareTableRowForSliding();
  }
  _$(nodeType + "." + selectorClass).hide().slideDown(transitionSpeed, transitionEasingSlideDown, function() {
    _$(this).find("input[type=text]:visible:first").focus();
  });
};

/**
 * Complete a form async
 *
 * @this {VrtxAdmin}
 * @param {object} options Configuration
 * @param {string} options.selector Selector for links that should complete a form async
 * @param {boolean} options.isReplacing Whether to replace instead of insert after
 * @param {string} options.updateSelectors One or more containers that should update after POST
 * @param {string} options.errorContainerInsertAfter Selector where to place the new error container
 * @param {string} options.errorContainer The className of the error container
 * @param {function} options.funcProceedCondition Callback function that proceedes with completeFormAsyncPost(options)
 * @param {function} options.funcComplete Callback function to run on success
 * @param {number} options.transitionSpeed Transition speed in ms
 * @param {string} options.transitionEasingSlideDown Transition easing algorithm for slideDown()
 * @param {string} options.transitionEasingSlideUp Transition easing algorithm for slideUp()
 * @param {boolean} options.post POST or only cancel
 * @return {boolean} Whether or not to proceed with regular link operation
 */
VrtxAdmin.prototype.completeFormAsync = function completeFormAsync(options) {
  var args = arguments,
      vrtxAdm = this,
      _$ = vrtxAdm._$;
      
  vrtxAdm.cachedBody.dynClick(options.selector, function (e) {
  
    var isReplacing = options.isReplacing || false,
        funcProceedCondition = options.funcProceedCondition,
        funcComplete = options.funcComplete,
        transitionSpeed = ((options.transitionSpeed != null) ? options.transitionSpeed : vrtxAdm.transitionSpeed),
        transitionEasingSlideDown = options.transitionEasingSlideDown || vrtxAdm.transitionEasingSlideDown,
        transitionEasingSlideUp = options.transitionEasingSlideUp || vrtxAdm.transitionEasingSlideUp,
        post = options.post || false,
        link = _$(this),
        isCancelAction = link.attr("name").toLowerCase().indexOf("cancel") != -1;
    
    if(!post) {
      if(isCancelAction && !isReplacing) {
        _$(".expandedForm").slideUp(transitionSpeed, transitionEasingSlideUp, function() {
          _$(this).remove();
        });
        e.preventDefault();
      } else {
        e.stopPropagation();
        if(funcComplete && !isCancelAction) {
          var returnVal = funcComplete(link);
          return returnVal;
        } else {
          return;
        }
      }
    } else {
      options.form = link.closest("form");;
      options.link = link;
      if(funcProceedCondition && !isCancelAction) {
        funcProceedCondition(options);
      } else {
        vrtxAdm.completeFormAsyncPost(options);
      }
      e.stopPropagation();
      e.preventDefault();
    }
  });
};

/**
 * Complete a form async POST
 *
 * @this {VrtxAdmin}
 * @param {object} options Configuration
 * @param {object} options.form The form
 * @param {object} options.link The action link
 */
VrtxAdmin.prototype.completeFormAsyncPost = function completeFormAsyncPost(options) {
    var vrtxAdm = vrtxAdmin,
        _$ = vrtxAdm._$,
        selector = options.selector,
        isReplacing = options.isReplacing || false,
        updateSelectors = options.updateSelectors,
        errorContainer = options.errorContainer,
        errorContainerInsertAfter = options.errorContainerInsertAfter,
        funcComplete = options.funcComplete,
        transitionSpeed = options.transitionSpeed || vrtxAdm.transitionSpeed,
        transitionEasingSlideDown = options.transitionEasingSlideDown || vrtxAdm.transitionEasingSlideDown,
        transitionEasingSlideUp = options.transitionEasingSlideUp || vrtxAdm.transitionEasingSlideUp,
        form = options.form,
        link = options.link,
        url = form.attr("action"),
        dataString = form.serialize() + "&" + link.attr("name"),
        modeUrl = location.href;
                      
  vrtxAdmin.serverFacade.postHtml(url, dataString, {
    success: function (results, status, resp) {
      if (vrtxAdm.hasErrorContainers(results, errorContainer)) {
        vrtxAdm.displayErrorContainers(results, form, errorContainerInsertAfter, errorContainer);
      } else {
        if (isReplacing) {
          form.parent().slideUp(transitionSpeed, transitionEasingSlideUp, function () {
            for (var i = updateSelectors.length; i--;) {
              var outer = vrtxAdm.outerHTML(results, updateSelectors[i]);
              vrtxAdm.cachedBody.find(updateSelectors[i]).replaceWith(outer);
            }
            var resultsResourceTitle = _$(results).find("#resource-title");
            var currentResourceTitle = vrtxAdm.cachedBody.find("#resource-title");
            if(resultsResourceTitle.length && currentResourceTitle.length) {
              if(resultsResourceTitle.hasClass("compact") && !currentResourceTitle.hasClass("compact")) {
                currentResourceTitle.addClass("compact");
              } else if(!resultsResourceTitle.hasClass("compact") && currentResourceTitle.hasClass("compact")) {
                currentResourceTitle.removeClass("compact");
              }
            }
            if (funcComplete) {
              funcComplete();
            }
          });
        } else {
          var sameMode = false;
          if(url.indexOf("&mode=") !== -1) {
            if(gup("mode", url) === gup("mode", modeUrl)) {
              sameMode = true;
            }
          }
          if(modeUrl.indexOf("&mode=") !== -1 && !sameMode) { // When we need the 'mode=' HTML. TODO: should only run when updateSelector is inside content
            vrtxAdmin.serverFacade.getHtml(modeUrl, {
              success: function (results, status, resp) {
                for (var i = updateSelectors.length; i--;) {
                  var outer = vrtxAdm.outerHTML(results, updateSelectors[i]);
                  vrtxAdm.cachedBody.find(updateSelectors[i]).replaceWith(outer);
                }
                var resultsResourceTitle = _$(results).find("#resource-title");
                var currentResourceTitle = vrtxAdm.cachedBody.find("#resource-title");
                if(resultsResourceTitle.length && currentResourceTitle.length) {
                  if(resultsResourceTitle.hasClass("compact") && !currentResourceTitle.hasClass("compact")) {
                    currentResourceTitle.addClass("compact");
                  } else if(!resultsResourceTitle.hasClass("compact") && currentResourceTitle.hasClass("compact")) {
                    currentResourceTitle.removeClass("compact");
                  }
                }
                if (funcComplete) {
                  funcComplete();
                }
                form.parent().slideUp(transitionSpeed, transitionEasingSlideUp, function () {
                  _$(this).remove();
                });
              }
            });
          } else {
            for (var i = updateSelectors.length; i--;) {
              var outer = vrtxAdm.outerHTML(results, updateSelectors[i]);
              vrtxAdm.cachedBody.find(updateSelectors[i]).replaceWith(outer);
            }
            var resultsResourceTitle = _$(results).find("#resource-title");
            var currentResourceTitle = vrtxAdm.cachedBody.find("#resource-title");
            if(resultsResourceTitle.length && currentResourceTitle.length) {
              if(resultsResourceTitle.hasClass("compact") && !currentResourceTitle.hasClass("compact")) {
                currentResourceTitle.addClass("compact");
              } else if(!resultsResourceTitle.hasClass("compact") && currentResourceTitle.hasClass("compact")) {
                currentResourceTitle.removeClass("compact");
              }
            }
            if (funcComplete) {
              funcComplete();
            }
            form.parent().slideUp(transitionSpeed, transitionEasingSlideUp, function () {
              _$(this).remove();
            });
          }
        }
      }
    }
  });
};

/**
 * Remove permission async
 *
 * @this {VrtxAdmin}
 * @param {string} selector Selector for links that should do removal async
 * @param {string} updateSelector The selector for container to be updated on success
 */
VrtxAdmin.prototype.removePermissionAsync = function removePermissionAsync(selector, updateSelector) {
  var args = arguments,
      vrtxAdm = this,
      _$ = vrtxAdm._$;

  vrtxAdm.cachedAppContent.on("click", selector, function (e) {
    var link = _$(this);
    var form = link.closest("form");
    var url = form.attr("action");
    var listElement = link.parent();

    var dataString = "&csrf-prevention-token=" + form.find("input[name='csrf-prevention-token']").val()
                   + "&" + escape(link.attr("name"));
    
    vrtxAdmin.serverFacade.postHtml(url, dataString, {
      success: function (results, status, resp) {
        form.find(updateSelector).html(_$(results).find(updateSelector).html());
        initSimplifiedPermissionForm();
      }
    });
    e.preventDefault();
  });
};

/**
 * Add permission async
 *
 * @this {VrtxAdmin}
 * @param {string} selector Selector for links that should do add async
 * @param {string} updateSelector The selector for container to be updated on success
 * @param {string} errorContainerInsertAfter Selector where to place the new error container
 * @param {string} errorContainer The className of the error container
 */
VrtxAdmin.prototype.addPermissionAsync = function addPermissionAsync(selector, updateSelector, errorContainerInsertAfter, errorContainer) {
  var args = arguments,
      vrtxAdm = this,
      _$ = vrtxAdm._$;

  vrtxAdm.cachedAppContent.on("click", selector + " input[type=submit]", function (e) {
    var link = _$(this);
    var form = link.closest("form");
    var url = form.attr("action");
    var parent = link.parent().parent();
    var textfield = parent.find("input[type=text]");
    var textfieldName = textfield.attr("name");
    var textfieldVal = textfield.val();
    var dataString = textfieldName + "=" + textfieldVal
                   + "&csrf-prevention-token=" + form.find("input[name='csrf-prevention-token']").val()
                   + "&" + link.attr("name");
                   
    var hiddenAC = parent.find("input#ac_userNames");
    if(hiddenAC.length) {
      var hiddenACName = hiddenAC.attr("name");
      var hiddenACVal = hiddenAC.val();
      dataString += "&" + hiddenACName + "=" + hiddenACVal;
    }

    vrtxAdmin.serverFacade.postHtml(url, dataString, {
      success: function (results, status, resp) {
        if (vrtxAdm.hasErrorContainers(results, errorContainer)) {
          vrtxAdm.displayErrorContainers(results, form, errorContainerInsertAfter, errorContainer);
        } else {
          var upSelector = form.find(updateSelector);
          upSelector.parent().find("div." + errorContainer).remove();
          upSelector.html(_$(results).find(updateSelector).html());
          textfield.val("");
          initSimplifiedPermissionForm();
        }
      }
    });
    e.preventDefault();
  });
};

/**
 * Retrieves HTML templates in a Mustache file seperated by ###
 *
 * @this {VrtxAdmin}
 * @param {string} fileName The filename for the Mustache file
 * @param {array} templateNames Preferred name of the templates
 * @param {object} templatesIsRetrieved Deferred
 * @return {array} Templates with templateName as hash
 */
VrtxAdmin.prototype.retrieveHTMLTemplates = function retrieveHTMLTemplates(fileName, templateNames, templatesIsRetrieved) { 
  var templatesHashArray = [];
  vrtxAdmin.serverFacade.getText("/vrtx/__vrtx/static-resources/js/templates/" + fileName + ".mustache", {
    success: function (results, status, resp) {
      var templates = results.split("###");
      for(var i = 0, len = templates.length; i < len; i++) {
        templatesHashArray[templateNames[i]] = $.trim(templates[i]);
      }
      templatesIsRetrieved.resolve();
    }
  });
  return templatesHashArray;
};


/*-------------------------------------------------------------------*\
    14. Async helper functions and AJAX server façade   
\*-------------------------------------------------------------------*/

/**
 * Check if results has error container
 *
 * @this {VrtxAdmin}
 * @param {object} results The results
 * @param {string} errorContainer The className of the error container
 * @return {boolean} Whether it exists or not
 */
VrtxAdmin.prototype.hasErrorContainers = function hasErrorContainers(results, errorContainer) {
  return this._$(results).find("div." + errorContainer).length > 0;
};

/* TODO: support for multiple errorContainers
  (place the correct one in correct place (e.g. users and groups)) */
/**
 * Display error containers
 *
 * @this {VrtxAdmin}
 * @param {object} results The results
 * @param {string} form The open form
 * @param {string} errorContainerInsertAfter Selector where to place the new error container
 * @param {string} errorContainer The className of the error container
 */
VrtxAdmin.prototype.displayErrorContainers = function displayErrorContainers(results, form, errorContainerInsertAfter, errorContainer) {
  var wrapper = form.find(errorContainerInsertAfter).parent(), _$ = this._$;
  if (wrapper.find("div." + errorContainer).length) {
    wrapper.find("div." + errorContainer).html(_$(results).find("div." + errorContainer).html());
  } else {
    var outer = vrtxAdmin.outerHTML(results, "div." + errorContainer); 
    _$(outer).insertAfter(wrapper.find(errorContainerInsertAfter));
  }
}; 

/**
 * Display error message
 *
 * @this {VrtxAdmin}
 * @param {string} msg The message
 */
VrtxAdmin.prototype.displayErrorMsg = function displayErrorMsg(msg) {
  var vrtxAdm = this;
  if(!vrtxAdm.ignoreAjaxErrors) {
    vrtxAdm.displayMsg(msg, "error");
  }
};

/**
 * Display info message
 *
 * @this {VrtxAdmin}
 * @param {string} msg The message
 */
VrtxAdmin.prototype.displayInfoMsg = function displayInfoMsg(msg) {
  this.displayMsg(msg, "info");
};

/**
 * Display message
 *
 * @this {VrtxAdmin}
 * @param {string} msg The message
 * @param {string} type "info" or "error" message
 */
VrtxAdmin.prototype.displayMsg = function displayMsg(msg, type) {
  var vrtxAdm = this, _$ = vrtxAdm._$;

  var current = (type === "info") ? "infomessage" : "errormessage";
  var other = (type === "info") ? "errormessage" : "infomessage";

  var currentMsg = vrtxAdm.cachedAppContent.find("> ." + current);
  var otherMsg = vrtxAdm.cachedAppContent.find("> ." + other);
  if(typeof msg !== "undefined" && msg != "") {
    if(currentMsg.length) {
      currentMsg.html(msg);
    } else if(otherMsg.length) {
      otherMsg.html(msg).removeClass(other).addClass(current);
    } else {
      vrtxAdm.cachedAppContent.prepend("<div class='" + current + " message'>" + msg + "</div>");
      // _$("." + current).hide().slideDown(vrtxAdm.transitionSpeed, vrtxAdm.transitionEasingSlideDown);
    }
  } else {
    if(currentMsg.length) {
      /* currentMsg.hide().slideUp(vrtxAdm.transitionSpeed, vrtxAdm.transitionEasingSlideUp, function() {
        _$(this).remove();
      }); */
      currentMsg.remove();
    }
    if(otherMsg.length) {
      /* otherMsg.hide().slideUp(vrtxAdm.transitionSpeed, vrtxAdm.transitionEasingSlideUp, function() {
        _$(this).remove();
      }); */
      otherMsg.remove();
    }
  }
};

/**
 * Remove message
 *
 * @this {VrtxAdmin}
 * @param {string} type "info" or "error" message
 */
VrtxAdmin.prototype.removeMsg = function removeMsg(type) {
  var vrtxAdm = this, _$ = vrtxAdm._$;
  
  var current = (type === "info") ? "infomessage" : "errormessage";
  var currentMsg = vrtxAdm.cachedAppContent.find("> ." + current);
  if(currentMsg.length) {
    currentMsg.remove();
  }
};

/**
 * Server facade (Async=>Ajax)
 * @namespace
 */
VrtxAdmin.prototype.serverFacade = {
/**
 * GET text
 *
 * @this {serverFacade}
 * @param {string} url The URL
 * @param {object} callbacks The callbacks
 */
  getText: function(url, callbacks) {
    this.get(url, callbacks, "text");
  },
/**
 * GET HTML
 *
 * @this {serverFacade}
 * @param {string} url The URL
 * @param {object} callbacks The callback functions
 */
  getHtml: function(url, callbacks) {
    this.get(url, callbacks, "html");
  },
/**
 * GET JSON
 *
 * @this {serverFacade}
 * @param {string} url The URL
 * @param {object} callbacks The callback functions
 */
  getJSON: function(url, callbacks) {
    this.get(url, callbacks, "json");
  },
/**
 * POST HTML
 *
 * @this {serverFacade}
 * @param {string} url The URL
 * @param {string} params The data
 * @param {object} callbacks The callback functions
 */
  postHtml: function(url, params, callbacks) {
    this.post(url, params, callbacks, "html", "application/x-www-form-urlencoded;charset=UTF-8");
  },
/**
 * POST JSON
 *
 * @this {serverFacade}
 * @param {string} url The URL
 * @param {string} params The data
 * @param {object} callbacks The callback functions
 */
  postJSON: function(url, params, callbacks) {
    this.post(url, params, callbacks, "json", "text/plain;charset=utf-8");
  },
/**
 * GET Ajax <data type>
 *
 * @this {serverFacade}
 * @param {string} url The URL
 * @param {object} callbacks The callback functions
 * @param {string} type The data type
 */
  get: function(url, callbacks, type) {
    vrtxAdmin._$.ajax({
      type: "GET",
      url: url,
      dataType: type,
      success: callbacks.success,
      error: function (xhr, textStatus) {
        vrtxAdmin.displayErrorMsg(vrtxAdmin.serverFacade.error(xhr, textStatus));
        if(callbacks.error) {
          callbacks.error(xhr, textStatus);
        }
      },
      complete: function (xhr, testStatus) {
        if(callbacks.complete) {
          callbacks.complete(xhr, testStatus);
        }
      }
    });
  },
/**
 * POST Ajax <data type>
 *
 * @this {serverFacade}
 * @param {string} url The URL
 * @param {string} params The data
 * @param {object} callbacks The callback functions
 * @param {string} type The data type
 * @param {string} contentType The content type
 */
  post: function(url, params, callbacks, type, contentType) {
    vrtxAdmin._$.ajax({
      type: "POST",
      url: url,
      data: params,
      dataType: type,
      contentType: contentType,
      success: callbacks.success,
      error: function (xhr, textStatus) {
        vrtxAdmin.displayErrorMsg(vrtxAdmin.serverFacade.error(xhr, textStatus));
        if(callbacks.error) {
          callbacks.error(xhr, textStatus);
        }
      },
      complete: function (xhr, testStatus) {
        if(callbacks.complete) {
          callbacks.complete(xhr, testStatus);
        }
      }
    });
  },
/**
 * Error Ajax handler
 *
 * @this {serverFacade}
 * @param {object} xhr The XMLHttpRequest object
 * @param {string} textStatus The text status
 * @return {string} The messsage
 */
  error: function(xhr, textStatus) { // TODO: detect function origin
    var status = xhr.status;
    if (xhr.readyState == 4 && status == 200) {
      var msg = "The service is not active: " + textStatus;
    } else {
      if (status == 401 || status == 403 || status == 404) {
        location.reload(true); // if you have no access anymore or page is removed: reload from server
      }
      var msg = "The service returned " + xhr.status + " and failed to retrieve/post form: " + textStatus;
    }
    return msg;
  }
};


/*-------------------------------------------------------------------*\
    15. CK browse server integration
\*-------------------------------------------------------------------*/

var urlobj;
$(document).ready(function() {

  /* Hide image previews on init (unobtrusive) */
  var previewInputFields = $("input.preview-image-inputfield");
  for(var i = previewInputFields.length; i--;) {
    if(previewInputFields[i].value === "") {
      hideImagePreviewCaption($(previewInputFields[i]), true);
    }
  } 
  
  /* Inputfield events for image preview */
  $(document).on("blur", "input.preview-image-inputfield", function(e) {
    previewImage(this.id)
  });
  $(document).on("keydown", "input.preview-image-inputfield", function(e) { // ENTER-key
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      previewImage(this.id);
      e.preventDefault();
    }
  });
});

function hideImagePreviewCaption(input, isInit) {
  var previewImg = $("div#" + input[0].id.replace(/\./g,'\\.') + '\\.preview:visible');
  if(!previewImg.length) return;
  
  var fadeSpeed = isInit ? 0 : "fast";
  
  previewImg.fadeOut(fadeSpeed);
  
  var captionWrp = input.closest(".introImageAndCaption");
  if(!captionWrp.length) {
    var captionWrp = input.closest(".picture-and-caption");
    if(captionWrp.length) {
      captionWrp = captionWrp.parent();
    }
  } else {
    var hidePicture = captionWrp.find(".hidePicture");
    if(hidePicture.length) {
      hidePicture.fadeOut(fadeSpeed);
    }
  }
  if(captionWrp.length) {
    captionWrp.find(".caption").fadeOut(fadeSpeed);
    captionWrp.animate({height: "58px"}, fadeSpeed);
  }
}

function showImagePreviewCaption(input) {
  var previewImg = $("div#" + input[0].id.replace(/\./g,'\\.') + '\\.preview:hidden');
  if(!previewImg.length) return;
  
  previewImg.fadeIn("fast");
  
  var captionWrp = input.closest(".introImageAndCaption");
  if(!captionWrp.length) {
    var captionWrp = input.closest(".picture-and-caption");
    if(captionWrp.length) {
      captionWrp = captionWrp.parent();
      var oldHeight = 241;
    }
  } else {
    var oldHeight = 244;
    var hidePicture = captionWrp.find(".hidePicture");
    if(hidePicture.length) {
      hidePicture.fadeIn("fast");
    }
  }
  if(captionWrp.length) {
    captionWrp.find(".caption").fadeIn("fast");
    captionWrp.animate({height: oldHeight + "px"}, "fast");
  }
}

function previewImage(urlobj) {
  if(typeof urlobj === "undefined") return;
  
  urlobj = urlobj.replace(/\./g,'\\.');
  var previewNode = $("#" + urlobj + '\\.preview-inner');
  if (previewNode.length) {
    var elm = $("#" + urlobj);
    var url = elm.val();
    var parentPreviewNode = previewNode.parent();
    if (url && url != "") {
      previewNode.find("img").attr("src", url + "?vrtx=thumbnail");
      if(parentPreviewNode.hasClass("no-preview")) {
        parentPreviewNode.removeClass("no-preview");
        previewNode.find("img").attr("alt", "thumbnail");
      }
      showImagePreviewCaption(elm);
    } else {
      hideImagePreviewCaption(elm, false);
    }
  }
}

function browseServer(obj, editorBase, baseFolder, editorBrowseUrl, type) {
  urlobj = obj; // NB: store to global var
  if (!type) {
    type = 'Image';
  }
  // Use 70% of screen dimension
  var serverBrowserWindow = openServerBrowser(editorBase + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' 
                                            + baseFolder + '&Type=' + type + '&Connector=' + editorBrowseUrl,
                                              screen.width * 0.7, screen.height * 0.7);
                                              
  serverBrowserWindow.focus(); 
  // TODO: Refocus when user closes window with [x] and tries to open it again via browse..
  //       Maybe with a timer: http://en.allexperts.com/q/Javascript-1520/set-window-top-working.htm
}
 
function openServerBrowser(url, width, height) {
  var sOptions = "toolbar=no,status=no,resizable=yes"; // http://www.quirksmode.org/js/popup.html
  return openGeneral(url, width, height, "BrowseServer", sOptions); // title must be without spaces in IE
}

function openRegular(url, width, height, winTitle) {
  var sOptions = "toolbar=yes,status=yes,resizable=yes";
  sOptions += ",location=yes,menubar=yes,scrollbars=yes";
  sOptions += ",directories=yes";
  var now = +new Date();
  return openGeneral(url, width, height, winTitle + now, sOptions);
}

function openGeneral(url, width, height, winTitle, sOptions) {
  var iLeft = (screen.width - width) / 2;
  var iTop = (screen.height - height) / 2;
  sOptions += ",width=" + width;
  sOptions += ",height=" + height;
  sOptions += ",left=" + iLeft;
  sOptions += ",top=" + iTop;
  var oWindow = window.open(url, winTitle, sOptions);
  return oWindow;
}

// Callback from the CKEditor image browser:
function SetUrl(url) {
  url = decodeURIComponent(url);
  if (urlobj) {
    document.getElementById(urlobj).value = url;
  }
  oWindow = null;
  previewImage(urlobj);
  urlobj = ""; // NB: reset global var
}


/*-------------------------------------------------------------------*\
    16. Utils
\*-------------------------------------------------------------------*/

/**
 * Wrap HTML in node
 *
 * @this {VrtxAdmin}
 * @param {string} node The node type
 * @param {string} cls The className(s)
 * @param {string} html The node HTML
 * @return {string} HTML node
 */
VrtxAdmin.prototype.wrap = function wrap(node, cls, html) {
  return "<" + node + " class='" + cls + "'>" 
         + html 
         + "</" + node + ">";
};

/**
 * jQuery outerHTML (because FF don't support regular outerHTML) 
 *
 * @this {VrtxAdmin}
 * @param {string} selector Context selector
 * @param {string} subselector The node to get outer HTML from
 * @return {string} outer HTML
 */
VrtxAdmin.prototype.outerHTML = function outerHTML(selector, subselector) {
  var _$ = this._$;

  if(_$(selector).find(subselector).length) { 
    if(typeof _$(selector).find(subselector)[0].outerHTML !== "undefined") {
      return _$(selector).find(subselector)[0].outerHTML;
    } else {
      return _$('<div>').append(_$(selector).find(subselector).clone()).html();
    }
  }
};

/**
 * Load script Async / lazy-loading
 *
 * @this {VrtxAdmin}
 * @param {string} url The url to the script
 * @param {function} callback Callback function to run on success
 */
VrtxAdmin.prototype.loadScript = function loadScript(url, callback) {
  $.getScript(url).done(function() {
    callback();
  }).fail(function(jqxhr, settings, exception) {
    vrtxAdmin.log({msg: exception});
  });
};

/**
 * Log to console with calle.name if exists (Function name)
 *
 * @this {VrtxAdmin}
 * @param {object} options Configuraton
 * @param {string} options.msg The message
 * @param {array} options.args Arguments
 */
VrtxAdmin.prototype.log = function log(options) {
  if(vrtxAdmin.hasConsoleLog) {
    var msgMid = options.args ? " -> " + options.args.callee.name : "";
    console.log("Vortex admin log" + msgMid + ": " + options.msg);
  }
};

/**
 * Error to console with calle.name if exists (Function name)
 * with fallback to regular log
 *
 * @this {VrtxAdmin}
 * @param {object} options Configuraton
 * @param {string} options.msg The message
 * @param {array} options.args Arguments
 */
VrtxAdmin.prototype.error = function error(options) {
  if(vrtxAdmin.hasConsoleError) {
    var msgMid = options.args ? " -> " + options.args.callee.name : "";
    console.error("Vortex admin error" + msgMid + ": " + options.msg);
  } else {
    this.log(options);
  }
};

/**
 * Generate zebra rows in table (PE)
 *
 * @this {VrtxAdmin}
 * @param {string} selector The table selector
 */
VrtxAdmin.prototype.zebraTables = function zebraTables(selector) {
  var _$ = this._$;
  var table = _$("table" + selector);
  if(!table.length) return;
  if((vrtxAdmin.isIE && vrtxAdmin.browserVersion < 9) || vrtxAdmin.isOpera) { // http://www.quirksmode.org/css/contents.html
   table.find("tbody tr:odd").addClass("even"); // hmm.. somehow even is odd and odd is even
   table.find("tbody tr:first-child").addClass("first");
  }
};

// Credits: http://www.javascripter.net/faq/readingacookie.htm
function readCookie(cookieName, defaultVal) {
  var match = (" "+document.cookie).match(new RegExp('[; ]'+cookieName+'=([^\\s;]*)'));
  return match ? unescape(match[1]) : defaultVal;
}

/*-------------------------------------------------------------------*\
    17. Override JavaScript / jQuery
\*-------------------------------------------------------------------*/
    
/*  Override slideUp() / slideDown() to animate rows in a table
 *  
 *  Credits: 
 *  o http://stackoverflow.com/questions/467336/jquery-how-to-use-slidedown-or-show-function-on-a-table-row/920480#920480
 *  o http://www.bennadel.com/blog/1624-Ask-Ben-Overriding-Core-jQuery-Methods.htm
 */  

if(vrtxAdmin.animateTableRows) {

jQuery.fn.prepareTableRowForSliding = function() {
  $tr = this;
  $tr.children('td').wrapInner('<div style="display: none;" />');
  return $tr;
};

var originalSlideUp = jQuery.fn.slideUp;
jQuery.fn.slideUp = function(speed, easing, callback) {
  $trOrOtherElm = this;
  if($trOrOtherElm.is("tr")) {
    $trOrOtherElm.find('td > div').animate({height: 'toggle'}, speed, easing, callback);
  } else {
    originalSlideUp.apply($trOrOtherElm, arguments);
  }
};

var originalSlideDown = jQuery.fn.slideDown;
jQuery.fn.slideDown = function(speed, easing, callback) {
  $trOrOtherElm = this;
  if($trOrOtherElm.is("tr")) {
    if ($trOrOtherElm.is(':hidden')) {
      $trOrOtherElm.show().find('td > div').animate({height: 'toggle'}, speed, easing, callback);
    }
  } else {
    originalSlideDown.apply($trOrOtherElm, arguments);
  }
};

}

$.cachedScript = function(url, options) {
  options = $.extend(options || {}, {
    dataType: "script",
    cache: true,
    url: url
  });
  return jQuery.ajax(options);
}; 

/* A little faster dynamic click handler 
 */
jQuery.fn.extend({
  dynClick: function(selector, fn) {
    var nodes = $(this);
    for(var i = nodes.length; i--;) {
      jQuery.event.add(nodes[i], "click", fn, undefined, selector);
    }
  }
});

/* debouncedresize: special jQuery event that happens once after a window resize
 *
 * latest version and complete README available on Github:
 * https://github.com/louisremi/jquery-smartresize
 *
 * Copyright 2012 @louis_remi
 * Licensed under the MIT license.
 *
 * This saved you an hour of work? 
 * Send me music http://www.amazon.co.uk/wishlist/HNTU0468LQON
 */
(function($) {
  var $event = $.event,
	  $special,
	  resizeTimeout;

  $special = $event.special.debouncedresize = {
    setup: function() {
      $(this).on("resize", $special.handler);
    },
    teardown: function() {
      $(this).off("resize", $special.handler);
    },
    handler: function(event, execAsap) {
	  var context = this, // Save the context
		  args = arguments,
		  dispatch = function() {
		    event.type = "debouncedresize"; // set correct event type
		    $event.dispatch.apply(context, args);
		  };
		  if (resizeTimeout) {
		    clearTimeout(resizeTimeout);
		  }
		  execAsap ? dispatch() : resizeTimeout = setTimeout(dispatch, $special.threshold);
	  },
	  threshold: 150
  };
})(jQuery);

vrtxAdmin._$(window).on("debouncedresize", function() {
  if(vrtxAdmin.runReadyLoad && vrtxAdmin.cachedBreadcrumbs && vrtxAdmin.resizeDebouncedWait) {
    vrtxAdmin.adaptiveBreadcrumbs();
    vrtxAdmin.resizeDebouncedWait = false;
    setTimeout(function() {
      vrtxAdmin.resizeDebouncedWait = true;
    }, 1000);
  }
});

/* Get URL parameter
 *
 * Credits: http://www.netlobo.com/url_query_string_javascript.html
 *
 * Modified slightly
 */
function gup(name, url) {
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec(url);
  return (results == null) ? "" : results[1];
}

/* Remove duplicates from an array
 *
 * Credits: http://www.shamasis.net/2009/09/fast-algorithm-to-find-unique-items-in-javascript-array/
 */
function unique(array) {
  var o = {}, i, l = array.length, r = [];
  for(i=0; i<l;i+=1) o[array[i]] = array[i];
  for(i in o) r.push(o[i]);
  return r;
}

/* ^ Vortex Admin enhancements */
