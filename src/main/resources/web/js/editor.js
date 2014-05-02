/*
 *  Vortex Editor
 *
 *  ToC: 
 *
 *  1.  Config
 *  2.  DOM is ready
 *  3.  DOM is fully loaded
 *  4.  RichTextEditor (CKEditor)
 *  5.  Validation and change detection
 *  6.  Image preview
 *  7.  Enhancements
 *  8.  Multiple fields and boxes
 *  9.  Accordions
 *  10. Send to approval
 *  11. Utils
 */
 
/*-------------------------------------------------------------------*\
    1. Config
\*-------------------------------------------------------------------*/

/**
 * Creates an instance of VrtxEditor
 * @constructor
 */
function VrtxEditor() {
  /** The editor form */
  this.editorForm = null;

  /** Text input fields at init */
  this.editorInitInputFields = [];
  /** Select fields at init */
  this.editorInitSelects = [];
  /** Checkboxes at init */
  this.editorInitCheckboxes = [];
  /** Radios at init */
  this.editorInitRadios = [];

  /** Select fields show/hide mappings 
    * Mapping: "select-id": ["option-value-1", ..., "option-value-n"]
    */
  this.selectMappings = {
    "teachingsemester":  ["particular-semester", "every-other"],
    "examsemester":      ["particular-semester", "every-other"],
    "typeToDisplay":     ["so", "nm", "em"],
    "type-of-agreement": ["other"]
  };

  /** Initial state for the need to confirm navigation away from editor */
  this.needToConfirm = true;

  this.multipleFieldsBoxes = {}; /* Make sure every new field and box have unique id's (important for CK-fields) */
  
  /** These needs better names. */
  this.multipleFieldsBoxesTemplates = [];
  this.multipleFieldsBoxesDeferred = null;
  this.multipleFieldsBoxesAccordionSwitchThenScrollTo = null;

  this.multipleBoxesTemplatesContract = [];
  this.multipleBoxesTemplatesContractBuilt = null;

  /** Check if this script is in admin or not */
  this.isInAdmin = typeof vrtxAdmin !== "undefined";
}

var vrtxEditor = new VrtxEditor();
var UNSAVED_CHANGES_CONFIRMATION;

 // Accordion JSON and grouped
var accordionJson = null;
var accordionGrouped = null;


/*-------------------------------------------------------------------*\
    2. DOM is ready
\*-------------------------------------------------------------------*/

$(document).ready(function () {
  var vrtxEdit = vrtxEditor;
  vrtxEdit.editorForm = $("#editor");

  if (!vrtxEdit.isInAdmin || !vrtxEdit.editorForm.length) {
    vrtxEdit.richtextEditorFacade.setupMultiple(false);
    return; /* Exit if not is in admin or have regular editor */
  }
  
  vrtxAdmin.cacheDOMNodesForReuse();

  // Skip UI helper as first element in editor
  vrtxEdit.editorForm.find(".ui-helper-hidden").filter(":not(:last)").filter(":first").next().addClass("first");

  vrtxEdit.initPreviewImage();
  
  var waitALittle = setTimeout(function() {
    autocompleteUsernames(".vrtx-autocomplete-username");
    autocompleteTags(".vrtx-autocomplete-tag");
    vrtxEdit.initSendToApproval();
    var getScriptFn = (typeof $.cachedScript === "function") ? $.cachedScript : $.getScript;
    var futureStickyBar = (typeof VrtxStickyBar === "undefined") ? getScriptFn("/vrtx/__vrtx/static-resources/js/vrtx-sticky-bar.js") : $.Deferred().resolve();
    $.when(futureStickyBar).done(function() {     
      var editorStickyBar = new VrtxStickyBar({
        wrapperId: "#vrtx-editor-title-submit-buttons",
        stickyClass: "vrtx-sticky-editor-title-submit-buttons",
        contentsId: "#contents",
        outerContentsId: "#main"
      });
    });
  }, 15);

  vrtxEdit.initEnhancements();
  vrtxEdit.richtextEditorFacade.setupMultiple(true);
});


/*-------------------------------------------------------------------*\
    3. DOM is fully loaded
\*-------------------------------------------------------------------*/

$(window).load(function () {
  if (!vrtxEditor.isInAdmin) return; /* Exit if not is in admin */

  var vrtxAdm = vrtxAdmin,
    _$ = vrtxAdm._$;

  // Store initial counts and values when all is initialized in editor
  var nullDeferred = _$.Deferred();
  nullDeferred.resolve();
  _$.when(((typeof MANUALLY_APPROVE_INITIALIZED === "object") ? MANUALLY_APPROVE_INITIALIZED : nullDeferred),
          ((typeof MULTIPLE_INPUT_FIELD_INITIALIZED === "object") ? MULTIPLE_INPUT_FIELD_INITIALIZED : nullDeferred),
          ((typeof JSON_ELEMENTS_INITIALIZED === "object") ? JSON_ELEMENTS_INITIALIZED : nullDeferred),
          ((typeof DATE_PICKER_INITIALIZED === "object") ? DATE_PICKER_INITIALIZED : nullDeferred),
          ((typeof IMAGE_EDITOR_INITIALIZED === "object") ? IMAGE_EDITOR_INITIALIZED : nullDeferred)).done(function () {
    vrtxAdm.log({ msg: "Editor initialized." });
    storeInitPropValues(vrtxAdm.cachedContent);
  });

  // CTRL+S save inside editors
  if (typeof CKEDITOR !== "undefined" && vrtxEditor.editorForm.length) { // Don't add event if not regular editor
    vrtxEditor.richtextEditorFacade.setupCTRLS();
  }
});


/*-------------------------------------------------------------------*\
    4. RichTextEditor (CKEditor)
\*-------------------------------------------------------------------*/

/**
 * RichTextEditor facade
 *
 * Uses CKEditor
 *
 * @namespace
 */
VrtxEditor.prototype.richtextEditorFacade = {
  toolbars: {},
  divContainerStylesSet: [{}],
  editorsForInit: [],
  initSyncMax: 15,
  initAsyncInterval: 15,
 /**
  * Setup multiple instances
  * @this {richtextEditorFacade}
  */
  setupMultiple: function(isInAdmin) {
    if(isInAdmin) this.setupMaximizeMinimize();
  
    for (var i = 0, len = this.editorsForInit.length; i < len && i < this.initSyncMax; i++) { // Initiate <=CKEditorsInitSyncMax CKEditors sync
      this.setup(this.editorsForInit[i]);
    }
    if (len > this.initSyncMax) {
      var rteFacade = this;
      var richTextEditorsInitLoadTimer = setTimeout(function () { // Initiate >CKEditorsInitSyncMax CKEditors async
        rteFacade.setup(rteFacade.editorsForInit[i]);
        i++;
        if (i < len) {
          setTimeout(richTextEditorsInitLoadTimer, rteFacade.initAsyncInterval);
        }
      }, rteFacade.initAsyncInterval);
    }
  },
  /**
   * Setup instance config
   *
   * @this {richtextEditorFacade}
   * @param {object} opts The options
   * @param {string} opts.name Name of textarea
   * @param {boolean} opts.isCompleteEditor Use complete toolbar
   * @param {boolean} opts.isWithoutSubSuper Don't display sub and sup buttons in toolbar
   * @param {string} opts.defaultLanguage Language in editor
   * @param {array} opts.cssFileList List of CSS-files to style content in editor
   * @param {string} opts.simple Make h1 format available (for old document types)
   */
  setup: function(opts) {
    var vrtxEdit = vrtxEditor,
        baseUrl = vrtxAdmin.multipleFormGroupingPaths.baseBrowserURL,
        baseFolder = vrtxAdmin.multipleFormGroupingPaths.baseFolderURL,
        browsePath = vrtxAdmin.multipleFormGroupingPaths.basePath;

    // File browser
    var linkBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=' + browsePath;
    var imageBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=' + browsePath;
    var flashBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=' + browsePath;

    // Classify
    var classification = vrtxEdit.classifyEditorInstance(opts);

    // Initialize
    this.init({
      name: opts.name,
      linkBrowseUrl: linkBrowseUrl,
      imageBrowseUrl: classification.isMain ? imageBrowseUrl : null,
      flashBrowseUrl: classification.isMain ? flashBrowseUrl : null,
      defaultLanguage: opts.defaultLanguage, 
      cssFileList: opts.cssFileList,
      height: vrtxEdit.setupEditorHeight(classification, opts),
      maxHeight: vrtxEdit.setupEditorMaxHeight(classification, opts),
      minHeight: opts.isCompleteEditor ? 50 : 40,
      toolbar: vrtxEdit.setupEditorToolbar(classification, opts),
      complete: classification.isMain,
      requiresStudyRefPlugin: classification.requiresStudyRefPlugin,
      resizable: vrtxEdit.setupEditorResizable(classification, opts),
      baseDocumentUrl: classification.isMessage ? null : vrtxAdmin.multipleFormGroupingPaths.baseDocURL,
      isSimple: classification.isSimple,
      isFrontpageBox: classification.isFrontpageBox
    });

  },
  /**
   * Initialize instance with config
   *
   * @this {richtextEditorFacade}
   * @param {object} opts The config
   * @param {string} opts.name Name of textarea
   * @param {string} opts.linkBrowseUrl Link browse integration URL
   * @param {string} opts.imageBrowseUrl Image browse integration URL
   * @param {string} opts.flashBrowseUrl Flash browse integration URL
   * @param {string} opts.defaultLanguage Language in editor 
   * @param {string} opts.cssFileList List of CSS-files to style content in editor
   * @param {number} opts.height Height of editor
   * @param {number} opts.maxHeight Max height of editor
   * @param {number} opts.minHeight Min height of editor
   * @param {object} opts.toolbar The toolbar config
   * @param {string} opts.complete Use complete toolbar
   * @param {boolean} opts.resizable Possible to resize editor
   * @param {string} opts.baseDocumentUrl URL to current document 
   * @param {string} opts.isSimple Make h1 format available (for old document types)
   * @param {string} opts.isFrontpageBox Make h2 format unavailable (for frontpage boxes)
   */
  init: function(opts) {
    var config = {};

    config.baseHref = opts.baseDocumentUrl;
    config.contentsCss = opts.cssFileList;
    config.entities = false;
    
    if (opts.linkBrowseUrl) {
      config.filebrowserBrowseUrl = opts.linkBrowseUrl;
      config.filebrowserImageBrowseLinkUrl = opts.linkBrowseUrl;
    }

    if (opts.complete) {
      config.filebrowserImageBrowseUrl = opts.imageBrowseUrl;
      config.filebrowserFlashBrowseUrl = opts.flashBrowseUrl;
      if(opts.requiresStudyRefPlugin) {
        config.extraPlugins = 'mediaembed,studyreferencecomponent,htmlbuttons,button-h2,button-h3,button-h4,button-h5,button-h6,button-normal';
      } else {
        config.extraPlugins = 'mediaembed,htmlbuttons,button-h2,button-h3,button-h4,button-h5,button-h6,button-normal';
      }
      config.stylesSet = this.divContainerStylesSet;
      if (opts.isSimple) { // XHTML
        config.format_tags = 'p;h1;h2;h3;h4;h5;h6;pre;div';
      } else {
        config.format_tags = 'p;h2;h3;h4;h5;h6;pre;div';
      }    
    } else {
      config.removePlugins = 'elementspath';
    }
  
    //  if (opts.isFrontpageBox) {
    //	config.format_tags = 'p;h3;h4;h5;h6;pre;div';
    //  }

    config.resize_enabled = opts.resizable;
    config.toolbarCanCollapse = false;
    config.defaultLanguage = 'no';
    if(opts.defaultLanguage) {
      config.language = opts.defaultLanguage;
    }
    config.toolbar = opts.toolbar;
    config.height = opts.height + 'px';
    config.autoGrow_maxHeight = opts.maxHeight + 'px';
    config.autoGrow_minHeight = opts.minHeight + 'px';

    config.forcePasteAsPlainText = false;
    config.disableObjectResizing = true;
    config.disableNativeSpellChecker = false;
    config.allowedContent = true;
    config.linkShowTargetTab = false;

    // Key strokes
    config.keystrokes = [
      [ CKEDITOR.CTRL + 50 /*2*/, 'button-h2' ],
      [ CKEDITOR.CTRL + 51 /*3*/, 'button-h3' ],
      [ CKEDITOR.CTRL + 52 /*4*/, 'button-h4' ],
      [ CKEDITOR.CTRL + 53 /*5*/, 'button-h5' ],
      [ CKEDITOR.CTRL + 54 /*6*/, 'button-h6' ],
      [ CKEDITOR.CTRL + 49 /*0*/, 'button-normal' ]
    ];

    // Tag formatting in source
    var rteFacade = this;
    config.on = {
      instanceReady: function (ev) {
        rteFacade.setupTagsFormatting(this, ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'], false);
        rteFacade.setupTagsFormatting(this, ['ol', 'ul', 'li'], true);
      }
    };
  
    if (!this.isInstance(opts.name)) {
      CKEDITOR.replace(opts.name, config);
    }
  },
  /**
   * Setup instance tags formatting
   *
   * @this {richTextEditorFacade}
   * @param {object} instance CKEditor instance
   * @param {array} tags Tags
   * @param {bool} isIndented If they should be indented
   */
  setupTagsFormatting: function(instance, tags, isIndented) {
    for (key in tags) {
      instance.dataProcessor.writer.setRules(tags[key], {
        indent: isIndented,
        breakBeforeOpen: true,
        breakAfterOpen: false,
        breakBeforeClose: false,
        breakAfterClose: true
      });
    }
  },
  setupCTRLS: function() {
    CKEDITOR.on('instanceReady', function (event) {
      _$(".cke_contents iframe").contents().find("body").bind('keydown', 'ctrl+s', $.debounce(150, true, function (e) {
        ctrlSEventHandler(_$, e);
      }));
      // Fix bug (http://dev.ckeditor.com/ticket/9958) with IE triggering onbeforeunload on dialog click
      event.editor.on('dialogShow', function(dialogShowEvent) {
        if(CKEDITOR.env.ie) {
          $(dialogShowEvent.data._.element.$).find('a[href*="void(0)"]').removeAttr('href');
        }
      });
    });
  },
  setupMaximizeMinimize: function() {
    vrtxAdmin.cachedAppContent.on("click", ".cke_button__maximize.cke_button_on", this.maximize);
    vrtxAdmin.cachedAppContent.on("click", ".cke_button__maximize.cke_button_off", this.minimize);
  },
  maximize: function() {
    var vrtxAdm = vrtxAdmin,
        _$ = vrtxAdm._$;
  
    var stickyBar = _$("#vrtx-editor-title-submit-buttons");
    stickyBar.hide();
    
    vrtxAdm.cachedBody.addClass("forms-new");
    vrtxAdm.cachedBody.addClass("js");

    var ckInject = _$(this).closest(".cke_reset")
                           .find(".cke_toolbar_end:last");

    if (!ckInject.find("#editor-help-menu").length) {
      var shortcuts = stickyBar.find(".submit-extra-buttons");
      var save = shortcuts.find("#vrtx-save").html();
      var helpMenu = "<div id='editor-help-menu' class='js-on'>" + shortcuts.find("#editor-help-menu").html() + "</div>";
      ckInject.append("<div class='ck-injected-save-help'>" + save + helpMenu + "</div>");

      // Fix markup
      var saveInjected = ckInject.find(".ck-injected-save-help > a");
      if (!saveInjected.hasClass("vrtx-button")) {
        saveInjected.addClass("vrtx-button");
      } else {
        saveInjected.removeClass("vrtx-focus-button");
      }
    } else {
      ckInject.find(".ck-injected-save-help").show();
    }
  },
  minimize: function() {
    var _$ = vrtxAdmin._$;
    var stickyBar = _$("#vrtx-editor-title-submit-buttons");
    stickyBar.show();
    var ckInject = _$(this).closest(".cke_reset").find(".ck-injected-save-help").hide();
  },
  getInstanceValue: function(name) {
    var inst = this.getInstance(name);
    return inst !== null ? inst.getData() : null;
  },
  updateInstances: function() {
    for (var instance in CKEDITOR.instances) {
      CKEDITOR.instances[instance].updateElement();
    }
  },
  getValue: function(instance) {
    return instance.getData();
  },
  setInstanceValue: function(name, data) {
    var inst = this.getInstance(name);
    if (inst !== null && data !== null) {
      inst.setData(data);
    }
  },
  isInstance: function(name) {
    return this.getInstance(name) !== null;
  },
  isChanged: function(instance) {
    return instance.checkDirty(); 
  },
  getInstance: function(name) {
    return CKEDITOR.instances[name] || null;
  },
  removeInstance: function(name) {
    if (this.isInstance(name)) {
      var ckInstance = this.getInstance(name);
      ckInstance.destroy();
      if (this.isInstance(name)) { /* Just in case not removed */
        this.deleteInstance(name);
      }
    }
  },
  deleteInstance: function(name) {
    delete CKEDITOR.instances[name];
  },
  swap: function(nameA, nameB) {
    var rteFacade = this;
    var waitAndSwap = setTimeout(function () {
      var ckInstA = rteFacade.getInstance(nameA);
      var ckInstB = rteFacade.getInstance(nameB);
      var ckValA = ckInstA.getData();
      var ckValB = ckInstB.getData();
      ckInstA.setData(ckValB, function () {
        ckInstB.setData(ckValA);
      });
    }, 10);
  }
}

/* Toolbars */

vrtxEditor.richtextEditorFacade.toolbars.inlineToolbar = [
  ['Source', 'PasteText', 'Link', 'Unlink', 'Bold', 'Italic', 'Strike', 'Subscript', 'Superscript', 'SpecialChar']
];

vrtxEditor.richtextEditorFacade.toolbars.withoutSubSuperToolbar = [
  ['Source', 'PasteText', 'Link', 'Unlink', 'Bold', 'Italic', 'Strike', 'SpecialChar']
];

vrtxEditor.richtextEditorFacade.toolbars.commentsToolbar = [
  ['Source', 'PasteText', 'Bold', 'Italic', 'Strike', 'NumberedList', 'BulletedList', 'Link', 'Unlink']
];

vrtxEditor.richtextEditorFacade.toolbars.completeToolbar = [
  ['PasteText', 'PasteFromWord', '-', 'Undo', 'Redo'], ['Replace'], ['Link', 'Unlink', 'Anchor'],
  ['Image', 'CreateDiv', 'MediaEmbed', 'Table', 'HorizontalRule', 'SpecialChar'],
  ['Maximize'], ['Source'], '/', ['Format'], 
  ['Bold', 'Italic', 'Strike', 'Subscript', 'Superscript', 'TextColor', '-', 'RemoveFormat'],
  ['NumberedList', 'BulletedList', '-', 'Outdent', 'Indent', '-', 'Blockquote']
];

vrtxEditor.richtextEditorFacade.toolbars.studyToolbar = [
  ['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
   'RemoveFormat', '-', 'Link', 'Unlink', 'Studyreferencecomponent', 'Anchor',
   'Image', 'CreateDiv', 'MediaEmbed', 'Table', 'Studytable', 'HorizontalRule', 'SpecialChar'],
  ['Format', 'Bold', 'Italic', 'Subscript', 'Superscript', 'NumberedList', 'BulletedList', 'Outdent', 'Indent', 'Maximize']
];

vrtxEditor.richtextEditorFacade.toolbars.studyRefToolbar = [
  ['Source', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo', '-', 'Replace',
   'RemoveFormat', '-', 'Link', 'Unlink', 'Studyreferencecomponent', 'Anchor',
   'Image', 'CreateDiv', 'MediaEmbed', 'Table', 'HorizontalRule', 'SpecialChar'],
  ['Format', 'Bold', 'Italic', 'Subscript', 'Superscript', 'NumberedList', 'BulletedList', 'Outdent', 'Indent', 'Maximize']
];

vrtxEditor.richtextEditorFacade.toolbars.messageToolbar = [
  ['Source', 'PasteText', 'Bold', 'Italic', 'Strike', '-', 'Undo', 'Redo', '-', 'Link',
   'Unlink', 'Subscript', 'Superscript', 'NumberedList', 'BulletedList', 'Outdent', 'Indent']
];

/* Div containers */

vrtxEditor.richtextEditorFacade.divContainerStylesSet = [
  { name: 'Facts left',                 element: 'div', attributes: { 'class': 'vrtx-facts-container vrtx-container-left'  } },
  { name: 'Facts right',                element: 'div', attributes: { 'class': 'vrtx-facts-container vrtx-container-right' } },
  { name: 'Image left',                 element: 'div', attributes: { 'class': 'vrtx-img-container vrtx-container-left'    } },
  { name: 'Image center',               element: 'div', attributes: { 'class': 'vrtx-img-container vrtx-container-middle vrtx-img-container-middle-ie' } },
  { name: 'Image right',                element: 'div', attributes: { 'class': 'vrtx-img-container vrtx-container-right' } },
  { name: 'Img & capt left (800px)',    element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xxl vrtx-container-left' } },
  { name: 'Img & capt left (700px)',    element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xl vrtx-container-left' } },
  { name: 'Img & capt left (600px)',    element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-l vrtx-container-left' } },
  { name: 'Img & capt left (500px)',    element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-m vrtx-container-left' } },
  { name: 'Img & capt left (400px)',    element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-s vrtx-container-left' } },
  { name: 'Img & capt left (300px)',    element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xs vrtx-container-left' } },
  { name: 'Img & capt left (200px)',    element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xxs vrtx-container-left' } },
  { name: 'Img & capt center (full)',   element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-full vrtx-container-middle' } },
  { name: 'Img & capt center (800px)',  element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xxl vrtx-container-middle' } },
  { name: 'Img & capt center (700px)',  element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xl vrtx-container-middle' } },
  { name: 'Img & capt center (600px)',  element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-l vrtx-container-middle' } },
  { name: 'Img & capt center (500px)',  element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-m vrtx-container-middle' } },
  { name: 'Img & capt center (400px)',  element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-s vrtx-container-middle' } },
  { name: 'Img & capt right (800px)',   element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xxl vrtx-container-right' } },
  { name: 'Img & capt right (700px)',   element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xl vrtx-container-right' } },
  { name: 'Img & capt right (600px)',   element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-l vrtx-container-right' } },
  { name: 'Img & capt right (500px)',   element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-m vrtx-container-right' } },
  { name: 'Img & capt right (400px)',   element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-s vrtx-container-right' } },
  { name: 'Img & capt right (300px)',   element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xs vrtx-container-right' } },
  { name: 'Img & capt right (200px)',   element: 'div', attributes: { 'class': 'vrtx-container vrtx-container-size-xxs vrtx-container-right' } }
];

/* Functions for generating editor config based on classification
 *
 * TODO: any better way to write this short and concise
 */

VrtxEditor.prototype.setupEditorHeight = function setupEditorHeight(c, opts) {
  return opts.isCompleteEditor ? ((c.isContent || c.isCourseGroup) ? 400 : (c.isSupervisorBox ? 130 : (c.isCourseDescriptionB ? 200 : 220)))
                               : (c.isMessage ? 250
                                              : (c.isCaption ? 55 
                                                             : ((c.isStudyField || c.isScheduleComment || c.isAdditionalContent) ? 150 
                                                                                                                                 : (c.isIntro ? 100 
                                                                                                                                              : 90))))
};

VrtxEditor.prototype.setupEditorMaxHeight = function setupEditorMaxHeight(c, opts) {
  return (c.isContent || c.isCourseGroup) ? 800 : (c.isSupervisorBox ? 300 : 400);
};

VrtxEditor.prototype.setupEditorToolbar = function setupEditorToolbar(c, opts) {
  var tb = vrtxEditor.richtextEditorFacade.toolbars;
  return classification.isMain ? ((c.isCourseDescriptionB || c.isCourseGroup) ? tb.studyRefToolbar 
                                                                              : (c.isStudyContent ? tb.studyToolbar
                                                                                                  : tb.completeToolbar))
                               : (c.isMessage ? tb.messageToolbar
                                              : (c.isStudyField ? tb.studyToolbar 
                                                                : ((c.isIntro || c.isCaption || c.isScheduleComment) ? tb.inlineToolbar
                                                                                                                     : tb.withoutSubSuperToolbar)));
};

VrtxEditor.prototype.setupEditorResizable = function setupEditorResizable(c, opts) {
  return classification.isMain || !(c.isCourseDescriptionA || c.isIntro || c.isCaption || c.isMessage || c.isStudyField || c.isScheduleComment);
};

/**
 * Classify editor based on its name
 *
 * @this {VrtxEditor}
 * @param {object} opts Config
 * @return {object} The classification with booleans
 */
VrtxEditor.prototype.classifyEditorInstance = function classifyEditorInstance(opts) {
  var vrtxEdit = this,
      name = opts.name;
      classification = {};

  // Content
  classification.isOldContent = name == "resource.content";
  classification.isStudyContent = name == "content-study";
  classification.isContent = name == "content" ||
                             classification.isOldContent ||
                             classification.isStudyContent;
  classification.isSimple = classification.isOldContent && opts.simple;
  classification.isFrontpageBox = vrtxEdit.editorForm.hasClass("vrtx-frontpage");
                             
  // Additional-content                  
  classification.isAdditionalContent = vrtxEdit.contains(name, "additional-content") ||
                                       vrtxEdit.contains(name, "additionalContents");
                                       
  classification.isMain = opts.isCompleteEditor || classification.isAdditionalContent;                   
  
  // Introduction / caption / sp.box
  classification.isIntro = vrtxEdit.contains(name, "introduction") ||
                           vrtxEdit.contains(name, "resource.description") ||
                           vrtxEdit.contains(name, "resource.image-description") ||
                           vrtxEdit.contains(name, "resource.video-description") ||
                           vrtxEdit.contains(name, "resource.audio-description");
  classification.isCaption = vrtxEdit.contains(name, "caption");
  classification.isMessage = vrtxEdit.contains(name, "message");
  classification.isSupervisorBox = vrtxEdit.contains("supervisor-box");
  
  // Studies
  classification.isStudyField = vrtxEdit.contains(name, "frist-frekvens-fri") ||
                                vrtxEdit.contains(name, "metode-fri") ||
                                vrtxEdit.contains(name, "internasjonale-sokere-fri") ||
                                vrtxEdit.contains(name, "nordiske-sokere-fri") ||
                                vrtxEdit.contains(name, "opptakskrav-fri") ||
                                vrtxEdit.contains(name, "generelle-fri") ||
                                vrtxEdit.contains(name, "spesielle-fri") ||
                                vrtxEdit.contains(name, "politiattest-fri") ||
                                vrtxEdit.contains(name, "rangering-sokere-fri") ||
                                vrtxEdit.contains(name, "forstevitnemal-kvote-fri") ||
                                vrtxEdit.contains(name, "ordinar-kvote-alle-kvalifiserte-fri") ||
                                vrtxEdit.contains(name, "innpassing-tidl-utdanning-fri") ||
                                vrtxEdit.contains(name, "regelverk-fri") ||
                                vrtxEdit.contains(name, "description-en") ||
                                vrtxEdit.contains(name, "description-nn") ||
                                vrtxEdit.contains(name, "description-no");
  classification.isScheduleComment = vrtxEdit.contains(name, "comment") && vrtxEdit.editorForm.hasClass("vrtx-schedule");
  classification.isCourseDescriptionA = name == "teachingsemester-other" ||
                                        name == "examsemester-other" ||
                                        name == "teaching-language-text-field" ||
                                        name == "eksamensspraak-text-field" ||
                                        name == "sensur-text-field" ||
                                        name == "antall-forsok-trekk-text-field" ||
                                        name == "tilrettelagt-eksamen-text-field";
  classification.isCourseDescriptionB = name == "course-content" ||
                                        name == "learning-outcomes" ||
                                        name == "opptak-og-adgang-text-field" ||
                                        name == "ikke-privatist-text-field" ||
                                        name == "obligatoriske-forkunnskaper-text-field" ||
                                        name == "recommended-prerequisites-text-field" ||
                                        name == "overlapping-courses-text-field" ||
                                        name == "teaching-text-field" ||
                                        name == "adgang-text-field" ||
                                        name == "assessment-and-grading" ||
                                        name == "hjelpemidler-text-field" ||
                                        name == "klage-text-field" ||
                                        name == "ny-utsatt-eksamen-text-field" ||
                                        name == "evaluering-av-emnet-text-field" ||
                                        name == "other-text-field";
  classification.isCourseGroup = name == "course-group-about" ||
                                 name == "courses-in-group" ||
                                 name == "course-group-admission" ||
                                 name == "relevant-study-programmes" ||
                                 name == "course-group-other";       
                                 
  classification.requiresStudyRefPlugin = classification.isStudyContent || classification.isCourseDescriptionB || classification.isCourseGroup || classification.isStudyField;
         
  return classification;
};


/*-------------------------------------------------------------------*\
    5. Validation and change detection
\*-------------------------------------------------------------------*/

function storeInitPropValues(contents, filterIn) {
  if (!contents.length) return;

  var vrtxEdit = vrtxEditor;

  var inputFields = contents.find("input").not("[type=submit]").not("[type=button]")
                            .not("[type=checkbox]").not("[type=radio]");
  var selects = contents.find("select");
  var checkboxes = contents.find("input[type=checkbox]:checked");
  var radioButtons = contents.find("input[type=radio]:checked");
  
  if(typeof filterIn !== "undefined") {
    inputFields = inputFields.filter(filterIn);
    selects = selects.filter(filterIn);
    checkboxes = checkboxes.filter(filterIn);
    radioButtons = radioButtons.filter(filterIn);
  }
  
  var len1 = vrtxEdit.editorInitInputFields.length;
  for (var i = 0, len = inputFields.length; i < len; i++) {
    vrtxEdit.editorInitInputFields[len1+i] = inputFields[i].value;
  }
  var len2 = vrtxEdit.editorInitSelects.length;
  for (i = 0, len = selects.length; i < len; i++) {
    vrtxEdit.editorInitSelects[len2+i] = selects[i].value;
  }
  var len3 = vrtxEdit.editorInitCheckboxes.length;
  for (i = 0, len = checkboxes.length; i < len; i++) {
    vrtxEdit.editorInitCheckboxes[len3+i] = checkboxes[i].name;
  }
  var len4 = vrtxEdit.editorInitRadios.length;
  for (i = 0, len = radioButtons.length; i < len; i++) {
    vrtxEdit.editorInitRadios[len4+i] = radioButtons[i].name + " " + radioButtons[i].value;
  }
}

function unsavedChangesInEditor() {
  if (!vrtxEditor.needToConfirm) {
    vrtxAdmin.ignoreAjaxErrors = true;
    return false;
  }
  
  var vrtxEdit = vrtxEditor;
  var contents = vrtxAdmin.cachedContent;

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
  if (textLen != vrtxEdit.editorInitInputFields.length || selectsLen != vrtxEdit.editorInitSelects.length || checkboxLen != vrtxEdit.editorInitCheckboxes.length || radioLen != vrtxEdit.editorInitRadios.length) return true;

  // Check if values have changed
  for (var i = 0; i < textLen; i++) if (currentStateOfInputFields[i].value !== vrtxEdit.editorInitInputFields[i]) return true;
  for (i = 0; i < selectsLen; i++) if (currentStateOfSelects[i].value !== vrtxEdit.editorInitSelects[i]) return true;
  for (i = 0; i < checkboxLen; i++) if (currentStateOfCheckboxes[i].name !== vrtxEdit.editorInitCheckboxes[i]) return true;
  for (i = 0; i < radioLen; i++) if (currentStateOfRadioButtons[i].name + " " + currentStateOfRadioButtons[i].value !== vrtxEdit.editorInitRadios[i]) return true;

  var currentStateOfTextFields = contents.find("textarea"); // CK->checkDirty()
  if (typeof CKEDITOR != "undefined") {
    var rteFacade = vrtxEdit.richtextEditorFacade;
    for (i = 0, len = currentStateOfTextFields.length; i < len; i++) {
      var ckInstance = rteFacade.getInstance(currentStateOfTextFields[i].name);
      if (ckInstance && rteFacade.isChanged(ckInstance) && rteFacade.getValue(ckInstance) !== "") {
        return true;
      }
    }
  }
  vrtxAdmin.ignoreAjaxErrors = true;
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
    RTE_NEW = ".vrtx-simple-html, .vrtx-simple-html-small", // aka. textareas
    RTE_OLD = "textarea:not(#resource\\.content)";

  var contents = vrtxAdmin.cachedContent;

  var validTextLengthsInEditorErrorFunc = validTextLengthsInEditorError; // Perf.

  // String textfields
  var currentInputFields = isOldEditor ? contents.find(INPUT_OLD) : contents.find(INPUT_NEW);
  for (var i = 0, textLen = currentInputFields.length; i < textLen; i++) {
    var strElm = $(currentInputFields[i]);
    var str = "";
    if (isOldEditor) {
      str = (typeof strElm.val() !== "undefined") ? str = strElm.val() : "";
    } else {
      var strInput = strElm.find("input");
      str = (strInput.length && typeof strInput.val() !== "undefined") ? str = strInput.val() : "";
    }
    if (str.length > MAX_LENGTH) {
      validTextLengthsInEditorErrorFunc(strElm, isOldEditor);
      return false;
    }
  }

  // Textareas that are not content-fields (RichText)
  if (typeof CKEDITOR != "undefined") {
    var currentTextAreas = isOldEditor ? contents.find(RTE_OLD) : contents.find(RTE_NEW);
    var rteFacade = vrtxEditor.richtextEditorFacade;
    for (i = 0, len = currentTextAreas.length; i < len; i++) {
      var txtAreaElm = $(currentTextAreas[i]);
      var txtArea = isOldEditor ? txtAreaElm : txtAreaElm.find("textarea");
      if (txtArea.length && typeof txtArea[0].name !== "undefined") {
        var ckInstance = rteFacade.getInstance(txtArea[0].name);
        if (ckInstance && rteFacade.getValue(ckInstance).length > MAX_LENGTH) {
          validTextLengthsInEditorErrorFunc(txtAreaElm, isOldEditor);
          return false;
        }
      }
    }
  }

  return true;
}

function validTextLengthsInEditorError(elm, isOldEditor) {
  if (typeof tooLongFieldPre !== "undefined" && typeof tooLongFieldPost !== "undefined") {
    $("html").scrollTop(0);
    var lbl = "";
    if (isOldEditor) {
      var elmPropWrapper = elm.closest(".property-item");
      if (elmPropWrapper.length) {
        lbl = elmPropWrapper.find(".property-label:first");
      }
    } else {
      lbl = elm.find("label");
    }
    if (lbl.length) {
      var d = new VrtxMsgDialog({msg: tooLongFieldPre + lbl.text() + tooLongFieldPost, title: ""});
      d.open();
    }
  }
}


/*-------------------------------------------------------------------*\
    6. Image preview
\*-------------------------------------------------------------------*/

VrtxEditor.prototype.initPreviewImage = function initPreviewImage() {
  var _$ = vrtxAdmin._$;

  // Box pictures
  var altTexts = $(".boxPictureAlt, .featuredPictureAlt");
  for (var i = altTexts.length; i--;) {
    var altText = $(altTexts[i]);
    var imageRef = altText.prev(".vrtx-image-ref");
    imageRef.addClass("vrtx-image-ref-alt-text");
    imageRef.find(".vrtx-image-ref-preview").append(altText.remove());
  }
  
  // Introduction pictures
  var introImageAndCaption = _$(".introImageAndCaption, #vrtx-resource\\.picture");
  var injectionPoint = introImageAndCaption.find(".picture-and-caption, .vrtx-image-ref");
  var caption = introImageAndCaption.find(".caption");
  var hidePicture = introImageAndCaption.find(".hidePicture");
  var pictureAlt = introImageAndCaption.next(".pictureAlt");
  if(caption.length)     injectionPoint.append(caption.remove());
  if(hidePicture.length) injectionPoint.append(hidePicture.remove());
  if(pictureAlt.length) {
    injectionPoint.append(pictureAlt.remove());
  } else {
    pictureAlt = introImageAndCaption.find(".pictureAlt");
    injectionPoint.append(pictureAlt.remove());
  }
  
  /* Hide image previews on init (unobtrusive) */
  var previewInputFields = _$("input.preview-image-inputfield"),
      hideImagePreviewCaptionFunc = hideImagePreviewCaption;
  for (i = previewInputFields.length; i--;) {
    if (previewInputFields[i].value === "") {
      hideImagePreviewCaptionFunc($(previewInputFields[i]), true);
    }
  }
  
  /* Inputfield events for image preview */
  vrtxAdmin.cachedDoc.on("blur", "input.preview-image-inputfield", function (e) {
    previewImage(this.id, true);
  });

  vrtxAdmin.cachedDoc.on("keydown", "input.preview-image-inputfield", _$.debounce(50, true, function (e) { // ENTER-key
    if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
      previewImage(this.id);
      e.preventDefault();
    }
  }));
};

function hideImagePreviewCaption(input, isInit) {
  var previewImg = $("div#" + input[0].id.replace(/\./g, '\\.') + '\\.preview:visible');
  if (!previewImg.length) return;

  var fadeSpeed = isInit ? 0 : "fast";

  previewImg.fadeOut(fadeSpeed);

  var captionWrp = input.closest(".introImageAndCaption, #vrtx-resource\\.picture");
  if (captionWrp.length) {
    captionWrp.find(".caption").fadeOut(fadeSpeed);
    captionWrp.find(".hidePicture").fadeOut(fadeSpeed);
    captionWrp.find(".pictureAlt").fadeOut(fadeSpeed);
    captionWrp.animate({
      height: "59px"
    }, fadeSpeed);
  }
}

function showImagePreviewCaption(input) {
  var previewImg = $("div#" + input[0].id.replace(/\./g, '\\.') + '\\.preview');
  if (!previewImg.length) return;

  previewImg.fadeIn("fast");

  var captionWrp = input.closest(".introImageAndCaption, #vrtx-resource\\.picture");
  if (captionWrp.length) {
    captionWrp.find(".caption").fadeIn("fast");
    captionWrp.find(".hidePicture").fadeIn("fast");
    captionWrp.find(".pictureAlt").fadeIn("fast");
    captionWrp.animate({
      height: "225px"
    }, "fast");
  }
}

function previewImage(urlobj, isBlurEvent) {
  if (typeof urlobj === "undefined") return;

  urlobj = urlobj.replace(/\./g, '\\.');
  var previewNode = $("#" + urlobj + '\\.preview-inner');
  if (previewNode.length) {
    var elm = $("#" + urlobj);
    if (elm.length) {
      var url = elm.val();
      if (url !== "") {
        var parentPreviewNode = previewNode.parent();
        previewNode.find("img").attr("src", url + "?vrtx=thumbnail");
        if (parentPreviewNode.hasClass("no-preview")) {
          parentPreviewNode.removeClass("no-preview");
          previewNode.find("img").attr("alt", "thumbnail");
        }
        showImagePreviewCaption(elm);
        if(typeof isBlurEvent === "undefined") elm.focus();
      } else {
        hideImagePreviewCaption(elm, false);
      } 
    }
  }
}


/*-------------------------------------------------------------------*\
    7. Enhancements
\*-------------------------------------------------------------------*/

VrtxEditor.prototype.initEnhancements = function initEnhancements() {
  var vrtxAdm = vrtxAdmin,
    _$ = vrtxAdm._$,
    vrtxEdit = this;
    
  var initResetAggregationManuallyApproved = function(_$, checkboxId, name) {
    if (!_$(checkboxId + "\\.true").is(":checked")) {
      _$("#vrtx-resource\\." + name).slideUp(0, "linear");
    }
    vrtxAdm.cachedAppContent.on("click", checkboxId + "\\.true", function (e) {
      if (!_$(this).is(":checked")) {
        _$("." + name + " .vrtx-multipleinputfield").remove();
        _$("#resource\\." + name).val("");
        _$(".vrtx-" + name + "-limit-reached").remove();
        _$("#vrtx-" + name + "-add").show();
      }
      _$("#vrtx-resource\\." + name).slideToggle(vrtxAdm.transitionDropdownSpeed, "swing");
      e.stopPropagation();
    });
  };
  
  initResetAggregationManuallyApproved(_$, "#resource\\.display-aggregation", "aggregation");
  initResetAggregationManuallyApproved(_$, "#resource\\.display-manually-approved", "manually-approve-from");

  vrtxAdm.cachedAppContent.on("change", "#resource\\.courseContext\\.course-status", function (e) {
    var courseStatus = _$(this);
    var animation = new VrtxAnimation({
      animationSpeed: vrtxAdm.transitionDropdownSpeed,
      easeIn: "swing",
      easeOut: "swing",
      afterIn: function(animation) {
        animation.__opts.elem.removeClass("hidden");
      },
      afterOut: function(animation) {
        animation.__opts.elem.addClass("hidden");
      }
    })
    if (courseStatus.val() === "continued-as") {
      animation.updateElem(_$("#vrtx-resource\\.courseContext\\.course-continued-as.hidden"));
      animation.topDown();
    } else {
      animation.updateElem(_$("#vrtx-resource\\.courseContext\\.course-continued-as:not(.hidden)"));
      animation.bottomUp();
    }
    e.stopPropagation();
  });
  _$("#resource\\.courseContext\\.course-status").change();
  
  // Show/hide mappings for radios/booleans
  
  // Exchange sub-folder title
  
  setShowHideBooleanOldEditor("#resource\\.show-subfolder-menu\\.true, #resource\\.show-subfolder-menu\\.unspecified",
    "#vrtx-resource\\.show-subfolder-title",
    "#resource\\.show-subfolder-menu\\.unspecified:checked",
    "");
    
  // Recursive

  setShowHideBooleanOldEditor("#resource\\.recursive-listing\\.false, #resource\\.recursive-listing\\.unspecified",
    "#vrtx-resource\\.recursive-listing-subfolders",
    "#resource\\.recursive-listing\\.false:checked",
    "false");
    
  // Calendar title

  setShowHideBooleanOldEditor("#resource\\.display-type\\.unspecified, #resource\\.display-type\\.calendar",
    "#vrtx-resource\\.event-type-title",
    "#resource\\.display-type\\.calendar:checked",
    null);

  setShowHideBooleanOldEditor("#resource\\.display-type\\.unspecified, #resource\\.display-type\\.calendar",
    "#vrtx-resource\\.hide-additional-content",
    "#resource\\.display-type\\.calendar:checked",
    "calendar");

  vrtxEdit.setShowHideSelectNewEditor();
  
  // Documenttype
  
  if(vrtxEdit.editorForm.hasClass("vrtx-course-schedule")) {
    courseSchedule();
  } else if (vrtxEdit.editorForm.hasClass("vrtx-hvordan-soke")) {
    vrtxEdit.accordionGroupedInit();
  } else if (vrtxEdit.editorForm.hasClass("vrtx-course-description")) {
    setShowHideBooleanNewEditor("course-fee", "div.course-fee-amount", false);
    vrtxEdit.accordionGroupedInit();
  } else if (vrtxEdit.editorForm.hasClass("vrtx-semester-page")) {
    setShowHideBooleanNewEditor("cloned-course", "div.cloned-course-code", false);
    vrtxEdit.accordionGroupedInit("[class*=link-box]");
  } else if (vrtxEdit.editorForm.hasClass("vrtx-student-exchange-agreement")) {
    vrtxEdit.accordionGroupedInit(".vrtx-sea-accordion");
  } else if (vrtxEdit.editorForm.hasClass("vrtx-frontpage")) {
    vrtxEdit.accordionGroupedInit(".vrtx-sea-accordion", "fast");
  } else if (vrtxEdit.editorForm.hasClass("vrtx-samlet-program")) {
    var samletElm = vrtxEdit.editorForm.find(".samlet-element");
    vrtxEdit.replaceTag(samletElm, "h6", "strong");
    vrtxEdit.replaceTag(samletElm, "h5", "h6");
    vrtxEdit.replaceTag(samletElm, "h4", "h5");
    vrtxEdit.replaceTag(samletElm, "h3", "h4");
    vrtxEdit.replaceTag(samletElm, "h2", "h3");
    vrtxEdit.replaceTag(samletElm, "h1", "h2");
  }
};

function courseSchedule() {
  
  retrievedScheduleData = {
         "courseid":"EXPHIL03",
         "terminnr":1,
         "plenary": {
           "vrtx-editable-description": {
             "vrtx-title": { type: "string" },
             "vrtx-staff": {
               type: "string",
               multiple: { movable: true },
               autocomplete: "username"
             },
             "vrtx-staff-external": {
               type: "json",
               multiple: { movable: true },
               props: [
                 { name: "name", type: "string" },
                 { name: "url", type: "resource_ref" }
               ]
             },
             "vrtx-resources": {
               type: "json",
               multiple: { movable: true },
               props: [
                 { name: "title", type: "string" },
                 { name: "url", type: "resource_ref" }
               ]
             },
             "vrtx-status": { type: "checkbox"  }
           },
           "data": [
            {
               "teachingmethod":"FOR",
               "teachingmethodname":"Forelesninger",
               "id":"1-1",
               "sessions":[
                  {
                     "id":"1-1/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-13T12:15:00.000+01:00",
                     "dtend":"2014-01-13T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ],
                     "vrtx-staff":[
                        "oyvhatl",
                        "rezam"
                     ],
                     "vrtx-title":"Åpningsforelesning",
                     "vrtx-resources":[
                        {
                           "title":"Forelesningsnotater",
                           "url":"/studier/emner/exphil03/v14/ressurser/forelesningsnotater.ppt"
                        },
                        {
                           "title":"Pensumliste",
                           "url":"/studier/emner/exphil03/v14/ressurser/pensumliste.pdf"
                        }
                     ],
                     "vrtx-status":"cancelled"
                  },
                  {
                     "id":"1-1/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-20T12:15:00.000+01:00",
                     "dtend":"2014-01-20T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-27T12:15:00.000+01:00",
                     "dtend":"2014-01-27T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-03T12:15:00.000+01:00",
                     "dtend":"2014-02-03T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-10T12:15:00.000+01:00",
                     "dtend":"2014-02-10T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-17T12:15:00.000+01:00",
                     "dtend":"2014-02-17T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-24T12:15:00.000+01:00",
                     "dtend":"2014-02-24T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-17T12:15:00.000+01:00",
                     "dtend":"2014-03-17T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-24T12:15:00.000+01:00",
                     "dtend":"2014-03-24T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/14",
                     "seqno":1,
                     "dtstart":"2014-03-31T12:15:00.000+02:00",
                     "dtend":"2014-03-31T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-07T12:15:00.000+02:00",
                     "dtend":"2014-04-07T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-28T12:15:00.000+02:00",
                     "dtend":"2014-04-28T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Forelesning I",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-1/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T12:15:00.000+01:00",
                     "dtend":"2014-03-06T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"cancelled"
                  },
                  {
                     "id":"1-1/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T12:15:00.000+01:00",
                     "dtend":"2014-03-13T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"cancelled"
                  },
                  {
                     "id":"1-1/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T12:15:00.000+02:00",
                     "dtend":"2014-04-17T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"1-1/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T12:15:00.000+02:00",
                     "dtend":"2014-04-24T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"cancelled"
                  }
               ]
            },
            {
               "teachingmethod":"FOR",
               "teachingmethodname":"Forelesninger",
               "id":"1-2",
               "sessions":[
                  {
                     "id":"1-2/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T16:15:00.000+01:00",
                     "dtend":"2014-01-14T18:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T16:15:00.000+01:00",
                     "dtend":"2014-01-21T18:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T16:15:00.000+01:00",
                     "dtend":"2014-01-28T18:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T16:15:00.000+01:00",
                     "dtend":"2014-02-04T18:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T16:15:00.000+01:00",
                     "dtend":"2014-02-11T18:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T16:15:00.000+01:00",
                     "dtend":"2014-02-18T18:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T16:15:00.000+01:00",
                     "dtend":"2014-02-25T18:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T16:15:00.000+01:00",
                     "dtend":"2014-03-04T18:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T16:15:00.000+01:00",
                     "dtend":"2014-03-18T18:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T16:15:00.000+01:00",
                     "dtend":"2014-03-25T18:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T16:15:00.000+02:00",
                     "dtend":"2014-04-01T18:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T16:15:00.000+02:00",
                     "dtend":"2014-04-08T18:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Tittel fra vortex",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ],
                     "staff":[
                        "alternativ-foreleser"
                     ],
                     "resources":[
                        {
                           "url":"http://www.uio.no",
                           "title":"UiO"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T16:15:00.000+02:00",
                     "dtend":"2014-04-22T18:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T16:15:00.000+02:00",
                     "dtend":"2014-04-29T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Forelesning II",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-2/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T16:15:00.000+01:00",
                     "dtend":"2014-03-11T18:00:00.000+01:00",
                     "weeknr":11,
                     "status":"cancelled"
                  },
                  {
                     "id":"1-2/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T10:15:00.000+01:00",
                     "dtend":"2014-03-12T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"cancelled"
                  },
                  {
                     "id":"1-2/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T16:15:00.000+02:00",
                     "dtend":"2014-04-15T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"1-2/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-16T10:15:00.000+02:00",
                     "dtend":"2014-04-16T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  }
               ]
            },
            {
               "teachingmethod":"FOR",
               "teachingmethodname":"Forelesninger",
               "id":"1-3",
               "sessions":[
                  {
                     "id":"1-3/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T10:15:00.000+01:00",
                     "dtend":"2014-01-15T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T10:15:00.000+01:00",
                     "dtend":"2014-01-22T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T10:15:00.000+01:00",
                     "dtend":"2014-01-29T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T10:15:00.000+01:00",
                     "dtend":"2014-02-05T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T10:15:00.000+01:00",
                     "dtend":"2014-02-12T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T10:15:00.000+01:00",
                     "dtend":"2014-02-19T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T10:15:00.000+01:00",
                     "dtend":"2014-02-26T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T10:15:00.000+01:00",
                     "dtend":"2014-03-19T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T10:15:00.000+01:00",
                     "dtend":"2014-03-26T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T10:15:00.000+02:00",
                     "dtend":"2014-04-02T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T10:15:00.000+02:00",
                     "dtend":"2014-04-09T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T10:15:00.000+02:00",
                     "dtend":"2014-04-23T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T10:15:00.000+02:00",
                     "dtend":"2014-04-30T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Forelesning III",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-3/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T12:15:00.000+01:00",
                     "dtend":"2014-03-04T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"cancelled"
                  },
                  {
                     "id":"1-3/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T12:15:00.000+01:00",
                     "dtend":"2014-03-11T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"cancelled"
                  },
                  {
                     "id":"1-3/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T12:15:00.000+02:00",
                     "dtend":"2014-04-15T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  }
               ]
            },
            {
               "teachingmethod":"FOR",
               "teachingmethodname":"Forelesninger",
               "id":"1-4",
               "sessions":[
                  {
                     "id":"1-4/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T10:15:00.000+02:00",
                     "dtend":"2014-04-24T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Ekstra forelesning Selvstudium",
                     "room":[
                        {
                           "buildingid":"BL27",
                           "roomid":"1501"
                        }
                     ]
                  },
                  {
                     "id":"1-4/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-28T12:15:00.000+02:00",
                     "dtend":"2014-04-28T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  }
               ]
            }
         ]},
         "group": {
           "vrtx-editable-description": {
             "vrtx-title": { type: "string" },
             "vrtx-staff": {
               type: "string",
               multiple: { movable: true },
               autocomplete: "username"
             },
             "vrtx-staff-external": {
               type: "json",
               multiple: { movable: true },
               props: [
                 { name: "name", type: "string" },
                 { name: "url", type: "resource_ref" }
               ]
             },
             "vrtx-resources": {
               type: "json",
               multiple: { movable: true },
               props: [
                 { name: "title", type: "string" },
                 { name: "url", type: "resource_ref" }
               ]
             },
             "vrtx-status": { type: "checkbox"  }
           },
           "data": [
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-11",
               "sessions":[
                  {
                     "id":"2-11/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T16:15:00.000+01:00",
                     "dtend":"2014-01-16T18:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T16:15:00.000+01:00",
                     "dtend":"2014-01-23T18:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T16:15:00.000+01:00",
                     "dtend":"2014-01-30T18:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T16:15:00.000+01:00",
                     "dtend":"2014-02-06T18:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T16:15:00.000+01:00",
                     "dtend":"2014-02-13T18:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T16:15:00.000+01:00",
                     "dtend":"2014-02-20T18:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T16:15:00.000+01:00",
                     "dtend":"2014-02-27T18:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T16:15:00.000+01:00",
                     "dtend":"2014-03-06T18:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T16:15:00.000+01:00",
                     "dtend":"2014-03-13T18:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T16:15:00.000+01:00",
                     "dtend":"2014-03-20T18:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T16:15:00.000+01:00",
                     "dtend":"2014-03-27T18:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T16:15:00.000+02:00",
                     "dtend":"2014-04-03T18:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T16:15:00.000+02:00",
                     "dtend":"2014-04-10T18:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T16:15:00.000+02:00",
                     "dtend":"2014-04-24T18:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T16:15:00.000+02:00",
                     "dtend":"2014-05-08T18:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T16:15:00.000+02:00",
                     "dtend":"2014-05-15T18:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 11 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1128"
                        }
                     ]
                  },
                  {
                     "id":"2-11/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T16:15:00.000+02:00",
                     "dtend":"2014-04-18T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-11/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T16:15:00.000+02:00",
                     "dtend":"2014-05-02T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-11/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-23T16:15:00.000+02:00",
                     "dtend":"2014-05-23T18:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"11"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-12",
               "sessions":[
                  {
                     "id":"2-12/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T16:15:00.000+01:00",
                     "dtend":"2014-01-16T18:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T16:15:00.000+01:00",
                     "dtend":"2014-01-23T18:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T16:15:00.000+01:00",
                     "dtend":"2014-01-30T18:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T16:15:00.000+01:00",
                     "dtend":"2014-02-06T18:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T16:15:00.000+01:00",
                     "dtend":"2014-02-13T18:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T16:15:00.000+01:00",
                     "dtend":"2014-02-20T18:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T16:15:00.000+01:00",
                     "dtend":"2014-02-27T18:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T16:15:00.000+01:00",
                     "dtend":"2014-03-06T18:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T16:15:00.000+01:00",
                     "dtend":"2014-03-13T18:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T16:15:00.000+01:00",
                     "dtend":"2014-03-20T18:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T16:15:00.000+01:00",
                     "dtend":"2014-03-27T18:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T16:15:00.000+02:00",
                     "dtend":"2014-04-03T18:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T16:15:00.000+02:00",
                     "dtend":"2014-04-10T18:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T16:15:00.000+02:00",
                     "dtend":"2014-04-24T18:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T16:15:00.000+02:00",
                     "dtend":"2014-05-08T18:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T16:15:00.000+02:00",
                     "dtend":"2014-05-15T18:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 12 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"1151"
                        }
                     ]
                  },
                  {
                     "id":"2-12/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T16:15:00.000+02:00",
                     "dtend":"2014-04-17T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-12/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T16:15:00.000+02:00",
                     "dtend":"2014-04-18T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-12/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-01T16:15:00.000+02:00",
                     "dtend":"2014-05-01T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-12/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T16:15:00.000+02:00",
                     "dtend":"2014-05-02T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-12/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-23T16:15:00.000+02:00",
                     "dtend":"2014-05-23T18:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"12"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-13",
               "sessions":[
                  {
                     "id":"2-13/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T16:15:00.000+01:00",
                     "dtend":"2014-01-16T18:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T16:15:00.000+01:00",
                     "dtend":"2014-01-23T18:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T16:15:00.000+01:00",
                     "dtend":"2014-01-30T18:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T16:15:00.000+01:00",
                     "dtend":"2014-02-06T18:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T16:15:00.000+01:00",
                     "dtend":"2014-02-13T18:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T16:15:00.000+01:00",
                     "dtend":"2014-02-20T18:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T16:15:00.000+01:00",
                     "dtend":"2014-02-27T18:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T16:15:00.000+01:00",
                     "dtend":"2014-03-06T18:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T16:15:00.000+01:00",
                     "dtend":"2014-03-13T18:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T16:15:00.000+01:00",
                     "dtend":"2014-03-20T18:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T16:15:00.000+01:00",
                     "dtend":"2014-03-27T18:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T16:15:00.000+02:00",
                     "dtend":"2014-04-03T18:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T16:15:00.000+02:00",
                     "dtend":"2014-04-10T18:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T16:15:00.000+02:00",
                     "dtend":"2014-04-24T18:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T16:15:00.000+02:00",
                     "dtend":"2014-05-08T18:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T16:15:00.000+02:00",
                     "dtend":"2014-05-15T18:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 13 (MED)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2015"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-13/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T16:15:00.000+02:00",
                     "dtend":"2014-04-17T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-13/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T16:15:00.000+02:00",
                     "dtend":"2014-04-18T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-13/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-01T16:15:00.000+02:00",
                     "dtend":"2014-05-01T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-13/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T16:15:00.000+02:00",
                     "dtend":"2014-05-02T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-13/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-23T16:15:00.000+02:00",
                     "dtend":"2014-05-23T18:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"13"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-14",
               "sessions":[
                  {
                     "id":"2-14/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T16:15:00.000+01:00",
                     "dtend":"2014-01-16T18:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T16:15:00.000+01:00",
                     "dtend":"2014-01-23T18:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T16:15:00.000+01:00",
                     "dtend":"2014-01-30T18:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T16:15:00.000+01:00",
                     "dtend":"2014-02-06T18:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T16:15:00.000+01:00",
                     "dtend":"2014-02-13T18:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T16:15:00.000+01:00",
                     "dtend":"2014-02-20T18:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T16:15:00.000+01:00",
                     "dtend":"2014-02-27T18:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T16:15:00.000+01:00",
                     "dtend":"2014-03-06T18:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T16:15:00.000+01:00",
                     "dtend":"2014-03-13T18:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T16:15:00.000+01:00",
                     "dtend":"2014-03-20T18:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T16:15:00.000+01:00",
                     "dtend":"2014-03-27T18:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T16:15:00.000+02:00",
                     "dtend":"2014-04-03T18:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T16:15:00.000+02:00",
                     "dtend":"2014-04-10T18:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T16:15:00.000+02:00",
                     "dtend":"2014-04-24T18:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T16:15:00.000+02:00",
                     "dtend":"2014-05-08T18:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T16:15:00.000+02:00",
                     "dtend":"2014-05-15T18:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 14 (ERN)",
                     "room":[
                        {
                           "buildingid":"GA01",
                           "roomid":"2031"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-14/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T12:15:00.000+02:00",
                     "dtend":"2014-04-14T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-14/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T16:15:00.000+02:00",
                     "dtend":"2014-04-17T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-14/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-28T12:15:00.000+02:00",
                     "dtend":"2014-04-28T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-14/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-01T16:15:00.000+02:00",
                     "dtend":"2014-05-01T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-14/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T12:15:00.000+02:00",
                     "dtend":"2014-05-19T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"14"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-15",
               "sessions":[
                  {
                     "id":"2-15/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-17T09:15:00.000+01:00",
                     "dtend":"2014-01-17T11:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-24T09:15:00.000+01:00",
                     "dtend":"2014-01-24T11:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-31T09:15:00.000+01:00",
                     "dtend":"2014-01-31T11:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-07T09:15:00.000+01:00",
                     "dtend":"2014-02-07T11:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-14T09:15:00.000+01:00",
                     "dtend":"2014-02-14T11:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-21T09:15:00.000+01:00",
                     "dtend":"2014-02-21T11:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-28T09:15:00.000+01:00",
                     "dtend":"2014-02-28T11:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-07T09:15:00.000+01:00",
                     "dtend":"2014-03-07T11:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-14T09:15:00.000+01:00",
                     "dtend":"2014-03-14T11:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-21T09:15:00.000+01:00",
                     "dtend":"2014-03-21T11:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-28T09:15:00.000+01:00",
                     "dtend":"2014-03-28T11:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-04T09:15:00.000+02:00",
                     "dtend":"2014-04-04T11:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-11T09:15:00.000+02:00",
                     "dtend":"2014-04-11T11:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-25T09:15:00.000+02:00",
                     "dtend":"2014-04-25T11:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T09:15:00.000+02:00",
                     "dtend":"2014-05-02T11:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-16T09:15:00.000+02:00",
                     "dtend":"2014-05-16T11:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 15 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-15/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T09:15:00.000+02:00",
                     "dtend":"2014-04-17T11:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-15/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T09:15:00.000+02:00",
                     "dtend":"2014-04-18T11:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-15/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T09:15:00.000+02:00",
                     "dtend":"2014-05-08T11:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-15/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-09T09:15:00.000+02:00",
                     "dtend":"2014-05-09T11:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-15/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-22T09:15:00.000+02:00",
                     "dtend":"2014-05-22T11:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"15"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-16",
               "sessions":[
                  {
                     "id":"2-16/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-17T09:15:00.000+01:00",
                     "dtend":"2014-01-17T11:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-24T09:15:00.000+01:00",
                     "dtend":"2014-01-24T11:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-31T09:15:00.000+01:00",
                     "dtend":"2014-01-31T11:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-07T09:15:00.000+01:00",
                     "dtend":"2014-02-07T11:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-14T09:15:00.000+01:00",
                     "dtend":"2014-02-14T11:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-21T09:15:00.000+01:00",
                     "dtend":"2014-02-21T11:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-28T09:15:00.000+01:00",
                     "dtend":"2014-02-28T11:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-07T09:15:00.000+01:00",
                     "dtend":"2014-03-07T11:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-14T09:15:00.000+01:00",
                     "dtend":"2014-03-14T11:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-21T09:15:00.000+01:00",
                     "dtend":"2014-03-21T11:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-28T09:15:00.000+01:00",
                     "dtend":"2014-03-28T11:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-04T09:15:00.000+02:00",
                     "dtend":"2014-04-04T11:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-11T09:15:00.000+02:00",
                     "dtend":"2014-04-11T11:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-25T09:15:00.000+02:00",
                     "dtend":"2014-04-25T11:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T09:15:00.000+02:00",
                     "dtend":"2014-05-02T11:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-16T09:15:00.000+02:00",
                     "dtend":"2014-05-16T11:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 16 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-16/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T09:15:00.000+02:00",
                     "dtend":"2014-04-18T11:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-16/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-09T09:15:00.000+02:00",
                     "dtend":"2014-05-09T11:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-16/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-23T09:15:00.000+02:00",
                     "dtend":"2014-05-23T11:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"16"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-17",
               "sessions":[
                  {
                     "id":"2-17/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-17T09:15:00.000+01:00",
                     "dtend":"2014-01-17T11:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-24T09:15:00.000+01:00",
                     "dtend":"2014-01-24T11:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-31T09:15:00.000+01:00",
                     "dtend":"2014-01-31T11:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-07T09:15:00.000+01:00",
                     "dtend":"2014-02-07T11:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-14T09:15:00.000+01:00",
                     "dtend":"2014-02-14T11:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-21T09:15:00.000+01:00",
                     "dtend":"2014-02-21T11:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-28T09:15:00.000+01:00",
                     "dtend":"2014-02-28T11:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-07T09:15:00.000+01:00",
                     "dtend":"2014-03-07T11:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-14T09:15:00.000+01:00",
                     "dtend":"2014-03-14T11:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-21T09:15:00.000+01:00",
                     "dtend":"2014-03-21T11:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-28T09:15:00.000+01:00",
                     "dtend":"2014-03-28T11:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-04T09:15:00.000+02:00",
                     "dtend":"2014-04-04T11:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-11T09:15:00.000+02:00",
                     "dtend":"2014-04-11T11:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-25T09:15:00.000+02:00",
                     "dtend":"2014-04-25T11:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T09:15:00.000+02:00",
                     "dtend":"2014-05-02T11:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-16T09:15:00.000+02:00",
                     "dtend":"2014-05-16T11:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 17 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-17/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T09:15:00.000+02:00",
                     "dtend":"2014-04-17T11:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-17/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T09:15:00.000+02:00",
                     "dtend":"2014-05-08T11:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-17/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-22T09:15:00.000+02:00",
                     "dtend":"2014-05-22T11:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"17"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-18",
               "sessions":[
                  {
                     "id":"2-18/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-17T09:15:00.000+01:00",
                     "dtend":"2014-01-17T11:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-24T09:15:00.000+01:00",
                     "dtend":"2014-01-24T11:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-31T09:15:00.000+01:00",
                     "dtend":"2014-01-31T11:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-07T09:15:00.000+01:00",
                     "dtend":"2014-02-07T11:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-14T09:15:00.000+01:00",
                     "dtend":"2014-02-14T11:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-21T09:15:00.000+01:00",
                     "dtend":"2014-02-21T11:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-28T09:15:00.000+01:00",
                     "dtend":"2014-02-28T11:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-07T09:15:00.000+01:00",
                     "dtend":"2014-03-07T11:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-14T09:15:00.000+01:00",
                     "dtend":"2014-03-14T11:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-21T09:15:00.000+01:00",
                     "dtend":"2014-03-21T11:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-28T09:15:00.000+01:00",
                     "dtend":"2014-03-28T11:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-04T09:15:00.000+02:00",
                     "dtend":"2014-04-04T11:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-11T09:15:00.000+02:00",
                     "dtend":"2014-04-11T11:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-25T09:15:00.000+02:00",
                     "dtend":"2014-04-25T11:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T09:15:00.000+02:00",
                     "dtend":"2014-05-02T11:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-16T09:15:00.000+02:00",
                     "dtend":"2014-05-16T11:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 18 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-18/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T09:15:00.000+02:00",
                     "dtend":"2014-04-18T11:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-18/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-09T09:15:00.000+02:00",
                     "dtend":"2014-05-09T11:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-18/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-23T09:15:00.000+02:00",
                     "dtend":"2014-05-23T11:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"18"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-19",
               "sessions":[
                  {
                     "id":"2-19/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-17T09:15:00.000+01:00",
                     "dtend":"2014-01-17T11:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-24T09:15:00.000+01:00",
                     "dtend":"2014-01-24T11:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-31T09:15:00.000+01:00",
                     "dtend":"2014-01-31T11:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-07T09:15:00.000+01:00",
                     "dtend":"2014-02-07T11:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-14T09:15:00.000+01:00",
                     "dtend":"2014-02-14T11:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-21T09:15:00.000+01:00",
                     "dtend":"2014-02-21T11:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-28T09:15:00.000+01:00",
                     "dtend":"2014-02-28T11:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-07T09:15:00.000+01:00",
                     "dtend":"2014-03-07T11:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-14T09:15:00.000+01:00",
                     "dtend":"2014-03-14T11:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-21T09:15:00.000+01:00",
                     "dtend":"2014-03-21T11:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-28T09:15:00.000+01:00",
                     "dtend":"2014-03-28T11:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-04T09:15:00.000+02:00",
                     "dtend":"2014-04-04T11:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-11T09:15:00.000+02:00",
                     "dtend":"2014-04-11T11:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-25T09:15:00.000+02:00",
                     "dtend":"2014-04-25T11:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T09:15:00.000+02:00",
                     "dtend":"2014-05-02T11:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-16T09:15:00.000+02:00",
                     "dtend":"2014-05-16T11:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 19 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-19/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T09:15:00.000+02:00",
                     "dtend":"2014-04-18T11:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-19/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-09T09:15:00.000+02:00",
                     "dtend":"2014-05-09T11:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-19/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-23T09:15:00.000+02:00",
                     "dtend":"2014-05-23T11:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"19"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-20",
               "sessions":[
                  {
                     "id":"2-20/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-17T09:15:00.000+01:00",
                     "dtend":"2014-01-17T11:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-24T09:15:00.000+01:00",
                     "dtend":"2014-01-24T11:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-31T09:15:00.000+01:00",
                     "dtend":"2014-01-31T11:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-07T09:15:00.000+01:00",
                     "dtend":"2014-02-07T11:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-14T09:15:00.000+01:00",
                     "dtend":"2014-02-14T11:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-21T09:15:00.000+01:00",
                     "dtend":"2014-02-21T11:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-28T09:15:00.000+01:00",
                     "dtend":"2014-02-28T11:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-07T09:15:00.000+01:00",
                     "dtend":"2014-03-07T11:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-14T09:15:00.000+01:00",
                     "dtend":"2014-03-14T11:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-21T09:15:00.000+01:00",
                     "dtend":"2014-03-21T11:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-28T09:15:00.000+01:00",
                     "dtend":"2014-03-28T11:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-04T09:15:00.000+02:00",
                     "dtend":"2014-04-04T11:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-11T09:15:00.000+02:00",
                     "dtend":"2014-04-11T11:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-25T09:15:00.000+02:00",
                     "dtend":"2014-04-25T11:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T09:15:00.000+02:00",
                     "dtend":"2014-05-02T11:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-16T09:15:00.000+02:00",
                     "dtend":"2014-05-16T11:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 20 (JUS)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"219"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-20/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T09:15:00.000+02:00",
                     "dtend":"2014-04-18T11:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-20/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-09T09:15:00.000+02:00",
                     "dtend":"2014-05-09T11:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-20/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-23T09:15:00.000+02:00",
                     "dtend":"2014-05-23T11:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"20"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-21",
               "sessions":[
                  {
                     "id":"2-21/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T12:15:00.000+01:00",
                     "dtend":"2014-01-16T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T12:15:00.000+01:00",
                     "dtend":"2014-01-23T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T12:15:00.000+01:00",
                     "dtend":"2014-01-30T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T12:15:00.000+01:00",
                     "dtend":"2014-02-06T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T12:15:00.000+01:00",
                     "dtend":"2014-02-13T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T12:15:00.000+01:00",
                     "dtend":"2014-02-20T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T12:15:00.000+01:00",
                     "dtend":"2014-02-27T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T12:15:00.000+01:00",
                     "dtend":"2014-03-06T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T12:15:00.000+01:00",
                     "dtend":"2014-03-13T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T12:15:00.000+01:00",
                     "dtend":"2014-03-20T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T12:15:00.000+01:00",
                     "dtend":"2014-03-27T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T12:15:00.000+02:00",
                     "dtend":"2014-04-03T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T12:15:00.000+02:00",
                     "dtend":"2014-04-10T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T12:15:00.000+02:00",
                     "dtend":"2014-04-24T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T12:15:00.000+02:00",
                     "dtend":"2014-05-08T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T12:15:00.000+02:00",
                     "dtend":"2014-05-15T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 21 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-21/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T12:15:00.000+02:00",
                     "dtend":"2014-04-17T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-21/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T12:15:00.000+02:00",
                     "dtend":"2014-04-18T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-21/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-01T12:15:00.000+02:00",
                     "dtend":"2014-05-01T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-21/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T12:15:00.000+02:00",
                     "dtend":"2014-05-02T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-21/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-23T12:15:00.000+02:00",
                     "dtend":"2014-05-23T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"21"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-22",
               "sessions":[
                  {
                     "id":"2-22/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T14:15:00.000+01:00",
                     "dtend":"2014-01-16T16:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T14:15:00.000+01:00",
                     "dtend":"2014-01-23T16:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T14:15:00.000+01:00",
                     "dtend":"2014-01-30T16:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T14:15:00.000+01:00",
                     "dtend":"2014-02-06T16:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T14:15:00.000+01:00",
                     "dtend":"2014-02-13T16:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T14:15:00.000+01:00",
                     "dtend":"2014-02-20T16:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T14:15:00.000+01:00",
                     "dtend":"2014-02-27T16:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T14:15:00.000+01:00",
                     "dtend":"2014-03-06T16:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T14:15:00.000+01:00",
                     "dtend":"2014-03-13T16:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T14:15:00.000+01:00",
                     "dtend":"2014-03-20T16:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T14:15:00.000+01:00",
                     "dtend":"2014-03-27T16:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T14:15:00.000+02:00",
                     "dtend":"2014-04-03T16:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T14:15:00.000+02:00",
                     "dtend":"2014-04-10T16:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T14:15:00.000+02:00",
                     "dtend":"2014-04-24T16:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T14:15:00.000+02:00",
                     "dtend":"2014-05-08T16:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T14:15:00.000+02:00",
                     "dtend":"2014-05-15T16:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 22 (PSY)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "kjellej"
                     ]
                  },
                  {
                     "id":"2-22/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T14:15:00.000+02:00",
                     "dtend":"2014-04-17T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-22/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T14:15:00.000+02:00",
                     "dtend":"2014-04-18T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-22/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-01T14:15:00.000+02:00",
                     "dtend":"2014-05-01T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-22/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T14:15:00.000+02:00",
                     "dtend":"2014-05-02T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-22/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-23T14:15:00.000+02:00",
                     "dtend":"2014-05-23T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"22"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-23",
               "sessions":[
                  {
                     "id":"2-23/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T10:15:00.000+01:00",
                     "dtend":"2014-01-14T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T10:15:00.000+01:00",
                     "dtend":"2014-01-21T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T10:15:00.000+01:00",
                     "dtend":"2014-01-28T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T10:15:00.000+01:00",
                     "dtend":"2014-02-04T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T10:15:00.000+01:00",
                     "dtend":"2014-02-11T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T10:15:00.000+01:00",
                     "dtend":"2014-02-18T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T10:15:00.000+01:00",
                     "dtend":"2014-02-25T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T10:15:00.000+01:00",
                     "dtend":"2014-03-04T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T10:15:00.000+01:00",
                     "dtend":"2014-03-11T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T10:15:00.000+01:00",
                     "dtend":"2014-03-18T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T10:15:00.000+01:00",
                     "dtend":"2014-03-25T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T10:15:00.000+02:00",
                     "dtend":"2014-04-01T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T10:15:00.000+02:00",
                     "dtend":"2014-04-08T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T10:15:00.000+02:00",
                     "dtend":"2014-04-22T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T10:15:00.000+02:00",
                     "dtend":"2014-04-29T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T10:15:00.000+02:00",
                     "dtend":"2014-05-13T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 23",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-23/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T10:15:00.000+02:00",
                     "dtend":"2014-04-15T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-23/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T14:15:00.000+02:00",
                     "dtend":"2014-04-15T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-23/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T10:15:00.000+02:00",
                     "dtend":"2014-05-06T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-23/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T14:15:00.000+02:00",
                     "dtend":"2014-05-06T16:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-23/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T14:15:00.000+02:00",
                     "dtend":"2014-05-20T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"23"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-24",
               "sessions":[
                  {
                     "id":"2-24/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T16:15:00.000+01:00",
                     "dtend":"2014-01-15T18:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T16:15:00.000+01:00",
                     "dtend":"2014-01-22T18:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T16:15:00.000+01:00",
                     "dtend":"2014-01-29T18:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T16:15:00.000+01:00",
                     "dtend":"2014-02-05T18:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T16:15:00.000+01:00",
                     "dtend":"2014-02-12T18:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T16:15:00.000+01:00",
                     "dtend":"2014-02-19T18:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T16:15:00.000+01:00",
                     "dtend":"2014-02-26T18:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T16:15:00.000+01:00",
                     "dtend":"2014-03-05T18:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T16:15:00.000+01:00",
                     "dtend":"2014-03-12T18:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T16:15:00.000+01:00",
                     "dtend":"2014-03-19T18:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T16:15:00.000+01:00",
                     "dtend":"2014-03-26T18:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T16:15:00.000+02:00",
                     "dtend":"2014-04-02T18:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T16:15:00.000+02:00",
                     "dtend":"2014-04-09T18:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T16:15:00.000+02:00",
                     "dtend":"2014-04-23T18:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T16:15:00.000+02:00",
                     "dtend":"2014-04-30T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T16:15:00.000+02:00",
                     "dtend":"2014-05-14T18:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 24 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-24/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T12:15:00.000+02:00",
                     "dtend":"2014-04-14T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-24/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T16:15:00.000+02:00",
                     "dtend":"2014-04-17T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-24/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T12:15:00.000+02:00",
                     "dtend":"2014-05-05T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-24/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T16:15:00.000+02:00",
                     "dtend":"2014-05-08T18:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-24/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T12:15:00.000+02:00",
                     "dtend":"2014-05-19T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"24"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-25",
               "sessions":[
                  {
                     "id":"2-25/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T12:15:00.000+01:00",
                     "dtend":"2014-01-14T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T12:15:00.000+01:00",
                     "dtend":"2014-01-21T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T12:15:00.000+01:00",
                     "dtend":"2014-01-28T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T12:15:00.000+01:00",
                     "dtend":"2014-02-04T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T12:15:00.000+01:00",
                     "dtend":"2014-02-11T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T12:15:00.000+01:00",
                     "dtend":"2014-02-18T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T12:15:00.000+01:00",
                     "dtend":"2014-02-25T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T12:15:00.000+01:00",
                     "dtend":"2014-03-04T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T12:15:00.000+01:00",
                     "dtend":"2014-03-11T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T12:15:00.000+01:00",
                     "dtend":"2014-03-18T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T12:15:00.000+01:00",
                     "dtend":"2014-03-25T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T12:15:00.000+02:00",
                     "dtend":"2014-04-01T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T12:15:00.000+02:00",
                     "dtend":"2014-04-08T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T12:15:00.000+02:00",
                     "dtend":"2014-04-22T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T12:15:00.000+02:00",
                     "dtend":"2014-04-29T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T12:15:00.000+02:00",
                     "dtend":"2014-05-13T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 25",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-25/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T12:15:00.000+02:00",
                     "dtend":"2014-04-15T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-25/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T12:15:00.000+02:00",
                     "dtend":"2014-05-06T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-25/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T12:15:00.000+02:00",
                     "dtend":"2014-05-20T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"25"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-26",
               "sessions":[
                  {
                     "id":"2-26/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T10:15:00.000+01:00",
                     "dtend":"2014-01-14T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T10:15:00.000+01:00",
                     "dtend":"2014-01-21T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T10:15:00.000+01:00",
                     "dtend":"2014-01-28T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T10:15:00.000+01:00",
                     "dtend":"2014-02-04T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T10:15:00.000+01:00",
                     "dtend":"2014-02-11T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T10:15:00.000+01:00",
                     "dtend":"2014-02-18T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T10:15:00.000+01:00",
                     "dtend":"2014-02-25T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T10:15:00.000+01:00",
                     "dtend":"2014-03-04T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T10:15:00.000+01:00",
                     "dtend":"2014-03-11T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T10:15:00.000+01:00",
                     "dtend":"2014-03-18T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T10:15:00.000+01:00",
                     "dtend":"2014-03-25T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T10:15:00.000+02:00",
                     "dtend":"2014-04-01T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T10:15:00.000+02:00",
                     "dtend":"2014-04-08T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T10:15:00.000+02:00",
                     "dtend":"2014-04-22T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T10:15:00.000+02:00",
                     "dtend":"2014-04-29T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T10:15:00.000+02:00",
                     "dtend":"2014-05-13T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 26",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "tollefse"
                     ]
                  },
                  {
                     "id":"2-26/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T10:15:00.000+02:00",
                     "dtend":"2014-04-15T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-26/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-16T10:15:00.000+02:00",
                     "dtend":"2014-04-16T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-26/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T10:15:00.000+02:00",
                     "dtend":"2014-05-06T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-26/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-07T10:15:00.000+02:00",
                     "dtend":"2014-05-07T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-26/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-21T10:15:00.000+02:00",
                     "dtend":"2014-05-21T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"26"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-27",
               "sessions":[
                  {
                     "id":"2-27/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T16:15:00.000+01:00",
                     "dtend":"2014-01-15T18:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T16:15:00.000+01:00",
                     "dtend":"2014-01-22T18:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T16:15:00.000+01:00",
                     "dtend":"2014-01-29T18:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T16:15:00.000+01:00",
                     "dtend":"2014-02-05T18:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T16:15:00.000+01:00",
                     "dtend":"2014-02-12T18:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T16:15:00.000+01:00",
                     "dtend":"2014-02-19T18:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T16:15:00.000+01:00",
                     "dtend":"2014-02-26T18:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T16:15:00.000+01:00",
                     "dtend":"2014-03-05T18:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T16:15:00.000+01:00",
                     "dtend":"2014-03-12T18:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T16:15:00.000+01:00",
                     "dtend":"2014-03-19T18:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T16:15:00.000+01:00",
                     "dtend":"2014-03-26T18:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T16:15:00.000+02:00",
                     "dtend":"2014-04-02T18:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T16:15:00.000+02:00",
                     "dtend":"2014-04-09T18:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T16:15:00.000+02:00",
                     "dtend":"2014-04-23T18:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T16:15:00.000+02:00",
                     "dtend":"2014-04-30T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T16:15:00.000+02:00",
                     "dtend":"2014-05-14T18:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 27",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-27/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T10:15:00.000+02:00",
                     "dtend":"2014-04-14T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-27/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T16:15:00.000+02:00",
                     "dtend":"2014-04-17T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-27/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T10:15:00.000+02:00",
                     "dtend":"2014-05-05T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-27/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T16:15:00.000+02:00",
                     "dtend":"2014-05-08T18:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-27/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T10:15:00.000+02:00",
                     "dtend":"2014-05-19T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"27"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-28",
               "sessions":[
                  {
                     "id":"2-28/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T10:15:00.000+01:00",
                     "dtend":"2014-01-14T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T10:15:00.000+01:00",
                     "dtend":"2014-01-21T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T10:15:00.000+01:00",
                     "dtend":"2014-01-28T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T10:15:00.000+01:00",
                     "dtend":"2014-02-04T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T10:15:00.000+01:00",
                     "dtend":"2014-02-11T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T10:15:00.000+01:00",
                     "dtend":"2014-02-18T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T10:15:00.000+01:00",
                     "dtend":"2014-02-25T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T10:15:00.000+01:00",
                     "dtend":"2014-03-04T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T10:15:00.000+01:00",
                     "dtend":"2014-03-11T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T10:15:00.000+01:00",
                     "dtend":"2014-03-18T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T10:15:00.000+01:00",
                     "dtend":"2014-03-25T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T10:15:00.000+02:00",
                     "dtend":"2014-04-01T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T10:15:00.000+02:00",
                     "dtend":"2014-04-08T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T10:15:00.000+02:00",
                     "dtend":"2014-04-22T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T10:15:00.000+02:00",
                     "dtend":"2014-04-29T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T10:15:00.000+02:00",
                     "dtend":"2014-05-13T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 28",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-28/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T10:15:00.000+02:00",
                     "dtend":"2014-04-15T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-28/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T10:15:00.000+02:00",
                     "dtend":"2014-05-06T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-28/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T10:15:00.000+02:00",
                     "dtend":"2014-05-20T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"28"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-29",
               "sessions":[
                  {
                     "id":"2-29/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-17T12:15:00.000+01:00",
                     "dtend":"2014-01-17T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-24T12:15:00.000+01:00",
                     "dtend":"2014-01-24T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-31T12:15:00.000+01:00",
                     "dtend":"2014-01-31T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-07T12:15:00.000+01:00",
                     "dtend":"2014-02-07T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-14T12:15:00.000+01:00",
                     "dtend":"2014-02-14T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-21T12:15:00.000+01:00",
                     "dtend":"2014-02-21T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-28T12:15:00.000+01:00",
                     "dtend":"2014-02-28T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-07T12:15:00.000+01:00",
                     "dtend":"2014-03-07T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-14T12:15:00.000+01:00",
                     "dtend":"2014-03-14T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-21T12:15:00.000+01:00",
                     "dtend":"2014-03-21T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-28T12:15:00.000+01:00",
                     "dtend":"2014-03-28T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-04T12:15:00.000+02:00",
                     "dtend":"2014-04-04T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-11T12:15:00.000+02:00",
                     "dtend":"2014-04-11T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-25T12:15:00.000+02:00",
                     "dtend":"2014-04-25T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T12:15:00.000+02:00",
                     "dtend":"2014-05-02T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-16T12:15:00.000+02:00",
                     "dtend":"2014-05-16T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 29",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-29/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-16T12:15:00.000+02:00",
                     "dtend":"2014-04-16T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-29/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T12:15:00.000+02:00",
                     "dtend":"2014-04-18T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-29/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-07T12:15:00.000+02:00",
                     "dtend":"2014-05-07T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-29/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-09T12:15:00.000+02:00",
                     "dtend":"2014-05-09T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-29/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-21T12:15:00.000+02:00",
                     "dtend":"2014-05-21T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"29"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-30",
               "sessions":[
                  {
                     "id":"2-30/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T12:15:00.000+01:00",
                     "dtend":"2014-01-15T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T12:15:00.000+01:00",
                     "dtend":"2014-01-22T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T12:15:00.000+01:00",
                     "dtend":"2014-01-29T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T12:15:00.000+01:00",
                     "dtend":"2014-02-05T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T12:15:00.000+01:00",
                     "dtend":"2014-02-12T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T12:15:00.000+01:00",
                     "dtend":"2014-02-19T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T12:15:00.000+01:00",
                     "dtend":"2014-02-26T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T12:15:00.000+01:00",
                     "dtend":"2014-03-05T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T12:15:00.000+01:00",
                     "dtend":"2014-03-12T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T12:15:00.000+01:00",
                     "dtend":"2014-03-19T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T12:15:00.000+01:00",
                     "dtend":"2014-03-26T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T12:15:00.000+02:00",
                     "dtend":"2014-04-02T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T12:15:00.000+02:00",
                     "dtend":"2014-04-09T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T12:15:00.000+02:00",
                     "dtend":"2014-04-23T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T12:15:00.000+02:00",
                     "dtend":"2014-04-30T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T12:15:00.000+02:00",
                     "dtend":"2014-05-14T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 30",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-30/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T12:15:00.000+02:00",
                     "dtend":"2014-04-15T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-30/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T12:15:00.000+02:00",
                     "dtend":"2014-05-06T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-30/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T12:15:00.000+02:00",
                     "dtend":"2014-05-20T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"30"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-31",
               "sessions":[
                  {
                     "id":"2-31/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T10:15:00.000+01:00",
                     "dtend":"2014-01-15T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T10:15:00.000+01:00",
                     "dtend":"2014-01-22T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T10:15:00.000+01:00",
                     "dtend":"2014-01-29T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T10:15:00.000+01:00",
                     "dtend":"2014-02-05T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T10:15:00.000+01:00",
                     "dtend":"2014-02-12T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T10:15:00.000+01:00",
                     "dtend":"2014-02-19T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T10:15:00.000+01:00",
                     "dtend":"2014-02-26T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T10:15:00.000+01:00",
                     "dtend":"2014-03-05T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T10:15:00.000+01:00",
                     "dtend":"2014-03-12T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T10:15:00.000+01:00",
                     "dtend":"2014-03-19T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T10:15:00.000+01:00",
                     "dtend":"2014-03-26T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T10:15:00.000+02:00",
                     "dtend":"2014-04-02T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T10:15:00.000+02:00",
                     "dtend":"2014-04-09T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T10:15:00.000+02:00",
                     "dtend":"2014-04-23T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T10:15:00.000+02:00",
                     "dtend":"2014-04-30T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T10:15:00.000+02:00",
                     "dtend":"2014-05-14T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 31",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-31/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T12:15:00.000+02:00",
                     "dtend":"2014-04-14T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-31/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T10:15:00.000+02:00",
                     "dtend":"2014-04-17T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-31/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T12:15:00.000+02:00",
                     "dtend":"2014-05-05T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-31/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T10:15:00.000+02:00",
                     "dtend":"2014-05-08T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-31/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T12:15:00.000+02:00",
                     "dtend":"2014-05-19T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"31"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-32",
               "sessions":[
                  {
                     "id":"2-32/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T12:15:00.000+01:00",
                     "dtend":"2014-01-14T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T12:15:00.000+01:00",
                     "dtend":"2014-01-21T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T12:15:00.000+01:00",
                     "dtend":"2014-01-28T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T12:15:00.000+01:00",
                     "dtend":"2014-02-04T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T12:15:00.000+01:00",
                     "dtend":"2014-02-11T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T12:15:00.000+01:00",
                     "dtend":"2014-02-18T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T12:15:00.000+01:00",
                     "dtend":"2014-02-25T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T12:15:00.000+01:00",
                     "dtend":"2014-03-04T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T12:15:00.000+01:00",
                     "dtend":"2014-03-11T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T12:15:00.000+01:00",
                     "dtend":"2014-03-18T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T12:15:00.000+01:00",
                     "dtend":"2014-03-25T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T12:15:00.000+02:00",
                     "dtend":"2014-04-01T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T12:15:00.000+02:00",
                     "dtend":"2014-04-08T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T12:15:00.000+02:00",
                     "dtend":"2014-04-22T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T12:15:00.000+02:00",
                     "dtend":"2014-04-29T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T12:15:00.000+02:00",
                     "dtend":"2014-05-13T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 32",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "jenssa"
                     ]
                  },
                  {
                     "id":"2-32/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T14:15:00.000+02:00",
                     "dtend":"2014-04-15T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-32/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T14:15:00.000+02:00",
                     "dtend":"2014-05-06T16:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-32/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T14:15:00.000+02:00",
                     "dtend":"2014-05-20T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"32"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-33",
               "sessions":[
                  {
                     "id":"2-33/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T12:15:00.000+01:00",
                     "dtend":"2014-01-14T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T12:15:00.000+01:00",
                     "dtend":"2014-01-21T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T12:15:00.000+01:00",
                     "dtend":"2014-01-28T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T12:15:00.000+01:00",
                     "dtend":"2014-02-04T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T12:15:00.000+01:00",
                     "dtend":"2014-02-11T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T12:15:00.000+01:00",
                     "dtend":"2014-02-18T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T12:15:00.000+01:00",
                     "dtend":"2014-02-25T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T12:15:00.000+01:00",
                     "dtend":"2014-03-04T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T12:15:00.000+01:00",
                     "dtend":"2014-03-11T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T12:15:00.000+01:00",
                     "dtend":"2014-03-18T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T12:15:00.000+01:00",
                     "dtend":"2014-03-25T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T12:15:00.000+02:00",
                     "dtend":"2014-04-01T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T12:15:00.000+02:00",
                     "dtend":"2014-04-08T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T12:15:00.000+02:00",
                     "dtend":"2014-04-22T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T12:15:00.000+02:00",
                     "dtend":"2014-04-29T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T12:15:00.000+02:00",
                     "dtend":"2014-05-13T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 33",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-33/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T10:15:00.000+02:00",
                     "dtend":"2014-04-15T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-33/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T12:15:00.000+02:00",
                     "dtend":"2014-04-15T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-33/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T10:15:00.000+02:00",
                     "dtend":"2014-05-06T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-33/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T12:15:00.000+02:00",
                     "dtend":"2014-05-06T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-33/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T10:15:00.000+02:00",
                     "dtend":"2014-05-20T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"33"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-34",
               "sessions":[
                  {
                     "id":"2-34/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T14:15:00.000+01:00",
                     "dtend":"2014-01-14T16:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T14:15:00.000+01:00",
                     "dtend":"2014-01-21T16:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T14:15:00.000+01:00",
                     "dtend":"2014-01-28T16:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T14:15:00.000+01:00",
                     "dtend":"2014-02-04T16:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T14:15:00.000+01:00",
                     "dtend":"2014-02-11T16:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T14:15:00.000+01:00",
                     "dtend":"2014-02-18T16:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T14:15:00.000+01:00",
                     "dtend":"2014-02-25T16:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T14:15:00.000+01:00",
                     "dtend":"2014-03-04T16:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T14:15:00.000+01:00",
                     "dtend":"2014-03-11T16:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T14:15:00.000+01:00",
                     "dtend":"2014-03-18T16:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T14:15:00.000+01:00",
                     "dtend":"2014-03-25T16:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T14:15:00.000+02:00",
                     "dtend":"2014-04-01T16:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T14:15:00.000+02:00",
                     "dtend":"2014-04-08T16:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T14:15:00.000+02:00",
                     "dtend":"2014-04-22T16:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T14:15:00.000+02:00",
                     "dtend":"2014-04-29T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T14:15:00.000+02:00",
                     "dtend":"2014-05-13T16:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 34",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "arnes",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-34/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T12:15:00.000+02:00",
                     "dtend":"2014-04-15T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-34/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T14:15:00.000+02:00",
                     "dtend":"2014-04-15T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-34/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T12:15:00.000+02:00",
                     "dtend":"2014-05-06T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-34/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T14:15:00.000+02:00",
                     "dtend":"2014-05-06T16:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-34/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T12:15:00.000+02:00",
                     "dtend":"2014-05-20T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"34"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-35",
               "sessions":[
                  {
                     "id":"2-35/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T12:15:00.000+01:00",
                     "dtend":"2014-01-15T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T12:15:00.000+01:00",
                     "dtend":"2014-01-22T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T12:15:00.000+01:00",
                     "dtend":"2014-01-29T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T12:15:00.000+01:00",
                     "dtend":"2014-02-05T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T12:15:00.000+01:00",
                     "dtend":"2014-02-12T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T12:15:00.000+01:00",
                     "dtend":"2014-02-19T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T12:15:00.000+01:00",
                     "dtend":"2014-02-26T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T12:15:00.000+01:00",
                     "dtend":"2014-03-05T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T12:15:00.000+01:00",
                     "dtend":"2014-03-12T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T12:15:00.000+01:00",
                     "dtend":"2014-03-19T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T12:15:00.000+01:00",
                     "dtend":"2014-03-26T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T12:15:00.000+02:00",
                     "dtend":"2014-04-02T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T12:15:00.000+02:00",
                     "dtend":"2014-04-09T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T12:15:00.000+02:00",
                     "dtend":"2014-04-23T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T12:15:00.000+02:00",
                     "dtend":"2014-04-30T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T12:15:00.000+02:00",
                     "dtend":"2014-05-14T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 35",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-35/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T14:15:00.000+02:00",
                     "dtend":"2014-04-14T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-35/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T14:15:00.000+02:00",
                     "dtend":"2014-05-05T16:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-35/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T14:15:00.000+02:00",
                     "dtend":"2014-05-19T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"35"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-36",
               "sessions":[
                  {
                     "id":"2-36/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T16:15:00.000+01:00",
                     "dtend":"2014-01-14T18:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T16:15:00.000+01:00",
                     "dtend":"2014-01-21T18:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T16:15:00.000+01:00",
                     "dtend":"2014-01-28T18:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T16:15:00.000+01:00",
                     "dtend":"2014-02-04T18:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T16:15:00.000+01:00",
                     "dtend":"2014-02-11T18:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T16:15:00.000+01:00",
                     "dtend":"2014-02-18T18:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T16:15:00.000+01:00",
                     "dtend":"2014-02-25T18:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T16:15:00.000+01:00",
                     "dtend":"2014-03-04T18:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T16:15:00.000+01:00",
                     "dtend":"2014-03-11T18:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T16:15:00.000+01:00",
                     "dtend":"2014-03-18T18:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T16:15:00.000+01:00",
                     "dtend":"2014-03-25T18:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T16:15:00.000+02:00",
                     "dtend":"2014-04-01T18:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T16:15:00.000+02:00",
                     "dtend":"2014-04-08T18:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T16:15:00.000+02:00",
                     "dtend":"2014-04-22T18:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T16:15:00.000+02:00",
                     "dtend":"2014-04-29T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T16:15:00.000+02:00",
                     "dtend":"2014-05-13T18:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 36",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ]
                  },
                  {
                     "id":"2-36/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T10:15:00.000+02:00",
                     "dtend":"2014-04-14T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-36/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T16:15:00.000+02:00",
                     "dtend":"2014-04-15T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-36/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T10:15:00.000+02:00",
                     "dtend":"2014-05-05T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-36/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T16:15:00.000+02:00",
                     "dtend":"2014-05-06T18:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-36/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T10:15:00.000+02:00",
                     "dtend":"2014-05-19T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"36"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-37",
               "sessions":[
                  {
                     "id":"2-37/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T10:15:00.000+01:00",
                     "dtend":"2014-01-15T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T10:15:00.000+01:00",
                     "dtend":"2014-01-22T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T10:15:00.000+01:00",
                     "dtend":"2014-01-29T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T10:15:00.000+01:00",
                     "dtend":"2014-02-05T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T10:15:00.000+01:00",
                     "dtend":"2014-02-12T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T10:15:00.000+01:00",
                     "dtend":"2014-02-19T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T10:15:00.000+01:00",
                     "dtend":"2014-02-26T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T10:15:00.000+01:00",
                     "dtend":"2014-03-05T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T10:15:00.000+01:00",
                     "dtend":"2014-03-12T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T10:15:00.000+01:00",
                     "dtend":"2014-03-19T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T10:15:00.000+01:00",
                     "dtend":"2014-03-26T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T10:15:00.000+02:00",
                     "dtend":"2014-04-02T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T10:15:00.000+02:00",
                     "dtend":"2014-04-09T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T10:15:00.000+02:00",
                     "dtend":"2014-04-23T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T10:15:00.000+02:00",
                     "dtend":"2014-04-30T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T10:15:00.000+02:00",
                     "dtend":"2014-05-14T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 37",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-37/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-16T10:15:00.000+02:00",
                     "dtend":"2014-04-16T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-37/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-07T10:15:00.000+02:00",
                     "dtend":"2014-05-07T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-37/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-21T10:15:00.000+02:00",
                     "dtend":"2014-05-21T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"37"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-38",
               "sessions":[
                  {
                     "id":"2-38/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T10:15:00.000+01:00",
                     "dtend":"2014-01-16T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T10:15:00.000+01:00",
                     "dtend":"2014-01-23T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T10:15:00.000+01:00",
                     "dtend":"2014-01-30T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T10:15:00.000+01:00",
                     "dtend":"2014-02-06T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T10:15:00.000+01:00",
                     "dtend":"2014-02-13T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T10:15:00.000+01:00",
                     "dtend":"2014-02-20T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T10:15:00.000+01:00",
                     "dtend":"2014-02-27T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T10:15:00.000+01:00",
                     "dtend":"2014-03-06T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T10:15:00.000+01:00",
                     "dtend":"2014-03-13T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T10:15:00.000+01:00",
                     "dtend":"2014-03-20T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T10:15:00.000+01:00",
                     "dtend":"2014-03-27T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T10:15:00.000+02:00",
                     "dtend":"2014-04-03T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T10:15:00.000+02:00",
                     "dtend":"2014-04-10T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T10:15:00.000+02:00",
                     "dtend":"2014-04-24T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T10:15:00.000+02:00",
                     "dtend":"2014-05-08T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T10:15:00.000+02:00",
                     "dtend":"2014-05-15T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 38",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "oasheim"
                     ]
                  },
                  {
                     "id":"2-38/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T14:15:00.000+02:00",
                     "dtend":"2014-04-15T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-38/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T10:15:00.000+02:00",
                     "dtend":"2014-04-17T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-38/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T14:15:00.000+02:00",
                     "dtend":"2014-04-29T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-38/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-01T10:15:00.000+02:00",
                     "dtend":"2014-05-01T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-38/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T14:15:00.000+02:00",
                     "dtend":"2014-05-20T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"38"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-39",
               "sessions":[
                  {
                     "id":"2-39/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T14:15:00.000+01:00",
                     "dtend":"2014-01-16T16:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T14:15:00.000+01:00",
                     "dtend":"2014-01-23T16:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T14:15:00.000+01:00",
                     "dtend":"2014-01-30T16:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T14:15:00.000+01:00",
                     "dtend":"2014-02-06T16:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T14:15:00.000+01:00",
                     "dtend":"2014-02-13T16:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T14:15:00.000+01:00",
                     "dtend":"2014-02-20T16:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T14:15:00.000+01:00",
                     "dtend":"2014-02-27T16:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T14:15:00.000+01:00",
                     "dtend":"2014-03-06T16:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T14:15:00.000+01:00",
                     "dtend":"2014-03-13T16:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T14:15:00.000+01:00",
                     "dtend":"2014-03-20T16:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T14:15:00.000+01:00",
                     "dtend":"2014-03-27T16:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T14:15:00.000+02:00",
                     "dtend":"2014-04-03T16:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T14:15:00.000+02:00",
                     "dtend":"2014-04-10T16:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T14:15:00.000+02:00",
                     "dtend":"2014-04-24T16:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T14:15:00.000+02:00",
                     "dtend":"2014-05-08T16:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T14:15:00.000+02:00",
                     "dtend":"2014-05-15T16:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 39",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-39/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T14:15:00.000+02:00",
                     "dtend":"2014-04-15T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-39/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T14:15:00.000+02:00",
                     "dtend":"2014-04-17T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-39/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T14:15:00.000+02:00",
                     "dtend":"2014-04-29T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-39/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-01T14:15:00.000+02:00",
                     "dtend":"2014-05-01T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-39/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T14:15:00.000+02:00",
                     "dtend":"2014-05-20T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"39"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-40",
               "sessions":[
                  {
                     "id":"2-40/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T10:15:00.000+01:00",
                     "dtend":"2014-01-14T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T10:15:00.000+01:00",
                     "dtend":"2014-01-21T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T10:15:00.000+01:00",
                     "dtend":"2014-01-28T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T10:15:00.000+01:00",
                     "dtend":"2014-02-04T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T10:15:00.000+01:00",
                     "dtend":"2014-02-11T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T10:15:00.000+01:00",
                     "dtend":"2014-02-18T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T10:15:00.000+01:00",
                     "dtend":"2014-02-25T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T10:15:00.000+01:00",
                     "dtend":"2014-03-04T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T10:15:00.000+01:00",
                     "dtend":"2014-03-11T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T10:15:00.000+01:00",
                     "dtend":"2014-03-18T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T10:15:00.000+01:00",
                     "dtend":"2014-03-25T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T10:15:00.000+02:00",
                     "dtend":"2014-04-01T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T10:15:00.000+02:00",
                     "dtend":"2014-04-08T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T10:15:00.000+02:00",
                     "dtend":"2014-04-22T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T10:15:00.000+02:00",
                     "dtend":"2014-04-29T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T10:15:00.000+02:00",
                     "dtend":"2014-05-13T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 40",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "dbozin"
                     ]
                  },
                  {
                     "id":"2-40/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T10:15:00.000+02:00",
                     "dtend":"2014-04-14T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-40/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T10:15:00.000+02:00",
                     "dtend":"2014-04-15T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-40/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T10:15:00.000+02:00",
                     "dtend":"2014-05-05T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-40/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T10:15:00.000+02:00",
                     "dtend":"2014-05-06T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-40/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T10:15:00.000+02:00",
                     "dtend":"2014-05-19T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"40"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-41",
               "sessions":[
                  {
                     "id":"2-41/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T10:15:00.000+01:00",
                     "dtend":"2014-01-16T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T10:15:00.000+01:00",
                     "dtend":"2014-01-23T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T10:15:00.000+01:00",
                     "dtend":"2014-01-30T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T10:15:00.000+01:00",
                     "dtend":"2014-02-06T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T10:15:00.000+01:00",
                     "dtend":"2014-02-13T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T10:15:00.000+01:00",
                     "dtend":"2014-02-20T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T10:15:00.000+01:00",
                     "dtend":"2014-02-27T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T10:15:00.000+01:00",
                     "dtend":"2014-03-06T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T10:15:00.000+01:00",
                     "dtend":"2014-03-13T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T10:15:00.000+01:00",
                     "dtend":"2014-03-20T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T10:15:00.000+01:00",
                     "dtend":"2014-03-27T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T10:15:00.000+02:00",
                     "dtend":"2014-04-03T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T10:15:00.000+02:00",
                     "dtend":"2014-04-10T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T10:15:00.000+02:00",
                     "dtend":"2014-04-24T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T10:15:00.000+02:00",
                     "dtend":"2014-05-08T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T10:15:00.000+02:00",
                     "dtend":"2014-05-15T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 41",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-41/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T12:15:00.000+02:00",
                     "dtend":"2014-04-15T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-41/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T12:15:00.000+02:00",
                     "dtend":"2014-04-29T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-41/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T12:15:00.000+02:00",
                     "dtend":"2014-05-20T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"41"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-42",
               "sessions":[
                  {
                     "id":"2-42/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T16:15:00.000+01:00",
                     "dtend":"2014-01-14T18:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T16:15:00.000+01:00",
                     "dtend":"2014-01-21T18:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T16:15:00.000+01:00",
                     "dtend":"2014-01-28T18:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T16:15:00.000+01:00",
                     "dtend":"2014-02-04T18:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T16:15:00.000+01:00",
                     "dtend":"2014-02-11T18:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T16:15:00.000+01:00",
                     "dtend":"2014-02-18T18:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T16:15:00.000+01:00",
                     "dtend":"2014-02-25T18:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T16:15:00.000+01:00",
                     "dtend":"2014-03-04T18:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T16:15:00.000+01:00",
                     "dtend":"2014-03-11T18:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T16:15:00.000+01:00",
                     "dtend":"2014-03-18T18:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T16:15:00.000+01:00",
                     "dtend":"2014-03-25T18:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T16:15:00.000+02:00",
                     "dtend":"2014-04-01T18:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T16:15:00.000+02:00",
                     "dtend":"2014-04-08T18:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T16:15:00.000+02:00",
                     "dtend":"2014-04-22T18:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T16:15:00.000+02:00",
                     "dtend":"2014-04-29T18:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T16:15:00.000+02:00",
                     "dtend":"2014-05-13T18:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 42",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-42/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T12:15:00.000+02:00",
                     "dtend":"2014-04-14T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-42/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T16:15:00.000+02:00",
                     "dtend":"2014-04-15T18:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-42/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T12:15:00.000+02:00",
                     "dtend":"2014-05-05T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-42/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T16:15:00.000+02:00",
                     "dtend":"2014-05-06T18:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-42/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T12:15:00.000+02:00",
                     "dtend":"2014-05-19T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"42"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-43",
               "sessions":[
                  {
                     "id":"2-43/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T14:15:00.000+01:00",
                     "dtend":"2014-01-15T16:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T14:15:00.000+01:00",
                     "dtend":"2014-01-22T16:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T14:15:00.000+01:00",
                     "dtend":"2014-01-29T16:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T14:15:00.000+01:00",
                     "dtend":"2014-02-05T16:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T14:15:00.000+01:00",
                     "dtend":"2014-02-12T16:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T14:15:00.000+01:00",
                     "dtend":"2014-02-19T16:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T14:15:00.000+01:00",
                     "dtend":"2014-02-26T16:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T14:15:00.000+01:00",
                     "dtend":"2014-03-05T16:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T14:15:00.000+01:00",
                     "dtend":"2014-03-12T16:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T14:15:00.000+01:00",
                     "dtend":"2014-03-19T16:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T14:15:00.000+01:00",
                     "dtend":"2014-03-26T16:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T14:15:00.000+02:00",
                     "dtend":"2014-04-02T16:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T14:15:00.000+02:00",
                     "dtend":"2014-04-09T16:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T14:15:00.000+02:00",
                     "dtend":"2014-04-23T16:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T14:15:00.000+02:00",
                     "dtend":"2014-04-30T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T14:15:00.000+02:00",
                     "dtend":"2014-05-14T16:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 43",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "arnes"
                     ]
                  },
                  {
                     "id":"2-43/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T10:15:00.000+02:00",
                     "dtend":"2014-04-15T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-43/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T10:15:00.000+02:00",
                     "dtend":"2014-05-06T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-43/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T10:15:00.000+02:00",
                     "dtend":"2014-05-20T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"43"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-44",
               "sessions":[
                  {
                     "id":"2-44/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T12:15:00.000+01:00",
                     "dtend":"2014-01-16T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T12:15:00.000+01:00",
                     "dtend":"2014-01-23T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T12:15:00.000+01:00",
                     "dtend":"2014-01-30T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T12:15:00.000+01:00",
                     "dtend":"2014-02-06T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T12:15:00.000+01:00",
                     "dtend":"2014-02-13T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T12:15:00.000+01:00",
                     "dtend":"2014-02-20T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T12:15:00.000+01:00",
                     "dtend":"2014-02-27T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T12:15:00.000+01:00",
                     "dtend":"2014-03-06T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T12:15:00.000+01:00",
                     "dtend":"2014-03-13T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T12:15:00.000+01:00",
                     "dtend":"2014-03-20T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T12:15:00.000+01:00",
                     "dtend":"2014-03-27T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T12:15:00.000+02:00",
                     "dtend":"2014-04-03T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T12:15:00.000+02:00",
                     "dtend":"2014-04-10T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T12:15:00.000+02:00",
                     "dtend":"2014-04-24T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T12:15:00.000+02:00",
                     "dtend":"2014-05-08T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T12:15:00.000+02:00",
                     "dtend":"2014-05-15T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 44",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ]
                  },
                  {
                     "id":"2-44/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T10:15:00.000+02:00",
                     "dtend":"2014-04-14T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-44/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T12:15:00.000+02:00",
                     "dtend":"2014-04-17T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-44/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-28T10:15:00.000+02:00",
                     "dtend":"2014-04-28T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-44/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-01T12:15:00.000+02:00",
                     "dtend":"2014-05-01T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-44/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T10:15:00.000+02:00",
                     "dtend":"2014-05-19T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"44"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-45",
               "sessions":[
                  {
                     "id":"2-45/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T12:15:00.000+01:00",
                     "dtend":"2014-01-15T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T12:15:00.000+01:00",
                     "dtend":"2014-01-22T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T12:15:00.000+01:00",
                     "dtend":"2014-01-29T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T12:15:00.000+01:00",
                     "dtend":"2014-02-05T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T12:15:00.000+01:00",
                     "dtend":"2014-02-12T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T12:15:00.000+01:00",
                     "dtend":"2014-02-19T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T12:15:00.000+01:00",
                     "dtend":"2014-02-26T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T12:15:00.000+01:00",
                     "dtend":"2014-03-05T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T12:15:00.000+01:00",
                     "dtend":"2014-03-12T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T12:15:00.000+01:00",
                     "dtend":"2014-03-19T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T12:15:00.000+01:00",
                     "dtend":"2014-03-26T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T12:15:00.000+02:00",
                     "dtend":"2014-04-02T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T12:15:00.000+02:00",
                     "dtend":"2014-04-09T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T12:15:00.000+02:00",
                     "dtend":"2014-04-23T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T12:15:00.000+02:00",
                     "dtend":"2014-04-30T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T12:15:00.000+02:00",
                     "dtend":"2014-05-14T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 45",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-45/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-16T12:15:00.000+02:00",
                     "dtend":"2014-04-16T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-45/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-07T12:15:00.000+02:00",
                     "dtend":"2014-05-07T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-45/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-21T12:15:00.000+02:00",
                     "dtend":"2014-05-21T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"45"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-46",
               "sessions":[
                  {
                     "id":"2-46/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T12:15:00.000+01:00",
                     "dtend":"2014-01-16T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T12:15:00.000+01:00",
                     "dtend":"2014-01-23T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T12:15:00.000+01:00",
                     "dtend":"2014-01-30T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T12:15:00.000+01:00",
                     "dtend":"2014-02-06T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T12:15:00.000+01:00",
                     "dtend":"2014-02-13T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T12:15:00.000+01:00",
                     "dtend":"2014-02-20T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T12:15:00.000+01:00",
                     "dtend":"2014-02-27T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T12:15:00.000+01:00",
                     "dtend":"2014-03-06T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T12:15:00.000+01:00",
                     "dtend":"2014-03-13T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T12:15:00.000+01:00",
                     "dtend":"2014-03-20T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T12:15:00.000+01:00",
                     "dtend":"2014-03-27T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T12:15:00.000+02:00",
                     "dtend":"2014-04-03T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T12:15:00.000+02:00",
                     "dtend":"2014-04-10T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T12:15:00.000+02:00",
                     "dtend":"2014-04-24T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T12:15:00.000+02:00",
                     "dtend":"2014-05-08T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T12:15:00.000+02:00",
                     "dtend":"2014-05-15T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 46",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "sjaastad"
                     ]
                  },
                  {
                     "id":"2-46/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T14:15:00.000+02:00",
                     "dtend":"2014-04-14T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-46/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-28T14:15:00.000+02:00",
                     "dtend":"2014-04-28T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-46/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T14:15:00.000+02:00",
                     "dtend":"2014-05-19T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"46"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-47",
               "sessions":[
                  {
                     "id":"2-47/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-17T12:15:00.000+01:00",
                     "dtend":"2014-01-17T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-24T12:15:00.000+01:00",
                     "dtend":"2014-01-24T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-31T12:15:00.000+01:00",
                     "dtend":"2014-01-31T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-07T12:15:00.000+01:00",
                     "dtend":"2014-02-07T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-14T12:15:00.000+01:00",
                     "dtend":"2014-02-14T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-21T12:15:00.000+01:00",
                     "dtend":"2014-02-21T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-28T12:15:00.000+01:00",
                     "dtend":"2014-02-28T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-07T12:15:00.000+01:00",
                     "dtend":"2014-03-07T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-14T12:15:00.000+01:00",
                     "dtend":"2014-03-14T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-21T12:15:00.000+01:00",
                     "dtend":"2014-03-21T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-28T12:15:00.000+01:00",
                     "dtend":"2014-03-28T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-04T12:15:00.000+02:00",
                     "dtend":"2014-04-04T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-11T12:15:00.000+02:00",
                     "dtend":"2014-04-11T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-25T12:15:00.000+02:00",
                     "dtend":"2014-04-25T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T12:15:00.000+02:00",
                     "dtend":"2014-05-02T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-16T12:15:00.000+02:00",
                     "dtend":"2014-05-16T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 47",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "elikseg"
                     ]
                  },
                  {
                     "id":"2-47/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-16T12:15:00.000+02:00",
                     "dtend":"2014-04-16T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-47/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-07T12:15:00.000+02:00",
                     "dtend":"2014-05-07T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-47/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-21T12:15:00.000+02:00",
                     "dtend":"2014-05-21T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"47"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-48",
               "sessions":[
                  {
                     "id":"2-48/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T08:15:00.000+01:00",
                     "dtend":"2014-01-14T10:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T08:15:00.000+01:00",
                     "dtend":"2014-01-21T10:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T08:15:00.000+01:00",
                     "dtend":"2014-01-28T10:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T08:15:00.000+01:00",
                     "dtend":"2014-02-04T10:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T08:15:00.000+01:00",
                     "dtend":"2014-02-11T10:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T08:15:00.000+01:00",
                     "dtend":"2014-02-18T10:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T08:15:00.000+01:00",
                     "dtend":"2014-02-25T10:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T08:15:00.000+01:00",
                     "dtend":"2014-03-04T10:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T08:15:00.000+01:00",
                     "dtend":"2014-03-11T10:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T08:15:00.000+01:00",
                     "dtend":"2014-03-18T10:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T08:15:00.000+01:00",
                     "dtend":"2014-03-25T10:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T08:15:00.000+02:00",
                     "dtend":"2014-04-01T10:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T08:15:00.000+02:00",
                     "dtend":"2014-04-08T10:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T08:15:00.000+02:00",
                     "dtend":"2014-04-22T10:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T08:15:00.000+02:00",
                     "dtend":"2014-04-29T10:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T08:15:00.000+02:00",
                     "dtend":"2014-05-13T10:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 48",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-48/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T08:15:00.000+02:00",
                     "dtend":"2014-04-15T10:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-48/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-16T10:15:00.000+02:00",
                     "dtend":"2014-04-16T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-48/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T08:15:00.000+02:00",
                     "dtend":"2014-05-06T10:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-48/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-07T10:15:00.000+02:00",
                     "dtend":"2014-05-07T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-48/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-21T10:15:00.000+02:00",
                     "dtend":"2014-05-21T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"48"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-49",
               "sessions":[
                  {
                     "id":"2-49/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T08:15:00.000+01:00",
                     "dtend":"2014-01-15T10:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T08:15:00.000+01:00",
                     "dtend":"2014-01-22T10:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T08:15:00.000+01:00",
                     "dtend":"2014-01-29T10:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T08:15:00.000+01:00",
                     "dtend":"2014-02-05T10:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T08:15:00.000+01:00",
                     "dtend":"2014-02-12T10:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T08:15:00.000+01:00",
                     "dtend":"2014-02-19T10:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T08:15:00.000+01:00",
                     "dtend":"2014-02-26T10:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T08:15:00.000+01:00",
                     "dtend":"2014-03-05T10:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T08:15:00.000+01:00",
                     "dtend":"2014-03-12T10:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T08:15:00.000+01:00",
                     "dtend":"2014-03-19T10:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T08:15:00.000+01:00",
                     "dtend":"2014-03-26T10:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T08:15:00.000+02:00",
                     "dtend":"2014-04-02T10:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T08:15:00.000+02:00",
                     "dtend":"2014-04-09T10:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T08:15:00.000+02:00",
                     "dtend":"2014-04-23T10:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T08:15:00.000+02:00",
                     "dtend":"2014-04-30T10:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T08:15:00.000+02:00",
                     "dtend":"2014-05-14T10:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 49",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"152"
                        }
                     ],
                     "staff":[
                        "tovepe"
                     ]
                  },
                  {
                     "id":"2-49/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-15T12:15:00.000+02:00",
                     "dtend":"2014-04-15T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-49/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T08:15:00.000+02:00",
                     "dtend":"2014-04-17T10:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-49/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-06T12:15:00.000+02:00",
                     "dtend":"2014-05-06T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-49/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T08:15:00.000+02:00",
                     "dtend":"2014-05-08T10:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-49/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-20T12:15:00.000+02:00",
                     "dtend":"2014-05-20T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"49"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-50",
               "sessions":[
                  {
                     "id":"2-50/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T10:15:00.000+01:00",
                     "dtend":"2014-01-15T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T10:15:00.000+01:00",
                     "dtend":"2014-01-22T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T10:15:00.000+01:00",
                     "dtend":"2014-01-29T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T10:15:00.000+01:00",
                     "dtend":"2014-02-05T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T10:15:00.000+01:00",
                     "dtend":"2014-02-12T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T10:15:00.000+01:00",
                     "dtend":"2014-02-19T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T10:15:00.000+01:00",
                     "dtend":"2014-02-26T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T10:15:00.000+01:00",
                     "dtend":"2014-03-05T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T10:15:00.000+01:00",
                     "dtend":"2014-03-12T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T10:15:00.000+01:00",
                     "dtend":"2014-03-19T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T10:15:00.000+01:00",
                     "dtend":"2014-03-26T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T10:15:00.000+02:00",
                     "dtend":"2014-04-02T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T10:15:00.000+02:00",
                     "dtend":"2014-04-09T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T10:15:00.000+02:00",
                     "dtend":"2014-04-23T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T10:15:00.000+02:00",
                     "dtend":"2014-04-30T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T10:15:00.000+02:00",
                     "dtend":"2014-05-14T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 50",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "oskar"
                     ]
                  },
                  {
                     "id":"2-50/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T14:15:00.000+02:00",
                     "dtend":"2014-04-14T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-50/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T14:15:00.000+02:00",
                     "dtend":"2014-05-05T16:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-50/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T14:15:00.000+02:00",
                     "dtend":"2014-05-19T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"50"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-51",
               "sessions":[
                  {
                     "id":"2-51/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T12:15:00.000+01:00",
                     "dtend":"2014-01-16T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T12:15:00.000+01:00",
                     "dtend":"2014-01-23T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T12:15:00.000+01:00",
                     "dtend":"2014-01-30T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T12:15:00.000+01:00",
                     "dtend":"2014-02-06T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T12:15:00.000+01:00",
                     "dtend":"2014-02-13T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T12:15:00.000+01:00",
                     "dtend":"2014-02-20T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T12:15:00.000+01:00",
                     "dtend":"2014-02-27T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T12:15:00.000+01:00",
                     "dtend":"2014-03-06T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T12:15:00.000+01:00",
                     "dtend":"2014-03-13T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T12:15:00.000+01:00",
                     "dtend":"2014-03-20T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T12:15:00.000+01:00",
                     "dtend":"2014-03-27T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T12:15:00.000+02:00",
                     "dtend":"2014-04-03T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T12:15:00.000+02:00",
                     "dtend":"2014-04-10T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T12:15:00.000+02:00",
                     "dtend":"2014-04-24T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T12:15:00.000+02:00",
                     "dtend":"2014-05-08T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T12:15:00.000+02:00",
                     "dtend":"2014-05-15T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 51",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "gunber"
                     ]
                  },
                  {
                     "id":"2-51/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-16T12:15:00.000+02:00",
                     "dtend":"2014-04-16T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-51/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T12:15:00.000+02:00",
                     "dtend":"2014-04-30T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-51/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-21T12:15:00.000+02:00",
                     "dtend":"2014-05-21T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"51"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-52",
               "sessions":[
                  {
                     "id":"2-52/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T14:15:00.000+01:00",
                     "dtend":"2014-01-15T16:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T14:15:00.000+01:00",
                     "dtend":"2014-01-22T16:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T14:15:00.000+01:00",
                     "dtend":"2014-01-29T16:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T14:15:00.000+01:00",
                     "dtend":"2014-02-05T16:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T14:15:00.000+01:00",
                     "dtend":"2014-02-12T16:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T14:15:00.000+01:00",
                     "dtend":"2014-02-19T16:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T14:15:00.000+01:00",
                     "dtend":"2014-02-26T16:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T14:15:00.000+01:00",
                     "dtend":"2014-03-05T16:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T14:15:00.000+01:00",
                     "dtend":"2014-03-12T16:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T14:15:00.000+01:00",
                     "dtend":"2014-03-19T16:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T14:15:00.000+01:00",
                     "dtend":"2014-03-26T16:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T14:15:00.000+02:00",
                     "dtend":"2014-04-02T16:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T14:15:00.000+02:00",
                     "dtend":"2014-04-09T16:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T14:15:00.000+02:00",
                     "dtend":"2014-04-23T16:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T14:15:00.000+02:00",
                     "dtend":"2014-04-30T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T14:15:00.000+02:00",
                     "dtend":"2014-05-14T16:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 52",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"206"
                        }
                     ],
                     "staff":[
                        "ingernp"
                     ]
                  },
                  {
                     "id":"2-52/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T14:15:00.000+02:00",
                     "dtend":"2014-04-14T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-52/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T14:15:00.000+02:00",
                     "dtend":"2014-05-05T16:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-52/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T14:15:00.000+02:00",
                     "dtend":"2014-05-19T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"52"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-53",
               "sessions":[
                  {
                     "id":"2-53/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-15T14:15:00.000+01:00",
                     "dtend":"2014-01-15T16:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-22T14:15:00.000+01:00",
                     "dtend":"2014-01-22T16:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-29T14:15:00.000+01:00",
                     "dtend":"2014-01-29T16:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-05T14:15:00.000+01:00",
                     "dtend":"2014-02-05T16:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-12T14:15:00.000+01:00",
                     "dtend":"2014-02-12T16:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-19T14:15:00.000+01:00",
                     "dtend":"2014-02-19T16:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-26T14:15:00.000+01:00",
                     "dtend":"2014-02-26T16:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-05T14:15:00.000+01:00",
                     "dtend":"2014-03-05T16:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-12T14:15:00.000+01:00",
                     "dtend":"2014-03-12T16:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-19T14:15:00.000+01:00",
                     "dtend":"2014-03-19T16:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-26T14:15:00.000+01:00",
                     "dtend":"2014-03-26T16:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-02T14:15:00.000+02:00",
                     "dtend":"2014-04-02T16:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-09T14:15:00.000+02:00",
                     "dtend":"2014-04-09T16:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-23T14:15:00.000+02:00",
                     "dtend":"2014-04-23T16:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-30T14:15:00.000+02:00",
                     "dtend":"2014-04-30T16:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-14T14:15:00.000+02:00",
                     "dtend":"2014-05-14T16:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 53",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ],
                     "staff":[
                        "ragnarm"
                     ]
                  },
                  {
                     "id":"2-53/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-16T10:15:00.000+02:00",
                     "dtend":"2014-04-16T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-53/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-07T10:15:00.000+02:00",
                     "dtend":"2014-05-07T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-53/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-21T10:15:00.000+02:00",
                     "dtend":"2014-05-21T12:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"53"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-54",
               "sessions":[
                  {
                     "id":"2-54/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-16T10:15:00.000+01:00",
                     "dtend":"2014-01-16T12:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-23T10:15:00.000+01:00",
                     "dtend":"2014-01-23T12:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-30T10:15:00.000+01:00",
                     "dtend":"2014-01-30T12:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-06T10:15:00.000+01:00",
                     "dtend":"2014-02-06T12:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-13T10:15:00.000+01:00",
                     "dtend":"2014-02-13T12:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-20T10:15:00.000+01:00",
                     "dtend":"2014-02-20T12:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-27T10:15:00.000+01:00",
                     "dtend":"2014-02-27T12:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-06T10:15:00.000+01:00",
                     "dtend":"2014-03-06T12:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-13T10:15:00.000+01:00",
                     "dtend":"2014-03-13T12:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-20T10:15:00.000+01:00",
                     "dtend":"2014-03-20T12:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-27T10:15:00.000+01:00",
                     "dtend":"2014-03-27T12:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-03T10:15:00.000+02:00",
                     "dtend":"2014-04-03T12:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-10T10:15:00.000+02:00",
                     "dtend":"2014-04-10T12:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-24T10:15:00.000+02:00",
                     "dtend":"2014-04-24T12:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T10:15:00.000+02:00",
                     "dtend":"2014-05-08T12:00:00.000+02:00",
                     "weeknr":19,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-15T10:15:00.000+02:00",
                     "dtend":"2014-05-15T12:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 54",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"204"
                        }
                     ],
                     "staff":[
                        "egilhj",
                        "jos"
                     ]
                  },
                  {
                     "id":"2-54/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T12:15:00.000+02:00",
                     "dtend":"2014-04-14T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-54/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T10:15:00.000+02:00",
                     "dtend":"2014-04-17T12:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-54/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-28T12:15:00.000+02:00",
                     "dtend":"2014-04-28T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-54/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-01T10:15:00.000+02:00",
                     "dtend":"2014-05-01T12:00:00.000+02:00",
                     "weeknr":18,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-54/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T12:15:00.000+02:00",
                     "dtend":"2014-05-19T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"54"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-55",
               "sessions":[
                  {
                     "id":"2-55/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-14T08:15:00.000+01:00",
                     "dtend":"2014-01-14T10:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-21T08:15:00.000+01:00",
                     "dtend":"2014-01-21T10:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-28T08:15:00.000+01:00",
                     "dtend":"2014-01-28T10:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-04T08:15:00.000+01:00",
                     "dtend":"2014-02-04T10:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-11T08:15:00.000+01:00",
                     "dtend":"2014-02-11T10:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-18T08:15:00.000+01:00",
                     "dtend":"2014-02-18T10:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-25T08:15:00.000+01:00",
                     "dtend":"2014-02-25T10:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-04T08:15:00.000+01:00",
                     "dtend":"2014-03-04T10:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-11T08:15:00.000+01:00",
                     "dtend":"2014-03-11T10:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-18T08:15:00.000+01:00",
                     "dtend":"2014-03-18T10:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-25T08:15:00.000+01:00",
                     "dtend":"2014-03-25T10:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-01T08:15:00.000+02:00",
                     "dtend":"2014-04-01T10:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-08T08:15:00.000+02:00",
                     "dtend":"2014-04-08T10:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-22T08:15:00.000+02:00",
                     "dtend":"2014-04-22T10:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/18",
                     "seqno":1,
                     "dtstart":"2014-04-29T08:15:00.000+02:00",
                     "dtend":"2014-04-29T10:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-13T08:15:00.000+02:00",
                     "dtend":"2014-05-13T10:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 55",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"205"
                        }
                     ],
                     "staff":[
                        "karibsl"
                     ]
                  },
                  {
                     "id":"2-55/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-17T12:15:00.000+02:00",
                     "dtend":"2014-04-17T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-55/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-08T12:15:00.000+02:00",
                     "dtend":"2014-05-08T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-55/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-22T12:15:00.000+02:00",
                     "dtend":"2014-05-22T14:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"55"
               }
            },
            {
               "teachingmethod":"SEM",
               "teachingmethodname":"Seminar",
               "id":"2-56",
               "sessions":[
                  {
                     "id":"2-56/1/3",
                     "seqno":1,
                     "dtstart":"2014-01-17T12:15:00.000+01:00",
                     "dtend":"2014-01-17T14:00:00.000+01:00",
                     "weeknr":3,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/4",
                     "seqno":1,
                     "dtstart":"2014-01-24T12:15:00.000+01:00",
                     "dtend":"2014-01-24T14:00:00.000+01:00",
                     "weeknr":4,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/5",
                     "seqno":1,
                     "dtstart":"2014-01-31T12:15:00.000+01:00",
                     "dtend":"2014-01-31T14:00:00.000+01:00",
                     "weeknr":5,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/6",
                     "seqno":1,
                     "dtstart":"2014-02-07T12:15:00.000+01:00",
                     "dtend":"2014-02-07T14:00:00.000+01:00",
                     "weeknr":6,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/7",
                     "seqno":1,
                     "dtstart":"2014-02-14T12:15:00.000+01:00",
                     "dtend":"2014-02-14T14:00:00.000+01:00",
                     "weeknr":7,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/8",
                     "seqno":1,
                     "dtstart":"2014-02-21T12:15:00.000+01:00",
                     "dtend":"2014-02-21T14:00:00.000+01:00",
                     "weeknr":8,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/9",
                     "seqno":1,
                     "dtstart":"2014-02-28T12:15:00.000+01:00",
                     "dtend":"2014-02-28T14:00:00.000+01:00",
                     "weeknr":9,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/10",
                     "seqno":1,
                     "dtstart":"2014-03-07T12:15:00.000+01:00",
                     "dtend":"2014-03-07T14:00:00.000+01:00",
                     "weeknr":10,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/11",
                     "seqno":1,
                     "dtstart":"2014-03-14T12:15:00.000+01:00",
                     "dtend":"2014-03-14T14:00:00.000+01:00",
                     "weeknr":11,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/12",
                     "seqno":1,
                     "dtstart":"2014-03-21T12:15:00.000+01:00",
                     "dtend":"2014-03-21T14:00:00.000+01:00",
                     "weeknr":12,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/13",
                     "seqno":1,
                     "dtstart":"2014-03-28T12:15:00.000+01:00",
                     "dtend":"2014-03-28T14:00:00.000+01:00",
                     "weeknr":13,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/14",
                     "seqno":1,
                     "dtstart":"2014-04-04T12:15:00.000+02:00",
                     "dtend":"2014-04-04T14:00:00.000+02:00",
                     "weeknr":14,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/15",
                     "seqno":1,
                     "dtstart":"2014-04-11T12:15:00.000+02:00",
                     "dtend":"2014-04-11T14:00:00.000+02:00",
                     "weeknr":15,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/17",
                     "seqno":1,
                     "dtstart":"2014-04-25T12:15:00.000+02:00",
                     "dtend":"2014-04-25T14:00:00.000+02:00",
                     "weeknr":17,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/18",
                     "seqno":1,
                     "dtstart":"2014-05-02T12:15:00.000+02:00",
                     "dtend":"2014-05-02T14:00:00.000+02:00",
                     "weeknr":18,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/20",
                     "seqno":1,
                     "dtstart":"2014-05-16T12:15:00.000+02:00",
                     "dtend":"2014-05-16T14:00:00.000+02:00",
                     "weeknr":20,
                     "status":"active",
                     "title":"Seminargruppe 56 (RES)",
                     "room":[
                        {
                           "buildingid":"BL16",
                           "roomid":"203"
                        }
                     ]
                  },
                  {
                     "id":"2-56/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-14T14:15:00.000+02:00",
                     "dtend":"2014-04-14T16:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-56/1/16",
                     "seqno":1,
                     "dtstart":"2014-04-18T12:15:00.000+02:00",
                     "dtend":"2014-04-18T14:00:00.000+02:00",
                     "weeknr":16,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-56/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-05T14:15:00.000+02:00",
                     "dtend":"2014-05-05T16:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-56/1/19",
                     "seqno":1,
                     "dtstart":"2014-05-09T12:15:00.000+02:00",
                     "dtend":"2014-05-09T14:00:00.000+02:00",
                     "weeknr":19,
                     "status":"cancelled"
                  },
                  {
                     "id":"2-56/1/21",
                     "seqno":1,
                     "dtstart":"2014-05-19T14:15:00.000+02:00",
                     "dtend":"2014-05-19T16:00:00.000+02:00",
                     "weeknr":21,
                     "status":"cancelled"
                  }
               ],
               "party":{
                  "name":"56"
               }
            }
         ]
      }
    };
  
  var retrievedScheduleDeferred = $.Deferred();
  
  retrievedScheduleDeferred.resolve();
   
  /*
  vrtxAdmin.serverFacade.getJSON(".?action=course-schedule", {
    success: function(data, xhr, textStatus) {
      retrievedScheduleData = data;
      retrievedScheduleDeferred.resolve();
    },
    error: function(xhr, textStatus) {
      if(textStatus === "parsererror") { // Running vortikal
        retrievedScheduleDeferred.resolve();
      }
    }
  });
  */
  
  vrtxEditor.editorForm.find(".properties").hide();
    
  initMultipleInputFields();
   
  $.when(retrievedScheduleDeferred, vrtxEditor.multipleFieldsBoxesDeferred).done(function() {
    if(retrievedScheduleData == null)  return;

    var isEn = vrtxAdmin.lang == "en",
        i18n = {
          "01": "jan",
          "02": "feb",
          "03": "mar",
          "04": "apr",
          "05": (isEn ? "may" : "mai"),
          "06": "jun",
          "07": "jul",
          "08": "aug",
          "09": "sept",
          "10": (isEn ? "oct" : "okt"),
          "11": (isEn ? "nov" : "nov"),
          "12": (isEn ? "dec" : "des"),
          room: (isEn ? "room" : "rom"),
          titles: {
            plenary: (isEn ? "Plenary teaching" : "Fellesundervisning"),
            group: (isEn ? "Group teaching" : "Partiundervisning")
          },
          "cancelled": (isEn ? "(cancelled in scheduling system)" : "(avlyst i timeplanleggingssystemet)"),
          "vrtx-title": (isEn ? "Title:" : "Tittel:"),
          "vrtx-staff": (isEn ? "Staff:" : "Forelesere:"),
          "vrtx-staff-external": (isEn ? "External staff:" : "Eksterne forelesere:"),
          "vrtx-resources": (isEn ? "Resources:" : "Ressurser:"),
          "vrtx-status": (isEn ? "Cancel" : "Avlys"),
          "vrtx-staff-external-name": (isEn ? "Name" : "Navn"),
          "vrtx-staff-external-url": (isEn ? "Link" : "Lenke"),
          "vrtx-resources-title": (isEn ? "Title" : "Tittel"),
          "vrtx-resources-url": (isEn ? "Link" : "Lenke")
        };

    var sessionsLookup = {};
      
    // Generate HTML
    var html = "<div class='accordion-title'>" + i18n.titles.plenary + "</div>" +
               generateCourseScheduleHTMLForType(retrievedScheduleData, "plenary", true, sessionsLookup, i18n) +
               "<div class='accordion-title'>" + i18n.titles.group + "</div>" +
               generateCourseScheduleHTMLForType(retrievedScheduleData, "group", false, sessionsLookup, i18n);
      
    // Add HTML to DOM
    $(".properties").prepend("<div class='vrtx-grouped'>" + html + "</div>"); 
       
    // Accordions
    // TODO: general accordion enhancing
    var accordionOnActivateTier3 = function (id, e, ui, accordion) {
      if(ui.newHeader[0]) { // Enhance multiple fields in session
        var sessionId = ui.newHeader[0].id;
        var session = sessionsLookup[id][sessionId];
        if(session && !session.isEnhanced) { // If not already enhanced
          var multiples = session.multiples;
          for(var i = multiplesLen = multiples.length; i--;) {
            var m = multiples[i];
            enhanceMultipleInputFields(m.name + "-" + sessionId, m.movable, m.browsable, 50, m.json);
          }
          session.isEnhanced = true;
          
          var newHeader = $(ui.newHeader);
          var contentWrp = newHeader.parent().find(".accordion-content");
          newHeader.closest(".session").addClass("session-touched");
        }
      } else { // Update custom session title on close
        var session = $(ui.oldHeader).closest("div");
        var titleElm = session.find("> .header > .header-title");
        var newTitle = session.find("> .accordion-content > div:first-child input[type='text']")
        if(newTitle.length && newTitle.val() != "") {
          titleElm.html(newTitle.val());
        } else {
          titleElm.html(titleElm.attr("data-orig-title"));
        }
      }
    };  
    
    var accordionOnActivateTier2 = function (id, isTier1, e, ui, accordion) {
      if(isTier1) {
        accordionOnActivateTier3(id, e, ui, accordion);
      } else {
        if(ui.newHeader[0]) {
          var contentWrp = $(ui.newHeader[0]).parent().find(".accordion-content");
          var optsH5 = {
            elem: contentWrp.find(".vrtx-grouped"),
            headerSelector: "h5",
            onActivate: function (e, ui, accordion) {
              accordionOnActivateTier3(id, e, ui, accordion);
            },
            animationSpeed: 200
          };
          var accH5 = new VrtxAccordion(optsH5);
          accH5.create();
          optsH5.elem.addClass("fast");
        }
      }
    };
    
    var accordionOnActivateTier1 = function (isTier1, e, ui, accordion) {
      if(ui.newHeader[0]) {
        var id = ui.newHeader[0].id;
        var contentWrp = $("#" + id).parent().find(".accordion-content");
        if(isTier1) { // Lookup and add sessions HTML to DOM
          if(!contentWrp.children().length) { // If not already added
            contentWrp.html("<div class='vrtx-grouped'>" + sessionsLookup["plenary"].html + "</div>");
          }
        }
        var optsH4 = {
          elem: contentWrp.find(".vrtx-grouped"),
          headerSelector: "h4",
          onActivate: function (e, ui, accordion) {
            if(!isTier1 && ui.newHeader[0]) { // Lookup and add sessions HTML to DOM
              id = ui.newHeader[0].id;
              var contentWrp = $("#" + id).parent().find(".accordion-content");
              if(!contentWrp.children().length) { // If not already added
                contentWrp.html("<div class='vrtx-grouped'>" + sessionsLookup[id].html + "</div>");
              }
            }
            accordionOnActivateTier2(isTier1 ? "plenary" : id, isTier1, e, ui, accordion);
          },
          animationSpeed: 200
        };
        var accH4 = new VrtxAccordion(optsH4);
        accH4.create();
        optsH4.elem.addClass("fast");
      }
    };
    
    var optsH3 = {
      elem: vrtxEditor.editorForm.find(".properties > .vrtx-grouped"),
      headerSelector: "h3",
      onActivate: function (e, ui, accordion) {
        if(ui.newHeader[0]) {
          var ident = $(ui.newHeader[0]).closest(".accordion-wrapper");
          accordionOnActivateTier1(ident.hasClass("skip-tier"), e, ui, accordion);
        }
      },
      animationSpeed: 200
    };
    var accH3 = new VrtxAccordion(optsH3);
    accH3.create();
    optsH3.elem.addClass("fast");

    
    JSON_ELEMENTS_INITIALIZED.resolve();
    
    var waitALittle = setTimeout(function() {
      vrtxEditor.editorForm.find(".properties").show();
    }, 50);
  });
}

function generateCourseScheduleHTMLForType(json, type, skipTier, sessionsLookup, i18n) {
  var generateCourseScheduleDateFunc = generateCourseScheduleDate,
      generateCourseScheduleSessionFunc = generateCourseScheduleSession,
      generateCourseScheduleContentFromSessionDataFunc = generateCourseScheduleContentFromSessionData;
      jsonType = json[type],
      descs = jsonType["vrtx-editable-description"],
      data = jsonType["data"],
      dtShortLast = "",
      html = "",
      htmlMiddle = "",
      sessionsHtml = "";
      
  // Store sessions HTML and multiple descriptions in lookup object
  for(var i = 0, len = data.length; i < len; i++) {
    var dt = data[i],
        dtShort = dt.teachingmethod.toLowerCase(),
        id = skipTier ? type : dtShort + "-" + dt.id,
        sessions = dt.sessions;

    if(!skipTier) {
      sessionsHtml = "";
    }
    if(skipTier && i == 0 || !skipTier) {
      sessionsLookup[id] = {};
    }
    
    for(var j = 0, sessionsLen = sessions.length; j < sessionsLen; j++) {
      sessionsHtml += generateCourseScheduleSessionFunc(id, dtShort, sessions[j], sessionsLookup, descs, i18n, skipTier,
                                                        generateCourseScheduleDateFunc, generateCourseScheduleContentFromSessionDataFunc);
    }

    if(!skipTier) {
      sessionsLookup[id].html = sessionsHtml;
      htmlMiddle += vrtxEditor.htmlFacade.getAccordionInteraction("4", id, type, sessions[0].title, "");
      if(i > 0 && dtShort != dtShortLast) {
        html += vrtxEditor.htmlFacade.getAccordionInteraction("3", dtShort, type, dt.teachingmethodname, "<div class='vrtx-grouped'>" + htmlMiddle + "</div>");
        htmlMiddle = "";
      }
    }
    dtShortLast = dtShort;
  }
  if(!skipTier) {
    if(len > 0) {
      html += vrtxEditor.htmlFacade.getAccordionInteraction("3", dtShort, type, dt.teachingmethodname, "<div class='vrtx-grouped'>" + htmlMiddle + "</div>");
    }
  } else {
    sessionsLookup[id].html = sessionsHtml;
    html += vrtxEditor.htmlFacade.getAccordionInteraction("3", id, (type + " skip-tier"), dt.teachingmethodname, "");
  }
   
  return html;
}

function generateCourseScheduleSession(id, dtShort, session, sessionsLookup, descs, i18n, skipTier, generateCourseScheduleDateFunc, generateCourseScheduleContentFromSessionDataFunc) {
  var sessionId = dtShort + "-" + session.id.replace(/\//g, "-"),
      sessionTitle = generateCourseScheduleDateFunc(session.dtstart, session.dtend, i18n) + " " +
                     "<span class='header-title' data-orig-title='" + (session.title || session.id) + "'>" + (session["vrtx-title"] || session.title || session.id) + "</span>" +
                     (session.room ? " - " + (session.room[0].buildingid + " " + i18n.room + " " + session.room[0].roomid) : "") +
                     (session.status && session.status === "cancelled" ? " <span class='header-status'>" + i18n[session.status] + "</span>" : ""),
      sessionContent = generateCourseScheduleContentFromSessionDataFunc(sessionId, session, descs, i18n);

   // Store multiple description for session
   sessionsLookup[id][sessionId] = {
     isEnhanced: false,
     multiples: sessionContent.multiples
   };
   
   return vrtxEditor.htmlFacade.getAccordionInteraction(!skipTier ? "5" : "4", sessionId, "session", sessionTitle, sessionContent.html);
}

function saveCourseSchedule(startTime, d) {
  var updateType = function(type) {
    var sessions = vrtxEditor.editorForm.find("." + type + " .session-touched .accordion-content");
    var sessionsTouched = sessions.length;
    
    if(!sessionsTouched) return 0;
    
    var jsonType = retrievedScheduleData[type];
    if(!jsonType) return 0;
    
    var descs = jsonType["vrtx-editable-description"];
    var data = jsonType.data;
    var dataLen = data.length;
    var saveCourseScheduleFindSessionInDataFunc = saveCourseScheduleFindSessionInData;
    var saveCourseScheduleExtractSessionFromDOMFunc = saveCourseScheduleExtractSessionFromDOM;
  
    // Sessions that has been opened
    for(var i = 0; i < sessionsTouched; i++) {
      var content = $(sessions[i]);
      if(!content.length) continue;
      
      var session = saveCourseScheduleFindSessionInDataFunc(content, data, dataLen);
      var domSessionElms = content.find("> div");
      for(var j = 0, domSessionElmsLen = domSessionElms.length; j < domSessionElmsLen; j++) {
        var domSessionElm = $(domSessionElms[j]);
        
        for(var name in descs) {
          var domSessionPropElm = domSessionElm.find("input[name='" + name + "']");
          if(!domSessionPropElm.length) continue;

          var val = saveCourseScheduleExtractSessionFromDOMFunc(descs[name], domSessionPropElm);
          if(val && val.length) { // Update
            session[name] = val;
          } else { // Delete if empty
            delete session[name];
          }
        }
      }
    }
    return sessionsTouched;
  };
  
  var sessionsTouched = 0;
  sessionsTouched += updateType("plenary");
  sessionsTouched += updateType("group");
  
  vrtxAdmin.log({msg: "Sessions touched: " + sessionsTouched});

  if(sessionsTouched) { // Save
    var postData = JSON.stringify({
      "resourcetype": "course-schedule",
      "properties": {"activities": retrievedScheduleData}
    }, null, 2);
    //var csrfPT = "&csrf-prevention-token=" + vrtxEditor.editorForm.find("input[name='csrf-prevention-token']").val();

    vrtxAdmin.serverFacade.postJSONA(this.location.href, postData, {
      success: function (results, status, resp) {
        ajaxSaveSuccess(startTime, d, results, status, resp);
      },
      error: function (xhr, textStatus) {
        ajaxSaveError(d, xhr, textStatus);
      }
    });
  } else {
    d.close();
  }
}

function generateCourseScheduleDate(s, e, i18n) { /* IE8: http://www.digital-portfolio.net/blog/view/ie8-and-iso-date-format */
  var sd = s.split("T")[0].split("-");
  var st = s.split("T")[1].split(".")[0].split(":");
  var ed = e.split("T")[0].split("-");
  var et = e.split("T")[1].split(".")[0].split(":");
  
  // Not same day
  if(sd[0] != ed[0] || sd[1] != ed[1] || sd[2] != ed[2]) {
    return sd[2] + ". " + i18n[sd[1]] + " " + sd[0] + " kl " + st[0] + ":" + st[1] + "&ndash;" +
           ed[2] + ". " + i18n[ed[1]] + " " + ed[0] + " kl " + et[0] + ":" + et[1];
  }
  return sd[2] + ". " + i18n[sd[1]] + " " + sd[0] + " - kl " +
         st[0] + ":" + st[1] + "&ndash;" + et[0] + ":" + et[1];
}

function saveCourseScheduleFindSessionInData(content, data, dataLen) {
  var id = content.attr("aria-labelledby").split("-");
  var teachingmethod = id[0].toUpperCase();
  var activityId = id[1] + "-" + id[2];
  var sessionId = id[1] + "-" + id[2] + "/" + id[3] + "/" + id[4];
  for(var i = 0; i < dataLen; i++) {
    if(data[i].teachingmethod === teachingmethod && data[i].id === activityId) {
      var sessions = data[i].sessions;
      for(var j = 0, len = sessions.length; j < len; j++) {
        if(sessions[j].id === sessionId) {
          return sessions[j];
        }
      }
    }
  }
}

/* General methods for putting and getting data to/from DOM. 
   TODO: missing some general variable names */

function generateCourseScheduleContentFromSessionData(id, data, descs, i18n) {
  var html = "";
  multiples = [];
  for(var name in descs) {
    var desc = descs[name],
        descProps = jQuery.extend(true, [], desc.props),
        val = data[name],
        placeholder = null,
        propsVal = "",
        browsable = false,
        size = 40;
    switch(desc.type) {
      case "json":
        for(var i = 0, descPropsLen = descProps.length; i < descPropsLen; i++) {
          descProps[i].title = i18n[name + "-" + descProps[i].name];
          if(desc.multiple && desc.props[i].type === "resource_ref") {
            browsable = true;
          }
        }
        if(val) {
          for(var j = 0, propsLen = val.length; j < propsLen; j++) {
            for(i = 0; i < descPropsLen; i++) {
              propsVal += val[j][descProps[i].name] + "###";
            }
            if(j < (propsLen - 1)) propsVal += ",";
          }
        }
        size = 20;
      case "string":
        val = (propsVal != "") ? propsVal : val;
        val = (desc.multiple && typeof val === "array") ? val.join(", ") : val;
        if(desc.multiple) {
          multiples.push({
            name: name,
            json: descProps ? descProps : null, 
            movable: desc.multiple.movable,
            browsable: browsable
          });
        } else if(desc.type === "string") {
          var origName = name.split("vrtx-")[1];
          if(origName) {
            var origTitle = data[name.split("vrtx-")[1]];
            if(origTitle != "") {
              placeholder = origTitle;
            }
          }
        }
        html += vrtxEditor.htmlFacade.getStringField({ title: i18n[name],
                                                       name: (desc.autocomplete ? "vrtx-autocomplete-" + desc.autocomplete + " " : "") + name + "-" + id,
                                                       id: name + "-" + id,
                                                       val: val,
                                                       size: size,
                                                       placeholder: placeholder
                                                     }, name);
        break;
      case "checkbox":
        html += vrtxEditor.htmlFacade.getCheckboxField({ title: i18n[name],
                                                         name: name + "-" + id,
                                                         id: name + "-" + id,
                                                         checked: val
                                                       }, name);
        break;
      default:
        break;
    }
  }
   
  return { html: html, multiples: multiples };
}

function saveCourseScheduleExtractSessionFromDOM(desc, elm) {
  var val = "";
  if(desc.type === "checkbox") {
    if(elm[0].checked) {
      val = "cancelled"; // TODO: Not very general
    }
  } else {
    val = elm.val(); // To string (string)
    if(desc.multiple && val.length) { // To array (multiple)
      val = val.split(",");
    }
    if(desc.type === "json" && val.length) { // Object props into array (JSON multiple)
      var arrProps = [];
      for(var i = 0, arrLen = val.length; i < arrLen; i++) {
        var newProp = null;
        var prop = val[i].split("###");
        for(var j = 0, descPropsLen = desc.props.length; j < descPropsLen; j++) { // Definition
          if(prop[j] !== "") {
            if(!newProp) {
              newProp = {};
            }
            newProp[desc.props[j].name] = prop[j];
          }
        }
        if(newProp) {
          arrProps.push(newProp);
        }
      }
      val = arrProps;
    }
  }
  return val;
}

/*
 * Boolean switch show/hide
 *
 */
function setShowHideBooleanNewEditor(name, properties, hideTrues) {
  vrtxEditor.initEventHandler('[name=' + name + ']', {
    wrapper: "#editor",
    callback: function (props, hideTrues, name, init) {
      if ($('#' + name + (hideTrues ? '-false' : '-true'))[0].checked) {
        toggleShowHideBoolean(props, true, init);
      } else if ($('#' + name + (hideTrues ? '-true' : '-false'))[0].checked) {
        toggleShowHideBoolean(props, false, init);
      }
    },
    callbackParams: [properties, hideTrues, name]
  });
}

function setShowHideBooleanOldEditor(radioIds, properties, conditionHide, conditionHideEqual) {
  vrtxEditor.initEventHandler(radioIds, {
    wrapper: "#editor",
    callback: function (props, conditionHide, conditionHideEqual, init) {
      if ($(conditionHide).val() != conditionHideEqual) {
        toggleShowHideBoolean(props, true, init);
      } else {
        toggleShowHideBoolean(props, false, init);
      }
    },
    callbackParams: [properties, conditionHide, conditionHideEqual]
  });
}

function toggleShowHideBoolean(props, show, init) {
  var theProps = $(props);
  if (init || vrtxAdmin.isIE9) {
    if (!vrtxAdmin.isIE9) {
      theProps.addClass("animate-optimized");
    }
    if (show && !init) {
      theProps.show();
    } else {
      theProps.hide();
    }
  } else {
    var animation = new VrtxAnimation({
      animationSpeed: vrtxAdmin.transitionPropSpeed,
      elem: theProps
    });
    if (show) {
      animation.topDown();
    } else {
      animation.bottomUp();
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

  for (var select in vrtxEdit.selectMappings) {
    vrtxEdit.initEventHandler("#" + select, {
      event: "change",
      callback: vrtxEdit.showHideSelect
    });
  }
};

/**
 * Select field show/hide
 *
 * @this {VrtxEditor}
 * @param {object} select The select field
 */
VrtxEditor.prototype.showHideSelect = function showHideSelect(select, init) {
  var vrtxEdit = this;

  var id = select.attr("id");
  var mappings = vrtxEdit.selectMappings[id];
  if (mappings) {
    var selectClassName = "select-" + id;
    if (!vrtxEdit.editorForm.hasClass(selectClassName)) {
      vrtxEdit.editorForm.addClass(selectClassName);
    }
    var selected = select.val();
    for (var i = 0, len = mappings.length; i < len; i++) {
      var mappedClass = selectClassName + "-" + mappings[i];
      var editorHasMappedClass = vrtxEdit.editorForm.hasClass(mappedClass);
      if (selected === mappings[i]) {
        if (!editorHasMappedClass) {
          vrtxEdit.editorForm.addClass(mappedClass);
        }
      } else {
        if (editorHasMappedClass) {
          vrtxEdit.editorForm.removeClass(mappedClass);
        }
      }
    }
  }
  if (!init && accordionGrouped) accordionGrouped.closeActiveHidden();
};


/*-------------------------------------------------------------------*\
    8. Multiple fields and boxes
    
    THIS IS A GENERAL MESSAGE TO THE WORLD:
    Abandon hope all ye who enter here..
    
    XXX: refactor / combine and optimize
\*-------------------------------------------------------------------*/

/*
 * Multiple comma-seperated inputfields (supports JSON inputfields)
 *
 */

function getMultipleFieldsBoxesTemplates() {
  if (!vrtxEditor.multipleFieldsBoxesDeferred) {
    vrtxEditor.multipleFieldsBoxesDeferred = $.Deferred();
    vrtxEditor.multipleFieldsBoxesTemplates = vrtxAdmin.templateEngineFacade.get("multiple-fields-boxes", 
      ["string", "html", "radio", "checkbox", "dropdown", "date", "browse",
       "browse-images", "add-remove-move", "button", "add-button",
       "multiple-inputfield", "accordion"],
    vrtxEditor.multipleFieldsBoxesDeferred);
  }
}

function initMultipleInputFields() {
  getMultipleFieldsBoxesTemplates();
  vrtxAdmin.cachedAppContent.on("click keyup", ".vrtx-multipleinputfield button.remove", function (e) {
    if(e.type == "click" || ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13))) {
      removeFormField($(this));
      e.preventDefault();
      e.stopPropagation();
    }
  });
  vrtxAdmin.cachedAppContent.on("click keyup", ".vrtx-multipleinputfield button.movedown", function (e) {
    if(e.type == "click" || ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13))) {
      swapContentTmp($(this), 1);
      e.preventDefault();
      e.stopPropagation();
    }
  });
  vrtxAdmin.cachedAppContent.on("click keyup", ".vrtx-multipleinputfield button.moveup", function (e) {
    if(e.type == "click" || ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13))) {
      swapContentTmp($(this), -1);
      e.preventDefault();
      e.stopPropagation();
    }
  });
  vrtxAdmin.cachedAppContent.on("click keyup", ".vrtx-multipleinputfield button.browse-resource-ref", function (e) {
    if(e.type == "click" || ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13))) {
      var m = $(this).closest(".vrtx-multipleinputfield");
      var elm = m.find('input.resource_ref');
      if(!elm.length) elm = m.find('input');
      browseServer(elm.attr('id'), vrtxAdmin.multipleFormGroupingPaths.baseBrowserURL, vrtxAdmin.multipleFormGroupingPaths.baseFolderURL, vrtxAdmin.multipleFormGroupingPaths.basePath, 'File');
      e.preventDefault();
      e.stopPropagation();
    }
  });
}

function enhanceMultipleInputFields(name, isMovable, isBrowsable, limit, json) { // TODO: simplify
  var inputField = $("." + name + " input[type='text']");
  if (!inputField.length) return;

  // Config
  var size = inputField.attr("size");
  
  var isDropdown = inputField.hasClass("vrtx-multiple-dropdown");
  isMovable = !isDropdown && isMovable;

  var inputFieldParent = inputField.parent();
  if (inputFieldParent.hasClass("vrtx-resource-ref-browse")) {
    isBrowsable = true;
    inputField.next().filter(".vrtx-button").hide();
  }

  inputFieldParent.addClass("vrtx-multipleinputfields").data("name", name); // Don't like to do this need get it easily
  $($.parseHTML(vrtxEditor.htmlFacade.getMultipleInputFieldsAddButton(name, size, isBrowsable, isMovable, isDropdown, JSON.stringify(json, null, 2)), document, true)).insertAfter(inputField);

  var inputFieldVal = inputField.hide().val();
  var formFields = inputFieldVal.split(",");

  vrtxEditor.multipleFieldsBoxes[name] = { counter: 1, limit: limit };

  var addFormFieldFunc = addFormField, html = "";
  for (var i = 0, len = formFields.length; i < len; i++) {
    html += addFormFieldFunc(name, len, $.trim(formFields[i]), size, isBrowsable, isMovable, isDropdown, true, json);
  }
  html = $.parseHTML(html, document, true);
  $(html).insertBefore("#vrtx-" + name + "-add");
  
  // Hide add button if limit is reached or gone over
  if(len >= vrtxEditor.multipleFieldsBoxes[name].limit) {
    var moreBtn = $("#vrtx-" + name + "-add");
    $("<p class='vrtx-" + name + "-limit-reached'>" + vrtxAdmin.multipleFormGroupingMessages.limitReached + "</p>").insertBefore(moreBtn);
    moreBtn.hide();
  }

  autocompleteUsernames(".vrtx-autocomplete-username");
}

function addFormField(name, len, value, size, isBrowsable, isMovable, isDropdown, init, json) {
  var fields = $("." + name + " div.vrtx-multipleinputfield"),
    idstr = "vrtx-" + name + "-",
    i = vrtxEditor.multipleFieldsBoxes[name].counter,
    removeButton = "",
    moveUpButton = "",
    moveDownButton = "",
    browseButton = "";
    
  len = !init ? fields.length : len; /* If new field set len to fields length */

  removeButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("remove", " " + name, idstr, vrtxAdmin.multipleFormGroupingMessages.remove);
  if (isMovable) {
    if (i > 1 && len > 0) {
      moveUpButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("moveup", "", idstr, "&uarr; " + vrtxAdmin.multipleFormGroupingMessages.moveUp);
    }
    if (i < len) {
      moveDownButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("movedown", "", idstr, "&darr; " + vrtxAdmin.multipleFormGroupingMessages.moveDown);
    }
  }
  if (isBrowsable) {
    browseButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("browse", "-resource-ref", idstr, vrtxAdmin.multipleFormGroupingMessages.browse);
  }
  
  if(json && value && value.indexOf("###") != -1) {
    value = value.split("###");
    var j = 0;
    for(var prop in json) {
      json[prop].val = value[j];
      j++;
    }
  } else if(json) {
    for(var prop in json) {
      json[prop].val = "";
    }
  }
  var html = vrtxEditor.htmlFacade.getMultipleInputfield(name, idstr, i, value, size, browseButton, removeButton, moveUpButton, moveDownButton, isDropdown, json);

  vrtxEditor.multipleFieldsBoxes[name].counter++;

  if (!init) {
    if (len > 0 && isMovable) {
      var last = fields.filter(":last");
      if (!last.find("button.movedown").length) {
        moveDownButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("movedown", "", idstr, "&darr; " + vrtxAdmin.multipleFormGroupingMessages.moveDown);
        last.append(moveDownButton);
      }
    }
    var addBtn = $("#vrtx-" + name + "-add");
    $($.parseHTML(html, document, true)).insertBefore(addBtn);
    
    autocompleteUsername(".vrtx-autocomplete-username", idstr + i);
    
    var focusable = addBtn.prev().find("input[type='text'], select")
    if(focusable.length) {
      focusable[0].focus();
    }

    // Hide add button if limit is reached
    if((len == (vrtxEditor.multipleFieldsBoxes[name].limit - 1))) {
      var moreBtn = $("#vrtx-" + name + "-add");
      $("<p class='vrtx-" + name + "-limit-reached'>" + vrtxAdmin.multipleFormGroupingMessages.limitReached + "</p>").insertBefore(moreBtn);
      moreBtn.hide();
    }
  } else {
    return html;
  }
}

function removeFormField(input) {
  var parent = input.closest(".vrtx-multipleinputfields");
  var field = input.closest(".vrtx-multipleinputfield");
  var name = parent.data("name");
  field.remove();

  var fields = parent.find(".vrtx-multipleinputfield");
  var firstField = fields.filter(":first");
  if(firstField.length) {
    var focusable = firstField.find("input[type='text'], select").filter(":first");
    if(focusable.length) {
      focusable[0].focus();
    }
  }
  
  // Show add button if is within limit again
  if(fields.length === (vrtxEditor.multipleFieldsBoxes[name].limit - 1)) {
    $(".vrtx-" + name + "-limit-reached").remove();
    $("#vrtx-" + name + "-add").show();
  }
  var moveUpFirst = fields.filter(":first").find("button.moveup");
  var moveDownLast = fields.filter(":last").find("button.movedown");
  if (moveUpFirst.length) moveUpFirst.remove();
  if (moveDownLast.length) moveDownLast.remove();
}

function swapContentTmp(moveBtn, move) {
  var curElm = moveBtn.closest(".vrtx-multipleinputfield");
  var movedElm = (move > 0) ? curElm.next() : curElm.prev();
  var curElmInputs = curElm.find("input");
  var movedElmInputs = movedElm.find("input");
  for(var i = curElmInputs.length;i--;) {
    var tmp = curElmInputs[i].value;
    curElmInputs[i].value = movedElmInputs[i].value;
    movedElmInputs[i].value = tmp;
  }
  movedElmInputs.filter(":first")[0].focus();
}

/* DEHANCE PART */
function saveMultipleInputFields() {
  var multipleFields = $(".vrtx-multipleinputfields");
  for (var i = 0, len = multipleFields.length; i < len; i++) {
    var multiple = $(multipleFields[i]);
    var multipleInput = multiple.find("> input");
    if (!multipleInput.length) continue;
    var multipleInputFields = multiple.find(".vrtx-multipleinputfield");
    if (!multipleInputFields.length) {
      multipleInput.val("");
      continue;
    }
    var result = "";
    for (var j = 0, len2 = multipleInputFields.length; j < len2; j++) {
      var multipleInputField = $(multipleInputFields[j]);
      var fields = multipleInputField.find("input");
      if (!fields.length) {
        fields = multipleInputField.find("select");
      }
      var fieldsLen = fields.length;
      if (!fieldsLen) continue;
      for(var k = 0; k < fieldsLen; k++) {
        result += $.trim(fields[k].value);
        if(fieldsLen > 1) {
          result += "###";
        }
      }
      if (j < (len2 - 1)) {
        result += ",";
      }
    }
    multipleInput.val(result);
  }
}

/* Multiple JSON boxes */

function initJsonMovableElements() {
  $.when(vrtxEditor.multipleFieldsBoxesDeferred, vrtxEditor.multipleBoxesTemplatesContractBuilt).done(function () {
    for (var i = 0, len = vrtxEditor.multipleBoxesTemplatesContract.length; i < len; i++) {
      var jsonName = vrtxEditor.multipleBoxesTemplatesContract[i].name;
      var jsonElm = $("#" + jsonName);
      jsonElm.append(vrtxEditor.htmlFacade.getJsonBoxesInteractionsButton("add", vrtxAdmin.multipleFormGroupingMessages.add))
        .find(".vrtx-add-button").data({
        'number': i
      });
      vrtxEditor.multipleFieldsBoxes[jsonName] = {counter: jsonElm.find(".vrtx-json-element").length, limit: -1};
    }

    accordionJsonInit();

    JSON_ELEMENTS_INITIALIZED.resolve();
  });

  vrtxAdmin.cachedAppContent.on("click keyup", ".vrtx-json .vrtx-move-down-button", function (e) {
    if(e.type == "click" || ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13))) {
      swapContent($(this), 1);
      e.stopPropagation();
      e.preventDefault();
    }
  });
  vrtxAdmin.cachedAppContent.on("click keyup", ".vrtx-json .vrtx-move-up-button", function (e) {
    if(e.type == "click" || ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13))) {
      swapContent($(this), -1);
      e.stopPropagation();
      e.preventDefault();
    }
  });
  vrtxAdmin.cachedAppContent.on("click keyup", ".vrtx-json .vrtx-add-button", function (e) {
    if(e.type == "click" || ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13))) {
      addJsonField($(this));
      e.stopPropagation();
      e.preventDefault();
    }
  });
  vrtxAdmin.cachedAppContent.on("click keyup", ".vrtx-json .vrtx-remove-button", function (e) {
    if(e.type == "click" || ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13))) {
      removeJsonField($(this));
      e.stopPropagation();
      e.preventDefault();
    }
  });
}

function addJsonField(btn) {
  var jsonParent = btn.closest(".vrtx-json");
  var numOfElements = jsonParent.find(".vrtx-json-element").length;
  var j = vrtxEditor.multipleBoxesTemplatesContract[parseInt(btn.data('number'), 10)];
  var htmlTemplate = "";
  var inputFieldName = "";

  // Add correct HTML for Vortex type
  var types = j.a;

  var ckHtmls = [];
  var ckSimpleHtmls = [];
  var dateTimes = [];

  for (var i in types) {
    inputFieldName = j.name + "." + types[i].name + "." + vrtxEditor.multipleFieldsBoxes[j.name].counter;
    htmlTemplate += vrtxEditor.htmlFacade.getTypeHtml(types[i], inputFieldName);
    switch (types[i].type) {
      case "html":
        ckHtmls.push(inputFieldName);
        break;
      case "simple_html":
        ckSimpleHtmls.push(inputFieldName);
        break;
      case "datetime":
        dateTimes.push(inputFieldName);
        break;
    }
  }

  // Interaction
  var isImmovable = jsonParent && jsonParent.hasClass("vrtx-multiple-immovable");
  var removeButton = vrtxEditor.htmlFacade.getJsonBoxesInteractionsButton('remove', vrtxAdmin.multipleFormGroupingMessages.remove);

  var newElementId = "vrtx-json-element-" + j.name + "-" + vrtxEditor.multipleFieldsBoxes[j.name].counter;
  var newElementHtml = htmlTemplate + "<input type=\"hidden\" class=\"id\" value=\"" + vrtxEditor.multipleFieldsBoxes[j.name].counter + "\" \/>" + removeButton;
  if (!isImmovable && numOfElements > 0) {
    var moveUpButton = vrtxEditor.htmlFacade.getJsonBoxesInteractionsButton('move-up', '&uarr; ' + vrtxAdmin.multipleFormGroupingMessages.moveUp);
    newElementHtml += moveUpButton;
  }
  newElementHtml = "<div class='vrtx-json-element last' id='" + newElementId + "'>" + newElementHtml + "<\/div>";

  var oldLast = jsonParent.find(".vrtx-json-element.last");
  if (oldLast.length) {
    oldLast.removeClass("last");
  }

  jsonParent.find(".vrtx-add-button").before(newElementHtml);

  var accordionWrapper = btn.closest(".vrtx-json-accordion");
  var hasAccordion = accordionWrapper.length;

  if (!isImmovable && numOfElements > 0 && oldLast.length) {
    var moveDownButton = vrtxEditor.htmlFacade.getJsonBoxesInteractionsButton('move-down', '&darr; ' + vrtxAdmin.multipleFormGroupingMessages.moveDown);
    if (hasAccordion) {
      oldLast.find("> div.ui-accordion-content").append(moveDownButton);
    } else {
      oldLast.append(moveDownButton);
    }
  }
  if (hasAccordion) {
    accordionJsonNew(accordionWrapper);
  }

  // Init CKEditors and enhance date inputfields
  var ckHtmlsLen = ckHtmls.length,
    ckSimpleHtmlsLen = ckSimpleHtmls.length,
    dateTimesLen = dateTimes.length,
    rteFacade = vrtxEditor.richtextEditorFacade;
  if (ckHtmlsLen || ckSimpleHtmlsLen || dateTimesLen) {
    var checkForAppendComplete = setTimeout(function () {
      if ($("#" + newElementId + " .vrtx-remove-button").length) {
        for (var i = 0; i < ckHtmlsLen; i++) {
          rteFacade.setup({
            name: ckHtmls[i],
            isCompleteEditor: true,
            defaultLanguage: requestLang,
            cssFileList: cssFileList
          });
        }
        for (i = 0; i < ckSimpleHtmlsLen; i++) {
          rteFacade.setup({
            name: ckSimpleHtmls[i],
            defaultLanguage: requestLang,
            cssFileList: cssFileList,
            simple: true
          });
        }
        datepickerEditor.initFields(dateTimes);
      } else {
        setTimeout(checkForAppendComplete, 25);
      }
    }, 25);
  }

  vrtxEditor.multipleFieldsBoxes[j.name].counter++;
}

function removeJsonField(btn) {
  var removeElement = btn.closest(".vrtx-json-element"),
      accordionWrapper = removeElement.closest(".vrtx-json-accordion"),
      hasAccordion = accordionWrapper.length,
      removeElementParent = removeElement.parent(),
      textAreas = removeElement.find("textarea"),
      rteFacade = vrtxEditor.richtextEditorFacade;
  for (var i = 0, len = textAreas.length; i < len; i++) {
    rteFacade.removeInstance(textAreas[i].name);
  }

  var updateLast = removeElement.hasClass("last");
  removeElement.remove();
  removeElementParent.find(".vrtx-json-element:first .vrtx-move-up-button").remove();
  var newLast = removeElementParent.find(".vrtx-json-element:last");
  newLast.find(".vrtx-move-down-button").remove();
  if (updateLast) {
    newLast.addClass("last");
  }
  if (hasAccordion) {
    accordionJsonRefresh(accordionWrapper.find(".fieldset"), false);
    accordionJson.create();
  }
}

// Move up or move down  
function swapContent(moveBtn, move) {
  var curElm = moveBtn.closest(".vrtx-json-element");
  var accordionWrapper = curElm.closest(".vrtx-json-accordion");
  var hasAccordion = accordionWrapper.length;
  var movedElm = (move > 0) ? curElm.next(".vrtx-json-element") : curElm.prev(".vrtx-json-element");
  var curCounter = curElm.find("input.id").val();
  var moveToCounter = movedElm.find("input.id").val();

  var j = vrtxEditor.multipleBoxesTemplatesContract[parseInt(curElm.closest(".vrtx-json").find(".vrtx-add-button").data('number'), 10)];
  var types = j.a;
  var swapElementFn = swapElement,
      rteFacade = vrtxEditor.richtextEditorFacade;
  var runOnce = false;
  for (var i = 0, len = types.length; i < len; i++) {
    var field = j.name + "\\." + types[i].name + "\\.";
    var fieldEditor = field.replace(/\\/g, "");

    var elementId1 = "#" + field + curCounter;
    var elementId2 = "#" + field + moveToCounter;
    var element1 = $(elementId1);
    var element2 = $(elementId2);

    /* We need to handle special cases like CK fields and date */
    var ckInstanceName1 = fieldEditor + curCounter;
    var ckInstanceName2 = fieldEditor + moveToCounter;

    if (rteFacade.isInstance(ckInstanceName1) && rteFacade.isInstance(ckInstanceName2)) {
      rteFacade.swap(ckInstanceName1, ckInstanceName2);
    } else if (element1.hasClass("date") && element2.hasClass("date")) {
      var element1Wrapper = element1.closest(".vrtx-string");
      var element2Wrapper = element2.closest(".vrtx-string");
      swapElementFn(element1Wrapper.find(elementId1 + '-date'), element2Wrapper.find(elementId2 + '-date'));
      swapElementFn(element1Wrapper.find(elementId1 + '-hours'), element2Wrapper.find(elementId2 + '-hours'));
      swapElementFn(element1Wrapper.find(elementId1 + '-minutes'), element2Wrapper.find(elementId2 + '-minutes'));
    }

    swapElementFn(element1, element2);

    if (hasAccordion && !runOnce) {
      accordionJson.updateHeader(element1, true, false);
      accordionJson.updateHeader(element2, true, false);
      runOnce = true;
    }
    /* Do we need these on all elements? */
    element1.blur();
    element2.blur();
    element1.change();
    element2.change();
  }
  curElm.focusout();
  movedElm.focusout();

  if (hasAccordion) { /* Wait with scroll until accordion switch */
    vrtxEditor.multipleFieldsBoxesAccordionSwitchThenScrollTo = movedElm;
    accordionWrapper.find(".fieldset").accordion("option", "active", (movedElm.index() - 1)).accordion("option", "refresh");
  } else {
    scrollToElm(movedElm);
  }
}

function swapElement(elemA, elemB) {
  var tmp = elemA.val();
  elemA.val(elemB.val());
  elemB.val(tmp);
}

/* NOTE: can be used generally if boolean hasScrollAnim is turned on */

function scrollToElm(movedElm) {
  if (typeof movedElm.offset() === "undefined") return;
  var absPos = movedElm.offset();
  var absPosTop = absPos.top;
  var stickyBar = $("#vrtx-editor-title-submit-buttons");
  if (stickyBar.css("position") == "fixed") {
    var stickyBarHeight = stickyBar.height();
    absPosTop -= (stickyBarHeight <= absPosTop) ? stickyBarHeight : 0;
  }
  $('body').scrollTo(absPosTop, 250, {
    easing: 'swing',
    queue: true,
    axis: 'y',
    onAfter: function () {
      vrtxEditor.multipleFieldsBoxesAccordionSwitchThenScrollTo = null;
    }
  });
}

/**
 * HTML facade (Input=>Template Engine=>HTML)
 *
 * @namespace
 */
VrtxEditor.prototype.htmlFacade = {
  /* 
   * Interaction
   */
  getMultipleInputfieldsInteractionsButton: function (clazz, name, idstr, text) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["button"], {
      type: clazz,
      name: name,
      idstr: idstr,
      buttonText: text
    });
  },
  getMultipleInputFieldsAddButton: function (name, size, isBrowsable, isMovable, isDropdown, json) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["add-button"], {
      name: name,
      size: size,
      isBrowsable: isBrowsable,
      isMovable: isMovable,
      isDropdown: isDropdown,
      buttonText: vrtxAdmin.multipleFormGroupingMessages.add,
      json: json
    });
  },
  getJsonBoxesInteractionsButton: function (clazz, text) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["add-remove-move"], {
      clazz: clazz,
      buttonText: text
    });
  },
  getAccordionInteraction: function (level, id, clazz, title, content) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["accordion"], {
      level: level,
      id: id,
      clazz: clazz,
      title: title,
      content: content
    });
  },
  /* 
   * Type / fields 
   */
  getMultipleInputfield: function (name, idstr, i, value, size, browseButton, removeButton, moveUpButton, moveDownButton, isDropdown, json) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["multiple-inputfield"], {
      idstr: idstr,
      i: i,
      value: value,
      size: size,
      browseButton: browseButton,
      removeButton: removeButton,
      moveUpButton: moveUpButton,
      moveDownButton: moveDownButton,
      isDropdown: isDropdown,
      dropdownArray: "dropdown" + name,
      json: json
    });
  },
  getTypeHtml: function (elem, inputFieldName) {
    var methodName = "get" + this.typeToMethodName(elem.type) + "Field";
    if (this[methodName]) { // If type maps to method
      return this[methodName](elem, inputFieldName);
    }
    return "";
  },
  typeToMethodName: function (str) { // Replaces "_" with "" and camelCase Vortex types. XXX: Optimize RegEx
    return str.replace("_", " ").replace(/(\w)(\w*)/g, function (g0, g1, g2) {
      return g1.toUpperCase() + g2.toLowerCase();
    }).replace(" ", "");
  },
  getStringField: function (elem, inputFieldName) {
    if (elem.dropdown && elem.valuemap) {
      return this.getDropdown(elem, inputFieldName);
    } else {
      return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["string"], {
        classes: "vrtx-string" + " " + elem.name,
        elemTitle: elem.title,
        inputFieldName: inputFieldName,
        elemId: elem.id || inputFieldName,
        elemVal: elem.val,
        elemSize: elem.size || 40,
        elemPlaceholder: elem.placeholder
      });
    }
  },
  getSimpleHtmlField: function (elem, inputFieldName) {
    return this.getHtmlField(elem, inputFieldName, "vrtx-simple-html");
  },
  getHtmlField: function (elem, inputFieldName, htmlType) {
    if (typeof htmlType === "undefined") htmlType = "vrtx-html";
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["html"], {
      classes: htmlType + " " + elem.name,
      elemTitle: elem.title,
      inputFieldName: inputFieldName
    });
  },
  getBooleanField: function (elem, inputFieldName) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["radio"], {
      elemTitle: elem.title,
      inputFieldName: inputFieldName
    });
  },
  getCheckboxField: function (elem, inputFieldName) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["checkbox"], {
      elemTitle: elem.title,
      elemId: elem.id || inputFieldName,
      elemChecked: elem.checked,
      inputFieldName: inputFieldName
    });
  },
  getDropdown: function (elem, inputFieldName) {
    var htmlOpts = [];
    for (var i in elem.valuemap) {
      var keyValuePair = elem.valuemap[i];
      var keyValuePairSplit = keyValuePair.split("$");
      htmlOpts.push({
        key: keyValuePairSplit[0],
        value: keyValuePairSplit[1]
      });
    }
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["dropdown"], {
      classes: "vrtx-string" + " " + elem.name,
      elemTitle: elem.title,
      inputFieldName: inputFieldName,
      options: htmlOpts
    });
  },
  getDatetimeField: function (elem, inputFieldName) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["date"], {
      elemTitle: elem.title,
      inputFieldName: inputFieldName
    });
  },
  getImageRefField: function (elem, inputFieldName) {
    return this.getBrowseField(elem, inputFieldName, "browse-images", "vrtx-image-ref", "", 30, {
      previewTitle: browseImagesPreview,
      previewNoImageText: browseImagesNoPreview
    });
  },
  getResourceRefField: function (elem, inputFieldName) {
    return this.getBrowseField(elem, inputFieldName, "browse", "vrtx-resource-ref", "File", 40, {});
  },
  getMediaRefField: function (elem, inputFieldName) {
    return this.getBrowseField(elem, inputFieldName, "browse", "vrtx-media-ref", "Media", 30, {});
  },
  getBrowseField: function (elem, inputFieldName, templateName, clazz, type, size, extraConfig) {
    var config = {
      clazz: clazz,
      elemTitle: elem.title,
      inputFieldName: inputFieldName,
      baseCKURL: vrtxAdmin.multipleFormGroupingPaths.baseBrowserURL,
      baseFolderURL: vrtxAdmin.multipleFormGroupingPaths.baseFolderURL,
      basePath: vrtxAdmin.multipleFormGroupingPaths.basePath,
      browseButtonText: vrtxAdmin.multipleFormGroupingMessages.browse,
      type: type,
      size: size
    };
    for (var key in extraConfig) { // Copy in extra config
      config[key] = extraConfig[key];
    }
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates[templateName], config);
  }
};


/*-------------------------------------------------------------------*\
    9. Accordions
\*-------------------------------------------------------------------*/

/**
 * Initialize grouped as accordion
 * @this {VrtxEditor}
 */
VrtxEditor.prototype.accordionGroupedInit = function accordionGroupedInit(subGroupedSelector, customSpeed) { /* param name pending */
  var vrtxEdit = this,
    _$ = vrtxAdmin._$;

  var accordionWrpId = "accordion-grouped", // TODO: multiple accordion group pr. page
      groupedSelector = ".vrtx-grouped" + ((typeof subGroupedSelector !== "undefined") ? subGroupedSelector : "") + ", " +
                        ".vrtx-pseudo-grouped" + ((typeof subGroupedSelector !== "undefined") ? subGroupedSelector : ""),
      grouped = vrtxEdit.editorForm.find(groupedSelector);

  grouped.wrapAll("<div id='" + accordionWrpId + "' />");

  accordionContentSplitHeaderPopulators(true);
  
  var opts = {
    elem: vrtxEdit.editorForm.find("#" + accordionWrpId),
    headerSelector: "> div > .header",
    onActivate: function (e, ui, accordion) {
      accordionGrouped.updateHeader(ui.oldHeader, false, false);
    }
  };
  if(typeof customSpeed !== "undefined" && customSpeed === "fast") {
    opts.animationSpeed = 200;
  }
  accordionGrouped = new VrtxAccordion(opts);

  // Because accordion needs one content wrapper
  for (var i = grouped.length; i--;) {
    var group = $(grouped[i]);
    if (group.hasClass("vrtx-pseudo-grouped")) {
      group.find("> label").wrap("<div class='header' />");
      group.addClass("vrtx-grouped");
    } else {
      group.find("> *:not(.header)").wrapAll("<div />");
    }
    accordionGrouped.updateHeader(group, false, true);
  }
  
  accordionGrouped.create();
  opts.elem.addClass("fast");
};

function accordionJsonInit() {
  accordionContentSplitHeaderPopulators(true);

  accordionJsonRefresh($(".vrtx-json-accordion .fieldset"), false);

  // Because accordion needs one content wrapper
  for (var grouped = $(".vrtx-json-accordion .vrtx-json-element"), i = grouped.length; i--;) {
    var group = $(grouped[i]);
    group.find("> *").wrapAll("<div />");
    accordionJson.updateHeader(group, true, true);
  }
  accordionJson.create();
}

function accordionJsonNew(accordionWrapper) {
  var accordionContent = accordionWrapper.find(".fieldset");
  var group = accordionContent.find(".vrtx-json-element").filter(":last");
  group.find("> *").wrapAll("<div />");
  group.prepend('<div class="header">' + (vrtxAdmin.lang !== "en" ? "Intet innhold" : "No content") + '</div>');
  accordionContentSplitHeaderPopulators(false);
  
  accordionJsonRefresh(accordionContent, false);
  accordionJson.create();
}

function accordionJsonRefresh(elem, active) {
  accordionJson = new VrtxAccordion({
    elem: elem,
    headerSelector: "> div > .header",
    activeElem: active,
    onActivate: function (e, ui, accordion) {
      accordion.updateHeader(ui.oldHeader, true, false);
      if (vrtxEditor.multipleFieldsBoxesAccordionSwitchThenScrollTo) {
        scrollToElm(vrtxEditor.multipleFieldsBoxesAccordionSwitchThenScrollTo);
      }
    }
  });
}

// XXX: avoid hardcoded enhanced fields
function accordionContentSplitHeaderPopulators(init) {
  var sharedTextItems = $("#editor.vrtx-shared-text #shared-text-box .vrtx-json-element");
  var semesterResourceLinksItems = $("#editor.vrtx-semester-page .vrtx-grouped[class*=link-box]");

  if(sharedTextItems.length) {
    if (!init) {
      sharedTextItems = sharedTextItems.filter(":last");
    }
    sharedTextItems.find(".title input").addClass("header-populators");
    sharedTextItems.find(".vrtx-html").addClass("header-empty-check-or");
  } else if (semesterResourceLinksItems.length) {
    semesterResourceLinksItems.find(".vrtx-string input[id*=-title]").addClass("header-populators");
    semesterResourceLinksItems.find(".vrtx-json-element").addClass("header-empty-check-and");
    if(init) {
      vrtxAdmin.cachedDoc.on("click", semesterResourceLinksItems.find(".vrtx-add-button"), function(e) {
        semesterResourceLinksItems.find(".vrtx-json-element:last").addClass("header-empty-check-and"); 
      });
    }
  }
}


/*-------------------------------------------------------------------*\
    10. Send to approval
\*-------------------------------------------------------------------*/

VrtxEditor.prototype.initSendToApproval = function initSendToApproval() {
  var vrtxAdm = vrtxAdmin,
    _$ = vrtxAdm._$;

  vrtxAdm.cachedDoc.on("click", "#vrtx-send-to-approval, #vrtx-send-to-approval-global", function (e) {
    vrtxEditor.openSendToApproval(this);
    e.stopPropagation();
    e.preventDefault();
  });

  vrtxAdm.cachedDoc.on("click", "#dialog-html-send-approval-content .vrtx-focus-button", function (e) {
    vrtxEditor.saveSendToApproval(_$(this));
    e.stopPropagation();
    e.preventDefault();
  });
};

VrtxEditor.prototype.openSendToApproval = function openSendToApproval(link) {
  var vrtxAdm = vrtxAdmin,
    _$ = vrtxAdm._$;

  var id = link.id + "-content";
  var dialogManageCreate = _$("#" + id);
  if (!dialogManageCreate.length) {
    vrtxAdm.serverFacade.getHtml(link.href, {
      success: function (results, status, resp) {
        vrtxAdm.cachedBody.append("<div id='" + id + "'>" + _$(_$.parseHTML(results)).find("#contents").html() + "</div>");
        dialogManageCreate = _$("#" + id);
        dialogManageCreate.hide();
        vrtxEditor.openSendToApprovalOpen(dialogManageCreate, link);
      }
    });
  } else {
    vrtxEditor.openSendToApprovalOpen(dialogManageCreate, link);
  }
};

VrtxEditor.prototype.openSendToApprovalOpen = function openSendToApprovalOpen(dialogManageCreate, link) {
  var vrtxAdm = vrtxAdmin,
    _$ = vrtxAdm._$;

  var hasEmailFrom = dialogManageCreate.find("#emailFrom").length;
  var d = new VrtxHtmlDialog({
    name: "send-approval",
    html: dialogManageCreate.html(),
    title: link.title,
    width: 430,
    height: 620
  });
  d.open();
  var dialog = _$(".ui-dialog");
  if (dialog.find("#emailTo").val().length > 0) {
    if (hasEmailFrom) {
      dialog.find("#emailFrom")[0].focus();
    } else {
      dialog.find("#yourCommentTxtArea")[0].focus();
    }
  }
};

VrtxEditor.prototype.saveSendToApproval = function saveSendToApproval(btn) {
  var vrtxAdm = vrtxAdmin,
    _$ = vrtxAdm._$;

  var form = btn.closest("form");
  var url = form.attr("action");
  var dataString = form.serialize();
  vrtxAdm.serverFacade.postHtml(url, dataString, {
    success: function (results, status, resp) {
      var formParent = form.parent();
      formParent.html(_$($.parseHTML(results)).find("#contents").html());
      var successWrapper = formParent.find("#email-approval-success");
      if (successWrapper.length) { // Save async if sent mail
        successWrapper.trigger("click");
        setTimeout(function () {
          _$("#vrtx-save-view-shortcut").trigger("click");
        }, 250);
      }
    }
  });
};


/*-------------------------------------------------------------------*\
    11. Utils
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
  selector.find(tag).replaceWith(function () {
    return "<" + replacementTag + ">" + $(this).text() + "</" + replacementTag + ">";
  });
};

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
  if (!select.length) return;

  opts.event = opts.event || "click";
  opts.wrapper = opts.wrapper || document;
  opts.callbackParams = opts.callbackParams || [$(selector)];
  opts.callbackChange = opts.callbackChange || function (p) {};

  var vrtxEdit = this;

  opts.callback.apply(vrtxEdit, opts.callbackParams, true);
  $(opts.wrapper).on(opts.event, select, function () {
    opts.callback.apply(vrtxEdit, opts.callbackParams, false);
  });
};

/* ^ Vortex Editor */