/*
 *  Vortex Editor
 *
 *  TODO: encapsulate in VrtxEditor
 *  TODO: JSDoc
 *  TODO: use RegExp where we need to match multiple text strings
 *  
 *  ToC: 
 *
 *  1.  Config
 *  2.  DOM is fully loaded
 *  3.  DOM is ready
 *  4.  CKEditor creation
 *  5.  Validation and change detection
 *  6.  Image preview
 *  7.  Show / hide
 *  8.  Multiple fields and boxes
 *  9.  Accordion grouping
 *  10. Utils
 */
 
/*-------------------------------------------------------------------*\
    1. Config
\*-------------------------------------------------------------------*/
 
/**
 * Creates an instance of VrtxEditor
 * @constructor
 */
var VrtxEditor;
(function() {
  var instance; /* Singleton */
  VrtxEditor = function VrtxEditor() {
    if(instance) {
      return instance;
    }
    instance = this;
    this.editorForm = null;
  
    /** CKEditor toolbars */
    this.CKEditorToolbars = {};
    /** CKEditor div-container styles */
    this.CKEditorDivContainerStylesSet = [{}];
  
    /** CKEditors at init that should be created */
    this.CKEditorsInit = [];
    /** CKEditors created sync */
    this.CKEditorsInitSyncMax = 15;
    /** CKEditors async creation interval in ms */
    this.CKEditorsInitAsyncInterval = 15;
  
    /** Text input fields at init */
    this.editorInitInputFields = [];
    /** Select fields at init */
    this.editorInitSelects = [];
    /** Checkboxes at init */
    this.editorInitCheckboxes = [];
    /** Radios at init */
    this.editorInitRadios = [];
  
    /** Initial state for the need to confirm navigation away from editor */
    this.needToConfirm = true;
  
    /** Select fields show/hide mappings */
    this.selectMappings = { "teachingsemester":                    { "particular-semester":   ".if-teachingsemester-particular",
                                                                     "every-other":           ".teachingsemester-every-other-semester",
                                                                     "other":                 ".teachingsemester-other"                 },
                            "examsemester":                        { "particular-semester":   ".if-examsemester-particular",
                                                                     "every-other":           ".examsemester-every-other-semester",
                                                                     "other":                 ".examsemester-other"                     },
                            "teaching-language":                   { "other":                 ".teaching-language-text-field"           }
                          };
    
    /* TODO: Need some rewrite of data structure to use 1 instead of 3 variables */
    this.multipleCommaSeperatedInputFieldNames = [];
    this.multipleCommaSeperatedInputFieldCounter = [];
    this.multipleCommaSeperatedInputFieldLength = [];
    this.multipleCommaSeperatedInputFieldTemplates = [];
    this.multipleCommaSeperatedInputFieldDeferred;
  
    /** Check if this script is in admin or not */                      
    this.isInAdmin = typeof vrtxAdmin !== "undefined";
  }
}());

var vrtxEditor = new VrtxEditor();

var UNSAVED_CHANGES_CONFIRMATION;

/* CKEditor toolbars */

vrtxEditor.CKEditorToolbars.inlineToolbar = [['Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                                         'Italic', 'Strike', 'Subscript', 'Superscript',
                                         'SpecialChar']];

vrtxEditor.CKEditorToolbars.withoutSubSuperToolbar = [['Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                                                  'Italic', 'Strike', 'SpecialChar']];

vrtxEditor.CKEditorToolbars.completeToolbar = [['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
                                           'RemoveFormat', '-', 'Link', 'Unlink', 'Anchor',
                                           'Image', 'CreateDiv', 'MediaEmbed', 'Table',
                                           'HorizontalRule', 'SpecialChar'
                                          ], ['Format', 'Bold', 'Italic', 'Strike',
                                            'Subscript', 'Superscript', 'NumberedList',
                                            'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                                            'JustifyCenter', 'JustifyRight', 'TextColor',
                                            'Maximize']];
                        
vrtxEditor.CKEditorToolbars.studyToolbar = [['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
                                        'RemoveFormat', '-', 'Link', 'Unlink', 'Studyreferencecomponent', 'Anchor',
                                        'Image', 'CreateDiv', 'MediaEmbed', 'Table', 'Studytable',
                                        'HorizontalRule', 'SpecialChar'
                                       ], ['Format', 'Bold', 'Italic', 
                                        'Subscript', 'Superscript', 'NumberedList',
                                        'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                                        'JustifyCenter', 'JustifyRight', 
                                        'Maximize']];
                        
vrtxEditor.CKEditorToolbars.courseGroupToolbar = [['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
                                              'RemoveFormat', '-', 'Link', 'Unlink', 'Studyreferencecomponent', 'Anchor',
                                              'Image', 'CreateDiv', 'MediaEmbed', 'Table',
                                              'HorizontalRule', 'SpecialChar'
                                             ], ['Format', 'Bold', 'Italic', 
                                              'Subscript', 'Superscript', 'NumberedList',
                                              'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                                              'JustifyCenter', 'JustifyRight', 
                                              'Maximize']];
                        
vrtxEditor.CKEditorToolbars.messageToolbar = [['Source', 'PasteText', 'Bold', 'Italic', 'Strike', '-', 'Undo', 'Redo', '-', 'Link', 'Unlink',
                                          'Subscript', 'Superscript', 'NumberedList', 'BulletedList', 'Outdent', 'Indent']];


vrtxEditor.CKEditorToolbars.completeToolbarOld = [['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
                                              'RemoveFormat', '-', 'Link', 'Unlink', 'Anchor',
                                              'Image', 'CreateDiv', 'MediaEmbed', 'Table',
                                              'HorizontalRule', 'SpecialChar'
                                             ], ['Format', 'Bold', 'Italic', 'Strike',
                                              'Subscript', 'Superscript', 'NumberedList',
                                              'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                                              'JustifyCenter', 'JustifyRight', 'TextColor',
                                              'Maximize']];

vrtxEditor.CKEditorToolbars.commentsToolbar = [['Source', 'PasteText', 'Bold',
                                           'Italic', 'Strike', 'NumberedList',
                                           'BulletedList', 'Link', 'Unlink']];
                                           
/* CKEditor Div containers */

vrtxEditor.CKEditorDivContainerStylesSet = [{
    name: 'Facts left',
    element: 'div',
    attributes: { 'class': 'vrtx-facts-container vrtx-container-left' }
  }, {
    name: 'Facts right',
    element: 'div',
    attributes: { 'class': 'vrtx-facts-container vrtx-container-right' }
  }, {
    name: 'Image left',
    element: 'div',
    attributes: { 'class': 'vrtx-img-container vrtx-container-left' }
  }, {
    name: 'Image center',
    element: 'div',
    attributes: { 'class': 'vrtx-img-container vrtx-container-middle vrtx-img-container-middle-ie' }
  }, {
    name: 'Image right',
    element: 'div',
    attributes: { 'class': 'vrtx-img-container vrtx-container-right' }
  }, {
    name: 'Img & capt left (800px)',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xxl vrtx-container-left' }
  }, {
    name: 'Img & capt left (700px)',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xl vrtx-container-left' }
  }, {
    name: 'Img & capt left (600px)',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-l vrtx-container-left' }
  }, {
    name: 'Img & capt left (500px)',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-m vrtx-container-left' }
  }, {
    name: 'Img & capt left (400px)',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-s vrtx-container-left' }
  }, {
    name: 'Img & capt left (300px)',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xs vrtx-container-left' }
  }, {
    name: 'Img & capt left (200px)',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xxs vrtx-container-left' }
  }, {
    name: 'Img & capt center (full)',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-full vrtx-container-middle' }
  }, {
    name: 'Img & capt center (800px)',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xxl vrtx-container-middle' }
  }, {
    name: 'Img & capt center (700px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xl vrtx-container-middle' }
  }, {
    name: 'Img & capt center (600px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-l vrtx-container-middle' }
  }, {
    name: 'Img & capt center (500px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-m vrtx-container-middle' }
  }, {
    name: 'Img & capt center (400px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-s vrtx-container-middle' }
  }, {
    name: 'Img & capt right (800px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xxl vrtx-container-right' }
  }, {
    name: 'Img & capt right (700px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xl vrtx-container-right' }
  }, {
    name: 'Img & capt right (600px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-l vrtx-container-right' }
  }, {
    name: 'Img & capt right (500px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-m vrtx-container-right' }
  }, {
    name: 'Img & capt right (400px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-s vrtx-container-right' }
  }, {
    name: 'Img & capt right (300px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xs vrtx-container-right' }
  }, {
    name: 'Img & capt right (200px) ',
    element: 'div',
    attributes: { 'class': 'vrtx-container vrtx-container-size-xxs vrtx-container-right'
  }
}];

/*-------------------------------------------------------------------*\
    2. DOM is fully loaded
\*-------------------------------------------------------------------*/

$(window).load(function () { 
  /* XXX: Exit if not is in admin */
  if(!vrtxEditor.isInAdmin) return;
  
  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;

  // Store initial counts and values when all is initialized in editor
  var nullDeferred = _$.Deferred();
      nullDeferred.resolve();
  _$.when(((typeof MANUALLY_APPROVE_INITIALIZED === "object") ? MANUALLY_APPROVE_INITIALIZED : nullDeferred),
          ((typeof MULTIPLE_INPUT_FIELD_INITIALIZED === "object") ? MULTIPLE_INPUT_FIELD_INITIALIZED : nullDeferred),
          ((typeof JSON_ELEMENTS_INITIALIZED === "object") ? JSON_ELEMENTS_INITIALIZED : nullDeferred),
          ((typeof DATE_PICKER_INITIALIZED === "object") ? DATE_PICKER_INITIALIZED : nullDeferred),
          ((typeof IMAGE_EDITOR_INITIALIZED === "object") ? IMAGE_EDITOR_INITIALIZED : nullDeferred)).done(function() {
    vrtxAdm.log({msg: "Editor initialized."});
    storeInitPropValues($("#contents"));
  });
  
  // CTRL+S save inside CKEditor
  if (typeof CKEDITOR !== "undefined" && vrtxEditor.editorForm.length) { // XXX: Don't add event if not regular editor
    CKEDITOR.on('instanceReady', function() {
      _$(".cke_contents iframe").contents().find("body").bind('keydown', 'ctrl+s', $.debounce(150, true, function(e) {
        ctrlSEventHandler(_$, e);
      }));
    });
  }
});

/*-------------------------------------------------------------------*\
    3. DOM is ready
\*-------------------------------------------------------------------*/

$(document).ready(function() {
  var vrtxEdit = vrtxEditor;
  vrtxEdit.editorForm = $("#editor");
  
  if(!vrtxEdit.isInAdmin || !vrtxEdit.editorForm.length) {
    vrtxEdit.initCKEditors();
    return; /* XXX: Exit if not is in admin or have regular editor */
  }

  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;
  
  // When ui-helper-hidden class is added => we need to add 'first'-class to next element (if it is not last and first of these)
  vrtxEdit.editorForm.find(".ui-helper-hidden").filter(":not(:last)").filter(":first").next().addClass("first");
  // XXX: make sure these are NOT first so that we can use pure CSS

  autocompleteUsernames(".vrtx-autocomplete-username");
  autocompleteTags(".vrtx-autocomplete-tag");
  
  /* Hide image previews on init (unobtrusive) */
  var previewInputFields = $("input.preview-image-inputfield");
  for(var i = previewInputFields.length; i--;) {
    if(previewInputFields[i].value === "") {
      hideImagePreviewCaption($(previewInputFields[i]), true);
    }
  } 

  /* Inputfield events for image preview */
  _$(document).on("blur", "input.preview-image-inputfield", function(e) {
    previewImage(this.id)
  });
  _$(document).on("keydown", "input.preview-image-inputfield", $.debounce(50, true, function(e) { // ENTER-key
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      previewImage(this.id);
      e.preventDefault();
    }
  }));
  
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

  // Stickybar
  var titleSubmitButtons = _$("#vrtx-editor-title-submit-buttons");
  var thisWindow = _$(window);
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
      } else {
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
    if(!_$(this).is(":checked")) { // If unchecked remove rows and clean prop textfield
      _$(".aggregation .vrtx-multipleinputfield").remove();
      _$("#resource\\.aggregation").val("");
    }
    _$("#vrtx-resource\\.aggregation").slideToggle(vrtxAdm.transitionDropdownSpeed, "swing");
    e.stopPropagation();
  });

  vrtxAdm.cachedAppContent.on("click", "#resource\\.display-manually-approved\\.true", function(e) {
    if(!_$(this).is(":checked")) { // If unchecked remove rows and clean prop textfield
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
            
  // Hide/show mappings for select
  for(var select in vrtxEdit.selectMappings) {
    var selectElm = _$("#" + select);
    if(selectElm.length) {
      vrtxEdit.hideShowSelect(selectElm);
      _$(document).on("change", "#" + select, function () {
        vrtxEdit.hideShowSelect(_$(this));
      });
    }
  }
  
  var docType = vrtxEdit.editorForm[0].className;

  if(docType && docType !== "") {
    switch(docType) {
      case "vrtx-hvordan-soke":
        hideShowStudy(vrtxEdit.editorForm, _$("#typeToDisplay"));
        _$(document).on("change", "#typeToDisplay", function () {
          hideShowStudy(vrtxEdit.editorForm, _$(this));
        });    
        vrtxEdit.accordionGroupedInit();
        break;
      case "vrtx-course-description":
        setShowHide('course-fee', ["course-fee-amount"], false);
        vrtxEdit.accordionGroupedInit();
        break;
      case "vrtx-semester-page":
        setShowHide('cloned-course', ["cloned-course-code"], false);
        vrtxEdit.accordionGroupedInit("[class*=link-box]");
        break;
      case "vrtx-samlet-program":
        var samletElm = vrtxEdit.editorForm.find(".samlet-element");
        vrtxEdit.replaceTag(samletElm, "h6", "strong");
        vrtxEdit.replaceTag(samletElm, "h5", "h6");  
        vrtxEdit.replaceTag(samletElm, "h4", "h5");
        vrtxEdit.replaceTag(samletElm, "h3", "h4");
        vrtxEdit.replaceTag(samletElm, "h2", "h3");
        vrtxEdit.replaceTag(samletElm, "h1", "h2");
        break;
      default:
        break;
    }
  }
  
  vrtxEdit.initCKEditors();
});

/*-------------------------------------------------------------------*\
    4. CKEditor creation
\*-------------------------------------------------------------------*/

/**
 * Initialize CKEditors sync and async from CKEditorsInit array
 * @this {VrtxEditor}
 */
VrtxEditor.prototype.initCKEditors = function initCKEditors() {
  var vrtxEdit = this;

  /* Initialize CKEditors */
  for(var i = 0, len = vrtxEdit.CKEditorsInit.length; i < len && i < vrtxEdit.CKEditorsInitSyncMax; i++) { // Initiate <=CKEditorsInitSyncMax CKEditors sync
    vrtxEdit.newEditor(vrtxEdit.CKEditorsInit[i]);
  }
  if(len > vrtxEdit.CKEditorsInitSyncMax) {
    var ckEditorInitLoadTimer = setTimeout(function() { // Initiate >CKEditorsInitSyncMax CKEditors async
      vrtxEdit.newEditor(vrtxEdit.CKEditorsInit[i]);
      i++;
      if(i < len) {
        setTimeout(arguments.callee, vrtxEdit.CKEditorsInitAsyncInterval);
      }
    }, vrtxEdit.CKEditorsInitAsyncInterval);
  }
};

/**
 * Create new CKEditor instance
 *
 * @this {VrtxEditor}
 * @param {string} name Name of textarea (or object with the other parameters)
 * @param {boolean} completeEditor Use complete toolbar
 * @param {boolean} withoutSubSuper Don't display sub and sup buttons in toolbar
 * @param {string} baseFolder Current folder URL for browse integration
 * @param {string} baseUrl Base URL for browse integration
 * @param {string} baseDocumentUrl URL to current document
 * @param {string} browsePath Browse integration URL
 * @param {string} defaultLanguage Language in editor
 * @param {array} cssFileList List of CSS-files to style content in editor
 * @param {string} simpleHTML Make h1 format available (for old document types)
 */
VrtxEditor.prototype.newEditor = function newEditor(name, completeEditor, withoutSubSuper, baseFolder, baseUrl, baseDocumentUrl, browsePath, defaultLanguage, cssFileList, simpleHTML) {
  var vrtxEdit = this;
  
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
  
  var editorElem = this.editorForm;

  // CKEditor configurations
  if (vrtxEdit.contains(name, "introduction")
   || vrtxEdit.contains(name, "resource.description")
   || vrtxEdit.contains(name, "resource.image-description")
   || vrtxEdit.contains(name, "resource.video-description")
   || vrtxEdit.contains(name, "resource.audio-description")) {
    vrtxEdit.setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 100, 400, 40, vrtxEdit.CKEditorToolbars.inlineToolbar,
                               isCompleteEditor, false, baseDocumentUrl, isSimpleHTML);
  } else if (vrtxEdit.contains(name, "comment") && editorElem.hasClass("vrtx-schedule")) {
    vrtxEdit.setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 150, 400, 40, vrtxEdit.CKEditorToolbars.inlineToolbar,
                               isCompleteEditor, false, baseDocumentUrl, isSimpleHTML);
  } else if (vrtxEdit.contains(name, "caption")) {
    vrtxEdit.setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 78, 400, 40, vrtxEdit.CKEditorToolbars.inlineToolbar, 
                               isCompleteEditor, false, baseDocumentUrl, isSimpleHTML);               
  } else if (vrtxEdit.contains(name, "frist-frekvens-fri") // Studies  
          || vrtxEdit.contains(name, "metode-fri")
          || vrtxEdit.contains(name, "internasjonale-sokere-fri")
          || vrtxEdit.contains(name, "nordiske-sokere-fri")
          || vrtxEdit.contains(name, "opptakskrav-fri")
          || vrtxEdit.contains(name, "generelle-fri")
          || vrtxEdit.contains(name, "spesielle-fri")
          || vrtxEdit.contains(name, "politiattest-fri")
          || vrtxEdit.contains(name, "rangering-sokere-fri")
          || vrtxEdit.contains(name, "forstevitnemal-kvote-fri")
          || vrtxEdit.contains(name, "ordinar-kvote-alle-kvalifiserte-fri")
          || vrtxEdit.contains(name, "innpassing-tidl-utdanning-fri")
          || vrtxEdit.contains(name, "regelverk-fri")
          || vrtxEdit.contains(name, "description-en")
          || vrtxEdit.contains(name, "description-nn")
          || vrtxEdit.contains(name, "description-no")) {
    isSimpleHTML = false;
    isCompleteEditor = true;
    vrtxEdit.setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 150, 400, 40, vrtxEdit.CKEditorToolbars.studyToolbar, 
                               isCompleteEditor, false, baseDocumentUrl, isSimpleHTML);
  } else if (vrtxEdit.contains(name, "message")) {
    vrtxEdit.setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 250, 400, 40, vrtxEdit.CKEditorToolbars.messageToolbar, 
                               isCompleteEditor, false, null, isSimpleHTML);           
  } else if (vrtxEdit.contains(name, "additional-content")
          || vrtxEdit.contains(name, "additionalContents")) { // Additional content
    vrtxEdit.setCKEditorConfig(name, linkBrowseUrl, imageBrowseUrl, flashBrowseUrl, defaultLanguage, cssFileList, 150, 400, 40, 
                               vrtxEdit.CKEditorToolbars.completeToolbar, true, false, baseDocumentUrl, isSimpleHTML);
  } else if (isCompleteEditor) { // Complete editor 
    var height = 220;
    var maxHeight = 400;
    var completeTB = vrtxEdit.CKEditorToolbars.completeToolbar;   
    if (vrtxEdit.contains("supervisor-box")) {
      height = 130;
      maxHeight = 300;
    } else if (name == "content"
            || name == "resource.content"
            || name == "content-study"
            || name == "course-group-about"
            || name == "courses-in-group"
            || name == "course-group-admission"
            || name == "relevant-study-programmes"
            || name == "course-group-other") {
      height = 400;
      maxHeight = 800;
      if (name == "resource.content") { // Old editor
        completeTB = vrtxEdit.CKEditorToolbars.completeToolbarOld;
      } 
      if (name == "content-study") { // Study toolbar
        completeTB = vrtxEdit.CKEditorToolbars.studyToolbar;
      } 
      if (name == "course-group-about"
       || name == "courses-in-group"
       || name == "course-group-admission"
       || name == "relevant-study-programmes"
       || name == "course-group-other") { // CourseGroup toolbar
        completeTB = vrtxEdit.CKEditorToolbars.courseGroupToolbar;
      }
    }
    vrtxEdit.setCKEditorConfig(name, linkBrowseUrl, imageBrowseUrl, flashBrowseUrl, defaultLanguage, cssFileList, height, maxHeight, 50, completeTB,
                               isCompleteEditor, true, baseDocumentUrl, isSimpleHTML);
  } else {
    vrtxEdit.setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 90, 400, 40, vrtxEdit.CKEditorToolbars.withoutSubSuperToolbar, 
                               isCompleteEditor, true, baseDocumentUrl, isSimpleHTML);
  }

};

/**
 * Set CKEditor config for an instance
 *
 * @this {VrtxEditor}
 * @param {string} name Name of textarea
 * @param {string} linkBrowseUrl Link browse integration URL
 * @param {string} imageBrowseUrl Image browse integration URL
 * @param {string} flashBrowseUrl Flash browse integration URL
 * @param {string} defaultLanguage Language in editor 
 * @param {string} cssFileList List of CSS-files to style content in editor
 * @param {number} height Height of editor
 * @param {number} maxHeight Max height of editor
 * @param {number} minHeight Min height of editor
 * @param {object} toolbar The toolbar config
 * @param {string} complete Use complete toolbar
 * @param {boolean} resizable Possible to resize editor
 * @param {string} baseDocumentUrl URL to current document 
 * @param {string} simple Make h1 format available (for old document types)
 */
VrtxEditor.prototype.setCKEditorConfig = function setCKEditorConfig(name, linkBrowseUrl, imageBrowseUrl, flashBrowseUrl, defaultLanguage, cssFileList, height, 
                                                                    maxHeight, minHeight, toolbar, complete, resizable, baseDocumentUrl, simple) {
  var vrtxEdit = this;                                                                 
  var config = [{}];

  config.baseHref = baseDocumentUrl;
  config.contentsCss = cssFileList;
  if (vrtxEdit.contains(name, "resource.")) { // Don't use HTML-entities for structured-documents
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
    config.stylesSet = vrtxEdit.CKEditorDivContainerStylesSet;
    if (name == "resource.content" && simple) { // XHTML
      config.format_tags = 'p;h1;h2;h3;h4;h5;h6;pre;div';
    } else {
      config.format_tags = 'p;h2;h3;h4;h5;h6;pre;div';
    }
  } else {
    config.removePlugins = 'elementspath';
  }

  config.resize_enabled = resizable;
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
  if(!isCkEditor(name)) {
    CKEDITOR.replace(name, config);
  }
};

function commentsCkEditor() {
  document.getElementById("comment-syntax-desc").style.display = "none";
  document.getElementById("comments-text-div").style.margin = "0";
  $("#comments-text").click(function () {
    vrtxEditor.setCKEditorConfig("comments-text", null, null, null, null, cssFileList, 150, 400, 40, vrtxEditor.CKEditorToolbars.commentsToolbar, false, true, null);
  });
}

/*-------------------------------------------------------------------*\
    5. Validation and change detection
\*-------------------------------------------------------------------*/

function storeInitPropValues(contents) {
  if(!contents.length) return;

  var vrtxEdit = vrtxEditor;

  var inputFields = contents.find("input").not("[type=submit]").not("[type=button]")
                                          .not("[type=checkbox]").not("[type=radio]");
  var selects = contents.find("select");
  var checkboxes = contents.find("input[type=checkbox]:checked");
  var radioButtons = contents.find("input[type=radio]:checked");
  
  for(var i = 0, len = inputFields.length; i < len; i++)  vrtxEdit.editorInitInputFields[i] = inputFields[i].value;
  for(    i = 0, len = selects.length; i < len; i++)      vrtxEdit.editorInitSelects[i] = selects[i].value;
  for(    i = 0, len = checkboxes.length; i < len; i++)   vrtxEdit.editorInitCheckboxes[i] = checkboxes[i].name;
  for(    i = 0, len = radioButtons.length; i < len; i++) vrtxEdit.editorInitRadios[i] = radioButtons[i].name + " " + radioButtons[i].value;
}

function unsavedChangesInEditor() {
  if (!vrtxEditor.needToConfirm) return false;
  
  var vrtxEdit = vrtxEditor;
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
  if(textLen != vrtxEdit.editorInitInputFields.length
  || selectsLen != vrtxEdit.editorInitSelects.length
  || checkboxLen != vrtxEdit.editorInitCheckboxes.length
  || radioLen != vrtxEdit.editorInitRadios.length) return true;

  // Check if values have changed
  for (var i = 0; i < textLen; i++) if(currentStateOfInputFields[i].value !== vrtxEdit.editorInitInputFields[i]) return true;
  for (    i = 0; i < selectsLen; i++) if(currentStateOfSelects[i].value !== vrtxEdit.editorInitSelects[i]) return true;
  for (    i = 0; i < checkboxLen; i++) if(currentStateOfCheckboxes[i].name !== vrtxEdit.editorInitCheckboxes[i]) return true;
  for (    i = 0; i < radioLen; i++) if(currentStateOfRadioButtons[i].name + " " + currentStateOfRadioButtons[i].value !== vrtxEdit.editorInitRadios[i]) return true;
  
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

/* Validate length for 2048 bytes fields */

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

/*-------------------------------------------------------------------*\
    6. Image preview
\*-------------------------------------------------------------------*/

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

/*-------------------------------------------------------------------*\
    7. Show / hide
\*-------------------------------------------------------------------*/

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

/* Dropdown show/hide mappings
 */
 
/**
 * Select field show/hide with mappings
 *
 * @this {VrtxEditor}
 * @param {object} select The select field
 */
VrtxEditor.prototype.hideShowSelect = function hideShowSelect(select) {
  var vrtxEdit = this;
  var id = select.attr("id");
  if(vrtxEdit.selectMappings.hasOwnProperty(id)) {
    var mappings = vrtxEdit.selectMappings[id];
    var selected = select.val();
    for(var item in mappings) {
      if(item === selected) {
        vrtxEdit.editorForm.find(mappings[item]).filter(":hidden").show();
      } else {
        vrtxEdit.editorForm.find(mappings[item]).filter(":visible").hide();
      }
    }
  }
};

/* XXX: should be more general */
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
  vrtxEditor.accordionGroupedRefresh();
}

/*-------------------------------------------------------------------*\
    8. Multiple fields and boxes
\*-------------------------------------------------------------------*/

/* Multiple comma seperated input textfields */

function loadMultipleInputFields(name, addName, removeName, moveUpName, moveDownName, browseName, isMovable, isBrowsable) { // TODO: simplify
  var inputField = $("." + name + " input[type=text]");
  var inputFieldVal = inputField.val();
  if (inputFieldVal == null) {
    return;
  }

  var formFields = inputFieldVal.split(",");

  vrtxEditor.multipleCommaSeperatedInputFieldCounter[name] = 1; // 1-index
  vrtxEditor.multipleCommaSeperatedInputFieldLength[name] = formFields.length;
  vrtxEditor.multipleCommaSeperatedInputFieldNames.push(name);

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

  var appendHtml = $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["add-button"], { name: name, removeName: removeName, moveUpName: moveUpName, 
                                                                                                   moveDownName: moveDownName, browseName: browseName,
                                                                                                   size: size, isBrowsable: isBrowsable, isMovable: isMovable,
                                                                                                   isDropdown: isDropdown, buttonText: addName });

  inputFieldParent.removeClass("vrtx-textfield").append(appendHtml);
    
  var addFormFieldFunc = addFormField;
  for (var i = 0; i < vrtxEditor.multipleCommaSeperatedInputFieldLength[name]; i++) {
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
  vrtxEditor.multipleCommaSeperatedInputFieldDeferred = $.Deferred();
  vrtxEditor.multipleCommaSeperatedInputFieldTemplates = vrtxAdmin.retrieveHTMLTemplates("multiple-inputfields",
                                                                                        ["button", "add-button", "multiple-inputfield"],
                                                                                        vrtxEditor.multipleCommaSeperatedInputFieldDeferred);
}

function addFormField(name, value, removeName, moveUpName, moveDownName, browseName, size, isBrowsable, init, isMovable, isDropdown) {
  if (value == null) value = "";

  var idstr = "vrtx-" + name + "-",
      i = vrtxEditor.multipleCommaSeperatedInputFieldCounter[name],
      removeButton = "", moveUpButton = "", moveDownButton = "", browseButton = "";

  if (removeName) {
    removeButton = $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["button"], { type: "remove", name: " " + name, 
                                                                                               idstr: idstr,   buttonText: removeName });
  }
  if (isMovable && moveUpName && i > 1) {
    moveUpButton = $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["button"], { type: "moveup", name: "", 
                                                                                               idstr: idstr,   buttonText: "&uarr; " + moveUpName });
  }
  if (isMovable && moveDownName && i < vrtxEditor.multipleCommaSeperatedInputFieldLength[name]) {
    moveDownButton = $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["button"], { type: "movedown", name: "", 
                                                                                                 idstr: idstr,     buttonText: "&darr; " + moveDownName });
  }
  if(isBrowsable) {
    browseButton = $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["button"], { type: "browse", name: "-resource-ref", 
                                                                                               idstr: idstr,   buttonText: browseName });
  }
    
  var html = $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["multiple-inputfield"], { idstr: idstr, i: i, value: value, 
                                                                                                      size: size, browseButton: browseButton,
                                                                                                      removeButton: removeButton, moveUpButton: moveUpButton,
                                                                                                      moveDownButton: moveDownButton, isDropdown: isDropdown,
                                                                                                      dropdownArray: "dropdown" + name });

  $(html).insertBefore("#vrtx-" + name + "-add");
    
  if(!init) {
    if(vrtxEditor.multipleCommaSeperatedInputFieldLength[name] > 0 && isMovable) {
      var fields = $("." + name + " div.vrtx-multipleinputfield");
      if(fields.eq(vrtxEditor.multipleCommaSeperatedInputFieldLength[name] - 1).not("has:button.movedown")) {
        moveDownButton = $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["button"], { type: "movedown", name: "", 
                                                                                                     idstr: idstr,     buttonText: "&darr; " + moveDownName });
        fields.eq(vrtxEditor.multipleCommaSeperatedInputFieldLength[name] - 1).append(moveDownButton);
      }
    }
    vrtxEditor.multipleCommaSeperatedInputFieldLength[name]++;
    autocompleteUsername(".vrtx-autocomplete-username", idstr + i);
  }

  vrtxEditor.multipleCommaSeperatedInputFieldCounter[name]++;   
}

function removeFormField(input) {
  var name = input.attr("class").replace("remove ", "");
  input.closest(".vrtx-multipleinputfield").remove();

  vrtxEditor.multipleCommaSeperatedInputFieldLength[name]--;
  vrtxEditor.multipleCommaSeperatedInputFieldCounter[name]--;

  var fields = $("." + name + " div.vrtx-multipleinputfield");

  if(fields.eq(vrtxEditor.multipleCommaSeperatedInputFieldLength[name] - 1).has("button.movedown")) {
    fields.eq(vrtxEditor.multipleCommaSeperatedInputFieldLength[name] - 1).find("button.movedown").parent().remove();
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
  for(var i = 0, len = vrtxEditor.multipleCommaSeperatedInputFieldNames.length; i < len; i++){
    formatMultipleInputFields(vrtxEditor.multipleCommaSeperatedInputFieldNames[i]);
  }
}

function formatMultipleInputFields(name) {
  var multipleTxt = $("." + name + " input[type=text]").filter(":hidden");  /*  Note: To achieve the best performance when using :hidden to select elements,
                                                                                      first select the elements using a pure CSS selector, then use .filter(":hidden") */
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

/* Multiple JSON boxes */

function initJsonMovableElements(templatesRetrieved, jsonElementsBuilt) {
  $.when(templatesRetrieved, jsonElementsBuilt).done(function() {
    for (var i = 0, len = LIST_OF_JSON_ELEMENTS.length; i < len; i++) {
      var json = { clazz: "add", buttonText: addBtn }
      $("#" + LIST_OF_JSON_ELEMENTS[i].name)
        .append($.mustache(TEMPLATES["add-remove-move"], json))
        .find(".vrtx-add-button").data({'number': i});
    }
        
    // TODO: avoid this being hardcoded here
    var syllbausItems = $("#editor.vrtx-syllabus #items");
    wrapJSONItemsLeftRight(syllbausItems.find(".vrtx-json-element"), ".author, .title, .year, .publisher, .isbn, .comment", ".linktext, .link, .bibsys, .fulltext, .articles");
    syllbausItems.find(".author input, .title input").addClass("header-populators");
    syllbausItems.find(".vrtx-html textarea").addClass("header-fallback-populator");
        
    var sharedTextItems = $("#editor.vrtx-shared-text #shared-text-box");
    sharedTextItems.find(".title input").addClass("header-populators");
    // ^ TODO: avoid this being hardcoded here
      
    // Because accordion needs one content wrapper
    for(var grouped = $(".vrtx-json-accordion .vrtx-json-element"), i = grouped.length; i--;) { 
      var group = $(grouped[i]);
      group.find("> *").wrapAll("<div />");
      accordionJsonUpdateHeader(group);
    }
      
    $(".vrtx-json-accordion .fieldset").accordion({ 
                                                   header: "> div > .header",
                                                   autoHeight: false,
                                                   collapsible: true,
                                                   active: false,
                                                   change: function(e, ui) {
                                                     accordionJsonUpdateHeader(ui.oldHeader);
                                                     if(ACCORDION_MOVE_TO_AFTER_CHANGE) {
                                                       scrollToElm(ACCORDION_MOVE_TO_AFTER_CHANGE);
                                                     }
                                                   }  
                                                  });
    JSON_ELEMENTS_INITIALIZED.resolve();
  });

  vrtxAdmin.cachedAppContent.on("click", ".vrtx-json .vrtx-add-button", function(e) {
    var accordionWrapper = $(this).closest(".vrtx-json-accordion");
    var hasAccordion = accordionWrapper.length;
           
    var btn = $(this);
    var jsonParent = btn.closest(".vrtx-json");
    var counter = jsonParent.find(".vrtx-json-element").length;
    var j = LIST_OF_JSON_ELEMENTS[parseInt(btn.data('number'))];
    var htmlTemplate = "";
    var arrayOfIds = [];

    // Add correct HTML for vrtx-type
    var types = j.a;
    
    for (var i in types) {
      var inputFieldName = j.name + "." + types[i].name + "." + counter;
      arrayOfIds[i] = new String(j.name + "." + types[i].name + ".").replace(/\./g, "\\.");
      switch (types[i].type) {
        case "string":
          if (types[i].dropdown && types[i].valuemap) {
            htmlTemplate += addDropdown(types[i], inputFieldName);
          } else {
            htmlTemplate += addStringField(types[i], inputFieldName);
          }
          break;
        case "html":
          htmlTemplate += addHtmlField(types[i], inputFieldName);
          break;
        case "simple_html":
          htmlTemplate += addHtmlField(types[i], inputFieldName);
          break;
        case "boolean":
          htmlTemplate += addBooleanField(types[i], inputFieldName);
          break;
        case "image_ref":
          htmlTemplate += addImageRef(types[i], inputFieldName);
          break;
        case "resource_ref":
          htmlTemplate += addResourceRef(types[i], inputFieldName);
          break;
        case "datetime":
          htmlTemplate += addDateField(j.a[i], inputFieldName);
          break;
        case "media":
          htmlTemplate += addMediaRef(types[i], inputFieldName);
          break;
        default:
          htmlTemplate += "";
          break;
      }
    }
      
    // Move up, move down, remove
    var isImmovable = jsonParent && jsonParent.hasClass("vrtx-multiple-immovable");
    if(!isImmovable) {
      var moveDownButton = $.mustache(TEMPLATES["add-remove-move"], { clazz: 'move-down', buttonText: '&darr; ' + moveDownBtn });
      var moveUpButton = $.mustache(TEMPLATES["add-remove-move"],   { clazz: 'move-up', buttonText: '&uarr; ' + moveUpBtn   });
    }
    var removeButton = $.mustache(TEMPLATES["add-remove-move"],   { clazz: 'remove', buttonText: removeBtn });
      
    var id = "<input type=\"hidden\" class=\"id\" value=\"" + counter + "\" \/>";
    var newElementId = "vrtx-json-element-" + j.name + "-" + counter;
        
    var newElementHtml = htmlTemplate;
    newElementHtml += id;
    newElementHtml += removeButton;
  
    if (!isImmovable && counter > 0) {
      newElementHtml += moveUpButton;
    }
      
    $("#" + j.name + " .vrtx-add-button").before("<div class='vrtx-json-element' id='" + newElementId + "'>" + newElementHtml + "<\/div>");
      
    var newElement = $("#" + newElementId);
    var prev = newElement.prev(".vrtx-json-element");
    newElement.addClass("last");
    prev.removeClass("last");
      
    if(!isImmovable && counter > 0) {
      newElement.find(".vrtx-move-up-button").click(function (e) {
        swapContent(counter, arrayOfIds, -1, j.name);
        e.stopPropagation();
        e.preventDefault();
      });

      if (prev.length) {
        if(hasAccordion) {
          prev.find("> div.ui-accordion-content").append(moveDownButton);
        } else {
          prev.append(moveDownButton);
        }
        prev.find(".vrtx-move-down-button").click(function (e) {
          swapContent(counter-1, arrayOfIds, 1, j.name);
          e.stopPropagation();
          e.preventDefault();
        });
      }
    }
        
    if(hasAccordion ) {
      var accordionContent = accordionWrapper.find(".fieldset");
      var group = accordionContent.find(".vrtx-json-element:last");
      group.find("> *").wrapAll("<div />");
      group.prepend('<div class="header">' + (vrtxAdmin.lang !== "en" ? "Inget innhold" : "No content") + '</div>');
          
      // TODO: avoid this being hardcoded here
      var lastSyllabusItem = $("#editor.vrtx-syllabus #items .vrtx-json-element:last");
      wrapJSONItemsLeftRight(lastSyllabusItem, ".author, .title, .year, .publisher, .isbn, .comment", ".linktext, .link, .bibsys, .fulltext, .articles");
      lastSyllabusItem.find(".author input, .title input").addClass("header-populators");
      lastSyllabusItem.find(".vrtx-html textarea").addClass("header-fallback-populator");
          
      var lastSharedTextItem = $("#editor.vrtx-shared-text #shared-text-box .vrtx-json-element:last");
      lastSharedTextItem.find(".title input").addClass("header-populators");
      // ^ TODO: avoid this being hardcoded here
          
      accordionJsonRefresh(accordionContent, false);
    }

    // CK and date inputfields
    for (i in types) {
      var inputFieldName = j.name + "." + types[i].name + "." + counter;
      if (types[i].type == "simple_html") {
        vrtxEditor.newEditor(inputFieldName, false, false, parentURI, ckBaseURI, ckBaseDocURI, ckBaseBrowsePath, requestLang, cssFileList, "true");
      } else if (types[i].type == "html") {
        vrtxEditor.newEditor(inputFieldName, true,  false, parentURI, ckBaseURI, ckBaseDocURI, ckBaseBrowsePath, requestLang, cssFileList, "false");
      } else if (types[i].type == "datetime") {
        displayDateAsMultipleInputFields(inputFieldName);
      }
    }

    e.stopPropagation();
    e.preventDefault();
  });

  vrtxAdmin.cachedAppContent.on("click", ".vrtx-json .vrtx-remove-button", function(e) {
    var removeElement = $(this).closest(".vrtx-json-element");
    var accordionWrapper = removeElement.closest(".vrtx-json-accordion");
    var hasAccordion = accordionWrapper.length;  
    var removeElementParent = removeElement.parent();
    
    var textAreas = removeElement.find("textarea");
    var i = textAreas.length;
    while(i--) {
      var textAreaName = textAreas[i].name;
      if (isCkEditor(textAreaName)) {
        var ckInstance = getCkInstance(textAreaName);
        ckInstance.destroy();
        delete ckInstance;
      }
    }
        
    var updateLast = removeElement.hasClass("last");
    removeElement.remove();
    removeElementParent.find(".vrtx-json-element:first .vrtx-move-up-button").remove();
    var newLast = removeElementParent.find(".vrtx-json-element:last");
    newLast.find(".vrtx-move-down-button").remove();
    if(updateLast) {
      newLast.addClass("last");
    }
    if(hasAccordion) {
      var accordionContent = accordionWrapper.find(".fieldset");
      accordionJsonRefresh(accordionContent, false);
    }
    e.stopPropagation();
    e.preventDefault();
  });
}
    
function wrapJSONItemsLeftRight(items, leftItems, rightItems) {
  if(items.length == 1) {
    items.find(leftItems).wrapAll("<div class='left' />");
    items.find(rightItems).wrapAll("<div class='right' />");
  } else if(items.length > 1) {
    var i = items.length;
    while(i--) {
      var item = $(items[i]);
      item.find(leftItems).wrapAll("<div class='left' />");
      item.find(rightItems).wrapAll("<div class='right' />");
    }
  }
}

function addStringField(elem, inputFieldName) {
  return $.mustache(TEMPLATES["string"], { classes: "vrtx-string" + " " + elem.name,
                                           elemTitle: elem.title,
                                           inputFieldName: inputFieldName }); 
}
    
function addHtmlField(elem, inputFieldName) {
  var baseclass = "vrtx-html";
  if (elem.type == "simple_html") {
    baseclass = "vrtx-simple-html";
  }
  return $.mustache(TEMPLATES["html"], { classes: baseclass + " " + elem.name,
                                         elemTitle: elem.title,
                                         inputFieldName: inputFieldName }); 
}
    
function addBooleanField(elem, inputFieldName) {
  return $.mustache(TEMPLATES["radio"], { elemTitle: elem.title,
                                          inputFieldName: inputFieldName }); 
}

function addDropdown(elem, inputFieldName) {
  var htmlOpts = [];
  for (i in elem.valuemap) {
    var keyValuePair = elem.valuemap[i];
    var keyValuePairSplit = keyValuePair.split("$");
    htmlOpts.push({key: keyValuePairSplit[0], value: keyValuePairSplit[1]});
  }
  return $.mustache(TEMPLATES["dropdown"], { classes: "vrtx-string" + " " + elem.name,
                                             elemTitle: elem.title,
                                             inputFieldName: inputFieldName,
                                             options: htmlOpts });  
}

function addDateField(elem, inputFieldName) {
  return $.mustache(TEMPLATES["date"], { elemTitle: elem.title,
                                         inputFieldName: inputFieldName }); 
}
    
function addImageRef(elem, inputFieldName) {
  return $.mustache(TEMPLATES["browse-images"], { clazz: 'vrtx-image-ref',
                                                  elemTitle: elem.title,
                                                  inputFieldName: inputFieldName,
                                                  fckEditorBaseUrl: ckBaseURI,
                                                  parentURI: parentURI,
                                                  fckBrowsePath: ckBaseBrowsePath,
                                                  browseButtonText: browseImagesBtn,
                                                  type: '',
                                                  size: 30,
                                                  previewTitle: browseImagesPreview,
                                                  previewNoImageText: browseImagesNoPreview }); 
}
    
function addResourceRef(elem, inputFieldName) {
  return $.mustache(TEMPLATES["browse"], { clazz: 'vrtx-resource-ref',
                                           elemTitle: elem.title,
                                           inputFieldName: inputFieldName,
                                           fckEditorBaseUrl: ckBaseURI,
                                           parentURI: parentURI,
                                           fckBrowsePath: ckBaseBrowsePath,
                                           browseButtonText: browseImagesBtn,
                                           type: 'File',
                                           size: 40 }); 
}
    
function addMediaRef(elem, inputFieldName) {      
  return $.mustache(TEMPLATES["browse"], { clazz: 'vrtx-media-ref',
                                           elemTitle: elem.title,
                                           inputFieldName: inputFieldName,
                                           fckEditorBaseUrl: ckBaseURI,
                                           parentURI: parentURI,
                                           fckBrowsePath: ckBaseBrowsePath,
                                           browseButtonText: browseImagesBtn,
                                           type: 'Media',
                                           size: 30 }); 
}
    
// Move up or move down  
function swapContent(counter, arrayOfIds, move, name) {
  var thisId = "#vrtx-json-element-" + name + "-" + counter;
  var thisElm = $(thisId);   
  var accordionWrapper = thisElm.closest(".vrtx-json-accordion");
  var hasAccordion = accordionWrapper.length;   
      
  if (move > 0) {
    var movedElm = thisElm.next(".vrtx-json-element");
  } else {
    var movedElm = thisElm.prev(".vrtx-json-element");
  }
  var movedId = "#" + movedElm.attr("id");
  var moveToCounter = movedElm.find("input.id").val();
      
  for (var x = 0, len = arrayOfIds.length; x < len; x++) {
    var elementId1 = '#' + arrayOfIds[x] + counter;
    var elementId2 = '#' + arrayOfIds[x] + moveToCounter;
    var element1 = $(elementId1);
    var element2 = $(elementId2);
        
    /* We need to handle special cases like CK fields and date */
    var ckInstanceName1 = arrayOfIds[x].replace(/\\/g, '') + counter;
    var ckInstanceName2 = arrayOfIds[x].replace(/\\/g, '') + moveToCounter;

    if (isCkEditor(ckInstanceName1) && isCkEditor(ckInstanceName2)) {
      var val1 = getCkValue(ckInstanceName1);
      var val2 = getCkValue(ckInstanceName2);
      setCkValue(ckInstanceName1, val2);
      setCkValue(ckInstanceName2, val1);
    } else if (element1.hasClass("date") && element2.hasClass("date")) {
      var element1Wrapper = element1.closest(".vrtx-string");
      var date1 = element1Wrapper.find(elementId1 + '-date');
      var hours1 = element1Wrapper.find(elementId1 + '-hours');
      var minutes1 = element1Wrapper.find(elementId1 + '-minutes');
      var element2Wrapper = element2.closest(".vrtx-string");
      var date2 = element2Wrapper.find(elementId2 + '-date');
      var hours2 = element2Wrapper.find(elementId2 + '-hours');
      var minutes2 = element2Wrapper.find(elementId2 + '-minutes');
      var dateVal1 = date1.val();
      var hoursVal1 = hours1.val();
      var minutesVal1 = minutes1.val();
      var dateVal2 = date2.val();
      var hoursVal2 = hours2.val();
      var minutesVal2 = minutes2.val();
      date1.val(dateVal2);
      hours1.val(hoursVal2);
      minutes1.val(minutesVal2);
      date2.val(dateVal1);
      hours2.val(hoursVal1);
      minutes2.val(minutesVal1);
    }    
        
    var val1 = element1.val();
    var val2 = element2.val();
    element1.val(val2);
    element2.val(val1);
    if(hasAccordion) {
      accordionJsonUpdateHeader(element1);
      accordionJsonUpdateHeader(element2);
    }
    element1.blur();
    element2.blur();
    element1.change();
    element2.change();
  }
  thisElm.focusout();
  movedElm.focusout();
      
  if(hasAccordion) {
    ACCORDION_MOVE_TO_AFTER_CHANGE = movedElm;
    var accordionContent = accordionWrapper.find(".fieldset");
    accordionContent.accordion("option", "active", (movedElm.index() - 1));
    accordionContent.accordion("option", "refresh");
  } else {
    scrollToElm(movedElm);
  }
}
    
function scrollToElm(movedElm) {
  var absPos = movedElm.offset();
  var absPosTop = absPos.top;
  var stickyBar = $("#vrtx-editor-title-submit-buttons");
  if(stickyBar.css("position") == "fixed") {
    var stickyBarHeight = stickyBar.height();
    absPosTop -= (stickyBarHeight <= absPosTop) ? stickyBarHeight : 0;
  }
  $('body').scrollTo(absPosTop, 250, {
    easing: 'swing',
    queue: true,
    axis: 'y'
  });
  setTimeout(function() {
    ACCORDION_MOVE_TO_AFTER_CHANGE = null;
  }, 270);
}

/*-------------------------------------------------------------------*\
    9. Accordion grouping
\*-------------------------------------------------------------------*/

/**
 * Initialize .vrtx-grouped elements as accordions
 * @this {VrtxEditor}
 */
VrtxEditor.prototype.accordionGroupedInit = function accordionGroupedInit(subGroupedSelector) { /* param name pending */
  var vrtxEdit = this, _$ = vrtxAdmin._$;

  var accordionWrpId = "accordion-grouped"; // TODO: multiple accordion group pr. page
  var groupedSelector = ".vrtx-grouped" + ((typeof subGroupedSelector !== "undefined") ? subGroupedSelector : "");

  // Because accordion needs one content wrapper
  for(var grouped = vrtxEdit.editorForm.find(groupedSelector), i = grouped.length; i--;) { 
    _$(grouped[i]).find("> *:not(.header)").wrapAll("<div />");
  }
  grouped.wrapAll("<div id='" + accordionWrpId + "' />");
  vrtxEdit.editorForm.find("#" + accordionWrpId).accordion({ header: "> div > .header",
                                                             autoHeight: false,
                                                             collapsible: true,
                                                             active: false
                                                           });
};

/**
 * Refresh accordion
 * @this {VrtxEditor}
 */
VrtxEditor.prototype.accordionGroupedRefresh = function accordionGroupedRefresh() { /* param name pending */
  var vrtxEdit = this, _$ = vrtxAdmin._$;
  
  vrtxEdit.editorForm.find("#accordion.grouped")
                     .accordion( "destroy" ).accordion({ header: "> div > .header",
                                                         autoHeight: false,
                                                         collapsible: true,
                                                         active: false
                                                       });
};

function accordionJsonRefresh(elem, active) {
  elem.accordion('destroy').accordion({
    header: "> div > .header",
    autoHeight: false,
    collapsible: true,
    active: active,
    change: function (e, ui) {
      accordionJsonUpdateHeader(ui.oldHeader);
      if (ACCORDION_MOVE_TO_AFTER_CHANGE) {
        scrollToElm(ACCORDION_MOVE_TO_AFTER_CHANGE);
      }
    }
  });
}


function accordionJsonUpdateHeader(elem) {
  var jsonElm = elem.closest(".vrtx-json-element");
  if (jsonElm.length) { // Prime header populators
    var str = "";
    var fields = jsonElm.find(".header-populators");
    for (var i = 0, len = fields.length; i < len; i++) {
      var val = $(fields[i]).val();
      if (!val.length) {
        continue;
      }
      str += (str.length) ? ", " + val : val;
    }
    if (!str.length) { // Fallback header populator
      var field = jsonElm.find(".header-fallback-populator");
      if (field.length) {
        var fieldId = field.attr("id");
        if (isCkEditor(fieldId)) { // Check if CK
          str = getCkValue(fieldId); // Get CK content
        } else {
          str = field.val();
        }
        if (field.is("textarea")) { // Remove markup and tabs
          str = $.trim(str.replace(/(<([^>]+)>|[\t\r]+)/ig, ""));
        }
        if (typeof str !== "undefined") {
          if (str.length > 30) {
            str = str.substring(0, 30) + "...";
          } else if (!str.length) {
            str = (vrtxAdmin.lang !== "en") ? "Inget innhold" : "No content";
          }
        }
      } else {
        str = (vrtxAdmin.lang !== "en") ? "Inget innhold" : "No content";
      }
    }
    var header = jsonElm.find("> .header");
    if (!header.length) {
      jsonElm.prepend('<div class="header">' + str + '</div>');
    } else {
      header.html('<span class="ui-icon ui-icon-triangle-1-e"></span>' + str);
    }
  }
}

/*-------------------------------------------------------------------*\
    10. Utils
\*-------------------------------------------------------------------*/

/**
 * Check if string contains substring
 *
 * @this {VrtxEditor}
 * @param {string} string The string
 * @param {string} substring The substring
 * @return {boolean} Existance 
 */
VrtxEditor.prototype.contains = function contains(string, substring) {
  return string.indexOf(substring) != -1; 
};

/**
 * Replace tags
 *
 * @this {VrtxEditor}
 * @param {string} selector The context selector
 * @param {string} tag The selector for tags to be replaced
 * @param {string} replacementTag The replacement tag name
 */
VrtxEditor.prototype.replaceTag = function replaceTag(selector, tag, replacementTag) {
  selector.find(tag).replaceWith(function() {
    return "<" + replacementTag + ">" + $(this).text() + "</" + replacementTag + ">";
  });
}

/* CK helper functions */

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

/* ^ Vortex Editor */