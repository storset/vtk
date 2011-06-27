/*
 *  Vortex Admin enhancements
 *
 */

var vrtxAdmin = {
  isIE: null,
  version: null,
  isIE6: null,
  isIE5OrHigher: null,
  isWin: null,
  transitionSpeed: 400, // same as 'default'
  transitionCustomPermissionSpeed: 200, // same as 'fast'
  transitionPropSpeed: 100,
  transitionDropdownSpeed: 100
};

// Using init-time branching
vrtxAdmin.isIE = $.browser.msie;
vrtxAdmin.version = $.browser.version;
vrtxAdmin.isIE6 = vrtxAdmin.isIE && vrtxAdmin.version <= 6;
vrtxAdmin.isIE5OrHigher = vrtxAdmin.isIE && vrtxAdmin.version >= 5;
var agt = navigator.userAgent.toLowerCase();
vrtxAdmin.isWin = ((agt.indexOf("win") != -1) || (agt.indexOf("16bit") != -1));

$(document).ready(function () {

  // Buttons into links
  logoutButtonAsLink();

  // Collectionlisting interaction
  placeMoveButtonInActiveTab();
  placeCopyButtonInActiveTab();
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

  // Dropdowns
  dropdownLanguageMenu();
  dropdownCollectionGlobalMenu();

  /* GET/POST forms with AJAX (initalization/config) */

  // Global menu service forms
  var globalMenuServices = ["renameService",
                            "publish\\.globalUnpublishService",
                            "publish\\.globalPublishService",
                            "manage\\.createArchiveService"];

  for (var i = 0, len = globalMenuServices.length; i < len; i++) {
    getAjaxForm("#titleContainer a#" + globalMenuServices[i], "globalmenu", "#titleContainer ul.globalMenu", false, "div");
  }

  // Tab menu service forms
  var tabMenuServices = ["fileUploadService",
                         "createDocumentService",
                         "createCollectionService"];

  for (var i = 0, len = tabMenuServices.length; i < len; i++) {
    getAjaxForm("ul.tabMenu2 a#" + tabMenuServices[i], "vrtx-admin-form", ".activeTab ul.tabMenu2", false, "div");
    if(tabMenuServices[i] != "fileUploadService") { // Only half-async for file upload
      postAjaxFormDelegator("form[name=" + tabMenuServices[i] + "] input[type=submit]", "#contents", "errorContainer", "> ul");
    }
  }

  // Permission privilegie forms (READ, READ_WRITE, ALL)
  var privilegiesPermissions = ["read",
                                "read-write",
                                "all"];

  for (i = 0, len = privilegiesPermissions.length; i < len; i++) {
    getAjaxForm("div.permissions-" + privilegiesPermissions[i] + "-wrapper a.full-ajax", "expandedForm-"
               + privilegiesPermissions[i], "div.permissions-" + privilegiesPermissions[i] + "-wrapper", true, "div");
    toggleConfigCustomPermissions("expandedForm-" + privilegiesPermissions[i]);
    interceptEnterKeyAndReroute(".expandedForm-" + privilegiesPermissions[i] + " .addUser input[type=text]",
                                ".expandedForm-" + privilegiesPermissions[i] + " input.addUserButton");
    
    /* TODO: fix autocomplete (possible for multiple fields at once)
    var permissionsAutocompleteParams = {minChars:4, selectFirst:false, width:300, max:30, delay:800};
    permissionsAutocomplete('userNames', 'userNames', permissionsAutocompleteParams);
    splitAutocompleteSuggestion('userNames');
    permissionsAutocomplete('groupNames', 'groupNames', permissionsAutocompleteParams); */
  }
  
  // More permission privilegie forms in table (ADD_COMMENT, READ_PROCESSED)
  var privilegiesPermissionsInTable = ["add-comment",
                                       "read-processed"];

  for (i = 0, len = privilegiesPermissionsInTable.length; i < len; i++) {
    getAjaxForm(".privilegeTable tr." + privilegiesPermissionsInTable[i] + " a.full-ajax", 
                privilegiesPermissionsInTable[i], "tr." + privilegiesPermissionsInTable[i], true, "tr");
                
    toggleConfigCustomPermissions(privilegiesPermissionsInTable[i]);
    interceptEnterKeyAndReroute("." + privilegiesPermissionsInTable[i] + " .addUser input[type=text]",
                                "." + privilegiesPermissionsInTable[i] + " input.addUserButton");
    
    /* TODO: fix autocomplete (possible for multiple fields at once)
    var permissionsAutocompleteParams = {minChars:4, selectFirst:false, width:300, max:30, delay:800};
    permissionsAutocomplete('userNames', 'userNames', permissionsAutocompleteParams);
    splitAutocompleteSuggestion('userNames');
    permissionsAutocomplete('groupNames', 'groupNames', permissionsAutocompleteParams); */
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

  for (i = 0, len = propsAbout.length; i < len; i++) {
    getAjaxForm("body#vrtx-about .prop-" + propsAbout[i] + " a.vrtx-button-small", "expandedForm-prop-" 
               + propsAbout[i], "tr.prop-" + propsAbout[i], true, "tr");
  }

  // Remove permission
  ajaxRemoveDelegator("input.removePermission");

  // Add permission(s)
  ajaxAddDelegator("span.addGroup", ".groups-wrapper", "errorContainer");
  ajaxAddDelegator("span.addUser", ".users-wrapper", "errorContainer");

  /* ^ GET/POST forms with AJAX (initalization/config) */

  // Show/hide multiple properties (initalization/config)
  showHide(new Array("#resource\\.recursive-listing\\.false", "#resource\\.recursive-listing\\.unspecified"),
                     "#resource\\.recursive-listing\\.false:checked", 'false', new Array("#vrtx-resource\\.recursive-listing-subfolders"));

  showHide(new Array("#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"),
                     "#resource\\.display-type\\.calendar:checked", null, new Array("#vrtx-resource\\.event-type-title"));

  showHide(new Array("#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"),
                     "#resource\\.display-type\\.calendar:checked", 'calendar', new Array("#vrtx-resource\\.hide-additional-content"));

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
  btn.after('&nbsp;(&nbsp;<a id=\"logoutAction.link\" name=\"logoutAction\" href="javascript:void(0);">'
          + btn.attr('value') + '</a>&nbsp;)');
  $('#logoutAction\\.link').click(function () {
    btn.click();
    return false;
  });
}

/* ^ Buttons into links */

/* Collectionlisting interaction */

function placeMoveButtonInActiveTab() {
  var btn = $('#collectionListing\\.action\\.move-resources');
  btn.hide();
  var li = $('li.moveResourcesService');
  li.html('<a id="moveResourceService" href="javascript:void(0);">' + btn.attr('title') + '</a>');
  $('#moveResourceService').click(function () {
    if (!$('form[name=collectionListingForm] input[type=checkbox]:checked').length) {
      alert(moveUncheckedMessage);
    } else {
      $('#collectionListing\\.action\\.move-resources').click();
    }
    return false;
  });
}


function placeCopyButtonInActiveTab() {
  var btn = $('#collectionListing\\.action\\.copy-resources');
  btn.hide();
  var li = $('li.copyResourcesService');
  li.html('<a id="copyResourceService" href="javascript:void(0);">' + btn.attr('title') + '</a>');
  $('#copyResourceService').click(function () {
    if (!$('form[name=collectionListingForm] input[type=checkbox]:checked').length) {
      alert(copyUncheckedMessage);
    } else {
      $('#collectionListing\\.action\\.copy-resources').click();
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
      var list = new String("");
      var boxesSize = boxes.size();
      for (i = 0; i < boxesSize && i < 10; i++) {
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

      var list = new String("");
      var boxesSize = boxes.size();
      for (i = 0; i < boxesSize && i < 10; i++) {
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

/* Permission shortcuts/custom toggling */

function toggleConfigCustomPermissions(selectorClass) {
    if ($("." + selectorClass).length) {
      if (!$($("." + selectorClass + " ul.shortcuts input:radio:last")).is(":checked")) {
        $("." + selectorClass).find(".principalList").hide(0);
      }
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

/* ^ Permission shortcuts/custon toggling */

/* Dropdowns */

function dropdownLanguageMenu() {
  var languageMenu = $(".localeSelection ul");
  if (languageMenu.length) {
    var parent = languageMenu.parent();
    parent.addClass("js-on");

    // Remove ':' and replace <span> with <a>
    var header = parent.find(".localeSelectionHeader");
    var headerText = header.text();
    header.replaceWith("<a href='javascript:void(0);' class='localeSelectionHeader'>"
                     + headerText.substring(0, headerText.length - 1) + "</a>");

    languageMenu.addClass("dropdown-shortcut-menu-container");

    $(".localeSelection").delegate(".localeSelectionHeader", "click", function () {
      $(this).next(".dropdown-shortcut-menu-container").slideToggle(vrtxAdmin.transitionDropdownSpeed);
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
    var coll = collectionGlobalMenu;
    $("ul.dropdown-shortcut-menu").parent().append("<div class='dropdown-shortcut-menu-container'><ul>" + coll.html() + "</ul></div>");

    collectionGlobalMenu.find("li").not(".first").remove();
    collectionGlobalMenu.find("li.first").append("<span id='dropdown-shortcut-menu-click-area'></span>");

    var shortcutMenu = $(".resource-title .dropdown-shortcut-menu-container");
    shortcutMenu.find("li.first").remove();
    shortcutMenu.css("left", (collectionGlobalMenu.width() - 24) + "px");

    collectionGlobalMenu.find("li.first #dropdown-shortcut-menu-click-area").click(function () {
      shortcutMenu.slideToggle(vrtxAdmin.transitionDropdownSpeed);
      return false;
    });

    collectionGlobalMenu.find("li.first #dropdown-shortcut-menu-click-area").hover(function () {
      $(this).parent().toggleClass('unhover');
      $(this).prev().toggleClass('hover');
    }, function () {
      $(this).parent().toggleClass('unhover');
      $(this).prev().toggleClass('hover');
    });

  }
}

/* ^ Dropdowns */

/**
 * GET form with AJAX
 *
 * @params pending..
 */

function getAjaxForm(link, selectorClass, insertAfterOrReplaceClass, isReplacing, nodeType) {
  $(link).click(function () {
    var serviceUrl = $(this).attr("href");
    $.ajax({
      type: "GET",
      url: serviceUrl,
      dataType: "html",
      success: function (results, status, resp) {
        var form = $(results).find("." + selectorClass).html();
        if (isReplacing) {
          $(insertAfterOrReplaceClass).replaceWith("<" + nodeType + " class='expandedForm " 
                                                 + selectorClass + "'>" + form + "</" + nodeType + ">");
          $(nodeType + "." + selectorClass).hide();
        } else {
          $("<" + nodeType + " class='expandedForm " + selectorClass + "'>" + form + "</" + nodeType + ">")
            .insertAfter(insertAfterOrReplaceClass).hide();
        }
        $(nodeType + "." + selectorClass).slideDown(vrtxAdmin.transitionSpeed);
        $(nodeType + "." + selectorClass).find("input[type=text]:first").focus();
      },
      error: function (xhr, textStatus) {
        if (xhr.readyState == 4 && xhr.status == 200) {
          var msg = "The service is not active: " + textStatus;
        } else {
          var msg = "The service returned " + xhr.status + " and failed to retrieve form.";
        }
        if ($("#app-content > .errormessage").length) {
          $("#app-content > .errormessage").html(msg);
        } else {
          $("#app-content").prepend("<div class='errormessage message'>" + msg + "</div>");
        }
      }
    });
    return false;
  });
}

/**
 * POST form with AJAX
 *
 * @params pending..
 */

function postAjaxFormDelegator(selector, updateSelector, errorContainer, errorContainerInsertAfter) {
  $("#app-content").delegate(selector, "click", function () {
    var link = $(this);
    var linkAction = link.attr("name");
    var form = link.closest("form");
    var url = form.attr("action");
    var encType = form.attr("enctype");

    var textfields = form.find("input[type=text]");
    var fileFields = form.find("input[type=file]");
    var checkedRadioButtons = form.find("input[type=radio]:checked");
    var csrfPreventionToken = form.find("input[name='csrf-prevention-token']").val();

    var dataString = "";
    for (var i = 0, len = textfields.length; i < len; i++) {
      var name = $(textfields[i]).attr("name");
      var value = $(textfields[i]).val();
      dataString += '&' + name + '=' + value;
    }
    for (var i = 0, len = fileFields.length; i < len; i++) {
      var name = $(fileFields[i]).attr("name");
      var value = $(fileFields[i]).val();
      dataString += '&' + name + '=' + value;
    }
    for (var i = 0, len = checkedRadioButtons.length; i < len; i++) {
      var name = $(checkedRadioButtons[i]).attr("name");
      var value = $(checkedRadioButtons[i]).val();
      dataString += '&' + name + '=' + value;
    }
    dataString += '&csrf-prevention-token=' + csrfPreventionToken + "&" + linkAction;

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
        if ($(results).find("div." + errorContainer).length) {
          if (form.find("div." + errorContainer).length) {
            form.find("div." + errorContainer).html($(results).find("div." + errorContainer).html());
          } else {
            $("<div class='" + errorContainer + "'>" + $(results).find("div." + errorContainer).html() + "</div>")
              .insertAfter(form.find(errorContainerInsertAfter));
          }
        } else {
          $("#app-content").find(updateSelector).html($(results).find(updateSelector).html());
          form.parent().slideUp(vrtxAdmin.transitionSpeed, function () {
            $(this).remove();
          });
        }
      },
      error: function (xhr, textStatus) {
        if (xhr.readyState == 4 && xhr.status == 200) {
          var msg = "The service is not active: " + textStatus;
        } else {
          var msg = "The service returned " + xhr.status + " and failed to retrieve form.";
        }
        if ($("#app-content > .errormessage").length) {
          $("#app-content > .errormessage").html(msg);
        } else {
          $("#app-content").prepend("<div class='errormessage message'>" + msg + "</div>");
        }
      }
    });
    return false;
  });
}

/**
 * Delegate AJAX (POST) remove-links (value is in the name)
 * 
 * @param selector: selector for links that should post asynchronous
 */

function ajaxRemoveDelegator(selector) {
  $("#app-content").delegate(selector, "click", function () {
    var link = $(this);
    var name = link.attr("name");
    var listElement = link.parent();
    var form = link.closest("form");
    var csrfPreventionToken = form.find("input[name='csrf-prevention-token']").val();
    var url = form.attr("action");
    var dataString = name + '&csrf-prevention-token=' + csrfPreventionToken;
    $.ajax({
      type: "POST",
      url: url,
      data: dataString,
      dataType: "html",
      success: function (results, status, resp) {
        listElement.remove();
      },
      error: function (xhr, textStatus) {
        if (xhr.readyState == 4 && xhr.status == 200) {
          var msg = "The service is not active: " + textStatus;
        } else {
          var msg = "The service returned " + xhr.status + " and failed to retrieve form.";
        }
        if ($("#app-content > .errormessage").length) {
          $("#app-content > .errormessage").html(msg);
        } else {
          $("#app-content").prepend("<div class='errormessage message'>" + msg + "</div>");
        }
      }
    });
    return false;
  });
}

/**
 * Delegate AJAX (POST) add-links (values is in the textfield)
 * 
 * @param selector: selector for links that should post asynchronous
 * @param updateSelector: selector for markup to update
 * @param errorContainer: selector for error container
 */

function ajaxAddDelegator(selector, updateSelector, errorContainer) {
  $("#app-content").delegate(selector + " input[type=submit]", "click", function () {
    var link = $(this);
    var linkAction = link.attr("name");
    var textfield = link.parent().parent().find("input[type=text]");
    var textfieldName = textfield.attr("name");
    var textfieldVal = textfield.val();
    var form = link.closest("form");
    var csrfPreventionToken = form.find("input[name='csrf-prevention-token']").val();
    var url = form.attr("action");
    var dataString = textfieldName + '=' + textfieldVal + "&" + linkAction + '&csrf-prevention-token=' + csrfPreventionToken;
    $.ajax({
      type: "POST",
      url: url,
      data: dataString,
      dataType: "html",
      success: function (results, status, resp) {
        if ($(results).find("div." + errorContainer).length) {
          var cont = form.find(updateSelector).parent();
          if (cont.find(" div." + errorContainer).length) {
            cont.find("div." + errorContainer).html($(results).find("div." + errorContainer).html());
          } else {
            $("<div class='" + errorContainer + "'>" + $(results).find("div." + errorContainer).html() + "</div>")
              .insertAfter(cont.find(updateSelector));
          }
        } else {
          form.find(updateSelector).html($(results).find(updateSelector).html());
          textfield.val("");
        }
      },
      error: function (xhr, textStatus) {
        if (xhr.readyState == 4 && xhr.status == 200) {
          var msg = "The service is not active: " + textStatus;
        } else {
          var msg = "The service returned " + xhr.status + " and failed to retrieve form.";
        }
        if ($("#app-content > .errormessage").length) {
          $("#app-content > .errormessage").html(msg);
        } else {
          $("#app-content").prepend("<div class='errormessage message'>" + msg + "</div>");
        }
      }
    });
    return false;
  });
}

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
  var data = $.find("input[id^='vrtx-featured-articles-']");
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

/* ^ Vortex Admin enhancements */