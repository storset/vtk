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
 *  4.  CKEditor
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
function VrtxEditor() {
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
    
    /** Select fields show/hide mappings */
    /*                      select-id            values to be added as class to form         */
   this.selectMappings = { "teachingsemester":  ["particular-semester", "every-other", "other"],
                           "examsemester":      ["particular-semester", "every-other", "other"],
                           "teaching-language": ["other"],
                           "typeToDisplay":     ["so", "nm", "em"],
                         };
  
    /** Initial state for the need to confirm navigation away from editor */
    this.needToConfirm = true;
    
    /* TODO: Need some rewrite of data structure to use 1 instead of 3 variables */
    this.multipleCommaSeperatedInputFieldNames = [];
    this.multipleCommaSeperatedInputFieldCounter = [];
    this.multipleCommaSeperatedInputFieldLength = [];
    this.multipleCommaSeperatedInputFieldTemplates = [];
    this.multipleCommaSeperatedInputFieldDeferred;
  
    /** Check if this script is in admin or not */                      
    this.isInAdmin = typeof vrtxAdmin !== "undefined";
}

var vrtxEditor = new VrtxEditor();

var UNSAVED_CHANGES_CONFIRMATION;

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
  
  vrtxEdit.initPreviewImage();
  vrtxEdit.initSendToApproval();
  vrtxEdit.initStickyBar();
  vrtxEdit.addSaveHelpCKMaximized();
  vrtxEdit.initShowHide();
  vrtxEdit.initStudyDocTypes();
  vrtxEdit.initCKEditors();
});

/*-------------------------------------------------------------------*\
    4. CKEditor
\*-------------------------------------------------------------------*/

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
VrtxEditor.prototype.newEditor = function newEditor(name, completeEditor, withoutSubSuper, defaultLanguage, cssFileList, simpleHTML) {
  var vrtxEdit = this;
  
  /* TMP inner mapping */
  var baseFolder = vrtxAdmin.multipleFormGroupingPaths.baseFolderURL,
      baseUrl = vrtxAdmin.multipleFormGroupingPaths.baseCKURL,
      baseDocumentUrl = vrtxAdmin.multipleFormGroupingPaths.baseDocURL,
      browsePath = vrtxAdmin.multipleFormGroupingPaths.basePath;
  
  // If pregenerated parameters is used for init
  if(typeof name === "object") {
    var obj = name;
    name = obj[0];
    completeEditor = obj[1];
    withoutSubSuper = obj[2];
    defaultLanguage = obj[3];
    cssFileList = obj[4];
    simpleHTML = obj[5];
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

VrtxEditor.prototype.addSaveHelpCKMaximized = function addSaveHelpCKMaximized() {
  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;

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
};

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

VrtxEditor.prototype.initPreviewImage = function initPreviewImage() {
  var _$ = vrtxAdmin._$;

  /* Hide image previews on init (unobtrusive) */
  var previewInputFields = _$("input.preview-image-inputfield");
  for(var i = previewInputFields.length; i--;) {
    if(previewInputFields[i].value === "") {
      hideImagePreviewCaption($(previewInputFields[i]), true);
    }
  } 

  /* Inputfield events for image preview */
  _$(document).on("blur", "input.preview-image-inputfield", function(e) {
    previewImage(this.id)
  });
  
  _$(document).on("keydown", "input.preview-image-inputfield", _$.debounce(50, true, function(e) { // ENTER-key
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      previewImage(this.id);
      e.preventDefault();
    }
  }));
};

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
       XXX: boolean field animations when not init
\*-------------------------------------------------------------------*/
  
VrtxEditor.prototype.initShowHide = function initShowHide() { 
  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$, vrtxEdit = this;

  // Show/hide mappings for radios/booleans
  if(!_$("#resource\\.display-aggregation\\.true").is(":checked")) {
    _$("#vrtx-resource\\.aggregation").slideUp(0, "linear");
  }
  if(!_$("#resource\\.display-manually-approved\\.true").is(":checked")) {
    _$("#vrtx-resource\\.manually-approve-from").slideUp(0, "linear");
  }

  vrtxAdm.cachedAppContent.on("click", "#resource\\.display-aggregation\\.true", function(e) {
    if(!_$(this).is(":checked")) {
      _$(".aggregation .vrtx-multipleinputfield").remove();
      _$("#resource\\.aggregation").val("");
    }
    _$("#vrtx-resource\\.aggregation").slideToggle(vrtxAdm.transitionDropdownSpeed, "swing");
    e.stopPropagation();
  });

  vrtxAdm.cachedAppContent.on("click", "#resource\\.display-manually-approved\\.true", function(e) {
    if(!_$(this).is(":checked")) {
      _$(".manually-approve-from .vrtx-multipleinputfield").remove();
      _$("#resource\\.manually-approve-from").val("");
    }
    _$("#vrtx-resource\\.manually-approve-from").slideToggle(vrtxAdm.transitionDropdownSpeed, "swing");
    e.stopPropagation();
  });
  
  vrtxAdm.cachedAppContent.on("change", "#resource\\.courseContext\\.course-status", function(e) {
    var courseStatus = _$(this);
    if(courseStatus.val() === "continued-as") {
      _$("#vrtx-resource\\.courseContext\\.course-continued-as.hidden").slideDown(vrtxAdm.transitionDropdownSpeed, "swing", function() {
        _$(this).removeClass("hidden");
      });
    } else {
      _$("#vrtx-resource\\.courseContext\\.course-continued-as:not(.hidden)").slideUp(vrtxAdm.transitionDropdownSpeed, "swing", function() {
        _$(this).addClass("hidden");
      });
    }
    e.stopPropagation();
  });
  _$("#resource\\.courseContext\\.course-status").change();

  setShowHideBooleanOldEditor("#resource\\.recursive-listing\\.false, #resource\\.recursive-listing\\.unspecified", 
                             "#vrtx-resource\\.recursive-listing-subfolders",
                             "#resource\\.recursive-listing\\.false:checked",                                       
                             "false");  
                                                                 
  setShowHideBooleanOldEditor("#resource\\.display-type\\.unspecified, #resource\\.display-type\\.calendar",
                             "#vrtx-resource\\.event-type-title",
                             "#resource\\.display-type\\.calendar:checked",
                             null);
                             
  setShowHideBooleanOldEditor("#resource\\.display-type\\.unspecified, #resource\\.display-type\\.calendar",
                             "#vrtx-resource\\.hide-additional-content",
                             "#resource\\.display-type\\.calendar:checked",
                             "calendar");
                              
  vrtxEdit.setShowHideSelectNewEditor();
};

/*
 * Boolean switch show/hide
 *
 */
function setShowHideBooleanNewEditor(name, properties, hideTrues) {
  vrtxEditor.initEventHandler('[name=' + name + ']', {
	wrapper: "#editor",
    callback: function(props, hideTrues, name , init) {
      if ($('#' + name + (hideTrues ? '-false' : '-true'))[0].checked) {
        toggleShowHideBoolean(props, true, init);
      } else if ($('#' + name + (hideTrues ? '-true' : '-false'))[0].checked) {
        toggleShowHideBoolean(props, false, init); 
      }
    },
	callbackParams: [properties, hideTrues, name]
  })	
}

function setShowHideBooleanOldEditor(radioIds, properties, conditionHide, conditionHideEqual) {
  vrtxEditor.initEventHandler(radioIds, {
    wrapper: "#editor",
	callback: function(props, conditionHide, conditionHideEqual, init) {
	  toggleShowHideBoolean(props, !($(conditionHide).val() == conditionHideEqual), init);
	},
	callbackParams: [properties, conditionHide, conditionHideEqual]
  });
}

function toggleShowHideBoolean(props, show, init) {
  var theProps = $(props);
  if(init || vrtxAdmin.isIE9) {
    if(!vrtxAdmin.isIE9) {
      theProps.addClass("animate-optimized");
    }
    if(show && !init) {
      theProps.show();  
    } else {
      theProps.hide();  
    }
  } else {
    if(show) {
      theProps.slideDown(vrtxAdmin.transitionPropSpeed, vrtxAdmin.transitionEasingSlideDown);
    } else {
      theProps.slideUp(vrtxAdmin.transitionPropSpeed, vrtxAdmin.transitionEasingSlideUp);
    }
  }
}

/**
 * Set select field show/hide
 *
 * @this {VrtxEditor}
 */
VrtxEditor.prototype.setShowHideSelectNewEditor = function setShowHideSelectNewEditor() {
  var vrtxEdit = this;
  
  for(var select in vrtxEdit.selectMappings) {
    vrtxEdit.initEventHandler("#" + select, {
      event: "change",
      callback: vrtxEdit.showHideSelect
    });
  }
}


/**
 * Select field show/hide
 *
 * @this {VrtxEditor}
 * @param {object} select The select field
 */
VrtxEditor.prototype.showHideSelect = function showHideSelect(select, init) {
  var vrtxEdit = this;
  
  var id = select.attr("id");
  if(vrtxEdit.selectMappings.hasOwnProperty(id)) {
    var selectClassName = "select-" + id;
    if(!vrtxEdit.editorForm.hasClass(selectClassName)) {
      vrtxEdit.editorForm.addClass(selectClassName);	
    }
    var mappings = vrtxEdit.selectMappings[id];
    var selected = select.val();
    for(var i = 0, len = mappings.length; i < len; i++) {
      var mappedClass = selectClassName + "-" + mappings[i];
      if(selected === mappings[i]) {
        if(!vrtxEdit.editorForm.hasClass(mappedClass)) {
          vrtxEdit.editorForm.addClass(mappedClass);
        }
      } else {
        if(vrtxEdit.editorForm.hasClass(mappedClass)) {
          vrtxEdit.editorForm.removeClass(mappedClass);
        }
      } 
    }
  }
  if(!init) vrtxEdit.accordionGroupedCloseActiveHidden();
};

/*-------------------------------------------------------------------*\
    8. Multiple fields and boxes
    XXX: refactor / combine and optimize
\*-------------------------------------------------------------------*/

/**
 * NEW CODE/API
 *
 * Initialize a multiple form grouping with enhancing possibilities
 *
 * - Add / Remove
 * - Move up / move down
 * - Accordion
 * - Animated scrolling
 * - (drag and drop)
 *
 * @this {VrtxEditor}

VrtxEditor.prototype.initMultipleFormGrouping = function initMultipleFormGrouping() {
  // name, isMovable, isBrowsable, isScrollable, hasAccordion(?)
};

 */

/* Multiple comma seperated input textfields */
function loadMultipleInputFields(name, isMovable, isBrowsable) { // TODO: simplify
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
    var inputFieldParentNext = inputFieldParent.next();
    if(inputFieldParentNext.hasClass("vrtx-button")) {
      inputFieldParentNext.hide();
    } 
  }

  inputField.hide();
  inputFieldParent.removeClass("vrtx-textfield").append(vrtxEditor.mustacheFacade.getMultipleInputFieldsAddButton(name, size, isBrowsable, isMovable, isDropdown));
    
  var addFormFieldFunc = addFormField;
  for (var i = 0; i < vrtxEditor.multipleCommaSeperatedInputFieldLength[name]; i++) {
    addFormFieldFunc(name, $.trim(formFields[i]), size, isBrowsable, true, isMovable, isDropdown);
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
    browseServer($(this).closest(".vrtx-multipleinputfield").find('input').attr('id'), vrtxAdmin.multipleFormGroupingPaths.baseCKURL, vrtxAdmin.multipleFormGroupingPaths.baseFolderURL, vrtxAdmin.multipleFormGroupingPaths.basePath, 'File');
    e.preventDefault();
    e.stopPropagation();
  });

  // Retrieve HTML templates
  vrtxEditor.multipleCommaSeperatedInputFieldDeferred = $.Deferred();
  vrtxEditor.multipleCommaSeperatedInputFieldTemplates = vrtxAdmin.retrieveHTMLTemplates("multiple-inputfields",
                                                                                        ["button", "add-button", "multiple-inputfield"],
                                                                                        vrtxEditor.multipleCommaSeperatedInputFieldDeferred);
}

function addFormField(name, value, size, isBrowsable, init, isMovable, isDropdown) {
  if (value == null) value = "";

  var idstr = "vrtx-" + name + "-",
      i = vrtxEditor.multipleCommaSeperatedInputFieldCounter[name],
      removeButton = "", moveUpButton = "", moveDownButton = "", browseButton = "";

  removeButton = vrtxEditor.mustacheFacade.getMultipleInputfieldsInteractionsButton("remove", " " + name, idstr, vrtxAdmin.multipleFormGroupingMessages.remove);
  if (isMovable && i > 1) {
    moveUpButton = vrtxEditor.mustacheFacade.getMultipleInputfieldsInteractionsButton("moveup", "", idstr, "&uarr; " + vrtxAdmin.multipleFormGroupingMessages.moveUp);
  }
  if (isMovable && i < vrtxEditor.multipleCommaSeperatedInputFieldLength[name]) {
    moveDownButton = vrtxEditor.mustacheFacade.getMultipleInputfieldsInteractionsButton("movedown", "", idstr, "&darr; " + vrtxAdmin.multipleFormGroupingMessages.moveDown);
  }
  if(isBrowsable) {
    browseButton = vrtxEditor.mustacheFacade.getMultipleInputfieldsInteractionsButton("browse", "-resource-ref", idstr, vrtxAdmin.multipleFormGroupingMessages.browseImages);
  }

  var html = vrtxEditor.mustacheFacade.getMultipleInputfield(name, idstr, i, value, size, browseButton, removeButton, moveUpButton, moveDownButton, isDropdown);

  $(html).insertBefore("#vrtx-" + name + "-add");
    
  if(!init) {
    if(vrtxEditor.multipleCommaSeperatedInputFieldLength[name] > 0 && isMovable) {
      var fields = $("." + name + " div.vrtx-multipleinputfield");
      if(fields.eq(vrtxEditor.multipleCommaSeperatedInputFieldLength[name] - 1).not("has:button.movedown")) {
        moveDownButton = vrtxEditor.mustacheFacade.getMultipleInputfieldsInteractionsButton("movedown", "", idstr, "&darr; " + vrtxAdmin.multipleFormGroupingMessages.moveDown);
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
      $("#" + LIST_OF_JSON_ELEMENTS[i].name)
        .append(vrtxEditor.mustacheFacade.getJsonBoxesInteractionsButton("add", vrtxAdmin.multipleFormGroupingMessages.add))
        .find(".vrtx-add-button").data({'number': i});
    }
     
    // TODO: avoid this being hardcoded here
    var syllabusItems = $("#editor.vrtx-syllabus #items");
    wrapItemsLeftRight(syllabusItems.find(".vrtx-json-element"), ".author, .title, .year, .publisher, .isbn, .comment", ".linktext, .link, .bibsys, .fulltext, .articles");
    syllabusItems.find(".author input, .title input").addClass("header-populators");
    syllabusItems.find(".vrtx-html textarea").addClass("header-fallback-populator");
        
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
            htmlTemplate += vrtxEditor.mustacheFacade.getDropdown(types[i], inputFieldName);
          } else {
            htmlTemplate += vrtxEditor.mustacheFacade.getStringField(types[i], inputFieldName);
          }
          break;
        case "html":
          htmlTemplate += vrtxEditor.mustacheFacade.getHtmlField(types[i], inputFieldName);
          break;
        case "simple_html":
          htmlTemplate += vrtxEditor.mustacheFacade.getHtmlField(types[i], inputFieldName);
          break;
        case "boolean":
          htmlTemplate += vrtxEditor.mustacheFacade.getBooleanField(types[i], inputFieldName);
          break;
        case "image_ref":
          htmlTemplate += vrtxEditor.mustacheFacade.getImageRef(types[i], inputFieldName);
          break;
        case "resource_ref":
          htmlTemplate += vrtxEditor.mustacheFacade.getResourceRef(types[i], inputFieldName);
          break;
        case "datetime":
          htmlTemplate += vrtxEditor.mustacheFacade.getDateField(j.a[i], inputFieldName);
          break;
        case "media":
          htmlTemplate += vrtxEditor.mustacheFacade.getMediaRef(types[i], inputFieldName);
          break;
        default:
          break;
      }
    }
      
    // Move up, move down, remove
    var isImmovable = jsonParent && jsonParent.hasClass("vrtx-multiple-immovable");
    if(!isImmovable) {
      var moveDownButton = vrtxEditor.mustacheFacade.getJsonBoxesInteractionsButton('move-down', '&darr; ' + vrtxAdmin.multipleFormGroupingMessages.moveDown); 
      var moveUpButton = vrtxEditor.mustacheFacade.getJsonBoxesInteractionsButton('move-up', '&uarr; ' + vrtxAdmin.multipleFormGroupingMessages.moveUp);
    }
    var removeButton = vrtxEditor.mustacheFacade.getJsonBoxesInteractionsButton('remove', vrtxAdmin.multipleFormGroupingMessages.remove);
      
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
      wrapItemsLeftRight(lastSyllabusItem, ".author, .title, .year, .publisher, .isbn, .comment", ".linktext, .link, .bibsys, .fulltext, .articles");
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
        vrtxEditor.newEditor(inputFieldName, false, false, requestLang, cssFileList, "true");
      } else if (types[i].type == "html") {
        vrtxEditor.newEditor(inputFieldName, true,  false, requestLang, cssFileList, "false");
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
    
/* NOTE: can be used generally if boolean hasScrollAnim is turned on */
    
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

/**
 * Mustache facade (JSON-input=>Mustache=>HTML)
 * @namespace
 */
VrtxEditor.prototype.mustacheFacade = {
  getMultipleInputfieldsInteractionsButton: function(clazz, name, idstr, text) {
    return $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["button"], { type: clazz, name: name, 
                                                                                        idstr: idstr, buttonText: text });
  },
  getMultipleInputFieldsAddButton: function(name, size, isBrowsable, isMovable, isDropdown) {
    return $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["add-button"], {
	                  name: name, size: size, isBrowsable: isBrowsable, isMovable: isMovable,
	                  isDropdown: isDropdown, buttonText: vrtxAdmin.multipleFormGroupingMessages.add });
  },
  getMultipleInputfield: function(name, idstr, i, value, size, browseButton, removeButton, moveUpButton, moveDownButton, isDropdown) {
    return $.mustache(vrtxEditor.multipleCommaSeperatedInputFieldTemplates["multiple-inputfield"], { idstr: idstr, i: i, value: value, 
                                                                                                     size: size, browseButton: browseButton,
                                                                                                     removeButton: removeButton, moveUpButton: moveUpButton,
                                                                                                     moveDownButton: moveDownButton, isDropdown: isDropdown,
                                                                                                     dropdownArray: "dropdown" + name });
  },
  getJsonBoxesInteractionsButton: function(clazz, text) {
    return $.mustache(TEMPLATES["add-remove-move"], { clazz: clazz, buttonText: text });	
  },
  getStringField: function(elem, inputFieldName) {
    return $.mustache(TEMPLATES["string"], { classes: "vrtx-string" + " " + elem.name,
                                             elemTitle: elem.title,
                                             inputFieldName: inputFieldName }); 
  }, 
  getHtmlField: function(elem, inputFieldName) {
    var baseclass = "vrtx-html";
    if (elem.type == "simple_html") {
      baseclass = "vrtx-simple-html";
    }
    return $.mustache(TEMPLATES["html"], { classes: baseclass + " " + elem.name,
                                           elemTitle: elem.title,
                                           inputFieldName: inputFieldName }); 
  },
  getBooleanField: function(elem, inputFieldName) {
    return $.mustache(TEMPLATES["radio"], { elemTitle: elem.title,
                                            inputFieldName: inputFieldName }); 
  },
  getDropdown: function(elem, inputFieldName) {
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
  },
  getDateField: function(elem, inputFieldName) {
    return $.mustache(TEMPLATES["date"], { elemTitle: elem.title,
                                           inputFieldName: inputFieldName }); 
  },
  getImageRef: function(elem, inputFieldName) {
    return $.mustache(TEMPLATES["browse-images"], { clazz: 'vrtx-image-ref',
                                                    elemTitle: elem.title,
                                                    inputFieldName: inputFieldName,
                                                    baseCKURL: vrtxAdmin.multipleFormGroupingPaths.baseCKURL,
                                                    baseFolderURL: vrtxAdmin.multipleFormGroupingPaths.baseFolderURL,
                                                    basePath: vrtxAdmin.multipleFormGroupingPaths.basePath,
                                                    browseButtonText: vrtxAdmin.multipleFormGroupingMessages.browseImages,
                                                    type: '',
                                                    size: 30,
                                                    previewTitle: browseImagesPreview,
                                                    previewNoImageText: browseImagesNoPreview }); 
  },
  getResourceRef: function(elem, inputFieldName) {
    return $.mustache(TEMPLATES["browse"], { clazz: 'vrtx-resource-ref',
                                             elemTitle: elem.title,
                                             inputFieldName: inputFieldName,
                                             baseCKURL: vrtxAdmin.multipleFormGroupingPaths.baseCKURL,
                                             baseFolderURL: vrtxAdmin.multipleFormGroupingPaths.baseFolderURL,
                                             basePath: vrtxAdmin.multipleFormGroupingPaths.basePath,
                                             browseButtonText: vrtxAdmin.multipleFormGroupingMessages.browseImages,
                                             type: 'File',
                                             size: 40 }); 
  },
  getMediaRef: function(elem, inputFieldName) {      
    return $.mustache(TEMPLATES["browse"], { clazz: 'vrtx-media-ref',
                                             elemTitle: elem.title,
                                             inputFieldName: inputFieldName,
                                             baseCKURL: vrtxAdmin.multipleFormGroupingPaths.baseCKURL,
                                             baseFolderURL: vrtxAdmin.multipleFormGroupingPaths.baseFolderURL,
                                             basePath: vrtxAdmin.multipleFormGroupingPaths.basePath,
                                             browseButtonText: vrtxAdmin.multipleFormGroupingMessages.browseImages,
                                             type: 'Media',
                                             size: 30 }); 
  }
}

/*-------------------------------------------------------------------*\
    9. Accordion grouping
\*-------------------------------------------------------------------*/

/**
 * Initialize grouped as accordion
 * @this {VrtxEditor}
 */
VrtxEditor.prototype.accordionGroupedInit = function accordionGroupedInit(subGroupedSelector) { /* param name pending */
  var vrtxEdit = this, _$ = vrtxAdmin._$;

  var accordionWrpId = "accordion-grouped"; // TODO: multiple accordion group pr. page
  var groupedSelector = ".vrtx-grouped, .vrtx-pseudo-grouped" + ((typeof subGroupedSelector !== "undefined") ? subGroupedSelector : "");

  // Because accordion needs one content wrapper
  for(var grouped = vrtxEdit.editorForm.find(groupedSelector), i = grouped.length; i--;) {
    var group = _$(grouped[i]);
    if(group.hasClass("vrtx-pseudo-grouped")) {
      group.find("> label").wrap("<div class='header' />");
      group.addClass("vrtx-grouped");
    } else {
      group.find("> *:not(.header)").wrapAll("<div />");
    }
  }
  // Initialize accordion
  grouped.wrapAll("<div id='" + accordionWrpId + "' />");
  vrtxEdit.editorForm.find("#" + accordionWrpId).accordion({ header: "> div > .header",
                                                             autoHeight: false,
                                                             collapsible: true,
                                                             active: false
                                                           });
};

/**
 * Close active grouped accordion if hidden
 * @this {VrtxEditor}
 */
VrtxEditor.prototype.accordionGroupedCloseActiveHidden = function accordionGroupedCloseActiveHidden() {
  var vrtxEdit = this, _$ = vrtxAdmin._$;

  var accordionWrp = vrtxEdit.editorForm.find("#accordion-grouped");
  var active = accordionWrp.find(".ui-state-active");
  if(active.length && active.filter(":hidden").length) {
    accordionWrp.accordion("activate", false);
  }
};

function accordionJsonRefresh(elem, active) {
  elem.accordion("destroy").accordion({
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
 * XXX: Should be chainable / jQuery fn
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

/**
 * Handler for events and init code applying a callback function with parameters
 * 
 * - If no parameters are provided then $(selector) is used as default
 * - 'this' in the callback is vrtxEditor with its prototype chain
 * 
 * @example
 * // Process special list links
 * vrtxEditor.initEventHandler("#list a.special", {
 *   callback: processSpecialListLinksFn
 * });
 *
 * @this {VrtxEditor}
 * @param {string} selector The selector
 * @param {object} opts The options 
 */
VrtxEditor.prototype.initEventHandler = function initEventHandler(selector, opts) {
  var select = $(selector);
  if(!select.length) return;

  opts.event = opts.event || "click";
  opts.wrapper = opts.wrapper || document;
  opts.callbackParams = opts.callbackParams || [$(selector)];
  opts.callbackChange = opts.callbackChange || function(p){};
  
  var vrtxEdit = this;

  opts.callback.apply(vrtxEdit, opts.callbackParams, true);
  $(opts.wrapper).on(opts.event, select, function () {
    opts.callback.apply(vrtxEdit, opts.callbackParams, false);
  });
};

VrtxEditor.prototype.initStudyDocTypes = function initStudyDocTypes() {
  var vrtxEdit = this;

  if(vrtxEdit.editorForm.hasClass("vrtx-hvordan-soke")) {
    vrtxEdit.accordionGroupedInit();
  } else if(vrtxEdit.editorForm.hasClass("vrtx-course-description")) {
    setShowHideBooleanNewEditor("course-fee", "div.course-fee-amount", false);
    vrtxEdit.accordionGroupedInit();  
  } else if(vrtxEdit.editorForm.hasClass("vrtx-semester-page")) {
    setShowHideBooleanNewEditor("cloned-course", "div.cloned-course-code", false);
    vrtxEdit.accordionGroupedInit("[class*=link-box]");  
  } else if(vrtxEdit.editorForm.hasClass("vrtx-samlet-program")) {
    var samletElm = vrtxEdit.editorForm.find(".samlet-element");
    vrtxEdit.replaceTag(samletElm, "h6", "strong");
    vrtxEdit.replaceTag(samletElm, "h5", "h6");  
    vrtxEdit.replaceTag(samletElm, "h4", "h5");
    vrtxEdit.replaceTag(samletElm, "h3", "h4");
    vrtxEdit.replaceTag(samletElm, "h2", "h3");
    vrtxEdit.replaceTag(samletElm, "h1", "h2");
  }
};

VrtxEditor.prototype.initSendToApproval = function initSendToApproval() {
  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;
  
  // Send to approval
  // TODO: generalize dialog jQuery UI function with AJAX markup/text
  // XXX: HTML content should set height (not hardcoded)
  _$(document).on("click", "#vrtx-send-to-approval, #vrtx-send-to-approval-global", function (e) {
    var link = this;
    var id = link.id + "-content";
    var dialogManageCreate = _$("#" + id);
    if (!dialogManageCreate.length) {
      vrtxAdm.serverFacade.getHtml(link.href, {
        success: function (results, status, resp) {
          _$("body").append("<div id='" + id + "'>" + _$(results).find("#contents").html() + "</div>");
          dialogManageCreate = _$("#" + id);
          dialogManageCreate.hide();
          var hasEmailFrom = dialogManageCreate.find("#emailFrom").length;
          vrtxSimpleDialogs.openHtmlDialog("send-approval", dialogManageCreate.html(), link.title, 410, (hasEmailFrom ? 620 : 545));
          var dialog = _$(".ui-dialog");
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
      var dialog = _$(".ui-dialog");
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
    var btn = _$(this);
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
};

VrtxEditor.prototype.initStickyBar = function initStickyBar() {
  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;

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
};

function wrapItemsLeftRight(items, leftItems, rightItems) {
  var len = items.length;
  if(len == 1) {
    items.find(leftItems).wrapAll("<div class='left' />");
    items.find(rightItems).wrapAll("<div class='right' />");
  } else if(len > 1) {
    var i = len;
    while(i--) {
      var item = $(items[i]);
      item.find(leftItems).wrapAll("<div class='left' />");
      item.find(rightItems).wrapAll("<div class='right' />");
    }
  }
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