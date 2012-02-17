/*
 *  Vortex Admin enhancements
 *
 *  TODO: i18n AJAX error messages
 *  TODO: prototypes vs. static (memory vs. speed?) -- it seem to be boiling down to using at the right time and test alot:
 *        http://stackoverflow.com/questions/3493252/javascript-prototype-operator-performance-saves-memory-but-is-it-faster
 *  TODO: CPU usage in ready() vs. wait for it in load()
 *
 */
 
/* 
 * ToC: 
 *
 * 1.  Config
 * 2.  DOM is fully loaded
 * 3.  DOM is ready
 * 4.  File upload
 * 5.  Keyboard interceptors / rerouters
 * 6.  Collectionlisting interaction
 * 7.  Permissions
 * 8.  Dropdowns
 * 9.  Async functions
 * 10. Async helper functions and AJAX server façade
 * 11. Show and hide properties
 * 12. Featured articles, aggregation and manually approved
 * 13. CK browse server integration
 * 14. Utils
 * 15. Override JavaScript / jQuery
 *
 */
 
/*-------------------------------------------------------------------*\
    1. Config
\*-------------------------------------------------------------------*/
 
var startLoadTime = +new Date(),
    ua = navigator.userAgent.toLowerCase();

function VrtxAdmin() {

  // Class-like singleton pattern (p.145 JavaScript Patterns)
  var instance; // cached instance
  VrtxAdmin = function VrtxAdmin() { // rewrite constructor
    return instance;
  };
  VrtxAdmin.prototype = this; // carry over properties
  instance = new VrtxAdmin(); // instance
  instance.constructor = VrtxAdmin; // reset construction pointer
  //--

  this.isIE = null;
  this.browserVersion = null;
  this.isIE7 = null;
  this.isIE6 = null;
  this.isIE5OrHigher = null;
  this.isOpera = null;
  this.isIPhone = null;
  this.isIPad = null;
  this.isAndroid = null;
  this.isMobileWebkitDevice = null;
  this.isWin = null;
  this.supportsFileAPI = null;
  
  this.hasConsole = null;
  this.hasConsoleLog = null;
  this.hasConsoleError = null;
  
  this.permissionsAutocompleteParams = null;
  
  this.transitionSpeed = 200; // same as 'fast'
  this.transitionCustomPermissionSpeed = 200; // same as 'fast'
  this.transitionPropSpeed = 100;
  this.transitionDropdownSpeed = 100;
  this.transitionEasingSlideDown = "linear";
  this.transitionEasingSlideUp = "linear";
  // this.infoMessageRemoveAfter = 5000; // 5 seconds
  
  return instance;
};

var vrtxAdmin = new VrtxAdmin();

// Browser info: used for progressive enhancement and performance scaling based on knowledge of current JS-engine
vrtxAdmin.isIE = $.browser.msie;
vrtxAdmin.browserVersion = $.browser.version;
vrtxAdmin.isIE7 = vrtxAdmin.isIE && vrtxAdmin.browserVersion <= 7;
vrtxAdmin.isIE6 = vrtxAdmin.isIE && vrtxAdmin.browserVersion <= 6;
vrtxAdmin.isIE5OrHigher = vrtxAdmin.isIE && vrtxAdmin.browserVersion >= 5;
vrtxAdmin.isOpera = $.browser.opera;
vrtxAdmin.isIPhone = /iphone/.test(ua);
vrtxAdmin.isIPad= /ipad/.test(ua);
vrtxAdmin.isAndroid = /android/.test(ua); // http://www.gtrifonov.com/2011/04/15/google-android-user-agent-strings-2/
vrtxAdmin.isMobileWebkitDevice = (vrtxAdmin.isIPhone || vrtxAdmin.isIPad || vrtxAdmin.isAndroid);
vrtxAdmin.isWin = ((ua.indexOf("win") != -1) || (ua.indexOf("16bit") != -1));
vrtxAdmin.supportsFileList = window.FileList;

// Logging capabilities
vrtxAdmin.hasConsole = typeof console !== "undefined";
vrtxAdmin.hasConsoleLog = vrtxAdmin.hasConsole && console.log;
vrtxAdmin.hasConsoleError = vrtxAdmin.hasConsole && console.error;

// Upgrade easing algorithm from 'linear' to 'easeOutQuad' and 'easeInQuad'
// -- if not < IE 9 (and not iPhone, iPad and Android devices)
if(!(vrtxAdmin.isIE && vrtxAdmin.browserVersion < 9) && !vrtxAdmin.isMobileWebkitDevice) {
  vrtxAdmin.transitionEasingSlideDown = "easeOutQuad";
  vrtxAdmin.transitionEasingSlideUp = "easeInQuad";
}

if(vrtxAdmin.isMobileWebkitDevice) { // turn off animation in iPhone, iPad and Android (consider when iOS 5)
  vrtxAdmin.transitionSpeed = 0;
  vrtxAdmin.transitionCustomPermissionSpeed = 0;
  vrtxAdmin.transitionPropSpeed = 0;
  vrtxAdmin.transitionDropdownSpeed = 0;
}

// Autocomplete parameters
vrtxAdmin.permissionsAutocompleteParams = { minChars: 4, 
                                            selectFirst: false, 
                                            width: 300, 
                                            max: 30,
                                            delay: 800 };
                                            
vrtxAdmin.usernameAutocompleteParams = { multiple: false,
                                         minChars: 2, 
                                         selectFirst: false, 
                                         width: 300, 
                                         max: 30,
                                         delay: 500 };
                                         
vrtxAdmin.tagAutocompleteParams = { minChars: 1 };
         
// When to timeout and possible to abort AJAX GET/POST   
// Credits: http://stackoverflow.com/questions/1802936/stop-all-active-ajax-requests-in-jquery
                      
var ajaxReqsPool = [];      
function abortAllAjaxReqs() {
  for(var i = ajaxReqsPool.length; i--;) {
    ajaxReqsPool[i].abort();
  }
  ajaxReqsPool = [];
}  
                    
$.ajaxSetup({
  timeout: 60000, // 1min
  beforeSend: function(xhr) {
    ajaxReqsPool.push(xhr);
  },
  complete: function(xhr) {
    var idx = $.inArray(xhr, ajaxReqsPool);
    if (idx > -1) {
      ajaxReqsPool.splice(idx, 1);
    }
  }
});
                           
// funcComplete for postAjaxForm()
var doReloadFromServer = false; // global var changed by checkStillAdmin() (funcProceedCondition)             
var reloadFromServer = function() {
  if(doReloadFromServer) {
    location.reload(true);
  } else {
    return;
  }
};



/*-------------------------------------------------------------------*\
    2. DOM is fully loaded ("load"-event) 
\*-------------------------------------------------------------------*/
                                            
$(window).load(function() {
  // More compact when no left resource menu and no buttons in right resource menu
  // Should never occur in IE because of "Show in file explorer" in root-folder 
  var resourceMenuRight = $("#resourceMenuRight"); 
  var resourceMenuRightListElements = resourceMenuRight.find("li");
  var buttonsInResourceMenuRightListElements = resourceMenuRightListElements.find(".vrtx-button-small");
  if(!$("ul#resourceMenuLeft li").length && !buttonsInResourceMenuRightListElements.length) {
    resourceMenuRight.addClass("smaller-seperator");
  }
  
  // When AJAX is turned of because of http->https we need to ensure form is in the right place
  var formResourceMenu = $("#title-container:last-child").hasClass("expandedForm");
  if(!formResourceMenu) {
    var expandedForm = $("#title-container .expandedForm").remove();
    $("#title-container").append(expandedForm);
  }

  vrtxAdmin.log({msg: "window.load() in " + (+new Date - startLoadTime) + "ms"});
});

var lastBreadcrumbPosLeft = -999;
$(window).resize(function() {
  adaptiveBreadcrumbs();
});


/*-------------------------------------------------------------------*\
    3. DOM is ready
       readyState === "complete" || "DOMContentLoaded"-event (++)
\*-------------------------------------------------------------------*/

$(document).ready(function () {   
  var startReadyTime = +new Date();

  // Buttons into links
  logoutButtonAsLink();

  // Dropdowns
  dropdownLanguageMenu("#locale-selection");
  dropdownLanguageMenu("#editor-help-menu");
  
  dropdown({
    selector: "#resource-title.true ul#resourceMenuLeft",
    proceedCondition: function(numOfListElements) {
      return numOfListElements > 1;
    }
  });
  dropdown({selector: "ul.manage-create"});
  dropdown({selector: "ul#editor-menu"});
  
  // Aggregation and manually approved
  if(!$("#resource\\.display-aggregation\\.true").is(":checked")) {
    $("#vrtx-resource\\.aggregation").slideUp(0, "linear");
  }
  if(!$("#resource\\.display-manually-approved\\.true").is(":checked")) {
    $("#vrtx-resource\\.manually-approve-from").slideUp(0, "linear");
  }
  $("#app-content").delegate("#resource\\.display-aggregation\\.true", "click", function() {
    $("#vrtx-resource\\.aggregation").slideToggle(vrtxAdmin.transitionDropdownSpeed, "swing");
  });
   $("#app-content").delegate("#resource\\.display-manually-approved\\.true", "click", function() {
    $("#vrtx-resource\\.manually-approve-from").slideToggle(vrtxAdmin.transitionDropdownSpeed, "swing");
  });
  
  // Slide up when choose something in dropdown
  $(".dropdown-shortcut-menu li a").click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(vrtxAdmin.transitionDropdownSpeed, "swing");
  });
  $(".dropdown-shortcut-menu-container li a").click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(vrtxAdmin.transitionDropdownSpeed, "swing");    
  });
  $(document).click(function() {
    $(".dropdown-shortcut-menu-container:visible").slideUp(vrtxAdmin.transitionDropdownSpeed, "swing");
    $(".tip:visible").fadeOut(vrtxAdmin.transitionDropdownSpeed, "swing");
    // Communicate this to create-iframe if exists
    var previewCreateIframe = $("#create-iframe");
    if(previewCreateIframe.length) { 
      var hasPostMessage = window['postMessage'] && (!($.browser.opera && $.browser.version < 9.65));
      var vrtxAdminOrigin = "*"; // TODO: TEMP Need real origin of adm
      if(hasPostMessage) {
        previewCreateIframe[0].contentWindow.postMessage("collapsedsize", vrtxAdminOrigin);
      }
    }
  });

  // Urchin stats
  $("#app-content").delegate("#vrtx-resource-visit-tab-menu a", "click", function(e) {
    $("#vrtx-resource-visit-wrapper").append("<span id='urchin-loading'></span>");
    $("#vrtx-resource-visit-chart").remove();
    $("#vrtx-resource-visit-stats").remove();
    $("#vrtx-resource-visit-info").remove();
    vrtxAdmin.serverFacade.getHtml(this.href, {
      success: function (results, status, resp) {
        $("#vrtx-resource-visit-wrapper").html("<div id='vrtx-resource-visit'>" + $(results).html() + "</div>");
      }
    });
    e.stopPropagation();
    e.preventDefault();
  });
  
  // Abort all AJAX reqs. on tab change
  $("#app-tabs").delegate("li a", "click", function(e) {
    abortAllAjaxReqs();
  });
  
  // Add autocomplete
  if($("form#editor").length) {  
    autocompleteUsernames(".vrtx-autocomplete-username");
    autocompleteTags(".vrtx-autocomplete-tag");
  }
  
  // Remove active tab if it has no children
  var activeTab = $("#active-tab");
  if (!activeTab.find(" > *").length) {
    activeTab.remove();
  }
  
  // Remove active tab-message if it is empty
  var activeTabMsg = activeTab.find(" > .tabMessage");
  if (!activeTabMsg.text().length) {
    activeTabMsg.remove();
  }
  
  // Hack for setting iframe width if english i18n
  if ($("#locale-selection li.active").hasClass("en")) {
    $("#create-iframe").css("width", "162px");
  }
  
  // Make breadcrumbs play along when you minimize window and have multiple rows of it
  adaptiveBreadcrumbs();
  
  // Move down resource menu when long title
  var titleSplits = $("h1 .title-split");
  var resourceMenuLeft = $("#resourceMenuLeft");
  var titleSplitsLength = titleSplits.length;
  if (resourceMenuLeft.length) {
    if (titleSplitsLength == 2) {
      resourceMenuLeft.css("marginTop", "-22px");
    } else if(titleSplitsLength >= 3) {
      resourceMenuLeft.css("marginTop", "0px"); 
    }
  }
  
  // Sticky editor title and save buttons  
  if($("form#editor").length) {
    var titleSubmitButtons = $("#vrtx-editor-title-submit-buttons");
    if(titleSubmitButtons.length) {
      var titleSubmitButtonsPos = titleSubmitButtons.offset();
      $(window).bind("scroll", function() {
        if($(window).scrollTop() >= titleSubmitButtonsPos.top) {
          titleSubmitButtons.addClass("vrtx-sticky-editor-title-submit-buttons"); 
          titleSubmitButtons.css("width", $("#contents").width() + "px");
          $("#contents").css("paddingTop", titleSubmitButtons.outerHeight(true) + "px");
        } else {
          titleSubmitButtons.removeClass("vrtx-sticky-editor-title-submit-buttons");
          titleSubmitButtons.css("width", "auto");
          $("#contents").css("paddingTop", "0px");
        }
      });
    }
  }
  
  // Preview image
  adjustImageAndCaptionContainer("#vrtx-resource\\.picture #resource\\.picture\\.preview");
  adjustImageAndCaptionContainer(".introImageAndCaption #picture\\.preview");
  
  // Collectionlisting interaction
  collectionListingInteraction();
  
  // Zebra-tables
  vrtxAdmin.zebraTables(".resourceInfo");
  
  // Async initialization

  // Resource menus
  var resourceMenuLeftServices = ["renameService",
                                  "manage\\.createArchiveService",
                                  "manage\\.expandArchiveService"];

  for (var i = resourceMenuLeftServices.length; i--;) {
    vrtxAdmin.getFormAsync({
        selector: "#title-container a#" + resourceMenuLeftServices[i],
        selectorClass: "globalmenu",
        insertAfterOrReplaceClass: "ul#resourceMenuLeft",
        isReplacing: false,
        nodeType: "div",
        simultanSliding: true,
        transitionSpeed: vrtxAdmin.transitionSpeed,
        transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
        transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
    });
    vrtxAdmin.completeFormAsync({
        selector: "form#" + resourceMenuLeftServices[i] + "-form input[type=submit]",
        isReplacing: false,
        updateSelectors: [],
        transitionSpeed: vrtxAdmin.transitionSpeed,
        transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
        transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
    });
  }
  
  var resourceMenuRightServices = ["vrtx-unpublish-document",
                                   "vrtx-publish-document"];

  for (i = resourceMenuRightServices.length; i--;) {
    vrtxAdmin.getFormAsync({
        selector: "#title-container a#" + resourceMenuRightServices[i],
        selectorClass: "globalmenu",
        insertAfterOrReplaceClass: "ul#resourceMenuLeft",
        isReplacing: false,
        nodeType: "div",
        simultanSliding: true,
        transitionSpeed: vrtxAdmin.transitionSpeed,
        transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
        transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
    });
    vrtxAdmin.completeFormAsync({
        selector: "form#" + resourceMenuRightServices[i] + "-form .submitButtons button",
        isReplacing: false,
        updateSelectors: [],
        transitionSpeed: vrtxAdmin.transitionSpeed,
        transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
        transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
    });
  }

  if($("body#vrtx-manage-collectionlisting").length) {
    var tabMenuServices = ["fileUploadService",
                           "createDocumentService",
                           "createCollectionService"];

    for (i = tabMenuServices.length; i--;) {
      if(tabMenuServices[i] != "fileUploadService") { // half-async for file upload
        vrtxAdmin.getFormAsync({
          selector: "ul#tabMenuRight a#" + tabMenuServices[i],
          selectorClass: "vrtx-admin-form",
          insertAfterOrReplaceClass: "#active-tab ul#tabMenuRight",
          isReplacing: false,
          nodeType: "div",
          simultanSliding: true,
          transitionSpeed: vrtxAdmin.transitionSpeed,
          transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
          transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
        });
        vrtxAdmin.completeFormAsync({
          selector: "form#" + tabMenuServices[i] + "-form input[type=submit]",
          isReplacing: false,
          updateSelectors: ["#contents"],
          errorContainer: "errorContainer",
          errorContainerInsertAfter: "> ul",
          funcComplete: collectionListingInteraction,
          transitionSpeed: vrtxAdmin.transitionSpeed,
          transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
          transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp,
          post: true
        });
      } else {
        vrtxAdmin.getFormAsync({
          selector: "ul#tabMenuRight a#" + tabMenuServices[i],
          selectorClass: "vrtx-admin-form",
          insertAfterOrReplaceClass: "#active-tab ul#tabMenuRight",
          isReplacing: false,
          nodeType: "div",
          funcComplete: function(p){ initFileUpload() },
          simultanSliding: true,
          transitionSpeed: vrtxAdmin.transitionSpeed,
          transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
          transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
        });
        vrtxAdmin.completeFormAsync({
          selector: "form#" + tabMenuServices[i] + "-form input[type=submit]",
          isReplacing: false,
          updateSelectors: ["#contents"],
          funcComplete: collectionListingInteraction,
          transitionSpeed: vrtxAdmin.transitionSpeed,
          transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
          transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
        });
        initFileUpload(); // when error message
      }
    }
  }

  // Permission privilegie forms (READ, READ_WRITE, ALL)
  if($("body#vrtx-permissions").length) {
    var privilegiesPermissions = ["read",
                                  "read-write",
                                  "all"];
                                  
    for (i = privilegiesPermissions.length; i--;) {
      vrtxAdmin.getFormAsync({
        selector: "div.permissions-" + privilegiesPermissions[i] + "-wrapper a.full-ajax",
        selectorClass: "expandedForm-" + privilegiesPermissions[i],
        insertAfterOrReplaceClass: "div.permissions-" + privilegiesPermissions[i] + "-wrapper",
        isReplacing: true,
        nodeType: "div",
        funcComplete: initPermissionForm,
        simultanSliding: false
      });
      vrtxAdmin.completeFormAsync({
        selector: "div.permissions-" + privilegiesPermissions[i] + "-wrapper .submitButtons input",
        isReplacing: true,
        updateSelectors: [".permissions-" + privilegiesPermissions[i] + "-wrapper",
                          ".readPermission"],
        errorContainer: "errorContainer",
        errorContainerInsertAfter: ".groups-wrapper",
        funcProceedCondition: checkStillAdmin,
        funcComplete: reloadFromServer,
        post: true
      });
    }
  
    // More permission privilegie forms in table (ADD_COMMENT, READ_PROCESSED)
    var privilegiesPermissionsInTable = ["add-comment",
                                         "read-processed"];
                                         
    for (i = privilegiesPermissionsInTable.length; i--;) {
      vrtxAdmin.getFormAsync({
        selector: ".privilegeTable tr." + privilegiesPermissionsInTable[i] + " a.full-ajax",
        selectorClass: privilegiesPermissionsInTable[i],
        insertAfterOrReplaceClass: "tr." + privilegiesPermissionsInTable[i],
        isReplacing: true,
        nodeType: "tr",
        funcComplete: initPermissionForm,
        simultanSliding: true,
        transitionSpeed: vrtxAdmin.transitionSpeed,
        transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
        transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
      });
      vrtxAdmin.completeFormAsync({
        selector: "tr." +  privilegiesPermissionsInTable[i] + " .submitButtons input",
        isReplacing: true,
        updateSelectors: ["tr." +  privilegiesPermissionsInTable[i],
                          ".resource-menu.read-permissions"],
        errorContainer: "errorContainer",
        errorContainerInsertAfter: ".groups-wrapper",
        transitionSpeed: vrtxAdmin.transitionSpeed,
        transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
        transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp,
        post: true
      });
    }
    // Remove/add permissions
    vrtxAdmin.removePermissionAsync("input.removePermission", ".principalList");
    vrtxAdmin.addPermissionAsync("span.addGroup", ".principalList", ".groups-wrapper", "errorContainer");
    vrtxAdmin.addPermissionAsync("span.addUser", ".principalList", ".users-wrapper", "errorContainer");
  }
  
  // About property forms
  if($("body#vrtx-about").length && !vrtxAdmin.isIE7) { // turn of tmp. in IE7
    var propsAbout = [
      "contentLocale",
      "commentsEnabled",
      "userTitle",
      "keywords",
      "description",
      "verifiedDate",
      "authorName",
      "authorEmail",
      "authorURL",
      "collection-type",
      "contentType",
      "userSpecifiedCharacterEncoding",
      "plaintext-edit",
      "xhtml10-type"
      ];

    for (i = propsAbout.length; i--;) {
      vrtxAdmin.getFormAsync({
        selector: "body#vrtx-about .prop-" + propsAbout[i] + " a.vrtx-button-small",
        selectorClass: "expandedForm-prop-" + propsAbout[i],
        insertAfterOrReplaceClass: "tr.prop-" + propsAbout[i],
        isReplacing: true,
        nodeType: "tr",
        simultanSliding: true,
        transitionSpeed: vrtxAdmin.transitionSpeed,
        transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
        transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
      });
      vrtxAdmin.completeFormAsync({
        selector: "body#vrtx-about .prop-" + propsAbout[i] + " form input[type=submit]",
        isReplacing: true,
        updateSelectors: ["tr.prop-" + propsAbout[i]],
        transitionSpeed: vrtxAdmin.transitionSpeed,
        transitionEasingSlideDown: vrtxAdmin.transitionEasingSlideDown,
        transitionEasingSlideUp: vrtxAdmin.transitionEasingSlideUp
      });
    }
  }
  
  $("#app-content").delegate("a.vrtx-revision-view", "click", function(e) {
    var openedRevision = openVersioning(this.href, 1020, 800);
    e.stopPropagation();
    e.preventDefault();
  });
 
  if($("body#vrtx-revisions").length) {
    $("#contents").delegate(".vrtx-revisions-delete-form input[type=submit]", "click", function(e) { // Delete revisions
      var form = $(this).closest("form")
      var url = form.attr("action");
      var dataString = form.serialize();
      vrtxAdmin.serverFacade.postHtml(url, dataString, {
        success: function (results, status, resp) {
          var tr = form.closest("tr");
          tr.prepareTableRowForSliding().hide(0).slideDown(0, "linear");
          // Check when multiple animations are complete; credits: http://tinyurl.com/83oodnp
          var animA = tr.find("td").animate({paddingTop: '0px', paddingBottom: '0px'}, 
                                             vrtxAdmin.transitionDropdownSpeed, vrtxAdmin.transitionEasingSlideUp, $.noop);
          var animB = tr.slideUp(vrtxAdmin.transitionDropdownSpeed, vrtxAdmin.transitionEasingSlideUp, $.noop);
          $.when(animA, animB).done(function() {
            $("#contents").html($(results).find("#contents").html());
            $("#app-tabs").html($(results).find("#app-tabs").html());
          });
        }
      });
      e.stopPropagation();
      e.preventDefault();
    });
    $("#contents").delegate(".vrtx-revisions-restore-form input[type=submit]", "click", function(e) { // Restore revisions
      var form = $(this).closest("form")
      var url = form.attr("action");
      var dataString = form.serialize();
      $("td.vrtx-revisions-buttons-column input").attr("disabled", "disabled"); // Lock buttons
      vrtxAdmin.serverFacade.postHtml(url, dataString, {
        success: function (results, status, resp) {
          $("#contents").html($(results).find("#contents").html());
          if(typeof versionsRestoredInfoMsg !== "undefined") {         
            var revisionNr = url.substring(url.lastIndexOf("=")+1, url.length);
            var versionsRestoredInfoMsgTmp = versionsRestoredInfoMsg.replace("X", revisionNr);
            vrtxAdmin.displayInfoMsg(versionsRestoredInfoMsgTmp);
          }
          scroll(0,0);
        },
        error: function (xhr, textStatus) {
          $("td.vrtx-revisions-buttons-column input").removeAttr("disabled"); // Unlock buttons
        }
      });
      e.stopPropagation();
      e.preventDefault();
    });
    $("#contents").delegate("#vrtx-revisions-make-current-form input[type=submit]", "click", function(e) { // Make working copy into current version
      var form = $(this).closest("form")
      var url = form.attr("action");
      var dataString = form.serialize();
      vrtxAdmin.serverFacade.postHtml(url, dataString, {
        success: function (results, status, resp) {
          $("#contents").html($(results).find("#contents").html());
          $("#app-tabs").html($(results).find("#app-tabs").html());
          if(typeof versionsMadeCurrentInfoMsg !== "undefined") {
            vrtxAdmin.displayInfoMsg(versionsMadeCurrentInfoMsg);
          }
        }
      });
      e.stopPropagation();
      e.preventDefault();
    });
  }

  // Show/hide multiple properties (initalization / config)
  // TODO: better / easier to understand interface (and remove old "." in CSS-ids / classes)
  
  if ($("form#editor").length) {
    showHide(["#resource\\.recursive-listing\\.false", "#resource\\.recursive-listing\\.unspecified"], //radioIds
              "#resource\\.recursive-listing\\.false:checked",                                         //conditionHide
              'false',                                                                                 //conditionHideEqual
              ["#vrtx-resource\\.recursive-listing-subfolders"]);                                      //showHideProps

    showHide(["#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"],
              "#resource\\.display-type\\.calendar:checked",
              null,
              ["#vrtx-resource\\.event-type-title"]);

    showHide(["#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"],
              "#resource\\.display-type\\.calendar:checked",
              'calendar',
              ["#vrtx-resource\\.hide-additional-content"]);
  }

  // Fix IE 6 collectionlisting hover
  if (vrtxAdmin.isIE6) {
    var ie6Msg = "You are using an outdated and not supported version of Internet Explorer. Please go to Windows Updates and install the latest version.";
    if ($("#app-content > .message").length) {
      $("#app-content > .message").html(ie6Msg);
    } else {
      $("#app-content").prepend("<div class='infomessage'>" + ie6Msg + "</div>");
    }
    $("#directory-listing tr").hover(function () {
      $(this).toggleClass('hover');
    }, function () {
      $(this).toggleClass('hover');
    });
  }
  
  vrtxAdmin.log({msg: "document.ready() in " + (+new Date - startReadyTime) + "ms"});

});

/* Used by "createDocumentService" available from "manageCollectionListingService" */
function changeTemplateName(n) {
  $("form[name=createDocumentService] input[type=text]").val(n);
}



/*-------------------------------------------------------------------*\
    4. File upload
\*-------------------------------------------------------------------*/

function initFileUpload() {
  var form = $("form[name=fileUploadService]");
  if(!form.length) return;
  var inputFile = form.find("#file");

  $("<div class='vrtx-textfield vrtx-file-upload'><input id='fake-file' type='text' /><a class='vrtx-button vrtx-file-upload'><span>Browse...</span></a></div>'")
    .insertAfter(inputFile);
      
  inputFile.addClass("js-on");
      
  inputFile.change(function(e) {
    var filePath = $(this).val();
    filePath = filePath.substring(filePath.lastIndexOf("\\")+1);
    if (vrtxAdmin.supportsFileList) {
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
    $("a.vrtx-file-upload").addClass("hover");
  }, function () {
    $("a.vrtx-file-upload").removeClass("hover");;
  });

  if (supportsReadOnly(document.getElementById("fake-file"))) {
    form.find("#fake-file").attr("readOnly", "readOnly");  
  }
  if (supportsMultipleAttribute(document.getElementById("file"))) {
    inputFile.attr("multiple", "multiple");
    if(typeof multipleFilesInfoText !== "undefined") {
      $("<p id='vrtx-file-upload-info-text'>" + multipleFilesInfoText + "</p>").insertAfter(".vrtx-textfield");
    }
  }
}

// Both methods taken from: http://miketaylr.com/code/input-type-attr.html (MIT license)
function supportsMultipleAttribute(inputfield) {
  return (!!(inputfield.multiple === false) && !!(inputfield.multiple !== "undefined"));
}
function supportsReadOnly(inputfield) {
  return (!!(inputfield.readOnly === false) && !!(inputfield.readOnly !== "undefined"));
}


/*-------------------------------------------------------------------*\
    5. Keyboard interceptors / rerouters
\*-------------------------------------------------------------------*/

function interceptEnterKey(idOrClass) {
  $("#app-content").delegate("form input" + idOrClass, "keypress", function (e) {
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      e.preventDefault(); // cancel the default browser click
    }
  });
}

function interceptEnterKeyAndReroute(txt, btn) {
  $("#app-content").delegate(txt, "keypress", function (e) {
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      $(btn).click(); // click the associated button
       e.preventDefault();
    }
  });
}



/*-------------------------------------------------------------------*\
    Buttons into links
\*-------------------------------------------------------------------*/

function logoutButtonAsLink() {
  var btn = $('input#logoutAction');
  if (!btn.length) return;
  btn.hide();
  btn.after('&nbsp;<a id=\"logoutAction.link\" name=\"logoutAction\" href="javascript:void(0);">'
          + btn.attr('value') + '</a>');
  $('#logoutAction\\.link').click(function (e) {
    btn.click();
    e.stopPropagation();
    e.preventDefault();
  });
}



/*-------------------------------------------------------------------*\
    Collectionlisting interaction
\*-------------------------------------------------------------------*/

function collectionListingInteraction() {
  if(!$("#directory-listing").length) return;
  
  if(typeof moveUncheckedMessage !== "undefined") { 
    var options = {
      formName: "collectionListingForm",
      btnId: "collectionListing\\.action\\.move-resources",
      service: "moveResourcesService",
      msg: moveUncheckedMessage
    };
    placeCopyMoveButtonInActiveTab(options);
  }
  if(typeof copyUncheckedMessage !== "undefined") {
    options = {
      formName: "collectionListingForm",
      btnId: "collectionListing\\.action\\.copy-resources",
      service: "copyResourcesService",
      msg: copyUncheckedMessage
    }; 
    placeCopyMoveButtonInActiveTab(options);
  }
  
  placeDeleteButtonInActiveTab();
  placeRecoverButtonInActiveTab();
  placeDeletePermanentButtonInActiveTab();
  
  initializeCheckUncheckAll();
}

function initializeCheckUncheckAll() {
  var tdCheckbox = $("td.checkbox");
  if(tdCheckbox.length && !$("form#editor").length) {
    $("th.checkbox").append("<input type='checkbox' name='checkUncheckAll' />");
    $("#directory-listing").delegate("th.checkbox input", "click", function() {
      var checkAll = this.checked;
      var checkboxes = $("td.checkbox input");
      var funcClassAddRemover = classAddRemover; 
      for(var i = 0, len = checkboxes.length; i < len; i++) {
        var isChecked = checkboxes[i].checked;
        var checkbox = $(checkboxes[i]);
        var tr = checkbox.closest("tr");
        if(!isChecked && checkAll) {
          checkbox.attr('checked', true).change();
          funcClassAddRemover(tr, "checked", true);
        }
        if(isChecked && !checkAll) {
          checkbox.attr('checked', false).change();
          funcClassAddRemover(tr, "checked", false);
        }
      }
    });
    $("#directory-listing").delegate("td.checkbox input", "click", function() {
      var checkbox = this;
      var isChecked = checkbox.checked;
      var tr = $(checkbox).closest("tr");
      if(isChecked) {
        classAddRemover(tr, "checked", true);
      } else {
        classAddRemover(tr, "checked", false);
      }
    });
  }
}

function classAddRemover(elem, name, isAdding) {
  if(isAdding) { // Add
    if(!elem.hasClass(name)) {
      elem.addClass(name);
    }
  } else { // Remove
    if(elem.hasClass(name)) {
      elem.removeClass(name);
    }
  }
}

// options: formName, btnId, service, msg
function placeCopyMoveButtonInActiveTab(options) {
  var btn = $("#" + options.btnId);
  if (!btn.length) return;
  btn.hide();
  var li = $("li." + options.service);
  li.html("<a id='" + options.service + "' href='javascript:void(0);'>" + btn.attr('title') + "</a>");
  $("#" + options.service).click(function (e) {
    if (!$("form[name=" + options.formName + "] td input[type=checkbox]:checked").length) {
      alert(options.msg);
    } else {
      $("#" + options.btnId).click();
    }
    e.stopPropagation();
    e.preventDefault();
  });
}

function placeDeleteButtonInActiveTab() {
  var btn = $('#collectionListing\\.action\\.delete-resources');
  if (!btn.length) return;
  btn.hide();
  var li = $('li.deleteResourcesService');
  li.html('<a id="deleteResourceService" href="javascript:void(0);">' + btn.attr('title') + '</a>');
  $('#deleteResourceService').click(function (e) {
    var boxes = $('form[name=collectionListingForm] td input[type=checkbox]:checked');
    if (!boxes.length) {
      alert(deleteUncheckedMessage);
    } else {
      var list = "";
      var boxesSize = boxes.size();
      var boxesSize = boxesSizeTmp = boxes.size();
      boxesSizeTmp = boxesSizeTmp > 10 ? 10 : boxesSizeTmp;
      for (var i = 0; i < boxesSizeTmp; i++) {
        var name = boxes[i].name.split("/");
        list += name[name.length-1] + '\n';
      }
      if (boxesSize > 10) {
        list += "... " + confirmDeleteAnd + " " + (boxesSize - 10) + " " + confirmDeleteMore;
      }
      if (confirm(confirmDelete.replace("(1)", boxesSize) + '\n\n' + list)) {
        $('#collectionListing\\.action\\.delete-resources').click();
      }
    }
    e.stopPropagation(); 
    e.preventDefault();
  });
}

function placeRecoverButtonInActiveTab() {
  var btn = $('.recoverResource');
  if (!btn.length) return;
  btn.hide();
  $("#active-tab").prepend('<ul class="list-menu" id="tabMenuRight"><li class="recoverResourceService">'
                              + '<a id="recoverResourceService" href="javascript:void(0);">' 
                              + btn.attr('value') + '</a></li></ul>');
  $('#recoverResourceService').click(function (e) {
    var boxes = $('form.trashcan td input[type=checkbox]:checked');
    if (!boxes.length) {
      alert(recoverUncheckedMessage); //TODO i18n from somewhere
    } else {
      $('.recoverResource').click();
    }
    e.stopPropagation(); 
    e.preventDefault();
  });
}

function placeDeletePermanentButtonInActiveTab() {
  var btn = $('.deleteResourcePermanent');
  if (!btn.length) return;
  btn.hide();
  $("#tabMenuRight")
    .append('<li class="deleteResourcePermanentService"><a id="deleteResourcePermanentService" href="javascript:void(0);">' 
          + btn.attr('value') + '</a></li>');
  $('#deleteResourcePermanentService').click(function (e) {
    var boxes = $('form.trashcan td input[type=checkbox]:checked');
    
    if (!boxes.length) {
      alert(deletePermanentlyUncheckedMessage);
    } else {
      var list = "";
      var boxesSize = boxesSizeTmp = boxes.size();
      boxesSizeTmp = boxesSizeTmp > 10 ? 10 : boxesSizeTmp;
      for (var i = 0; i < boxesSizeTmp; i++) {
        var name = boxes[i].title.split("/");
        list += name[name.length-1] + '\n';
      }
      if (boxesSize > 10) {
        list += "... " + confirmDeletePermanentlyAnd + " " + (boxesSize - 10) + " " + confirmDeletePermanentlyMore;
      }
      if (confirm(confirmDeletePermanently.replace("(1)", boxesSize) + '\n\n' + list)) {
        $('.deleteResourcePermanent').click();
      }
    }
    e.stopPropagation(); 
    e.preventDefault();
  });
}



/*-------------------------------------------------------------------*\
    7. Permissions	
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
      $("." + selectorClass).find(".principalList").hide(0);
  }
  $("#app-content").delegate("." + selectorClass + " ul.shortcuts label[for=custom]", "click", function (e) {
    $(this).closest("form").find(".principalList:hidden").slideDown(vrtxAdmin.transitionCustomPermissionSpeed, vrtxAdmin.transitionEasingSlideDown);
    e.stopPropagation(); 
  });
  $("#app-content").delegate("." + selectorClass + " ul.shortcuts label:not([for=custom])", "click", function (e) {
    $(this).closest("form").find(".principalList:visible").slideUp(vrtxAdmin.transitionCustomPermissionSpeed, vrtxAdmin.transitionEasingSlideUp);
    e.stopPropagation(); 
  });
}

function checkStillAdmin(selector) {
  var stillAdmin = selector.find(".still-admin").text();
  doReloadFromServer = false;
  if(stillAdmin == "false") {
    var msg = "Are you sure you want to remove all admin permissions for yourself?";
    if(removeAdminPermissionsMsg !== "undefined") {
      msg = removeAdminPermissionsMsg;
    }
    var confirmRemoveAllAdmin = confirm(msg);
    if(!confirmRemoveAllAdmin) {
      return false;
    } else {
      doReloadFromServer = true;
    }
  }
  return true; 
}

function autocompleteUsernames(selector) {
  var autocompleteTextfields = $(selector).find('.vrtx-textfield input');
  var i = autocompleteTextfields.length;
  while(i--) {
    permissionsAutocomplete($(autocompleteTextfields[i]).attr("id"), 'userNames', vrtxAdmin.usernameAutocompleteParams, true);
  }
}

function autocompleteTags(selector) {
  var autocompleteTextfields = $(selector).find('.vrtx-textfield input');
  var i = autocompleteTextfields.length;
  while(i--) {
    setAutoComplete($(autocompleteTextfields[i]).attr("id"), 'tags', vrtxAdmin.tagAutocompleteParams);
  }
}


/*-------------------------------------------------------------------*\
    8. Dropdowns	
\*-------------------------------------------------------------------*/

function dropdownLanguageMenu(selector) {
  var languageMenu = $(selector + " ul");
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

  $(selector).delegate(selector + "-header", "click", function (e) {
    $(".dropdown-shortcut-menu-container:visible").slideUp(vrtxAdmin.transitionDropdownSpeed, "swing");
    $(this).next(".dropdown-shortcut-menu-container").not(":visible").slideDown(vrtxAdmin.transitionDropdownSpeed, "swing");
    e.stopPropagation();
    e.preventDefault();
  });
}

function dropdown(options) {
  var list = $(options.selector);
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
    shortcutMenu.css("left", (list.width()+5) + "px");
    
    list.find("li" + dropdownClickArea).addClass("dropdown-init");
    
    list.find("li.dropdown-init #dropdown-shortcut-menu-click-area").click(function (e) {
      $(".dropdown-shortcut-menu-container:visible").slideUp(vrtxAdmin.transitionDropdownSpeed, "swing");
      shortcutMenu.not(":visible").slideDown(vrtxAdmin.transitionDropdownSpeed, "swing");   
      e.stopPropagation();
      e.preventDefault();
    });

    list.find("li.dropdown-init #dropdown-shortcut-menu-click-area").hover(function () {
      var $this = $(this);
      $this.parent().toggleClass('unhover');
      $this.prev().toggleClass('hover');
    }, function () {
      var $this = $(this);
      $this.parent().toggleClass('unhover');
      $this.prev().toggleClass('hover');
    });
  }
}

function closeDropdowns() {
  var dropdowns = $(".dropdown-shortcut-menu-container:visible");
  if(dropdowns.length) {
    dropdowns.slideUp(vrtxAdmin.transitionDropdownSpeed, "swing");
  }
}

function adaptiveBreadcrumbs() {
  var breadcrumbs = $("#vrtx-breadcrumb > span"), 
      i = breadcrumbs.length,
      runnedAtStart = false;
  while(i--) {
    var breadcrumb = $(breadcrumbs[i]);
    var breadcrumbPos = breadcrumb.position();
    var breadcrumbPosTop = breadcrumbPos.top;
    var breadcrumbPosLeft = breadcrumbPos.left;
    if (!runnedAtStart) {
      if (lastBreadcrumbPosLeft == breadcrumbPosLeft) {
        return;     
      } else {
        lastBreadcrumbPosLeft = breadcrumbPosLeft;
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
}



/*-------------------------------------------------------------------*\
    9. Async functions	
\*-------------------------------------------------------------------*/

/**
 * Retrieve form async
 *
 * @param options: selector: selector for links that should GET asynchronous form
 *                 selectorClass: selector for form
 *                 insertAfterOrReplaceClass: where to put the form
 *                 isReplacing: replace instead of insert after
 *                 nodeType: node type that should be replaced or inserted
 *                 funcComplete: callback function(selectorClass) to run when AJAX is completed and form is visible
 *                 simultanSliding: whether to slideUp existing form at the same time slideDown new form 
 *                                  (only when there is an existing form)
 *                 transitionSpeed: transition speed in ms
 *                 transitionEasingSlideDown: transition easing algorithm for slideDown()
 *                 transitionEasingSlideUp: transition easing algorithm for slideUp()
 */

VrtxAdmin.prototype.getFormAsync = function getFormAsync(options) {
  var args = arguments, // this function
      vrtxAdm = this; // use prototypal hierarchy 

  $("#app-content").delegate(options.selector, "click", function (e) {
  
    var url = $(this).attr("href") || $(this).closest("form").attr("action");
    if(location.protocol == "http:" && url.indexOf("https://") != -1) {
      return; // no AJAX when http -> https (tmp. solution)
    }

    var selector = options.selector,
        selectorClass = options.selectorClass,
        simultanSliding = options.simultanSliding,
        transitionSpeed = options.transitionSpeed || 0,
        transitionEasingSlideDown = options.transitionEasingSlideDown || "linear";
        transitionEasingSlideUp = options.transitionEasingSlideUp || "linear",
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
        var form = $(results).find("." + selectorClass).html();

        // If something went wrong
        if(!form) {
          vrtxAdm.error({args: args, msg: "retrieved form from " + url + " is null"});
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
                    vrtxAdm.addOriginalMarkup(modeUrl, results, resultSelectorClass, expandedForm);
                    vrtxAdm.addNewMarkup(options, selectorClass, transitionSpeed, transitionEasingSlideDown, transitionEasingSlideUp, form);
                  }
                });
              } else {
                vrtxAdm.addOriginalMarkup(url, results, resultSelectorClass, expandedForm);
              }
            } else {
              var node = $(this).parent().parent();
              if(node.is("tr")) {  // Because 'this' is tr > td > div
                node.remove();
              } else {
                $(this).remove();            
              }
            }
            if(!simultanSliding && !fromModeToNotMode) {
              vrtxAdm.addNewMarkup(options, selectorClass, transitionSpeed, transitionEasingSlideDown, transitionEasingSlideUp, form);
            }
          });
        }
        if ((!existExpandedForm || simultanSliding) && !fromModeToNotMode) {
          vrtxAdm.addNewMarkup(options, selectorClass, transitionSpeed, transitionEasingSlideDown, transitionEasingSlideUp, form);
        }
      }
    });

    e.stopPropagation(); 
    e.preventDefault();
  });
};

VrtxAdmin.prototype.addOriginalMarkup= function addOriginalMarkupForReplacedExpandedForm(url, results, resultSelectorClass, expanded) {
  var args = arguments,
      vrtxAdm = this;

  var resultHtml = vrtxAdm.outerHTML(results, resultSelectorClass);
  if(!resultHtml) { // If all went wrong
    vrtxAdm.error({args: args, msg: "trying to retrieve existing expandedForm from " + url + " returned null"});
  }
  var node = expanded.parent().parent();
  if(node.is("tr")) {  // Because 'this' is tr > td > div
    node.replaceWith(resultHtml).show(0);
  } else {
    expanded.replaceWith(resultHtml).show(0);              
  }
};

VrtxAdmin.prototype.addNewMarkup = function(options, selectorClass, transitionSpeed, transitionEasingSlideDown, transitionEasingSlideUp, form) {
  var vrtxAdm = this,
      insertAfterOrReplaceClass = options.insertAfterOrReplaceClass,
      isReplacing = options.isReplacing,
      nodeType = options.nodeType,
      funcComplete = options.funcComplete;

  if (isReplacing) {
    var classes = $(insertAfterOrReplaceClass).attr("class");
    $(insertAfterOrReplaceClass).replaceWith(vrtxAdm.wrap(nodeType, "expandedForm expandedFormIsReplaced nodeType"
                                                        + nodeType + " " + selectorClass + " " + classes, form));
  } else {
    $(vrtxAdm.wrap(nodeType, "expandedForm nodeType" + nodeType + " " + selectorClass, form))
      .insertAfter(insertAfterOrReplaceClass);
  }
  if(funcComplete) {
    funcComplete(selectorClass);
  }
  if(nodeType == "tr") {
    $(nodeType + "." + selectorClass).prepareTableRowForSliding();
  }
  $(nodeType + "." + selectorClass).hide().slideDown(transitionSpeed, transitionEasingSlideDown, function() {
    $(this).find("input[type=text]:first").focus();
  });
};

/**
 * Complete form async
 *
 * @param option: selector: selector for links that should POST asynchronous form
 *                isReplacing: replace instead of insert after
 *                updateSelectors: one or more selectors for markup that should update after POST (Array)
 *                errorContainer: selector for error container
 *                errorContainerInsertAfter: selector for where error container should be inserted after
 *                funcProceedCondition: must return true to continue
 *                funcComplete: callback function to run when AJAX is completed
 *                transitionSpeed: transition speed in ms
 *                transitionEasing: transition easing algorithm
 *                transitionEasingSlideDown: transition easing algorithm for slideDown()
 *                transitionEasingSlideUp: transition easing algorithm for slideUp()
 *                post: post also or only cancel
 */

VrtxAdmin.prototype.completeFormAsync = function completeFormAsync(options) {
  var args = arguments,
      vrtxAdm = this;   
      
  $("#app-content").delegate(options.selector, "click", function (e) {
  
    var selector = options.selector,
        isReplacing = options.isReplacing,
        updateSelectors = options.updateSelectors,
        errorContainer = options.errorContainer,
        errorContainerInsertAfter = options.errorContainerInsertAfter,
        funcProceedCondition = options.funcProceedCondition,
        funcComplete = options.funcComplete,
        transitionSpeed = options.transitionSpeed || 0,
        transitionEasingSlideDown = options.transitionEasingSlideDown || "linear",
        transitionEasingSlideUp = options.transitionEasingSlideUp || "linear",
        post = options.post || false;
  
    var link = $(this);
    var form = link.closest("form");
    
    var isCancelAction = link.attr("name").toLowerCase().indexOf("cancel") != -1;
    
    if(!post) {
      if(isCancelAction && !isReplacing) {
        $(".expandedForm").slideUp(transitionSpeed, transitionEasingSlideUp, function() {
          $(this).remove();
        });
        e.stopPropagation(); 
        e.preventDefault();
      } else {
        return;
      }
    } else {
      if(isCancelAction || !funcProceedCondition || funcProceedCondition(form)) {
        var url = form.attr("action");

        // TODO: test with form.serialize()
        var vrtxAdmAppendInputNameValuePairsToDataString = vrtxAdm.appendInputNameValuePairsToDataString; // cache to function scope
        var dataString = vrtxAdmAppendInputNameValuePairsToDataString(form.find("input[type=text]"));
        dataString += vrtxAdmAppendInputNameValuePairsToDataString(form.find("input[type=file]"));
        dataString += vrtxAdmAppendInputNameValuePairsToDataString(form.find("input[type=radio]:checked"));
        dataString += vrtxAdmAppendInputNameValuePairsToDataString(form.find("input[type=checkbox]:checked"));
        dataString += '&csrf-prevention-token=' + form.find("input[name='csrf-prevention-token']").val()
                    + "&" + link.attr("name");
                      
        vrtxAdmin.serverFacade.postHtml(url, dataString, {
          success: function (results, status, resp) {
            if (vrtxAdm.hasErrorContainers(results, errorContainer)) {
              vrtxAdm.displayErrorContainers(results, form, errorContainerInsertAfter, errorContainer);
            } else {
              if (isReplacing) {
                form.parent().slideUp(transitionSpeed, transitionEasingSlideUp, function () {
                  for(var i = updateSelectors.length; i--;) {
                    var outer = vrtxAdm.outerHTML(results, updateSelectors[i]);
                    $("#app-content " + updateSelectors[i]).replaceWith(outer);
                  }
                  if (funcComplete) {
                    funcComplete();
                  }
                });
              } else {
                for(var i = updateSelectors.length; i--;) {
                  var outer = vrtxAdm.outerHTML(results, updateSelectors[i]);
                  $("#app-content " + updateSelectors[i]).replaceWith(outer);
                }
                if (funcComplete) {
                  funcComplete();
                }
                form.parent().slideUp(transitionSpeed, transitionEasingSlideUp, function () {
                  $(this).remove();
                });            
              }
            }
          }
        });
      }
      e.stopPropagation(); 
      e.preventDefault();
    }
  });
};

/**
 * Remove permission async (value is in the name)
 * 
 * @param selector: selector for links that should post asynchronous
 * @param updateSelector: selector for markup to update
 */

VrtxAdmin.prototype.removePermissionAsync = function removePermissionAsync(selector, updateSelector) {
  var args = arguments,
      vrtxAdm = this;

  $("#app-content").delegate(selector, "click", function (e) {
    var link = $(this);
    var form = link.closest("form");
    var url = form.attr("action");
    var listElement = link.parent();

    var dataString = '&csrf-prevention-token=' + form.find("input[name='csrf-prevention-token']").val()
                   + "&" + escape(link.attr("name"));
    
    vrtxAdmin.serverFacade.postHtml(url, dataString, {
      success: function (results, status, resp) {
        form.find(updateSelector).html($(results).find(updateSelector).html());
        initSimplifiedPermissionForm();
      }
    });

    e.stopPropagation(); 
    e.preventDefault();
  });
};

/**
 * Add permission async (values is in the textfield)
 * 
 * @param selector: selector for links that should post asynchronous
 * @param updateSelector: selector for markup to update
 * @param errorContainer: selector for error container
 */

VrtxAdmin.prototype.addPermissionAsync = function addPermissionAsync(selector, updateSelector, errorContainerInsertAfter, errorContainer) {
  var args = arguments;
  var vrtxAdm = this;

  $("#app-content").delegate(selector + " input[type=submit]", "click", function (e) {
    var link = $(this);
    var form = link.closest("form");
    var url = form.attr("action");
    var textfield = link.parent().parent().find("input[type=text]");
    var textfieldName = textfield.attr("name");
    var textfieldVal = textfield.val();

    var dataString = textfieldName + '=' + textfieldVal
                   + '&csrf-prevention-token=' + form.find("input[name='csrf-prevention-token']").val()
                   + "&" + link.attr("name");
    
    vrtxAdmin.serverFacade.postHtml(url, dataString, {
      success: function (results, status, resp) {
        if (vrtxAdm.hasErrorContainers(results, errorContainer)) {
          vrtxAdm.displayErrorContainers(results, form, errorContainerInsertAfter, errorContainer);
        } else {
          var upSelector = form.find(updateSelector);
          upSelector.parent().find("div." + errorContainer).remove();
          upSelector.html($(results).find(updateSelector).html());
          textfield.val("");
          initSimplifiedPermissionForm();
        }
      }
    });

    e.stopPropagation(); 
    e.preventDefault();
  });
};

/**
 * Retrieve HTML async as text
 * 
 * @param url: url that retrieves HTML-text
 * @param insertAfterSelector: where to insert the HTML
 * @param wrapperSelector: wrapper for the HTML
 */
VrtxAdmin.prototype.getHtmlAsTextAsync = function getHtmlAsTextAsync(url, insertAfterSelector, wrapperSelector) {
  var args = arguments;
  var vrtxAdm = this;

  var wrapper = $(wrapperSelector);
  if(wrapper.length) {
    wrapper.html("<span id='urchin-loading'></span>");
  } else {
    $("<div id='" + wrapperSelector.substring(1) + "'><span id='urchin-loading'></span></div>").insertAfter(insertAfterSelector);
  }
  
  vrtxAdmin.serverFacade.getText(url, {
    success : function (results, status, resp) {
      var trimmedResults = $.trim(results);
      var wrapper = $(wrapperSelector);
      if(trimmedResults.length) { // if there is text
        if(wrapper.length) {
          wrapper.html(trimmedResults);
        } else {
          $(trimmedResults).insertAfter(insertAfterSelector);
        }
      } else {
        wrapper.remove();
      }
    }
  });
  
};


/*-------------------------------------------------------------------*\
    10. Async helper functions and AJAX server façade	
\*-------------------------------------------------------------------*/

VrtxAdmin.prototype.appendInputNameValuePairsToDataString = function(inputFields) {
  var dataStringChunk = "";
  if(typeof inputFields !== "undefined") {
    for (i = inputFields.length; i--;) {
      dataStringChunk += '&' + $(inputFields[i]).attr("name")
                       + '=' + $(inputFields[i]).val();
    }
  }
  return dataStringChunk;
};

VrtxAdmin.prototype.hasErrorContainers = function(results, errorContainer) {
  return $(results).find("div." + errorContainer).length > 0;
};

/* TODO: support for multiple errorContainers
  (place the correct one in correct place (e.g. users and groups)) */
VrtxAdmin.prototype.displayErrorContainers = function(results, form, errorContainerInsertAfter, errorContainer) {
  var wrapper = form.find(errorContainerInsertAfter).parent();
  if (wrapper.find("div." + errorContainer).length) {
    wrapper.find("div." + errorContainer).html($(results).find("div." + errorContainer).html());
  } else {
    var outer = vrtxAdmin.outerHTML(results, "div." + errorContainer); 
    $(outer).insertAfter(wrapper.find(errorContainerInsertAfter));
  }
}; 

VrtxAdmin.prototype.displayErrorMsg = function(msg) {
  var vrtxAdm = this;
  if ($("#app-content > .errormessage").length) {
    $("#app-content > .errormessage").html(msg);
  } else {
    $("#app-content").prepend("<div class='errormessage message'>" + msg + "</div>");
    $("#app-content > .infomessage").slideUp(0, "linear", function() {
      $(this).slideDown(vrtxAdm.transitionDropdownSpeed, vrtxAdm.transitionEasingSlideDown);
    });
  }
};

VrtxAdmin.prototype.displayInfoMsg = function(msg) {
  var vrtxAdm = this;
  if ($("#app-content > .infomessage").length) {
    $("#app-content > .infomessage").html(msg);
  } else {
    $("#app-content").prepend("<div class='infomessage message'>" + msg + "</div>")
    $("#app-content > .infomessage").slideUp(0, "linear", function() {
      $(this).slideDown(vrtxAdm.transitionDropdownSpeed, vrtxAdm.transitionEasingSlideDown);
    });
  }
};

VrtxAdmin.prototype.serverFacade = {
  getText: function(url, callbacks) {
    this.get(url, callbacks, "text");
  },
  getHtml: function(url, callbacks) {
    this.get(url, callbacks, "html");
  },
  postHtml: function(url, params, callbacks) {
    this.post(url, params, callbacks, "html");
  },
  get: function(url, callbacks, type) {
    $.ajax({
      type: "GET",
      url: url,
      dataType: type,
      success: callbacks.success,
      error: function (xhr, textStatus) {
        vrtxAdmin.displayErrorMsg(vrtxAdmin.serverFacade.error(xhr, textStatus));
        if(callbacks.error) {
          callbacks.error(xhr, textStatus);
        }
      }
    });
  },
  post: function(url, params, callbacks, type) {
    $.ajax({
      type: "POST",
      url: url,
      data: params,
      dataType: type,
      contentType: "application/x-www-form-urlencoded;charset=UTF-8",
      success: callbacks.success,
      error: function (xhr, textStatus) {
        vrtxAdmin.displayErrorMsg(vrtxAdmin.serverFacade.error(xhr, textStatus));
        if(callbacks.error) {
          callbacks.error(xhr, textStatus);
        }
      }
    });
  },
  error: function(xhr, textStatus) {
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
    11. Show and hide properties

    @param radioIds: Multiple id's for radiobuttons binding click events (Array)
    @param conditionHide: Condition to be checked for hiding
    @param conditionHideEqual: What it should equal
    @param showHideProps: Multiple props / id's / classnames to show / hide (Array)
\*-------------------------------------------------------------------*/

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
  var conditionHideVal = $(conditionHide).val(),
      showHidePropertyFunc = showHideProperty,  // cache to function scope
      i = 0,
      len = showHideProps.length;
  for (; i < len; i++) {
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
	12. Featured articles, aggregation and manually approved
	    TODO: cleanup, simplify
\*-------------------------------------------------------------------*/

var definedMultipleFields = [];

function loadMultipleDocuments(appendParentLast, textfieldId, browse, addName, removeName, browseName, editorBase, baseFolder, editorBrowseUrl) {
  var documents = $("#" + textfieldId);
  if(!documents.length) return;

  var documentsVal = documents.val();
  if (documentsVal == null) return;
  
  var simpleTextfieldId = textfieldId.substring(textfieldId.indexOf(".")+1);
  
  definedMultipleFields.push(simpleTextfieldId); // Register
   
  documents.hide();
  
  var appendHtml = "<div id='vrtx-" + simpleTextfieldId + "-add'>"
                   + "<div class=\"vrtx-button\"><button onclick=\"addFormField('" + simpleTextfieldId  + "'," + browse + ",null, '" 
                   + removeName + "', '" + browseName + "', '" + editorBase + "', '" + baseFolder 
                   + "', '" + editorBrowseUrl + "'); return false;\">" + addName + "</button></div>"
                   + "<input type='hidden' id='id-" + simpleTextfieldId  + "' name='id' value='1' /></div>";
                   
  var documentsParent = documents.parent();     
  documentsParent.hide();           
  if(appendParentLast) {
    documentsParent.parent().append(appendHtml);
  } else {
    $(appendHtml).insertAfter(documentsParent.parent().find(".vrtx-textfield:first"));
  }
  
  if($.trim(documentsVal) !== "") {
    var listOfFiles = documentsVal.split(",");
    var addFormFieldFunc = addFormField;
    for (var i = 0, len = listOfFiles.length; i < len; i++) {
      addFormFieldFunc(simpleTextfieldId, browse, $.trim(listOfFiles[i]), removeName, browseName, editorBase, baseFolder, editorBrowseUrl);
    }
  } else {
    addFormField(simpleTextfieldId, browse, "", removeName, browseName, editorBase, baseFolder, editorBrowseUrl);
  }
  
  // TODO !spageti && !run twice
  if (requestFromEditor()) {
    storeInitPropValues();
  }
}

// Stupid test to check if script is loaded from editor
// UNSAVED_CHANGES_CONFIRMATION is defined in "structured-resource/editor.ftl"
function requestFromEditor() {
  return !(typeof(UNSAVED_CHANGES_CONFIRMATION) === "undefined");
}

var countId = 1;

function addFormField(textfieldId, browse, value, removeName, browsName, editorBase, baseFolder, editorBrowseUrl) {
  var idstr = textfieldId + "-";
  if (value == null) {
    value = "";
  }

  if (!removeName == null) {
    var deleteRow = "";
  } else {
    var deleteRow = "<div class=\"vrtx-button\"><button type='button' id='" + idstr
                  + "remove' onclick='removeFormField(\"#" + idstr + "row-" + countId + "\"); return false;'>" 
                  + removeName + "</button></div>";
  }
  if(browse) {
    var browseServer = "<div class=\"vrtx-button\"><button type=\"button\" id=\"" + idstr 
                     + "browse\" onclick=\"browseServer('" + idstr + countId + "', '" + editorBase 
                     + "', '" + baseFolder + "', '" + editorBrowseUrl + "', 'File');\">" + browsName + "</button></div>";
  } else {
    var browseServer = "";
  }

  var html = "<div class='" + idstr + "row' id='" + idstr + "row-" + countId + "'><div class=\"vrtx-textfield\"><input value='" 
    + value + "' type='text' size='20′ name='txt[]' id='" + idstr + countId + "' /></div>" 
    + browseServer + deleteRow + "</div>";

  $(html).insertBefore("#vrtx-" + textfieldId + "-add");

  countId++;
}

function removeFormField(id) {
  $(id).remove();
}

function formatDocumentsData() {
  var i = definedMultipleFields.length; 
  while(i--) {
    formatDocumentsDataSubFunc(definedMultipleFields[i]);
  }
}

function formatDocumentsDataSubFunc(id) {
  var data = $("input[id^='" + id + "-']");
  var result = "";
  for (var i = 0, len = data.length; i < len; i++) {
    var value = $.trim(data[i].value);
    if(value != "") {
      if(value.lastIndexOf("/") === (value.length-1)) { // Remove last forward slash if not root
        if(value.length > 1) {
          value = value.substring(value, (value.length-1))
        }
      }
      result += value;
      if (i < (len-1)) {
        result += ",";
      }
    }
  }
  $("#resource\\." + id).val(result);
}



/*-------------------------------------------------------------------*\
	13. CK browse server integration
\*-------------------------------------------------------------------*/

var urlobj;

function previewImage(urlobj) {
  var previewNode = document.getElementById(urlobj + '.preview');
  if (previewNode) {
    var url = document.getElementById(urlobj).value;
    if (url && url != "") {
      previewNode.innerHTML = '<img src="' + url + '?vrtx=thumbnail" alt="thumbnail" />';
      adjustImageAndCaptionContainer(previewNode);
    } else {
      previewNode.innerHTML = '';
    }
  }
}

// Make sure these is space below previewed image
function adjustImageAndCaptionContainer(previewNode) {
  $(previewNode).find("img").load(function() {
    var previewNodeImg = $(this);
    var container = $(previewNode).parent().parent();
    
    if(container.attr("id") == "vrtx-resource.picture") { // old
      var origHeight = 241;
      var extraMarginHeight = 29;
    } else if(container.attr("class").indexOf("introImageAndCaption") != -1) { // new
      var origHeight = 260;
      var extraMarginHeight = 49;
    } else {
      return;
    }
 
    if((previewNodeImg.height() + extraMarginHeight) > origHeight) {
      container.css("height", (previewNodeImg.height() + extraMarginHeight) + "px");
    } else {
      container.css("height", origHeight + "px");
    }
  });
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
  var iLeft = (screen.width - width) / 2;
  var iTop = (screen.height - height) / 2;
  var sOptions = "toolbar=no,status=no,resizable=yes"; // http://www.quirksmode.org/js/popup.html
  sOptions += ",width=" + width;
  sOptions += ",height=" + height;
  sOptions += ",left=" + iLeft;
  sOptions += ",top=" + iTop;
  var oWindow = window.open(url, "BrowseServer", sOptions); // title must be without spaces in IE
  return oWindow;
}

function openVersioning(url, width, height) {
  var iLeft = (screen.width - width) / 2;
  var iTop = (screen.height - height) / 2;
  var sOptions = "toolbar=yes,status=yes,resizable=yes";
  sOptions += ",location=yes,menubar=yes,scrollbars=yes";
  sOptions += ",directories=yes";
  sOptions += ",width=" + width;
  sOptions += ",height=" + height;
  sOptions += ",left=" + iLeft;
  sOptions += ",top=" + iTop;
  var now = +new Date();
  var oWindow = window.open(url, "DisplayRevision" + now, sOptions); // title must be without spaces in IE
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
	14. Utils
\*-------------------------------------------------------------------*/

// Use our own wrap function
VrtxAdmin.prototype.wrap = function(node, cls, html) {
  return "<" + node + " class='" + cls + "'>" 
         + html 
         + "</" + node + ">";
};

// jQuery outerHTML (because FF don't support regular outerHTML)
VrtxAdmin.prototype.outerHTML = function(selector, subselector) {
  if($(selector).find(subselector).length) { 
    if(typeof $(selector).find(subselector)[0].outerHTML !== "undefined") {
      return $(selector).find(subselector)[0].outerHTML;
    } else {
      return $('<div>').append($(selector).find(subselector).clone()).html();
    }
  }
};

VrtxAdmin.prototype.log = function(options) {
  if(vrtxAdmin.hasConsoleLog) {
    var msgMid = options.args ? " -> " + options.args.callee.name : "";
    console.log("Vortex admin log" + msgMid + ": " + options.msg);
  }
};

VrtxAdmin.prototype.error = function(options) {
  if(vrtxAdmin.hasConsoleError) {
    var msgMid = options.args ? " -> " + options.args.callee.name : "";
    console.error("Vortex admin error" + msgMid + ": " + options.msg);
  } else if(vrtxAdmin.hasConsoleLog) {
    var msgMid = options.args ? " -> " + options.args.callee.name : "";
    console.log("Vortex admin error" + msgMid + ": " + options.msg);
  }
};

VrtxAdmin.prototype.zebraTables = function(selector) {
  if(!$("table" + selector).length || $("table" + selector).hasClass("revisions")) return;
  if((vrtxAdmin.isIE && vrtxAdmin.browserVersion < 9) || vrtxAdmin.isOpera) { // http://www.quirksmode.org/css/contents.html
    $("table" + selector + " tbody tr:odd").addClass("even"); // hmm.. somehow even is odd and odd is even
    $("table" + selector + " tbody tr:first-child").addClass("first");
  }
};



/*-------------------------------------------------------------------*\
	15. Override JavaScript / jQuery
\*-------------------------------------------------------------------*/	
	
/* 
	Override slideUp() / slideDown() to animate rows in a table
	
	Credits: 
    o http://stackoverflow.com/questions/467336/jquery-how-to-use-slidedown-or-show-function-on-a-table-row/920480#920480
    o http://www.bennadel.com/blog/1624-Ask-Ben-Overriding-Core-jQuery-Methods.htm
*/	

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

/* ^ Vortex Admin enhancements */
