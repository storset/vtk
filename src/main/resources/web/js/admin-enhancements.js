/*
 *  Vortex Admin enhancements
 *
 *  TODO: i18n
 *
 */
 
var agent = navigator.userAgent.toLowerCase();

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
  this.isIE6 = null;
  this.isIE5OrHigher = null;
  this.isOpera = null;
  this.isWin = null;
  // v3.?: this.supportsFileAPI = null;
  this.permissionsAutocompleteParams = null;
  this.transitionSpeed = 200; // same as 'fast'
  this.transitionCustomPermissionSpeed = 200; // same as 'fast'
  this.transitionPropSpeed = 100;
  this.transitionDropdownSpeed = 100;
  this.transitionEasing = "swing";
  
  return instance;
};

var vrtxAdmin = new VrtxAdmin();

// Browser info
vrtxAdmin.isIE = $.browser.msie;
vrtxAdmin.browserVersion = $.browser.version;
vrtxAdmin.isIE6 = vrtxAdmin.isIE && vrtxAdmin.browserVersion <= 6;
vrtxAdmin.isIE5OrHigher = vrtxAdmin.isIE && vrtxAdmin.browserVersion >= 5;
vrtxAdmin.isOpera = $.browser.opera;
vrtxAdmin.isWin = ((agent.indexOf("win") != -1) || (agent.indexOf("16bit") != -1));
// v3.?: vrtxAdmin.supportsFileAPI = window.File && window.FileReader && window.FileList && window.Blob;

// Upgrade easing if not < IE 9
if(!(vrtxAdmin.isIE && vrtxAdmin.browserVersion < 9)) {
  this.transitionEasing = "easeInQuad"; // ..wondering if we can just use "swing" everywhere because of fast transitions
}

// Permission Autocomplete parameters
vrtxAdmin.permissionsAutocompleteParams = { minChars: 4, 
                                            selectFirst: false, 
                                            width: 300, 
                                            max: 30,
                                            delay: 800 };
       
// More compact when no left resource menu and only 'Read permission' in right resource menu                                     
$(window).load(function() {
  var resourceMenuRight = $("ul.list-menu.resourceMenuRight"); 
  var resourceMenuRightListElements = resourceMenuRight.find("li");
  if(!$("ul.resourceMenuLeft").length && resourceMenuRightListElements.length == 1) {
    resourceMenuRight.addClass("smaller-seperator");
  }
});
                                            
$(document).ready(function () {

  var startTime = +new Date();

  // Buttons into links
  logoutButtonAsLink();

  // Dropdowns
  dropdownLanguageMenu();
  dropdownCollectionGlobalMenu();
  
  // Remove active tab if it has no children
  if (!$("#main .activeTab > *").length) {
    $("#main .activeTab").remove();
  }
  
  adjustImageAndCaptionContainer("#vrtx-resource\\.picture #resource\\.picture\\.preview");
  adjustImageAndCaptionContainer(".introImageAndCaption #picture\\.preview");
  
  // Collectionlisting interaction
  collectionListingInteraction();
  
  // Zebra-tables
  vrtxAdmin.zebraTables(".resourceInfo");
  
  var getAjaxOptions = {};
  var postAjaxOptions = {};

  // AJAX INIT: Resource menu service forms
  
  var resourceMenuLeftServices = ["renameService",
                                  "manage\\.createArchiveService"];

  for (var i = resourceMenuLeftServices.length; i--;) {
    getAjaxOptions = {
        selector: "#titleContainer a#" + resourceMenuLeftServices[i],
        selectorClass: "globalmenu",
        insertAfterOrReplaceClass: "#titleContainer ul.resourceMenuLeft",
        isReplacing: false,
        nodeType: "div",
        simultanSliding: true
    };
    vrtxAdmin.getAjaxForm(getAjaxOptions);
  }
  
  var resourceMenuRightServices = ["vrtx-unpublish-document\\.submit",
                                   "vrtx-publish-document\\.submit"];

  for (var i = resourceMenuRightServices.length; i--;) {
    getAjaxOptions = {
        selector: "#titleContainer #" + resourceMenuRightServices[i],
        selectorClass: "globalmenu",
        insertAfterOrReplaceClass: "#titleContainer ul.resourceMenuLeft",
        isReplacing: false,
        nodeType: "div",
        simultanSliding: true
    };
    vrtxAdmin.getAjaxForm(getAjaxOptions);
  }
  

  // AJAX INIT: Tab menu service forms
  
  if($("body#vrtx-manage-collectionlisting").length) {
    var tabMenuServices = ["fileUploadService",
                           "createDocumentService",
                           "createCollectionService"];

    for (i = tabMenuServices.length; i--;) {
      if(tabMenuServices[i] != "fileUploadService") { // half-async for file upload
        getAjaxOptions = {
          selector: "ul.tabMenu2 a#" + tabMenuServices[i],
          selectorClass: "vrtx-admin-form",
          insertAfterOrReplaceClass: ".activeTab ul.tabMenu2",
          isReplacing: false,
          nodeType: "div",
          simultanSliding: true
        };
        vrtxAdmin.getAjaxForm(getAjaxOptions);
      
        postAjaxOptions = {
          selector: "form[name=" + tabMenuServices[i] + "] input[type=submit]",
          updateSelectors: ["#contents"],
          errorContainer: "errorContainer",
          errorContainerInsertAfter: "> ul",
          funcComplete: collectionListingInteraction
        };
        vrtxAdmin.postAjaxForm(postAjaxOptions);
      
      } else {
        getAjaxOptions = {
          selector: "ul.tabMenu2 a#" + tabMenuServices[i],
          selectorClass: "vrtx-admin-form",
          insertAfterOrReplaceClass: ".activeTab ul.tabMenu2",
          isReplacing: false,
          nodeType: "div",
          funcComplete: function(p){ initFileUpload() },
          simultanSliding: true
        };
        vrtxAdmin.getAjaxForm(getAjaxOptions);
        initFileUpload(); // when error message
      }
    }
  }

  // AJAX INIT: Permission privilegie forms (READ, READ_WRITE, ALL)
  if($("body#vrtx-permissions").length) {
    var privilegiesPermissions = ["read",
                                  "read-write",
                                  "all"];

    for (i = privilegiesPermissions.length; i--;) {
      initPermissionForm("expandedForm-" + privilegiesPermissions[i]);
    }

    /* TODO: trekkspilleffekt
    for (i = privilegiesPermissions.length; i--;) {
      getAjaxOptions = {
        selector: "div.permissions-" + privilegiesPermissions[i] + "-wrapper a.full-ajax",
        selectorClass: "expandedForm-" + privilegiesPermissions[i],
        insertAfterOrReplaceClass: "div.permissions-" + privilegiesPermissions[i] + "-wrapper",
        isReplacing: true,
        nodeType: "div",
        funcComplete: initPermissionForm,
        simultanSliding: true
      };
      vrtxAdmin.getAjaxForm(getAjaxOptions);
    
      postAjaxOptions = {
        selector: "div.permissions-" + privilegiesPermissions[i] + "-wrapper input[type=submit][name=saveAction]",
        updateSelectors: [".permissions-" + privilegiesPermissions[i] + "-wrapper",
                          ".resource-menu.read-permissions"],
        errorContainer: "errorContainer",
        errorContainerInsertAfter: ".groups-wrapper",
        funcProceedCondition: checkStillAdmin
      };
      vrtxAdmin.postAjaxForm(postAjaxOptions);
    }
    */
  
    // AJAX INIT: More permission privilegie forms in table (ADD_COMMENT, READ_PROCESSED)
  
    var privilegiesPermissionsInTable = ["add-comment",
                                         "read-processed"];

    for (i = privilegiesPermissionsInTable.length; i--;) {
      getAjaxOptions = {
        selector: ".privilegeTable tr." + privilegiesPermissionsInTable[i] + " a.full-ajax",
        selectorClass: privilegiesPermissionsInTable[i],
        insertAfterOrReplaceClass: "tr." + privilegiesPermissionsInTable[i],
        isReplacing: true,
        nodeType: "tr",
        funcComplete: initPermissionForm,
        simultanSliding: true
      };
      vrtxAdmin.getAjaxForm(getAjaxOptions);
    
      postAjaxOptions = {
        selector: "tr." +  privilegiesPermissionsInTable[i] + " input[type=submit][name=saveAction]",
        updateSelectors: ["tr." +  privilegiesPermissionsInTable[i],
                          ".resource-menu.read-permissions"],
        errorContainer: "errorContainer",
        errorContainerInsertAfter: ".groups-wrapper"
      };
      vrtxAdmin.postAjaxForm(postAjaxOptions);
    }
    
    vrtxAdmin.ajaxRemove("input.removePermission", ".principalList");
    vrtxAdmin.ajaxAdd("span.addGroup", ".groups-wrapper", "errorContainer");
    vrtxAdmin.ajaxAdd("span.addUser", ".users-wrapper", "errorContainer");
  }
  
  // AJAX INIT: About property forms
  
  if($("body#vrtx-about").length) {
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
      getAjaxOptions = {
        selector: "body#vrtx-about .prop-" + propsAbout[i] + " a.vrtx-button-small",
        selectorClass: "expandedForm-prop-" + propsAbout[i],
        insertAfterOrReplaceClass: "tr.prop-" + propsAbout[i],
        isReplacing: true,
        nodeType: "tr",
        simultanSliding: true
      };
      vrtxAdmin.getAjaxForm(getAjaxOptions);
    }
  }

  // AJAX INIT: Remove/add permissions

  // Show/hide multiple properties (initalization/config)
  
  showHide(["#resource\\.recursive-listing\\.false", "#resource\\.recursive-listing\\.unspecified"],
            "#resource\\.recursive-listing\\.false:checked", 'false', ["#vrtx-resource\\.recursive-listing-subfolders"]);

  showHide(["#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"],
            "#resource\\.display-type\\.calendar:checked", null, ["#vrtx-resource\\.event-type-title"]);

  showHide(["#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"],
            "#resource\\.display-type\\.calendar:checked", 'calendar', ["#vrtx-resource\\.hide-additional-content"]);

  // Fix IE 6 collectionlisting hover
  if (vrtxAdmin.isIE6) {
    $("table.directoryListing tr").hover(function () {
      $(this).toggleClass('hover');
    }, function () {
      $(this).toggleClass('hover');
    });
  }
  
  vrtxAdmin.log({msg: "document.ready() in " + (+new Date - startTime) + "ms"});

});

/* Used by "createDocumentService" available from "manageCollectionListingService" */
function changeTemplateName(n) {
  $("form[name=createDocumentService] input[type=text]").val(n);
}

/* File upload */

function initFileUpload() {
  var form = $("form[name=fileUploadService]");
  if(form.length) {
    var inputFile = form.find("#file");

    $("<div class='vrtx-textfield vrtx-file-upload'><input id='fake-file' /><a class='vrtx-button vrtx-file-upload'><span>Browse...</span></a></div>'")
      .insertAfter(inputFile);
      
    inputFile.addClass("js-on");
      
    inputFile.change(function(e) {
      form.find("#fake-file").val($(this).val());
    });
    
    inputFile.hover(function () {
      $("a.vrtx-file-upload").addClass("hover");
      $(this).css("cursor", "pointer");
    }, function () {
      $("a.vrtx-file-upload").removeClass("hover");
      $(this).css("cursor", "auto");
    });
      
    if (supportsMultipleAttribute(document.getElementById("file"))) {
      inputFile.attr("multiple", "multiple");
      if(typeof multipleFilesInfoText !== "undefined") {
        $("<p id='vrtx-file-upload-info-text'>" + multipleFilesInfoText + "</p>").insertAfter(".vrtx-textfield");
      }
    }
  }
}

// Taken from: http://miketaylr.com/code/input-type-attr.html (MIT license)
function supportsMultipleAttribute(inputfield) {
  return (!!(inputfield.multiple === false) && !!(inputfield.multiple !== "undefined"));
}

// Credits: http://www.html5rocks.com/en/tutorials/file/dndfiles/
/* File API for v3.?

function fileInfo(file) {  
  if (vrtxAdmin.supportsFileAPI) {
    var files = document.getElementById(file).files;
    if(files) {
      var output = [];
      for (var i = 0, f; f = files[i]; i++) {
        output.push('<li><strong>', f.name, '</strong> (', f.type || 'n/a', ') - ',
                    f.size, ' bytes, last modified: ',
                    f.lastModifiedDate.toLocaleDateString(), '</li>');
      }
      var fileList = $("#vrtx-file-upload-file-list");
      if(fileList.length) {
        fileList.html(output.join(""));
      } else {
        $("<ul id='vrtx-file-upload-file-list'>" 
          + output.join("") + "</ul>").insertAfter("a.vrtx-button");
      }
    }
  }
}
*/

/* ^ File upload */

/* Keyboard interceptors/rerouters */

function interceptEnterKey(idOrClass) {
  $("#app-content").delegate("form input" + idOrClass, "keypress", function (e) {
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      return false; // cancel the default browser click
    }
  });
}

function interceptEnterKeyAndReroute(txt, btn) {
  $("#app-content").delegate(txt, "keypress", function (e) {
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      $(btn).click(); // click the associated button
      return false; // cancel the default browser click
    }
  });
}

/* ^ Keyboard interceptors/rerouters */

/* Buttons into links */

function logoutButtonAsLink() {
  var btn = $('input#logoutAction');
  if (!btn.length) {
    return;
  }
  btn.hide();
  btn.after('&nbsp;<a id=\"logoutAction.link\" name=\"logoutAction\" href="javascript:void(0);">'
          + btn.attr('value') + '</a>');
  $('#logoutAction\\.link').click(function () {
    btn.click();
    return false;
  });
}

/* ^ Buttons into links */

/* Collectionlisting interaction */

function collectionListingInteraction() {
  if(!$(".directoryListing").length) { return; }
  
  if(typeof moveUncheckedMessage != "undefined") { 
    var options = {
      formName: "collectionListingForm",
      btnId: "collectionListing\\.action\\.move-resources",
      service: "moveResourcesService",
      msg: moveUncheckedMessage
    };
    placeCopyMoveButtonInActiveTab(options);
  }
  if(typeof copyUncheckedMessage != "undefined") {
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
  
  // Checking rows in collectionlisting
  if($("td.checkbox").length) {
    $("th.checkbox").append("<input type='checkbox' name='checkUncheckAll' />")
    $("th.checkbox input").click(function() {
      if(this.checked) {
        checkAll();
      } else {
        uncheckAll();
      }
    });
  }

  $("td.checkbox input").click(toggleChecked);
  $("td.checkbox").click(function () {
    $(this).find("input").each(toggleChecked);
  });
}

// options: formName, btnId, service, msg
function placeCopyMoveButtonInActiveTab(options) {
  var btn = $("#" + options.btnId); 
  btn.hide();
  var li = $("li." + options.service);
  li.html("<a id='" + options.service + "' href='javascript:void(0);'>" + btn.attr('title') + "</a>");
  $("#" + options.service).click(function () {
    if (!$("form[name=" + options.formName + "] input[type=checkbox]:checked").length) {
      alert(options.msg);
    } else {
      $("#" + options.btnId).click();
    }
    return false;
  });
}

function placeDeleteButtonInActiveTab() {
  var btn = $('#collectionListing\\.action\\.delete-resources');
  btn.hide();
  var li = $('li.deleteResourcesService');
  li.html('<a id="deleteResourceService" href="javascript:void(0);">' + btn.attr('title') + '</a>');
  $('#deleteResourceService').click(function () {
    var boxes = $('form[name=collectionListingForm] input[type=checkbox]:checked');
    if (!boxes.length) {
      alert(deleteUncheckedMessage);
    } else {
      var list = "";
      var boxesSize = boxes.size();
      for (var i = 0; i < boxesSize && i < 10; i++) {
        list += boxes[i].name + '\n';
      }
      if (boxesSize > 10) {
        list += "... " + confirmDeleteAnd + " " + (boxesSize - 10) + " " + confirmDeleteMore;
      }
      if (confirm(confirmDelete.replace("(1)", boxesSize) + '\n\n' + list)) {
        $('#collectionListing\\.action\\.delete-resources').click();
      }
    }
    return false;
  });
}

function placeRecoverButtonInActiveTab() {
  var btn = $('.recoverResource');
  if (!btn.length) {
    return;
  }
  btn.hide();
  $("#main .activeTab").prepend('<ul class="list-menu tabMenu2"><li class="recoverResourceService">'
                              + '<a id="recoverResourceService" href="javascript:void(0);">' 
                              + btn.attr('value') + '</a></li></ul>');
  $('#recoverResourceService').click(function () {
    var boxes = $('form.trashcan input[type=checkbox]:checked');
    if (!boxes.length) {
      alert(recoverUncheckedMessage); //TODO i18n from somewhere
    } else {
      $('.recoverResource').click();
    }
    return false;
  });
}

function placeDeletePermanentButtonInActiveTab() {
  var btn = $('.deleteResourcePermanent');
  if (!btn.length) {
    return;
  }
  btn.hide();
  $("#main .activeTab .tabMenu2")
    .append('<li class="deleteResourcePermanentService"><a id="deleteResourcePermanentService" href="javascript:void(0);">' 
          + btn.attr('value') + '</a></li>');
  $('#deleteResourcePermanentService').click(function () {
    var boxes = $('form.trashcan input[type=checkbox]:checked');
    
    if (!boxes.length) {
      alert(deletePermanentlyUncheckedMessage);
    } else {
      var list = "";
      var boxesSize = boxes.size();
      for (var i = 0; i < boxesSize && i < 10; i++) {
        list += boxes[i].title + '\n';
      }
      if (boxesSize > 10) {
        list += "... " + confirmDeletePermanentlyAnd + " " + (boxesSize - 10) + " " + confirmDeletePermanentlyMore;
      }
      if (confirm(confirmDeletePermanently.replace("(1)", boxesSize) + '\n\n' + list)) {
        $('.deleteResourcePermanent').click();
      }
    }
    return false;
  });
}

function checkAll() {
  $("td.checkbox input").each(function () {
    this.checked = true;
    switchCheckedRow(this);
  });
}

function uncheckAll() {
  $("td.checkbox input").each(function () {
    this.checked = false;
    switchCheckedRow(this);
  });
}

function toggleChecked() {
  if (this.checked) {
    this.checked = false;
  } else {
    this.checked = true;
  }
  switchCheckedRow(this);
}

function switchCheckedRow(checkbox) {
  if (checkbox.checked) {
    $(checkbox).parent().parent().addClass("checked");
  } else {
    $(checkbox).parent().parent().removeClass("checked");
  }
}

/* ^ Collectionlisting interaction */

/* Permissions */

function initPermissionForm(selectorClass) {
  if(!$("." + selectorClass + " .aclEdit").length) return;

  toggleConfigCustomPermissions(selectorClass);
  interceptEnterKeyAndReroute("." + selectorClass + " .addUser input[type=text]",
                              "." + selectorClass + " input.addUserButton");
  interceptEnterKeyAndReroute("." + selectorClass + " .addGroup input[type=text]",
                              "." + selectorClass + " input.addGroupButton");
  permissionsAutocomplete('userNames', 
                          'userNames', vrtxAdmin.permissionsAutocompleteParams);
  splitAutocompleteSuggestion('userNames');
  permissionsAutocomplete('groupNames', 
                          'groupNames', vrtxAdmin.permissionsAutocompleteParams);
}

function toggleConfigCustomPermissions(selectorClass) {
    if (!$("." + selectorClass + " ul.shortcuts label[for=custom] input").is(":checked")
        && $("." + selectorClass + " ul.shortcuts label[for=custom] input").length) {
      $("." + selectorClass).find(".principalList").hide(0);
    }
    $("#app-content").delegate("." + selectorClass + " ul.shortcuts label[for=custom]", "click", function (e) {
      $(this).closest("form").find(".principalList:hidden").slideDown(vrtxAdmin.transitionCustomPermissionSpeed, vrtxAdmin.transitionEasing);
      e.stopPropagation();
    });
    $("#app-content").delegate("." + selectorClass + " ul.shortcuts label:not([for=custom])", "click", function (e) {
      $(this).closest("form").find(".principalList:visible").slideUp(vrtxAdmin.transitionCustomPermissionSpeed, vrtxAdmin.transitionEasing);
      e.stopPropagation();
    });
}

function checkStillAdmin(selector) {
  var stillAdmin = selector.find(".still-admin").text();
  if(stillAdmin == "false") {
    if(!confirm("Are you sure you want to remove all admin permissions for yourself?'")) {
      return false;
    }
  }
  return true; 
}

/* ^ Permissions */

/* Dropdowns */

function dropdownLanguageMenu() {
  var languageMenu = $(".localeSelection ul");
  if (languageMenu.length) {
    var parent = languageMenu.parent();
    parent.addClass("js-on");

    // Remove ':' and replace <span> with <a>
    var header = parent.find(".localeSelectionHeader");
    var headerText = header.text();
    // outerHtml
    header.replaceWith("<a href='javascript:void(0);' class='localeSelectionHeader'>"
                     + headerText.substring(0, headerText.length - 1) + "</a>");

    languageMenu.addClass("dropdown-shortcut-menu-container");

    $(".localeSelection").delegate(".localeSelectionHeader", "click", function (e) {
      $(this).next(".dropdown-shortcut-menu-container").slideToggle(vrtxAdmin.transitionDropdownSpeed, vrtxAdmin.transitionEasing);
      e.stopPropagation();
      return false;
    });
  }
}

function dropdownCollectionGlobalMenu() {
  var collectionGlobalMenu = $("#titleContainer .resource-title.true ul.resourceMenuLeft");
  var numberOfShortcuts = collectionGlobalMenu.find("li").size();

  // Make sure it is a folder with more than one choice
  if (collectionGlobalMenu.length && numberOfShortcuts > 1) {
    collectionGlobalMenu.addClass("dropdown-shortcut-menu");

    // Move listelements except .first into container
    $("ul.dropdown-shortcut-menu").parent().append("<div class='dropdown-shortcut-menu-container'><ul>" + collectionGlobalMenu.html() + "</ul></div>");
    collectionGlobalMenu.find("li").not(".first").remove();
    collectionGlobalMenu.find("li.first").append("<span id='dropdown-shortcut-menu-click-area'></span>");

    var shortcutMenu = $(".resource-title .dropdown-shortcut-menu-container");
    shortcutMenu.find("li.first").remove();
    shortcutMenu.css("left", (collectionGlobalMenu.width() - 24) + "px");

    collectionGlobalMenu.find("li.first #dropdown-shortcut-menu-click-area").click(function (e) {
      shortcutMenu.slideToggle(vrtxAdmin.transitionDropdownSpeed, vrtxAdmin.transitionEasing);
      e.stopPropagation();
      return false;
    });

    collectionGlobalMenu.find("li.first #dropdown-shortcut-menu-click-area").hover(function () {
      var elm = $(this);
      elm.parent().toggleClass('unhover');
      elm.prev().toggleClass('hover');
    }, function () {
      var elm = $(this);
      elm.parent().toggleClass('unhover');
      elm.prev().toggleClass('hover');
    });
  }
}

/* ^ Dropdowns */

/**
 * GET form with AJAX
 *
 * @param options: selector: selector for links that should GET asynchronous form
 *                 selectorClass: selector for form
 *                 insertAfterOrReplaceClass: where to put the form
 *                 isReplacing: replace instead of insert after
 *                 nodeType: node type that should be replaced or inserted
 *                 funcComplete: callback function(selectorClass) to run when AJAX is completed and form is visible
 *                 simultanSliding: whether to slideUp existing form at the same time slideDown new form 
 *                                  (only when there is an existing form)
 */

VrtxAdmin.prototype.getAjaxForm = function getAjaxForm(options) {
  var args = arguments; // this function
  
  $("#app-content").delegate(options.selector, "click", function (e) {
                          
    var url = $(this).attr("href") || $(this).closest("form").attr("action");
                       
    if(location.protocol == "http:" && url.indexOf("https://") != -1) {
      return; // no AJAX when http -> https (tmp. solution)
    }
    
    // Make sure we get the mode markup (current page) if service is not mode
    // -- only if a expandedForm exists and is of the replaced kind..
    //
    var fromModeToNotMode = false;
    var modeUrl = "";
    var existExpandedFormIsReplacing = false;
    var existExpandedForm = false;
    
    if($(".expandedForm").length) {
      if($(".expandedForm").hasClass("expandedFormIsReplaced")) {                      
        if(url.indexOf("&mode=") == -1) {
          var currentHref = location.href; 
          // Partly based on: http://snipplr.com/view/799/get-url-variables/
          var hashes = currentHref.slice(currentHref.indexOf('?') + 1).split('&');
          for(var i = hashes.length; i--;) {
            if(hashes[i].indexOf("mode=") != -1) {
              fromModeToNotMode = true; 
              modeUrl = currentHref;
            }
          } 
        }
        existExpandedFormIsReplacing = true;
      }
      existExpandedForm = true;
    }
    //---
    
    $.ajax({
      type: "GET",
      url: url,
      dataType: "html",
      success: function (results, status, resp) {
        var form = $(results).find("." + options.selectorClass).html();

        // If something went wrong
        if(!form) {
          vrtxAdmin.error({args: args, msg: "retrieved form from " + url + " is null"});
        }

        // Another form is already open
        if(existExpandedForm) {
          var expandedHtml = vrtxAdmin.outerHTML("#app-content", ".expandedForm");

          // Filter out selector class to get original markup for the existing form
          var resultSelectorClasses = $(expandedHtml).attr("class").split(" ");
          var resultSelectorClass = "";
          // Must have full control over additional classes in FTL we need to remove
          // (meaning which is the unique selector class)
          // TODO: add a postfix to original markup, e.g. 'ajax-' to unique class where we use AJAX
          //       -- to avoid filtering out others
          for(var i = resultSelectorClasses.length; i--;) {
            var resultSelectorClassCache = resultSelectorClasses[i];
            if(resultSelectorClassCache.indexOf("expandedForm") == -1
               && resultSelectorClassCache.indexOf("nodeType") == -1
               && resultSelectorClassCache 
               && resultSelectorClass.indexOf(resultSelectorClasses[i]) == -1
               && resultSelectorClassCache.indexOf("even") == -1
               && resultSelectorClassCache.indexOf("odd") == -1
               && resultSelectorClassCache.indexOf("first") == -1
               && resultSelectorClassCache.indexOf("last") == -1) {
                 resultSelectorClass = "." + resultSelectorClasses[i];
                 break;
            }  
          } 
          // --
          
          $("#app-content .expandedForm").slideUp(vrtxAdmin.transitionSpeed, vrtxAdmin.transitionEasing, function() {
            if(existExpandedFormIsReplacing) {
              var expanded = $(this);

              // When we need the 'mode=' HTML when requesting a 'not mode=' service
              if(fromModeToNotMode) {
                $.ajax({
                  type: "GET",
                  url: modeUrl,
                  dataType: "html",
                  success: function (results, status, resp) {
                    var resultHtml = vrtxAdmin.outerHTML(results, $.trim(resultSelectorClass));
                    
                    // If all went wrong
                    if(!resultHtml) {
                      vrtxAdmin.error({args: args, msg: "retrieved existing expandedForm from " + modeUrl + " is null"});
                    }
                    
                    if(expanded.parent().parent().is("tr")) {  // Because 'this' is tr > td > div
                      expanded.parent().parent().replaceWith(resultHtml).show(0);
                    } else {
                      expanded.replaceWith(resultHtml).show(0);              
                    }
                    
                     vrtxAdmin.getAjaxFormShow(options, form);
                  },
                  error: function (xhr, textStatus) {
                    displayAjaxErrorMessage(xhr, textStatus);
                  }
                });
              } else {
                var resultHtml = vrtxAdmin.outerHTML(results, $.trim(resultSelectorClass));
                
                // If all went wrong
                if(!resultHtml) {
                  vrtxAdmin.error({args: args, msg: "retrieved existing expandedForm from " + url + " is null"});
                }
              
                if(expanded.parent().parent().is("tr")) {  // Because 'this' is tr > td > div
                  expanded.parent().parent().replaceWith(resultHtml).show(0);
                } else {
                  expanded.replaceWith(resultHtml).show(0);              
                }
              }
            } else {
              if($(this).parent().parent().is("tr")) {  // Because 'this' is tr > td > div
                $(this).parent().parent().remove();  
              } else {
                $(this).remove();            
              }
            }
            if(!options.simultanSliding && !fromModeToNotMode) {
              vrtxAdmin.getAjaxFormShow(options, form);
            }
          });
        }
        if ((!existExpandedForm || options.simultanSliding) && !fromModeToNotMode) {
          vrtxAdmin.getAjaxFormShow(options, form);
        }
      },
      error: function (xhr, textStatus) {
        vrtxAdmin.displayAjaxErrorMessage(xhr, textStatus); 
      }
    });

    e.stopPropagation();
    return false;
  });
};

VrtxAdmin.prototype.getAjaxFormShow = function(options, form) {
  if (options.isReplacing) {
    var classes = $(options.insertAfterOrReplaceClass).attr("class");
    $(options.insertAfterOrReplaceClass).replaceWith(vrtxAdmin.wrap(options.nodeType, "expandedForm expandedFormIsReplaced nodeType"
                                                                  + options.nodeType + " " + options.selectorClass + " " + classes, form));
  } else {
    $(vrtxAdmin.wrap(options.nodeType, "expandedForm nodeType" + options.nodeType + " " + options.selectorClass, form))
      .insertAfter(options.insertAfterOrReplaceClass);
  }
  if(options.funcComplete) {
    options.funcComplete(options.selectorClass);
  }
  if(options.nodeType == "tr") {
    $(options.nodeType + "." + options.selectorClass).prepareTableRowForSliding();
  }
  $(options.nodeType + "." + options.selectorClass).hide().slideDown(vrtxAdmin.transitionSpeed, vrtxAdmin.transitionEasing, function() {
    $(this).find("input[type=text]:first").focus();
  });
};

/**
 * POST form with AJAX
 *
 * @param option: selector: selector for links that should POST asynchronous form
 *                updateSelectors: one or more selectors for markup that should update after POST (Array)
 *                errorContainer: selector for error container
 *                errorContainerInsertAfter: selector for where error container should be inserted after
 *                funcProceedCondition: must return true to continue
 *                funcComplete: callback function to run when AJAX is completed
 */

VrtxAdmin.prototype.postAjaxForm = function postAjaxForm(options) {
  var args = arguments; // this function

  $("#app-content").delegate(options.selector, "click", function (e) {
    var link = $(this);
    var form = link.closest("form");
    if(!options.funcProceedCondition || options.funcProceedCondition(form)) {
      var url = form.attr("action");
      var encType = form.attr("enctype");

      var dataString = vrtxAdmin.appendInputNameValuePairsToDataString(form.find("input[type=text]"));
      dataString += vrtxAdmin.appendInputNameValuePairsToDataString(form.find("input[type=file]"));
      dataString += vrtxAdmin.appendInputNameValuePairsToDataString(form.find("input[type=radio]:checked"));
      dataString += vrtxAdmin.appendInputNameValuePairsToDataString(form.find("input[type=checkbox]:checked"));
      dataString += '&csrf-prevention-token=' + form.find("input[name='csrf-prevention-token']").val()
                  + "&" + link.attr("name");
                  
      if (typeof encType === "undefined" || !encType.length) {
        encType = "application/x-www-form-urlencoded";
      }

      $.ajax({
        type: "POST",
        url: url,
        data: dataString,
        dataType: "html",
        contentType: encType,
        success: function (results, status, resp) {
          if (vrtxAdmin.hasErrorContainers(results, options.errorContainer)) {
            vrtxAdmin.displayErrorContainers(results, form, options.errorContainerInsertAfter, options.errorContainer);
          } else {
            for(var i = options.updateSelectors.length; i--;) {
             var outer = vrtxAdmin.outerHTML(results, options.updateSelectors[i]);
             $("#app-content " + options.updateSelectors[i]).replaceWith(outer);
            }
            if(options.funcComplete) {
              options.funcComplete();
            }
            form.parent().slideUp(vrtxAdmin.transitionSpeed, vrtxAdmin.transitionEasing, function () {
              $(this).remove();
            });
          }
        },
        error: function (xhr, textStatus) {
          vrtxAdmin.displayAjaxErrorMessage(xhr, textStatus);
        }
      });

    }

    e.stopPropagation();
    return false;
  });
};

/**
 * POST remove-links (value is in the name)
 * 
 * @param selector: selector for links that should post asynchronous
 * @param updateSelector: selector for markup to update
 */

VrtxAdmin.prototype.ajaxRemove = function ajaxRemove(selector, updateSelector) {
  var args = arguments; // this function

  $("#app-content").delegate(selector, "click", function (e) {
    var link = $(this);
    var form = link.closest("form");
    var url = form.attr("action");
    var listElement = link.parent();

    var dataString = '&csrf-prevention-token=' + form.find("input[name='csrf-prevention-token']").val()
                   + "&" + link.attr("name");
    $.ajax({
      type: "POST",
      url: url,
      data: dataString,
      dataType: "html",
      success: function (results, status, resp) {
        form.find(updateSelector).html($(results).find(updateSelector).html());
      },
      error: function (xhr, textStatus) {
        vrtxAdmin.displayAjaxErrorMessage(xhr, textStatus); 
      }
    });

    e.stopPropagation();
    return false;
  });
};

/**
 * POST add-links (values is in the textfield)
 * 
 * @param selector: selector for links that should post asynchronous
 * @param updateSelector: selector for markup to update
 * @param errorContainer: selector for error container
 */

VrtxAdmin.prototype.ajaxAdd = function ajaxAdd(selector, updateSelector, errorContainer) {
  var args = arguments; // this function

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

    $.ajax({
      type: "POST",
      url: url,
      data: dataString,
      dataType: "html",
      success: function (results, status, resp) {
        if (vrtxAdmin.hasErrorContainers(results, errorContainer)) {
          vrtxAdmin.displayErrorContainers(results, form, updateSelector, errorContainer);
        } else {
          form.find(updateSelector).html($(results).find(updateSelector).html());
          textfield.val("");
        }
      },
      error: function (xhr, textStatus) {
        vrtxAdmin.displayAjaxErrorMessage(xhr, textStatus);
      }
    });

    e.stopPropagation();
    return false;
  });
};

/* AJAX helper functions */

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

VrtxAdmin.prototype.displayAjaxErrorMessage = function(xhr, textStatus) {
  if (xhr.readyState == 4 && xhr.status == 200) {
    var msg = "The service is not active: " + textStatus;
  } else {
    var msg = "The service returned " + xhr.status + " and failed to retrieve/post form.";
  }
  if ($("#app-content > .errormessage").length) {
    $("#app-content > .errormessage").html(msg);
  } else {
    $("#app-content").prepend("<div class='errormessage message'>" + msg + "</div>");
  }
};

/* ^ AJAX helper functions */

/**
 * Show and hide properties
 * 
 * @param radioIds: Multiple id's for radiobuttons binding click events (Array)
 * @param conditionHide: Condition to be checked for hiding
 * @param conditionHideEqual: What it should equal
 * @param showHideProps: Multiple props / id's / classnames to show / hide (Array)
 */

function showHide(radioIds, conditionHide, conditionHideEqual, showHideProps) {
  var showHidePropertiesFunc = showHideProperties;

  // Init
  showHidePropertiesFunc(true, conditionHide, conditionHideEqual, showHideProps);

  for (var j = 0, len = radioIds.length; j < len; j++) {
    $(radioIds[j]).click(function () {
      showHidePropertiesFunc(false, conditionHide, conditionHideEqual, showHideProps);
    });
  }
}

function showHideProperties(init, conditionHide, conditionHideEqual, showHideProps) {
  var conditionHideVal = $(conditionHide).val();
  var showHidePropertyFunc = showHideProperty;
  for (var i = 0, len = showHideProps.length; i < len; i++) {
    showHidePropertyFunc(showHideProps[i], init, conditionHideVal == conditionHideEqual ? false : true);
  }
}

function showHideProperty(id, init, show) {
  if (init) {
    if (show) {
      $(id).show();
    } else {
      $(id).hide();
    }
  } else {
    if (show) {
      $(id).slideDown(vrtxAdmin.transitionPropSpeed, vrtxAdmin.transitionEasing);
    } else {
      $(id).slideUp(vrtxAdmin.transitionPropSpeed, vrtxAdmin.transitionEasing);
    }
  }
}

VrtxAdmin.prototype.zebraTables = function(selector) {
  // http://www.quirksmode.org/css/contents.html
  if((vrtxAdmin.isIE && vrtxAdmin.browserVersion < 9) || vrtxAdmin.isOpera) {
    $("table" + selector + " tbody tr:odd").addClass("even"); // hmm.. somehow even is odd and odd is even
    $("table" + selector + " tbody tr:first-child").addClass("first");
  }
};

/* Featured articles */

function loadFeaturedArticles(addName, removeName, browseName, editorBase, baseFolder, editorBrowseUrl) {
  if ($("#resource\\.featured-articles").val() == null) {
    return;
  }

  $("#resource\\.featured-articles").hide();
  $("#resource\\.featured-articles").parent().hide();

  $("#vrtx-resource\\.featured-articles").append("<div id='vrtx-featured-article-add'>"
                                               + "<div class=\"vrtx-button\"><button onclick=\"addFormField(null, '" 
                                               + removeName + "', '" + browseName + "', '" + editorBase + "', '" + baseFolder 
                                               + "', '" + editorBrowseUrl + "'); return false;\">" + addName + "</button></div>"
                                               + "<input type='hidden' id='id' name='id' value='1' />");

  var listOfFiles = document.getElementById("resource\.featured-articles").value.split(",");
  var addFormFieldFunc = addFormField;
  for (var i = 0, len = listOfFiles.length; i < len; i++) {
    addFormFieldFunc(jQuery.trim(listOfFiles[i]), removeName, browseName, editorBase, baseFolder, editorBrowseUrl);
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

function addFormField(value, removeName, browsName, editorBase, baseFolder, editorBrowseUrl) {
  var idstr = "vrtx-featured-articles-";
  if (value == null) {
    value = "";
  }
  if (removeName == null) {
    var deleteRow = "";
  } else {
    var deleteRow = "<div class=\"vrtx-button\"><button type='button' id='" + idstr
                  + "remove' onClick='removeFormField(\"#" + idstr + "row-" + countId + "\"); return false;'>" 
                  + removeName + "</button></div>";
  }

  var browseServer = "<div class=\"vrtx-button\"><button type=\"button\" id=\"" + idstr 
                   + "browse\" onclick=\"browseServer('" + idstr + countId + "', '" + editorBase 
                   + "', '" + baseFolder + "', '" + editorBrowseUrl + "', 'File');\">" + browsName + "</button></div>";

  var html = "<div class='" + idstr + "row' id='" + idstr + "row-" + countId + "'><div class=\"vrtx-textfield\"><input value='" 
    + value + "' type='text' size='20′ name='txt[]' id='" + idstr + countId + "' /></div>" 
    + browseServer + deleteRow + "</div>";

  $(html).insertBefore("#vrtx-featured-article-add");

  countId++;
}

function removeFormField(id) {
  $(id).remove();
}

function formatFeaturedArticlesData() {
  if ($("#resource\\.featured-articles").val() == null) {
    return;
  }
  var data = $("input[id^='vrtx-featured-articles-']");
  var result = "";
  for (var i = 0, len = data.length; i < len; i++) {
    result += data[i].value;
    if (i < (len - 1)) {
      result += ",";
    }
  }
  document.getElementById("resource\.featured-articles").value = result;
}

/* ^ Featured articles */

/* CK browse server integration */

var urlobj;

function previewImage(urlobj) {
  var previewNode = document.getElementById(urlobj + '.preview');
  if (previewNode) {
    var url = document.getElementById(urlobj).value;
    if (url) {
      previewNode.innerHTML = '<img src="' + url + '?vrtx=thumbnail" alt="thumbnail" />';
    } else {
      previewNode.innerHTML = '';
    }
  }
  adjustImageAndCaptionContainer(previewNode);
}

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
  urlobj = obj;
  if (type) {
    openServerBrowser(editorBase + '/plugins/filemanager/browser/default/browser.html?BaseFolder='
                    + baseFolder + '&Type=' + type + '&Connector=' + editorBrowseUrl, screen.width * 0.7, screen.height * 0.7);

  } else {
    openServerBrowser(editorBase + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' 
                    + baseFolder + '&Type=Image&Connector=' + editorBrowseUrl, screen.width * 0.7, screen.height * 0.7);
  }
}
 
function openServerBrowser(url, width, height) {
  var iLeft = (screen.width - width) / 2;
  var iTop = (screen.height - height) / 2;
  var sOptions = "toolbar=no,status=no,resizable=yes,dependent=yes";
  sOptions += ",width=" + width;
  sOptions += ",height=" + height;
  sOptions += ",left=" + iLeft;
  sOptions += ",top=" + iTop;
  var oWindow = window.open(url, "BrowseWindow", sOptions);
}

// Callback from the CKEditor image browser:
function SetUrl(url, width, height, alt) {
  url = decodeURIComponent(url);
  if (urlobj) {
    document.getElementById(urlobj).value = url;
  }
  oWindow = null;
  previewImage(urlobj);
  urlobj = "";
}

/* ^ CK browse server integration */

/* Utils */

// Use jQuery wrap function instead?
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
  if(typeof console !== "undefined" && console.log) {
    if(options.args) {
      console.log("Vortex admin log -> " + options.args.callee.name + ": " + options.msg);
    } else {
      console.log("Vortex admin log: " + options.msg);    
    }
  }
};

VrtxAdmin.prototype.error = function(options) {
  if(typeof console !== "undefined") {
    if(console.error) {
      if(options.args) {
        console.error("Vortex admin error -> " + options.args.callee.name + ": " + options.msg);
      } else {
        console.error("Vortex admin error: " + options.msg);     
      }
    } else if(console.log) {
      if(options.args) {
        console.log("Vortex admin error -> " + options.args.callee.name + ": " + options.msg);   
      } else {
        console.log("Vortex admin error: " + options.msg);        
      } 
    }
  }
};

/* ^ Utils */

/* Override slideUp() / slideDown() to handle rows in a table
 *
 * Credits: 
 * http://stackoverflow.com/questions/467336/jquery-how-to-use-slidedown-or-show-function-on-a-table-row/920480#920480
 * http://www.bennadel.com/blog/1624-Ask-Ben-Overriding-Core-jQuery-Methods.htm
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

/* ^ Override slideUp() / slideDown() to handle rows in a table */

/* ^ Vortex Admin enhancements */