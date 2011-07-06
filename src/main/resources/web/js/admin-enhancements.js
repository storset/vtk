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
  
  this.isIE = null;
  this.version = null;
  this.isIE6 = null;
  this.isIE5OrHigher = null;
  this.isWin = null;
  this.supportsFileAPI = null;
  this.permissionsAutocompleteParams = null;
  this.transitionSpeed = 200; // same as 'fast'
  this.transitionCustomPermissionSpeed = 200; // same as 'fast'
  this.transitionPropSpeed = 100;
  this.transitionDropdownSpeed = 100;
  
  return instance;
};

var vrtxAdmin = new VrtxAdmin();

// Browser info
vrtxAdmin.isIE = $.browser.msie;
vrtxAdmin.version = $.browser.version;
vrtxAdmin.isIE6 = vrtxAdmin.isIE && vrtxAdmin.version <= 6;
vrtxAdmin.isIE5OrHigher = vrtxAdmin.isIE && vrtxAdmin.version >= 5;
vrtxAdmin.isWin = ((agent.indexOf("win") != -1) || (agent.indexOf("16bit") != -1));
vrtxAdmin.supportsFileAPI = window.File && window.FileReader && window.FileList && window.Blob;

// Permission Autocomplete parameters
vrtxAdmin.permissionsAutocompleteParams = { minChars: 4, 
                                            selectFirst: false, 
                                            width: 300, 
                                            max: 30,
                                            delay: 800 };
                                            
$(document).ready(function () {

  // Buttons into links
  logoutButtonAsLink();

  // Collectionlisting interaction
  collectionListingInteraction();

  // Dropdowns
  dropdownLanguageMenu();
  dropdownCollectionGlobalMenu();

  /* GET/POST forms with AJAX (initalization/config) */
  
  var getAjaxOptions = {};
  var postAjaxOptions = {};

  // Global menu service forms
  var globalMenuServices = ["renameService",
                            "publish\\.globalUnpublishService",
                            "publish\\.globalPublishService",
                            "manage\\.createArchiveService"];

  for (var i = globalMenuServices.length; i--;) {
    getAjaxOptions = {
        selector: "#titleContainer a#" + globalMenuServices[i],
        selectorClass: "globalmenu",
        insertAfterOrReplaceClass: "#titleContainer ul.globalMenu",
        isReplacing: false,
        nodeType: "div"
    };
    vrtxAdmin.getAjaxForm(getAjaxOptions);
  }

  // Tab menu service forms
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
        nodeType: "div"
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
        funcComplete: function(p){ initFileUpload() }
      };
      vrtxAdmin.getAjaxForm(getAjaxOptions);
      initFileUpload(); // when error message
    }
  }
  
  // Permission privilegie forms (READ, READ_WRITE, ALL)
  var privilegiesPermissions = ["read",
                                "read-write",
                                "all"];

  for (i = privilegiesPermissions.length; i--;) {
    getAjaxOptions = {
      selector: "div.permissions-" + privilegiesPermissions[i] + "-wrapper a.full-ajax",
      selectorClass: "expandedForm-" + privilegiesPermissions[i],
      insertAfterOrReplaceClass: "div.permissions-" + privilegiesPermissions[i] + "-wrapper",
      isReplacing: true,
      nodeType: "div",
      funcComplete: initPermissionForm
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
  
  // More permission privilegie forms in table (ADD_COMMENT, READ_PROCESSED)
  var privilegiesPermissionsInTable = ["add-comment",
                                       "read-processed"];

  for (i = privilegiesPermissionsInTable.length; i--;) {
    getAjaxOptions = {
      selector: ".privilegeTable tr." + privilegiesPermissionsInTable[i] + " a.full-ajax",
      selectorClass: privilegiesPermissionsInTable[i],
      insertAfterOrReplaceClass: "tr." + privilegiesPermissionsInTable[i],
      isReplacing: true,
      nodeType: "tr",
      funcComplete: initPermissionForm
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

  // About property forms
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
      nodeType: "tr"
    };
    vrtxAdmin.getAjaxForm(getAjaxOptions);
  }
  
  // Remove/add permissions
  vrtxAdmin.ajaxRemove("input.removePermission", ".principalList");
  vrtxAdmin.ajaxAdd("span.addGroup", ".groups-wrapper", "errorContainer");
  vrtxAdmin.ajaxAdd("span.addUser", ".users-wrapper", "errorContainer");

  /* ^ GET/POST forms with AJAX (initalization/config) */

  // Show/hide multiple properties (initalization/config)
  showHide(["#resource\\.recursive-listing\\.false", "#resource\\.recursive-listing\\.unspecified"],
            "#resource\\.recursive-listing\\.false:checked", 'false', ["#vrtx-resource\\.recursive-listing-subfolders"]);

  showHide(["#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"],
            "#resource\\.display-type\\.calendar:checked", null, ["#vrtx-resource\\.event-type-title"]);

  showHide(["#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"],
            "#resource\\.display-type\\.calendar:checked", 'calendar', ["#vrtx-resource\\.hide-additional-content"]);

  // Remove active tab if it has no children
  if (!$("#main .activeTab > *").length) {
    $("#main .activeTab").remove();
  }

  // Fix IE 6 collectionlisting hover
  if (vrtxAdmin.isIE6) {
    $("table.directoryListing tr").hover(function () {
      $(this).toggleClass('hover');
    }, function () {
      $(this).toggleClass('hover');
    });
  }

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
    inputFile.change(function() {
      var txt = $(this).val();
      $(this).closest("form").find("#fake-file").val(txt);
      // fileInfo("file");
    });

    var textfieldWrapper = form.find(".vrtx-textfield"); 
    textfieldWrapper.addClass("vrtx-file-upload");
    textfieldWrapper.append("<input id='fake-file' />");

    $("<a class='vrtx-button vrtx-file-upload'><span>Browse...</span></a>")
      .insertBefore("form[name=fileUploadService] #submitButtons").click(function() {
        $(this).closest("form").find("#file").trigger("click");
        return false;
    });
    if (supportsMultipleAttribute(document.getElementById("file"))) {
      inputFile.attr("multiple", "multiple");
      var multipleFilesInfoText = "<strong>Laste opp flere filer samtidig</strong>?<br />"
                                + "Hold nede CTRL eller CMD (på Mac) når du velger filer i filutforskeren.";
      $("<p id='vrtx-file-upload-info-text'>" + multipleFilesInfoText + "</p>").insertAfter(".vrtx-button.vrtx-file-upload");
    }
  }
}

// Taken from: http://miketaylr.com/code/input-type-attr.html (MIT license)
function supportsMultipleAttribute(inputfield) {
  return (!! (inputfield.multiple === false) && !! (inputfield.multiple !== "undefined"));
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
  $(".vrtx-check-all").click(checkAll);
  $(".vrtx-uncheck-all").click(uncheckAll);

  $(".checkbox input").click(toggleChecked);
  $(".checkbox").click(function () {
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
  $("#main .activeTab").prepend('<ul class="listMenu tabMenu2"><li class="recoverResourceService">'
                              + '<a id="recoverResourceService" href="javascript:void(0);">' 
                              + btn.attr('value') + '</a></li></ul>');
  $('#recoverResourceService').click(function () {
    var boxes = $('form.trashcan input[type=checkbox]:checked');
    //TODO i18n from somewhere
    var recoverUncheckedMessage = 'You must check at least one element to recover';

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
    //TODO i18n from somewhere
    var deletePermanentlyUncheckedMessage = 'You must check at least one element to delete permanently';

    if (!boxes.length) {
      alert(deletePermanentlyUncheckedMessage);
    } else {
      //TODO i18n from somewhere
      var confirmDeletePermanently = 'Are you sure you want to delete:';
      var confirmDeletePermanentlyAnd = 'and';
      var confirmDeletePermanentlyMore = 'more';

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
  $(".checkbox input").each(function () {
    this.checked = true;
    switchCheckedRow(this);
  });
}

function uncheckAll() {
  $(".checkbox input").each(function () {
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
      $(this).closest("form").find(".principalList:hidden").slideDown(vrtxAdmin.transitionCustomPermissionSpeed);
      e.stopPropagation();
    });
    $("#app-content").delegate("." + selectorClass + " ul.shortcuts label:not([for=custom])", "click", function (e) {
      $(this).closest("form").find(".principalList:visible").slideUp(vrtxAdmin.transitionCustomPermissionSpeed);
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
      $(this).next(".dropdown-shortcut-menu-container").slideToggle(vrtxAdmin.transitionDropdownSpeed);
      e.stopPropagation();
      return false;
    });
  }
}

function dropdownCollectionGlobalMenu() {
  var collectionGlobalMenu = $("#titleContainer .resource-title.true ul.globalMenu");
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
      shortcutMenu.slideToggle(vrtxAdmin.transitionDropdownSpeed);
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
 */

VrtxAdmin.prototype.getAjaxForm = function(options) {
  $("#app-content").delegate(options.selector, "click", function (e) {
    var url = $(this).attr("href");
    $.ajax({
      type: "GET",
      url: url,
      dataType: "html",
      success: function (results, status, resp) {
        var form = $(results).find("." + options.selectorClass).html();
          
        // Another form is already open
        if($(".expandedForm").length) {
          var classes = $(".expandedForm").attr("class").split(" ");
          var i = classes.length;
          var finalClass = "";
          var isReplaced = false;
          var theNodeType = "div";
          while(i--) {
            if(classes[i].indexOf("expandedForm") == -1) {
              if(classes[i].indexOf("nodeType") != -1) {
                theNodeType = classes[i].split("nodeType")[1];
              } else if(finalClass.indexOf(classes[i]) == -1) {
                finalClass += classes[i] + " ";
              }
            } else if(classes[i] == "expandedFormIsReplaced") {
              isReplaced = true;
            }
          }
          theNodeType = $.trim(theNodeType);
          finalClass = $.trim(finalClass.split(" ")[0]);
          $("#app-content .expandedForm").slideUp(vrtxAdmin.transitionSpeed, function() {
            if(isReplaced) {
              var elm = $(results).find("." + finalClass);
              var html = vrtxAdmin.wrap(theNodeType, elm.attr("class"), elm.html());
              if(theNodeType == "tr") {  // Because 'this' is tr > td > div
                $(this).parent().parent().replaceWith(html).show(0);
              } else {
                $(this).replaceWith(html).show(0);              
              }
            } else {
              if(theNodeType == "tr") {  // Because 'this' is tr > td > div
                $(this).parent().parent().remove();  
              } else {
                $(this).remove();            
              }
            }
          });
        }
        
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
        $(options.nodeType + "." + options.selectorClass).hide().slideDown(vrtxAdmin.transitionSpeed, function() {
          $(this).find("input[type=text]:first").focus();
        });
      },
      error: function (xhr, textStatus) {
        vrtxAdmin.displayAjaxErrorMessage(xhr, textStatus); 
      }
    });
    e.stopPropagation();
    return false;
  });
}

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

VrtxAdmin.prototype.postAjaxForm = function(options) {
  $("#app-content").delegate(options.selector, "click", function (e) {
    var link = $(this);
    var form = link.closest("form");
    if(!options.funcProceedCondition || options.funcProceedCondition(form)) {
      var url = form.attr("action");
      var encType = form.attr("enctype");

      var dataString = appendInputNameValuePairsToDataString(form.find("input[type=text]"));
      dataString += appendInputNameValuePairsToDataString(form.find("input[type=file]"));
      dataString += appendInputNameValuePairsToDataString(form.find("input[type=radio]:checked"));
      dataString += appendInputNameValuePairsToDataString(form.find("input[type=checkbox]:checked"));
      dataString += '&csrf-prevention-token=' + form.find("input[name='csrf-prevention-token']").val()
                  + "&" + link.attr("name");

      if (!encType.length) {
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
              // Filter out 'expandedForm'-classes
              var classes = $(options.updateSelectors[i]).attr("class").split(" ");
              var j = classes.length;
              var finalClass = "";
              while(j--) {
                if(classes[j].indexOf("expandedForm") == -1) {
                  finalClass += classes[j] + " ";
                }
              }
              $(options.updateSelectors[i]).attr("class", finalClass);
              $("#app-content").find(options.updateSelectors[i])
                .html($(results).find(options.updateSelectors[i]).html());
            }
            if(options.funcComplete) {
              options.funcComplete();
            }
            form.parent().slideUp(vrtxAdmin.transitionSpeed, function () {
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
}

/**
 * POST remove-links (value is in the name)
 * 
 * @param selector: selector for links that should post asynchronous
 * @param updateSelector: selector for markup to update
 */

VrtxAdmin.prototype.ajaxRemove = function(selector, updateSelector) {
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
}

/**
 * POST add-links (values is in the textfield)
 * 
 * @param selector: selector for links that should post asynchronous
 * @param updateSelector: selector for markup to update
 * @param errorContainer: selector for error container
 */

VrtxAdmin.prototype.ajaxAdd = function(selector, updateSelector, errorContainer) {
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
}

/* AJAX helper functions */

// Use jQuery wrap function instead?
VrtxAdmin.prototype.wrap = function(node, cls, html) {
  return "<" + node + " class='" + cls + "'>" 
         + html 
         + "</" + node + ">";
}

VrtxAdmin.prototype.appendInputNameValuePairsToDataString = function(inputFields) {
  var dataStringChunk = "";
  for (i = inputFields.length; i--;) {
    dataStringChunk += '&' + $(inputFields[i]).attr("name")
                     + '=' + $(inputFields[i]).val();
  }
  return dataStringChunk;
}

VrtxAdmin.prototype.hasErrorContainers = function(results, errorContainer) {
  return $(results).find("div." + errorContainer).length > 0;
}

/* TODO: support for multiple errorContainers
  (place the correct one in correct place (e.g. users and groups)) */
VrtxAdmin.prototype.displayErrorContainers = function(results, form, errorContainerInsertAfter, errorContainer) {
  var wrapper = form.find(errorContainerInsertAfter).parent();
  if (wrapper.find("div." + errorContainer).length) {
    wrapper.find("div." + errorContainer).html($(results).find("div." + errorContainer).html());
  } else {
    $(vrtxAdmin.wrap("div", errorContainer, $(results).find("div." + errorContainer).html()))
        .insertAfter(wrapper.find(errorContainerInsertAfter));
  }
}

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
}

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
      $(id).slideDown(vrtxAdmin.transitionPropSpeed);
    } else {
      $(id).slideUp(vrtxAdmin.transitionPropSpeed);
    }
  }
}

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
}

function addFormField(value, removeName, browsName, editorBase, baseFolder, editorBrowseUrl) {
  var idstr = "vrtx-featured-articles-";
  var id = document.getElementById("id").value;
  if (value == null) {
    value = "";
  }
  if (removeName == null) {
    var deleteRow = "";
  } else {
    var deleteRow = "<div class=\"vrtx-button\"><button type='button' id='" + idstr
                  + "remove' onClick='removeFormField(\"#" + idstr + "row-" + id + "\"); return false;'>" 
                  + removeName + "</button></div>";
  }

  var browseServer = "<div class=\"vrtx-button\"><button type=\"button\" id=\"" + idstr 
                   + "browse\" onclick=\"browseServer('" + idstr + id + "', '" + editorBase 
                   + "', '" + baseFolder + "', '" + editorBrowseUrl + "', 'File');\">" + browsName + "</button></div>";

  $("<div class='" + idstr + "row' id='" + idstr + "row-" + id + "'><div class=\"vrtx-textfield\"><input value='" 
    + value + "'type='text' size='20′ name='txt[]' id='" + idstr + id + "' /></div>" 
    + browseServer + deleteRow + "</div></div>").insertBefore("#vrtx-featured-article-add");

  id++;
  document.getElementById("id").value = id;
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
}

var urlobj;
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
jQuery.fn.slideUp = function(speed, callback) {
  $trOrOtherElm = this;
  if($trOrOtherElm.is("tr")) {
    $trOrOtherElm.find('td > div').animate({height: 'toggle'}, speed, callback);
  } else {
    originalSlideUp.apply($trOrOtherElm, arguments);
  }
}

var originalSlideDown = jQuery.fn.slideDown;
jQuery.fn.slideDown = function(speed, callback) {
  $trOrOtherElm = this;
  if($trOrOtherElm.is("tr")) {
    if ($trOrOtherElm.is(':hidden')) {
      $trOrOtherElm.show().find('td > div').animate({height: 'toggle'}, speed, callback);
    }
  } else {
    originalSlideDown.apply($trOrOtherElm, arguments);
  }
}

/* ^ Override slideUp() / slideDown() to handle rows in a table */

/* ^ Vortex Admin enhancements */