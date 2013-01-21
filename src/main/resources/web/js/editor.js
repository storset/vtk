/*
 *  Vortex Editor
 *
 *  TODO: encapsulate in VrtxEditor
 *  TODO: JSDoc
 *  TODO: use RegExp where we need to match multiple text strings
 *
 */
 
/**
 * Creates an instance of VrtxEditor
 * @constructor
 */
function VrtxEditor() {
  var instance; // Class-like singleton pattern (p.145 JavaScript Patterns)
  VrtxEditor = function VrtxEditor() {
    return instance;
  };
  VrtxEditor.prototype = this;
  instance = new VrtxEditor();
  instance.constructor = VrtxEditor;
  
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
                          "examsemester":                        { "particular-semester":   ".examsemester-every-other-semester",
                                                                   "every-other":           ".if-examsemester-particular",
                                                                   "other":                 ".examsemester-other"                     },
                          "teaching-language":                   { "other":                 ".teaching-language-text-field"           }
                        };
  
  /** Check if this script is in admin or not */                      
  this.isInAdmin = typeof vrtxAdmin !== "undefined";
  
  return instance;
};

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

  CKEDITOR.replace(name, config);
};

function commentsCkEditor() {
  document.getElementById("comment-syntax-desc").style.display = "none";
  document.getElementById("comments-text-div").style.margin = "0";
  $("#comments-text").click(function () {
    vrtxEditor.setCKEditorConfig("comments-text", null, null, null, null, cssFileList, 150, 400, 40, vrtxEditor.CKEditorToolbars.commentsToolbar, false, true, null);
  });
}

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
        vrtxEdit.initAccordionGrouped();
        break;
      case "vrtx-course-description":
        setShowHide('course-fee', ["course-fee-amount"], false);
        vrtxEdit.initAccordionGrouped();
        break;
      case "vrtx-semester-page":
        vrtxEdit.initAccordionGrouped("[class*=link-box]")
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

/**
 * Initialize .vrtx-grouped elements as accordions
 *
 */
VrtxEditor.prototype.initAccordionGrouped = function initAccordionGrouped(subGroupedSelector) { /* param name pending */
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
 * Initialize CKEditors sync and async from CKEditorsInit array
 *
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

/* Store and check if inputfields or textareas (CK) have changed onbeforeunload */
$(window).load(function () { 
  /* XXX: Exit if not is in admin */
  if(!vrtxEditor.isInAdmin) return;
  
  var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;

  var nullDeferred = _$.Deferred();
      nullDeferred.resolve();
  _$.when(((typeof MANUALLY_APPROVE_INITIALIZED === "object") ? MANUALLY_APPROVE_INITIALIZED : nullDeferred),
          ((typeof MULTIPLE_INPUT_FIELD_INITIALIZED === "object") ? MULTIPLE_INPUT_FIELD_INITIALIZED : nullDeferred),
          ((typeof JSON_ELEMENTS_INITIALIZED === "object") ? JSON_ELEMENTS_INITIALIZED : nullDeferred),
          ((typeof DATE_PICKER_INITIALIZED === "object") ? DATE_PICKER_INITIALIZED : nullDeferred),
          ((typeof IMAGE_EDITOR_INITIALIZED === "object") ? IMAGE_EDITOR_INITIALIZED : nullDeferred)).done(function() {
    vrtxAdm.log({msg: "Editor initialized."});
    storeInitPropValues($("#contents")); // Store initial counts and values when all is initialized in editor
  });
  
  if (typeof CKEDITOR !== "undefined" && vrtxEditor.editorForm.length) { // XXX: Don't add event if not regular editor
    CKEDITOR.on('instanceReady', function() {
      _$(".cke_contents iframe").contents().find("body").bind('keydown', 'ctrl+s', function(e) {
        ctrlSEventHandler(_$, e);
      });
    });
  }
});

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
 */
 
/**
 * Select field show/hide with mappings
 *
 * @this {VrtxEditor}
 * @param {object} select The select field
 */
VrtxEditor.prototype.hideShowSelect = function hideShowSelect(select) {
  var vrtxEdit = this;
  var selected = select.val();
  var id = select.attr("id");
  if(vrtxEdit.selectMappings.hasOwnProperty(id)) {
    var mappings = vrtxEdit.selectMappings[id];
    for(var item in mappings) {
      if(item === selected) {
        vrtxEdit.editorForm.find(mappings[item]).filter(":hidden").show();
      } else {
        vrtxEdit.editorForm.find(mappings[item]).filter(":visible").hide();
      }
    }
  }
};

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

/* ^ Vortex Editor */
