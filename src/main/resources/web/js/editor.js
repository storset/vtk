/*
 * Editor CK setup
 *
 */

var inlineToolbar = [['Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                      'Italic', 'Strike', 'Subscript', 'Superscript',
                      'SpecialChar']];

var withoutSubSuperToolbar = [['Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                               'Italic', 'Strike', 'SpecialChar']];

var completeToolbar = [['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
                        'RemoveFormat', '-', 'Link', 'Unlink', 'Anchor',
                        'Image', 'CreateDiv', 'MediaEmbed', 'Table',
                        'HorizontalRule', 'SpecialChar'
                    ], ['Format', 'Bold', 'Italic', 'Strike',
                        'Subscript', 'Superscript', 'NumberedList',
                        'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                        'JustifyCenter', 'JustifyRight', 'TextColor',
                        'Maximize']];
                        
var studyToolbar = [['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
                        'RemoveFormat', '-', 'Link', 'Unlink', 'Studyreferencecomponent', 'Anchor',
                        'Image', 'CreateDiv', 'MediaEmbed', 'Table', 'Studytable',
                        'HorizontalRule', 'SpecialChar'
                    ], ['Format', 'Bold', 'Italic', 
                        'Subscript', 'Superscript', 'NumberedList',
                        'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                        'JustifyCenter', 'JustifyRight', 
                        'Maximize']];
                        
var courseGroupToolbar = [['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
                        'RemoveFormat', '-', 'Link', 'Unlink', 'Studyreferencecomponent', 'Anchor',
                        'Image', 'CreateDiv', 'MediaEmbed', 'Table',
                        'HorizontalRule', 'SpecialChar'
                    ], ['Format', 'Bold', 'Italic', 
                        'Subscript', 'Superscript', 'NumberedList',
                        'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                        'JustifyCenter', 'JustifyRight', 
                        'Maximize']];
                        
var messageToolbar = [['PasteText', 'Bold', 'Italic', 'Strike', '-', 'Undo', 'Redo', '-', 'Link', 'Unlink',
                       'Subscript', 'Superscript', 'NumberedList', 'BulletedList', 'Outdent', 'Indent']];


var completeToolbarOld = [['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
                           'RemoveFormat', '-', 'Link', 'Unlink', 'Anchor',
                           'Image', 'CreateDiv', 'MediaEmbed', 'Table',
                           'HorizontalRule', 'SpecialChar'
                         ], ['Format', 'Bold', 'Italic', 'Strike',
                             'Subscript', 'Superscript', 'NumberedList',
                             'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                             'JustifyCenter', 'JustifyRight', 'TextColor',
                             'Maximize']];

var commentsToolbar = [['Source', 'PasteText', 'Bold',
                        'Italic', 'Strike', 'NumberedList',
                        'BulletedList', 'Link', 'Unlink']];

// TODO: Try to remove some hardcoded fields - should maybe be class-based
function newEditor(name, completeEditor, withoutSubSuper, baseFolder, baseUrl, baseDocumentUrl, browsePath, defaultLanguage, cssFileList, simpleHTML) {
  
  // If pregenerated parameters is used for init
  if(typeof name === "object") {
    var obj = name;
    name = obj[0];
    completeEditor = obj[1];
    withoutSubSuper = obj[2];
    baseFolder = obj[3];
    baseUrl = obj[4];
    baseDocumentUrl = obj[5];
    browsePath = obj[6];
    defaultLanguage = obj[7];
    cssFileList = obj[8];
    simpleHTML = obj[9];
    obj = null; // Avoid any mem leak
  }

  // File browser
  var linkBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=' + browsePath;
  var imageBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=' + browsePath;
  var flashBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=' + browsePath;

  var isCompleteEditor = completeEditor != null ? completeEditor : false;
  var isWithoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false;
  var isSimpleHTML = (simpleHTML != null && simpleHTML == "true") ? true : false;
  
  var editorElem = $("form#editor");

  // CKEditor configurations
  if (contains(name, "introduction")
   || contains(name, "resource.description")
   || contains(name, "resource.image-description")
   || contains(name, "resource.video-description")
   || contains(name, "resource.audio-description")) {
    setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 100, 400, 40, inlineToolbar,
                      isCompleteEditor, false, baseDocumentUrl, isSimpleHTML);
  } else if (contains(name, "comment") && editorElem.hasClass("vrtx-schedule")) {
    setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 150, 400, 40, inlineToolbar,
                      isCompleteEditor, false, baseDocumentUrl, isSimpleHTML);
  } else if (contains(name, "caption")) {
    setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 78, 400, 40, inlineToolbar, 
                      isCompleteEditor, false, baseDocumentUrl, isSimpleHTML);               
  } else if (contains(name, "frist-frekvens-fri") // Studies  
          || contains(name, "metode-fri")
          || contains(name, "internasjonale-sokere-fri")
          || contains(name, "nordiske-sokere-fri")
          || contains(name, "opptakskrav-fri")
          || contains(name, "generelle-fri")
          || contains(name, "spesielle-fri")
          || contains(name, "politiattest-fri")
          || contains(name, "rangering-sokere-fri")
          || contains(name, "forstevitnemal-kvote-fri")
          || contains(name, "ordinar-kvote-alle-kvalifiserte-fri")
          || contains(name, "innpassing-tidl-utdanning-fri")
          || contains(name, "regelverk-fri")
          || contains(name, "description-en")
          || contains(name, "description-nn")
          || contains(name, "description-no")) {
    isSimpleHTML = false;
    isCompleteEditor = true;
    setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 150, 400, 40, studyToolbar, 
                      isCompleteEditor, false, baseDocumentUrl, isSimpleHTML);
  } else if (contains(name, "message")) {
    setCKEditorConfig(name, null, null, null, defaultLanguage, cssFileList, 250, 400, 40, messageToolbar, 
                      isCompleteEditor, false, null, isSimpleHTML);           
  } else if (contains(name, "additional-content")
          || contains(name, "additionalContents")) { // Additional content
    setCKEditorConfig(name, linkBrowseUrl, imageBrowseUrl, flashBrowseUrl, defaultLanguage, cssFileList, 150, 400, 40, 
                      completeToolbar, true, false, baseDocumentUrl, isSimpleHTML);
  } else if (isCompleteEditor) { // Complete editor 
    var height = 220;
    var maxHeight = 400;
    var completeTB = completeToolbar;   
    if (name.indexOf("supervisor-box") != -1) {
      height = 130;
      maxHeight = 300;
    } else if (name == "content"
            || name == "resource.content"
            || name == "content-study"
            || name == "courses-in-group"
            || name == "relevant-study-programmes") {
      height = 400;
      maxHeight = 800;
      if (name == "resource.content") { // Old editor
        completeTB = completeToolbarOld;
      } 
      if (name == "content-study") { // Study toolbar
        completeTB = studyToolbar;
      } 
      if (name == "courses-in-group"
       || name == "relevant-study-programmes") { // CourseGroup toolbar
        completeTB = courseGroupToolbar;
      }
    }
    setCKEditorConfig(name, linkBrowseUrl, imageBrowseUrl, flashBrowseUrl, defaultLanguage, cssFileList, height, maxHeight, 50, completeTB,
                      isCompleteEditor, true, baseDocumentUrl, isSimpleHTML);
  } else {
    setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 90, 400, 40, withoutSubSuperToolbar, 
                      isCompleteEditor, true, baseDocumentUrl, isSimpleHTML);
  }

}

function contains(string, substring) {
  return string.indexOf(substring) != -1; 
}

function setCKEditorConfig(name, linkBrowseUrl, imageBrowseUrl, flashBrowseUrl, defaultLanguage, cssFileList, height, 
                           maxHeight, minHeight, toolbar, complete, resizable, baseDocumentUrl, simple) {

  var config = [{}];

  config.baseHref = baseDocumentUrl;
  config.contentsCss = cssFileList;
  
  // Don't use HTML-entities for structured-documents
  if (name.indexOf("resource.") != 0) {
    config.entities = false;
  }

  if (linkBrowseUrl != null) {
    config.filebrowserBrowseUrl = linkBrowseUrl;
    config.filebrowserImageBrowseLinkUrl = linkBrowseUrl;
  }

  if (complete) {
    config.filebrowserImageBrowseUrl = imageBrowseUrl;
    config.filebrowserFlashBrowseUrl = flashBrowseUrl;
    config.extraPlugins = 'mediaembed,studyreferencecomponent,htmlbuttons';
    config.stylesSet = divContainerStylesSet;
    // XHTML
    if (name == "resource.content" && simple) {
      config.format_tags = 'p;h1;h2;h3;h4;h5;h6;pre;div';
    } else {
      config.format_tags = 'p;h2;h3;h4;h5;h6;pre;div';
    }
  } else {
    config.removePlugins = 'elementspath';
  }

  if (resizable) {
    config.resize_enabled = true;
  } else {
    config.resize_enabled = false;
  }
  config.toolbarCanCollapse = false;
  config.defaultLanguage = 'no';
  if (defaultLanguage) {
    config.language = defaultLanguage;
  }
  config.toolbar = toolbar;
  config.height = height + 'px';
  config.autoGrow_maxHeight = maxHeight + 'px';
  config.autoGrow_minHeight = minHeight + 'px';

  config.forcePasteAsPlainText = false;

  config.disableObjectResizing = true;

  config.disableNativeSpellChecker = false;

  // Configure tag formatting in source
  config.on = {
    instanceReady: function (ev) {

      var tags = ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'];

      for (var key in tags) {
        this.dataProcessor.writer.setRules(tags[key], {
          indent: false,
          breakBeforeOpen: true,
          breakAfterOpen: false,
          breakBeforeClose: false,
          breakAfterClose: true
        });
      }

      tags = ['ol', 'ul', 'li'];

      for (key in tags) {
        this.dataProcessor.writer.setRules(tags[key], {
          indent: true,
          breakBeforeOpen: true,
          breakAfterOpen: false,
          breakBeforeClose: false,
          breakAfterClose: true
        });
      }
    }
  }

  CKEDITOR.replace(name, config);
}

function commentsCkEditor() {
  document.getElementById("comment-syntax-desc").style.display = "none";
  document.getElementById("comments-text-div").style.margin = "0";
  $("#comments-text").click(function () {
    setCKEditorConfig("comments-text", null, null, null, null, cssFileList, 150, 400, 40, commentsToolbar, false, true, null);
  });
}

var EDITORS_MAX_SYNC_AT_INIT = 15;
var EDITORS_ASYNC_INIT_INTERVAL = 15;
var NEED_TO_CONFIRM = true;
var UNSAVED_CHANGES_CONFIRMATION;
var INITIAL_INPUT_FIELDS = [];
var INITIAL_SELECTS = [];
var INITIAL_CHECKBOXES = [];
var INITIAL_RADIO_BUTTONS = [];

$(document).ready(function() { 
  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;

  var editor = $("#editor");
  if(!editor.length) return;
  
  // When ui-helper-hidden class is added => we need to add 'first'-class to next element (if it is not last and first of these)
  editor.find(".ui-helper-hidden").filter(":not(:last)").filter(":first").next().addClass("first");
  // TODO: make sure these are NOT first so that we can use pure CSS

  autocompleteUsernames(".vrtx-autocomplete-username");
  autocompleteTags(".vrtx-autocomplete-tag");

  // Stickybar
  var titleSubmitButtons = _$("#vrtx-editor-title-submit-buttons");
  var thisWindow = _$(window);
  
  // TODO: also check minimum device height (with high density displays on new devices accounted for)
  if(titleSubmitButtons.length && !vrtxAdm.isIPhone) { // Turn off for iPhone. 
    var titleSubmitButtonsPos = titleSubmitButtons.offset();
    if(vrtxAdm.isIE8) {
      titleSubmitButtons.append("<span id='sticky-bg-ie8-below'></span>");
    }
    thisWindow.on("scroll", function() {
      if(thisWindow.scrollTop() >= titleSubmitButtonsPos.top) {
        if(!titleSubmitButtons.hasClass("vrtx-sticky-editor-title-submit-buttons")) {
          titleSubmitButtons.addClass("vrtx-sticky-editor-title-submit-buttons");
          _$("#contents").css("paddingTop", titleSubmitButtons.outerHeight(true) + "px");
        }
        titleSubmitButtons.css("width", (_$("#main").outerWidth(true) - 2) + "px");
      } else {
        if(titleSubmitButtons.hasClass("vrtx-sticky-editor-title-submit-buttons")) {
          titleSubmitButtons.removeClass("vrtx-sticky-editor-title-submit-buttons");
          titleSubmitButtons.css("width", "auto");
          _$("#contents").css("paddingTop", "0px");
        }
      }
    });
    thisWindow.on("resize", function() {
      if(thisWindow.scrollTop() >= titleSubmitButtonsPos.top) {
        titleSubmitButtons.css("width", (_$("#main").outerWidth(true) - 2) + "px");
      }
    });
  }

  // Add save/help when CK is maximized
  vrtxAdm.cachedAppContent.on("click", ".cke_button_maximize.cke_on", function(e) { 
    var stickyBar = _$("#vrtx-editor-title-submit-buttons");          
    stickyBar.hide();
         
    var ckInject = _$(this).closest(".cke_skin_kama")
                           .find(".cke_toolbar_end:last");
                               
    if(!ckInject.find("#editor-help-menu").length) {  
      var shortcuts = stickyBar.find(".submit-extra-buttons");
      var save = shortcuts.find("#vrtx-save").html();
      var helpMenu = "<div id='editor-help-menu' class='js-on'>" + shortcuts.find("#editor-help-menu").html() + "</div>";
      ckInject.append("<div class='ck-injected-save-help'>" + save + helpMenu + "</div>");
        
      // Fix markup
      var saveInjected = ckInject.find(".ck-injected-save-help > a");
      if(!saveInjected.hasClass("vrtx-button")) {
        saveInjected.addClass("vrtx-button");
        if(!saveInjected.find("> span").length) {
          saveInjected.wrapInner("<span />");
        }
      } else {
        saveInjected.removeClass("vrtx-focus-button");
      }
        
    } else {
      ckInject.find(".ck-injected-save-help").show();
    }
  }); 

  vrtxAdm.cachedAppContent.on("click", ".cke_button_maximize.cke_off", function(e) {    
    var stickyBar = _$("#vrtx-editor-title-submit-buttons");          
    stickyBar.show();
    var ckInject = _$(this).closest(".cke_skin_kama").find(".ck-injected-save-help").hide();
  }); 
  
  /* Toggle fields - TODO: should be more general (and without '.'s) but old editor need an update first */
    
  // Aggregation and manually approved
  if(!_$("#resource\\.display-aggregation\\.true").is(":checked")) {
    _$("#vrtx-resource\\.aggregation").slideUp(0, "linear");
  }
  if(!_$("#resource\\.display-manually-approved\\.true").is(":checked")) {
    _$("#vrtx-resource\\.manually-approve-from").slideUp(0, "linear");
  }

  vrtxAdm.cachedAppContent.on("click", "#resource\\.display-aggregation\\.true", function(e) {
    if(!_$(this).is(":checked")) {                   // If unchecked remove rows and clean prop textfield
      _$(".aggregation .vrtx-multipleinputfield").remove();
      _$("#resource\\.aggregation").val("");
    }
    _$("#vrtx-resource\\.aggregation").slideToggle(vrtxAdm.transitionDropdownSpeed, "swing");
    e.stopPropagation();
  });

  vrtxAdm.cachedAppContent.on("click", "#resource\\.display-manually-approved\\.true", function(e) {
    if(!_$(this).is(":checked")) {                   // If unchecked remove rows and clean prop textfield
      _$(".manually-approve-from .vrtx-multipleinputfield").remove();
      _$("#resource\\.manually-approve-from").val("");
    }
    _$("#vrtx-resource\\.manually-approve-from").slideToggle(vrtxAdm.transitionDropdownSpeed, "swing");
    e.stopPropagation();
  });
  
  // Course status - continued as
  vrtxAdm.cachedAppContent.on("change", "#resource\\.courseContext\\.course-status", function(e) {
    var courseStatus = _$(this);
    if(courseStatus.val() === "continued-as") {
      _$("#vrtx-resource\\.courseContext\\.course-continued-as.hidden").slideDown(vrtxAdm.transitionDropdownSpeed, "swing", function() {
        $(this).removeClass("hidden");
      });
    } else {
      _$("#vrtx-resource\\.courseContext\\.course-continued-as:not(.hidden)").slideUp(vrtxAdm.transitionDropdownSpeed, "swing", function() {
        $(this).addClass("hidden");
      });
    }
    e.stopPropagation();
  });
  _$("#resource\\.courseContext\\.course-status").change();

  // Show/hide multiple properties (initalization / config)
  // TODO: better / easier to understand interface (and remove old "." in CSS-ids / classes)
  showHide(["#resource\\.recursive-listing\\.false", "#resource\\.recursive-listing\\.unspecified"], // radioIds
            "#resource\\.recursive-listing\\.false:checked",                                         // conditionHide
            'false',                                                                                 // conditionHideEqual
            ["#vrtx-resource\\.recursive-listing-subfolders"]);                                      // showHideProps

  showHide(["#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"],
            "#resource\\.display-type\\.calendar:checked",
            null,
            ["#vrtx-resource\\.event-type-title"]);

  showHide(["#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"],
            "#resource\\.display-type\\.calendar:checked",
            'calendar',
            ["#vrtx-resource\\.hide-additional-content"]);
   
  var docType = editor[0].className;
  if(docType && docType !== "") {
    switch(docType) {
      case "vrtx-hvordan-soke":
        hideShowStudy(editor, _$("#typeToDisplay"));
        _$(document).on("change", "#typeToDisplay", function () {
          hideShowStudy(editor, $(this));
          editor.find(".ui-accordion > .vrtx-string.last").removeClass("last");
          editor.find(".ui-accordion > .vrtx-string:visible:last").addClass("last");
        });    
     
        // Because accordion needs one content wrapper
        for(var grouped = editor.find(".vrtx-grouped"), i = grouped.length; i--;) { 
          _$(grouped[i]).find("> *:not(.header)").wrapAll("<div />");
        }
        editor.find(".properties").accordion({ header: "> div > .header",
                                 autoHeight: false,
                                 collapsible: true,
                                 active: false
                               });
        editor.find(".ui-accordion > .vrtx-string:visible:last").addClass("last");
        break;
      case "vrtx-course-description":
        for(var semesters = ["teaching", "exam"], i = semesters.length, semesterId, semesterType; i--;) {
          semesterId = "#" + semesters[i] + "semester";
          semesterType = _$(semesterId);
          if(semesterType.length) {
            hideShowSemester(editor, semesterType);
            _$(document).on("change", semesterId, function () {
              hideShowSemester(editor, $(this));
            });
          }
        }
        setShowHide('course-fee', ["course-fee-amount"], false);
        break;
      case "vrtx-semester-page":
        for(var grouped = editor.find(".vrtx-grouped[class*=link-box]"), i = grouped.length; i--;) { 
          _$(grouped[i]).find("> *:not(.header)").wrapAll("<div />");
        }
        grouped.wrapAll("<div id='link-boxes' />");
        editor.find("#link-boxes").accordion({ header: "> div > .header",
                                               autoHeight: false,
                                               collapsible: true,
                                               active: false
                                             });
        break;
      case "vrtx-samlet-program":
        var samletElm = editor.find(".samlet-element");
        replaceTag(samletElm, "h6", "strong");
        replaceTag(samletElm, "h5", "h6");  
        replaceTag(samletElm, "h4", "h5");
        replaceTag(samletElm, "h3", "h4");
        replaceTag(samletElm, "h2", "h3");
        replaceTag(samletElm, "h1", "h2");
        break;
      default:
        break;
    }
  }
  
  /* Initialize CKEditors */
  
  if(typeof EDITORS_AT_INIT !== "undefined") {
    var len = EDITORS_AT_INIT.length;
    for(var i = 0; i < len && i < EDITORS_MAX_SYNC_AT_INIT; i++) { // Initiate <=25 CKEditors
      newEditor(EDITORS_AT_INIT[i]);
    }
    if(len > EDITORS_MAX_SYNC_AT_INIT) {
      var ckEditorInitLoadTimer = setTimeout(function() { // Initiate >25 CKEditors
        newEditor(EDITORS_AT_INIT[i]);
        i++;
        if(i < len) {
          setTimeout(arguments.callee, EDITORS_ASYNC_INIT_INTERVAL);
        }
      }, EDITORS_ASYNC_INIT_INTERVAL);
    }
  }
});

/*
 * Check if inputfields or textareas (CK) have changes
 *
 */

$(window).load(function () { // Store initial counts and values when all is initialized in editor
  var nullDeferred = $.Deferred();
      nullDeferred.resolve();
  $.when(((typeof MANUALLY_APPROVE_INITIALIZED === "object") ? MANUALLY_APPROVE_INITIALIZED : nullDeferred),
         ((typeof MULTIPLE_INPUT_FIELD_INITIALIZED === "object") ? MULTIPLE_INPUT_FIELD_INITIALIZED : nullDeferred),
         ((typeof JSON_ELEMENTS_INITIALIZED === "object") ? JSON_ELEMENTS_INITIALIZED : nullDeferred),
         ((typeof DATE_PICKER_INITIALIZED === "object") ? DATE_PICKER_INITIALIZED : nullDeferred),
         ((typeof IMAGE_EDITOR_INITIALIZED === "object") ? IMAGE_EDITOR_INITIALIZED : nullDeferred)).done(function() {
    vrtxAdmin.log({msg: "Editor initialized."});
    storeInitPropValues();
  });
  
  if (typeof CKEDITOR !== "undefined") {
    CKEDITOR.on('instanceReady', function() {
      $(".cke_contents iframe").contents().find("body").bind('keydown', 'ctrl+s', function(e) {
        ctrlSEventHandler($, e);
      });
    });
  }
});

function storeInitPropValues() {
  var contents = $("#contents");

  var inputFields = contents.find("input").not("[type=submit]").not("[type=button]")
                                          .not("[type=checkbox]").not("[type=radio]");
  var selects = contents.find("select");
  var checkboxes = contents.find("input[type=checkbox]:checked");
  var radioButtons = contents.find("input[type=radio]:checked");
  
  for(var i = 0, len = inputFields.length; i < len; i++)  INITIAL_INPUT_FIELDS[i] = inputFields[i].value;
  for(    i = 0, len = selects.length; i < len; i++)      INITIAL_SELECTS[i] = selects[i].value;
  for(    i = 0, len = checkboxes.length; i < len; i++)   INITIAL_CHECKBOXES[i] = checkboxes[i].name;
  for(    i = 0, len = radioButtons.length; i < len; i++) INITIAL_RADIO_BUTTONS[i] = radioButtons[i].name + " " + radioButtons[i].value;
}

function unsavedChangesInEditor() {
  if (!NEED_TO_CONFIRM) return false;
  var contents = $("#contents");

  var currentStateOfInputFields = contents.find("input").not("[type=submit]").not("[type=button]")
                                                        .not("[type=checkbox]").not("[type=radio]"),
      textLen = currentStateOfInputFields.length,
      currentStateOfSelects = contents.find("select"),
      selectsLen = currentStateOfSelects.length,
      currentStateOfCheckboxes = contents.find("input[type=checkbox]:checked"),
      checkboxLen = currentStateOfCheckboxes.length,
      currentStateOfRadioButtons = contents.find("input[type=radio]:checked"),
      radioLen = currentStateOfRadioButtons.length;
  
  // Check if count has changed
  if(selectsLen != INITIAL_SELECTS.length
  || checkboxLen != INITIAL_CHECKBOXES.length
  || radioLen != INITIAL_RADIO_BUTTONS.length
  || textLen != INITIAL_INPUT_FIELDS.length) return true;

  // Check if values have changed
  for (var i = 0; i < textLen; i++) if(currentStateOfInputFields[i].value !== INITIAL_INPUT_FIELDS[i]) return true;
  for (    i = 0; i < selectsLen; i++) if(currentStateOfSelects[i].value !== INITIAL_SELECTS[i]) return true;
  for (    i = 0; i < checkboxLen; i++) if(currentStateOfCheckboxes[i].name !== INITIAL_CHECKBOXES[i]) return true;
  for (    i = 0; i < radioLen; i++) if(currentStateOfRadioButtons[i].name + " " + currentStateOfRadioButtons[i].value !== INITIAL_RADIO_BUTTONS[i]) return true;
  
  var currentStateOfTextFields = contents.find("textarea"); // CK->checkDirty()
  if (typeof CKEDITOR !== "undefined") {
    for (i = 0, len = currentStateOfTextFields.length; i < len; i++) {
      var ckInstance = getCkInstance(currentStateOfTextFields[i].name);
      if (ckInstance && ckInstance.checkDirty() && ckInstance.getData() !== "") {
        return true;
      }
    }
  }

  return false;
}

function unsavedChangesInEditorMessage() {
  if (unsavedChangesInEditor()) {
    return UNSAVED_CHANGES_CONFIRMATION;
  }
}

function validTextLengthsInEditor(isOldEditor) {
  var MAX_LENGTH = 1500, // Back-end limits it to 2048
      // NEW starts on wrapper and OLD starts on field (because of slightly different semantic/markup build-up)
      INPUT_NEW = ".vrtx-string:not(.vrtx-multiple), .vrtx-resource-ref, .vrtx-image-ref, .vrtx-media-ref",
      INPUT_OLD = "input[type=text]:not(.vrtx-multiple)", // RT# 1045040 (skip aggregate and manually approve hidden input-fields)
      CK_NEW = ".vrtx-simple-html, .vrtx-simple-html-small", // aka. textareas
      CK_OLD = "textarea:not(#resource\\.content)";

  var contents = $("#contents");
  
  var validTextLengthsInEditorErrorFunc = validTextLengthsInEditorError; // Perf.
  
  // String textfields
  var currentInputFields = isOldEditor ? contents.find(INPUT_OLD) : contents.find(INPUT_NEW);
  for (var i = 0, textLen = currentInputFields.length; i < textLen; i++) {
    var strElm = $(currentInputFields[i]);
    if(isOldEditor) {
      var str = (typeof strElm.val() !== "undefined") ? str = strElm.val() : "";
    } else {
      var strInput = strElm.find("input");
      var str = (strInput.length && typeof strInput.val() !== "undefined") ? str = strInput.val() : "";
    }
    if(str.length > MAX_LENGTH) {
      validTextLengthsInEditorErrorFunc(strElm, isOldEditor);
      return false;  
    }
  }
  
  // Textareas that are not content-fields (CK)
  var currentTextAreas = isOldEditor ? contents.find(CK_OLD) : contents.find(CK_NEW);
  for (i = 0, len = currentTextAreas.length; i < len; i++) {
    if (typeof CKEDITOR !== "undefined") {
      var txtAreaElm = $(currentTextAreas[i]);
      var txtArea = isOldEditor ? txtAreaElm : txtAreaElm.find("textarea");
      if(txtArea.length && typeof txtArea[0].name !== "undefined") {
        var ckInstance = getCkInstance(txtArea[0].name);
        if (ckInstance && ckInstance.getData().length > MAX_LENGTH) { // && guard
          validTextLengthsInEditorErrorFunc(txtAreaElm, isOldEditor);
          return false;
        }
      }
    }
  }
  
  return true;
}

function validTextLengthsInEditorError(elm, isOldEditor) {
  if(typeof tooLongFieldPre !== "undefined" && typeof tooLongFieldPost !== "undefined") {
    $("html").scrollTop(0);
    var lbl = "";
    if(isOldEditor) {
      var elmPropWrapper = elm.closest(".property-item");
      if(elmPropWrapper.length) {
        var lbl = elmPropWrapper.find(".property-label:first");
      }
    } else {
      var lbl = elm.find("label");
    }
    if(lbl.length) {
      vrtxSimpleDialogs.openMsgDialog(tooLongFieldPre + lbl.text() + tooLongFieldPost, "");
    }
  }
}

/* Boolean show/hide */

function setShowHide(name, parameters, hideTrues) {
  toggle(name, parameters, hideTrues);
  $("#editor").on("click", '[name=' + name + ']', function () {
    toggle(name, parameters, hideTrues);
  });
}

function toggle(name, parameters, hideTrues) {
  var hide = hideTrues ? '-true' : '-false';
  var show = hideTrues ? '-false' : '-true';

  var trues = $('#' + name + hide);
  for(var i = 0, truesLen = trues.length; i < truesLen; i++) {
    if (trues[i].checked) {
      for (var k = 0, parametersLen = parameters.length; k < parametersLen; k++) {
        $('div.' + parameters[k]).hide("fast");
      }
    }
  }
  var falses = $('#' + name + show);
  for(i = 0, falsesLen = falses.length; i < falsesLen; i++) {
    if (falses[i].checked) {
      for (k = 0, parametersLength = parameters.length; k < parametersLength; k++) {
        $('div.' + parameters[k]).show("fast");
      }
    }
  }
}

/* Dropdown show/hide mappings
 * TODO: need to do some refactoring of show/hide for dropdowns
 */

function hideShowStudy(container, typeToDisplayElem) {
  switch (typeToDisplayElem.val()) {
    case "so":
      container.removeClass("nm").removeClass("em").addClass("so");
      break;
    case "nm":
      container.removeClass("so").removeClass("em").addClass("nm");
      break;
    case "em":
      container.removeClass("so").removeClass("nm").addClass("em");
      break;
    default:
      container.removeClass("so").removeClass("nm").removeClass("em");
      break;
  }
}

function hideShowSemester(container, typeSemesterElem) {
  var prefix = typeSemesterElem.attr("id") + "-selected";
  switch (typeSemesterElem.val()) {
    case "particular-semester":
      container.removeClass(prefix + "-every-other").removeClass(prefix + "-other").addClass(prefix + "-particular");
      break;
    case "every-other":
      container.removeClass(prefix + "-particular").removeClass(prefix + "-other").addClass(prefix + "-every-other");
      break;
    case "other":
      container.removeClass(prefix + "-every-other").removeClass(prefix + "-particular").addClass(prefix + "-other");
      break;  
    default:
      container.removeClass(prefix + "-particular").removeClass(prefix + "-every-other").removeClass(prefix + "-other");
      break;
  }
}

/* Helper functions */

function getCkValue(instanceName) {
  var oEditor = getCkInstance(instanceName);
  return oEditor.getData();
}

function getCkInstance(instanceName) {
  for (var i in CKEDITOR.instances) {
    if (CKEDITOR.instances[i].name == instanceName) {
      return CKEDITOR.instances[i];
    }
  }
  return null;
}

function setCkValue(instanceName, data) {
  var oEditor = getCkInstance(instanceName);
  oEditor.setData(data);
}

function isCkEditor(instanceName) {
  var oEditor = getCkInstance(instanceName);
  return oEditor != null;
}

function replaceTag(selector, tag, replaceTag) {
  selector.find(tag).replaceWith(function() {
    return "<" + replaceTag + ">" + $(this).text() + "</" + replaceTag + ">";
  });
}

/* ^ Helper functions */

/* ^ Check if inputfields or textareas (CK) have changes */

var divContainerStylesSet = [{
  name: 'Facts left',
  element: 'div',
  attributes: { 'class': 'vrtx-facts-container vrtx-container-left'
  }
},
  {
  name: 'Facts right',
  element: 'div',
  attributes: { 'class': 'vrtx-facts-container vrtx-container-right'
  }
},
  {
  name: 'Image left',
  element: 'div',
  attributes: { 'class': 'vrtx-img-container vrtx-container-left'
  }
},
  {
  name: 'Image center',
  element: 'div',
  attributes: { 'class': 'vrtx-img-container vrtx-container-middle vrtx-img-container-middle-ie'
  }
},
  {
  name: 'Image right',
  element: 'div',
  attributes: { 'class': 'vrtx-img-container vrtx-container-right'
  }
},
  {
  name: 'Img & capt left (800px)',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xxl vrtx-container-left'
  }
},
  {
  name: 'Img & capt left (700px)',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xl vrtx-container-left'
  }
},
  {
  name: 'Img & capt left (600px)',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-l vrtx-container-left'
  }
},
  {
  name: 'Img & capt left (500px)',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-m vrtx-container-left'
  }
},
  {
  name: 'Img & capt left (400px)',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-s vrtx-container-left'
  }
},
  {
  name: 'Img & capt left (300px)',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xs vrtx-container-left'
  }
},
  {
  name: 'Img & capt left (200px)',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xxs vrtx-container-left'
  }
},
  {
  name: 'Img & capt center (full)',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-full vrtx-container-middle'
  }
},
  {
  name: 'Img & capt center (800px)',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xxl vrtx-container-middle'
  }
},
  {
  name: 'Img & capt center (700px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xl vrtx-container-middle'
  }
},
  {
  name: 'Img & capt center (600px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-l vrtx-container-middle'
  }
},
  {
  name: 'Img & capt center (500px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-m vrtx-container-middle'
  }
},
  {
  name: 'Img & capt center (400px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-s vrtx-container-middle'
  }
},
  {
  name: 'Img & capt right (800px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xxl vrtx-container-right'
  }
},
  {
  name: 'Img & capt right (700px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xl vrtx-container-right'
  }
},
  {
  name: 'Img & capt right (600px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-l vrtx-container-right'
  }
},
  {
  name: 'Img & capt right (500px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-m vrtx-container-right'
  }
},
  {
  name: 'Img & capt right (400px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-s vrtx-container-right'
  }
},
  {
  name: 'Img & capt right (300px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xs vrtx-container-right'
  }
},
  {
  name: 'Img & capt right (200px) ',
  element: 'div',
  attributes: { 'class': 'vrtx-container vrtx-container-size-xxs vrtx-container-right'
  }
}];

/* ^ Editor CK setup */
