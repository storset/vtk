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
 *  7.5 Course schedule editor
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

var editorCourseSchedule = "";

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
    
    if(!vrtxEdit.isInAdmin || (vrtxEdit.isInAdmin && !isEmbedded)) {
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
    }
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
  classification.isMessage = vrtxEdit.contains(name, "message") ||
                             vrtxEdit.contains(name, "vrtxResourcesText");
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

function storeInitPropValues(contents) {
  if (!contents.length || vrtxEditor.editorForm.hasClass("vrtx-course-schedule")) return;

  var vrtxEdit = vrtxEditor;

  var inputFields = contents.find("input").not("[type=submit]").not("[type=button]")
                            .not("[type=checkbox]").not("[type=radio]");
  var selects = contents.find("select");
  var checkboxes = contents.find("input[type=checkbox]:checked");
  var radioButtons = contents.find("input[type=radio]:checked");
  
  for (var i = 0, len = inputFields.length; i < len; i++) {
    vrtxEdit.editorInitInputFields[i] = inputFields[i].value;
  }
  for (i = 0, len = selects.length; i < len; i++) {
    vrtxEdit.editorInitSelects[i] = selects[i].value;
  }
  for (i = 0, len = checkboxes.length; i < len; i++) {
    vrtxEdit.editorInitCheckboxes[i] = checkboxes[i].name;
  }
  for (i = 0, len = radioButtons.length; i < len; i++) {
    vrtxEdit.editorInitRadios[i] = radioButtons[i].name + " " + radioButtons[i].value;
  }
}

function unsavedChangesInEditor() {
  if (!vrtxEditor.needToConfirm) {
    vrtxAdmin.ignoreAjaxErrors = true;
    return false;
  }
  
  var vrtxEdit = vrtxEditor;
  
  if(typeof editorCourseSchedule === "object") {
    return editorCourseSchedule.checkUnsavedChanges();
  }
  
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
  initBoxPictures(altTexts);
  
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

function initPictureAddJsonField(elm) {
  initBoxPictures(elm.find(".boxPictureAlt, .featuredPictureAlt"));
  hideImagePreviewCaption(elm.find("input.preview-image-inputfield"), true);
}

function initBoxPictures(altTexts) {
  for (var i = altTexts.length; i--;) {
    var altText = $(altTexts[i]);
    var imageRef = altText.prev(".vrtx-image-ref");
    imageRef.addClass("vrtx-image-ref-alt-text");
    imageRef.find(".vrtx-image-ref-preview").append(altText.remove());
  }
}

function hideImagePreviewCaption(input, isInit) {
  if (!input.length) return;
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
    editorCourseSchedule = new courseSchedule();
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
      toggleShowHideBoolean(props, $(conditionHide).val() != conditionHideEqual, init);
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

    7.5 Course schedule editor (some is general and uses 8. for multiple enhancing)

\*-------------------------------------------------------------------*/

var onlySessionId = gup("sessionid", window.location.href);
function courseSchedule() {

  var url = location.protocol + "//" + location.host + location.pathname;
  if(/\/$/.test(url)) {
    url += "index.html";
  }
  url += "?action=course-schedule&mode=edit&t=" + (+new Date());
  // Debug: Local development
  // url = "/vrtx/__vrtx/static-resources/js/tp-test.json";
  
  // Hide shortcut for saving working copy
  $("#vrtx-save-as-working-copy-shortcut, #saveWorkingCopyAction, #buttons-or-text").hide();

  var allI18n = {
    "no": {
      "01": "jan",
      "02": "feb",
      "03": "mar",
      "04": "apr",
      "05": "mai",
      "06": "jun",
      "07": "jul",
      "08": "aug",
      "09": "sept",
      "10": "okt",
      "11": "nov",
      "12": "des",
      
      "room": "rom",
      "titles": {
        "plenary": "Fellesundervisning",
        "group": "Partiundervisning",
        "activities": "Aktiviteter"
      },
      "editOnlySessionTitle": "Rediger aktivitet",
      "loadingRetrievingData": "Henter timeplandata",
      "loadingGenerating": "Lager timeplan",
      "noData": "Ingen data",
      "noSessionData": "Ingen aktivitetsdata",
      "cancelled": "AVLYST",
      "cancelledVortexTooltip": "Aktiviteten kan avlyses i Vortex",
      "cancelledTPTooltip": "Aktiviteten er avlyst i timeplanleggingssystemet",
      
      "vrtxTitle": "Tittel:",
      "vrtxStaff": "Forelesere:",
      "vrtxStaffExternal": "Eksterne forelesere:",
      "vrtxStaffExternal-name": "Navn",
      "vrtxStaffExternal-url": "Lenke",
      "vrtxResources": "Ressurser:",
      "vrtxResources-title": "Tittel",
      "vrtxResources-url": "Lenke",
      "vrtxResourcesText": "Fritekst ressurser:",
      "vrtxResourcesFixed": "Faste ressurser:",
      "vrtxStatus": "Avlys"
    },
    "nn": {
      "01": "jan",
      "02": "feb",
      "03": "mar",
      "04": "apr",
      "05": "mai",
      "06": "jun",
      "07": "jul",
      "08": "aug",
      "09": "sept",
      "10": "okt",
      "11": "nov",
      "12": "des",
      
      "room": "rom",
      "titles": {
        "plenary": "Fellesundervisning",
        "group": "Partiundervisning",
        "activities": "Aktiviteter"
      },
      "editOnlySessionTitle": "Rediger aktivitet",
      "loadingRetrievingData": "Henter timeplandata",
      "loadingGenerating": "Lager timeplan",
      "noData": "Ingen data",
      "noSessionData": "Ingen aktivitetsdata",
      "cancelled": "AVLYST",
      "cancelledVortexTooltip": "Aktiviteten kan avlyses i Vortex",
      "cancelledTPTooltip": "Aktiviteten er avlyst i timeplanleggingssystemet",
      
      "vrtxTitle": "Tittel:",
      "vrtxStaff": "Forelesere:",
      "vrtxStaffExternal": "Eksterne forelesere:",
      "vrtxStaffExternal-name": "Namn",
      "vrtxStaffExternal-url": "Lenkje",
      "vrtxResources": "Ressursar:",
      "vrtxResources-title": "Tittel",
      "vrtxResources-url": "Lenkje",
      "vrtxResourcesText": "Fritekst ressursar:",
      "vrtxResourcesFixed": "Faste ressursar:",
      "vrtxStatus": "Avlys"
    },
    "en": {
      "01": "jan",
      "02": "feb",
      "03": "mar",
      "04": "apr",
      "05": "may",
      "06": "jun",
      "07": "jul",
      "08": "aug",
      "09": "sept",
      "10": "oct",
      "11": "nov",
      "12": "dec",
      
      "room": "room",
      "titles": {
        "plenary": "Plenary teaching",
        "group": "Group teaching",
        "activities": "Activities"
      },
      "editOnlySessionTitle": "Edit activity",
      "loadingRetrievingData": "Retrieving schedule data",
      "loadingGenerating": "Generating schedule",
      "noData": "No data",
      "noSessionData": "No activity data",
      "cancelled": "CANCELLED",
      "cancelledVortexTooltip": "The activity can be cancelled in Vortex",
      "cancelledTPTooltip": "The activity is cancelled in the schedulling system",
      
      "vrtxTitle": "Title:",
      "vrtxStaff": "Staff:",
      "vrtxStaffExternal": "External staff:",
      "vrtxStaffExternal-name": "Name",
      "vrtxStaffExternal-url": "Link",
      "vrtxResources": "Resources:",
      "vrtxResources-title": "Title",
      "vrtxResources-url": "Link",
      "vrtxResourcesText": "Text resources:",
      "vrtxResourcesFixed": "Fixed resources:",
      "vrtxStatus": "Cancel"
    }
  };
  
  this.sessionsLookup = {};
  
  this.lastId = "";
  this.lastSessionId = "";
  this.lastElm = null;
  
  this.retrievedScheduleData = null;
  this.i18n = allI18n[vrtxAdmin.lang];
  this.getSessionOnlyHtml = function(sessionId) {
    var sessionData = this.getSessionJSONFromId(sessionId);
    if(!sessionData) return null;
    
    var id = sessionData.id;
    var session = sessionData.session;
    var sequences = sessionData.sequences;
    var type = sessionData.type;
    var skipTier = sessionData.skipTier;
    
    var descs = this.retrievedScheduleData[type].vrtxEditableDescription;

    if(!this.sessionsLookup[id]) {
      this.sessionsLookup[id] = {};
    }
    var sessionHtml = this.getSessionHtml(id, session, sequences, descs, skipTier, editorJSONToHtml);    
    
    this.lastElm = $(".properties"); 
    this.lastId = id;
    this.lastSessionId = sessionId;
                                                    
    return { id: id, html: sessionHtml.html, title: sessionHtml.title };
  };
  this.getActivitiesForTypeHtml = function(type, skipTier) {
    if(!this.retrievedScheduleData[type]) return "";
    var descs = this.retrievedScheduleData[type].vrtxEditableDescription,
        data = this.retrievedScheduleData[type].activities;
    if(!descs || !data) return "";
        
    var dataLen = data.length;
    if(!dataLen) return "";
    
    // Store sessions HTML and multiple descriptions in lookup object
    var editorJSONToHtmlFunc = editorJSONToHtml,
        vrtxEdit = vrtxEditor,
        html = "",
        htmlMiddle = "",
        sessions = [],
        sequences = {}, // For fixed resources
        sessionsHtml = "",
        self = this;
    for(var i = 0; i < dataLen; i++) {
      var dt = data[i],
          dtShort = dt.teachingMethod.toLowerCase(),
          id = skipTier ? type : dtShort + "-" + dt.id;
      
      if(!skipTier || (skipTier && (i === 0))) {
        this.sessionsLookup[id] = {};
      }
      
      // Add together sessions from sequences
      for(var j = 0, len = dt.sequences.length; j < len; j++) {
        var sequence = dt.sequences[j];
        var fixedResources = sequence.vrtxResourcesFixed;
        if(fixedResources && fixedResources.length) {
          sequences[sequence.id] = fixedResources;
        }
        sessions = sessions.concat(sequence.sessions);
      }
      
      if(!skipTier || (skipTier && (i === (dataLen-1)))) {
        // Sort sessions
        sessions.sort(function(a, b) { // XXX: avoid parsing datetime for sessions twice
          var dateTimeA = self.getDateTime(a.dtStart, a.dtEnd);
          var startA = dateTimeA.start;
          var endA = dateTimeA.end;
          var a = startA.year + "" + startA.month + "" + startA.date + "" + startA.hh + "" + startA.mm + "" + endA.hh + "" + endA.mm;;
        
          var dateTimeB = self.getDateTime(b.dtStart, b.dtEnd);
          var startB = dateTimeB.start;
          var endB = dateTimeB.end;
          var b = startB.year + "" + startB.month + "" + startB.date + "" + startB.hh + "" + startB.mm + "" + endB.hh + "" + endB.mm;;
        
          return parseInt(a, 10) - parseInt(b, 10);
        });
        
        // Generate sessions HTML
        for(j = 0, len = sessions.length; j < len; j++) {
          var sessionHtml = this.getSessionHtml(id, sessions[j], sequences, descs, skipTier, editorJSONToHtmlFunc);
          sessionsHtml += vrtxEdit.htmlFacade.getAccordionInteraction(!skipTier ? "5" : "4", sessionHtml.sessionId, "session", sessionHtml.title, sessionHtml.html);
        }
        if(!skipTier) {
          this.sessionsLookup[id].html = "<span class='accordion-content-title'>" + this.i18n.titles.activities + "</span>" + sessionsHtml;
          htmlMiddle += vrtxEdit.htmlFacade.getAccordionInteraction("4", id, type, sessions[0].title, "");
          if(!data[i+1] || data[i+1].teachingMethod.toLowerCase() !== dtShort) {
            html += vrtxEdit.htmlFacade.getAccordionInteraction("3", dtShort, type, dt.teachingMethodName, "<div class='vrtx-grouped'>" + htmlMiddle + "</div>");
            htmlMiddle = "";
          }
          sessionsHtml = "";
          sessions = [];
        }
      }
    }
    if(skipTier) {
      this.sessionsLookup[id].html = "<span class='accordion-content-title'>" + this.i18n.titles.activities + "</span>" + sessionsHtml;
      html += vrtxEdit.htmlFacade.getAccordionInteraction("3", id, (type + " skip-tier"), dt.teachingMethodName, "");
    }
     
    return html;
  };
  this.getSessionHtml = function(id, session, sequences, descs, skipTier, editorJSONToHtmlFunc) {
    var sessionDatePostFixId = this.getDateAndPostFixId(session.dtStart, session.dtEnd),
        sessionId = id + "-" + session.id.replace(/\//g, "-") + "-" + sessionDatePostFixId.postFixId,
        sequenceId = session.id.replace(/\/[^\/]*$/, ""),
        sessionCancelled = (session.vrtxStatus && session.vrtxStatus === "cancelled") || (session.status && session.status === "cancelled"),
        rooms = session.rooms,
        sessionTitle = sessionDatePostFixId.date + " " +
                       "<span class='header-title'>" + (sessionCancelled ? "<span class='header-status'>" + this.i18n.cancelled + "</span> - " : "") + (session.vrtxTitle || session.title || session.id) + "</span>" +
                       (rooms ? (" - " + (rooms[0].buildingAcronym || rooms[0].buildingId) + " " + this.i18n.room + " " + rooms[0].roomId) : ""),
        sessionContent = editorJSONToHtmlFunc(id, sessionId, session, { "vrtxResourcesFixed": sequences[sequenceId] }, descs, this.i18n);

     this.sessionsLookup[id][sessionId] = {
       isEnhanced: false,
       isCancelled: sessionCancelled,
       hasChanges: false,
       multiples: sessionContent.multiples,
       rtEditors: sessionContent.rtEditors,
       rawPtr: session,
       descsPtr: descs,
       rawOrig: jQuery.extend(true, {}, session) // Copy object
     };
     
     return { sessionId: sessionId, html: sessionContent.html, title: sessionTitle };
  };
  this.loadingUpdate = function(msg) {
    var loader = $("#editor-loading-inner");
    if(!loader.length) {
      var loaderHtml = "<span id='editor-loading'><span id='editor-loading-inner'>" + msg + "...</span></span>";
      $(loaderHtml).insertAfter(".properties");
    } else {
      if(msg.length) {
        loader.text(msg + "...");
      } else {
        loader.parent().remove();
      }
    }
  };
  this.getDateAndPostFixId = function(s, e) {
    var start = this.parseDate(s);
    var end = this.parseDate(e);
    var strDate = start.date + ". " + this.i18n[start.month] + " " + start.year + " - kl " +
                  start.hh + ":" + start.mm + "&ndash;" + end.hh + ":" + end.mm;
    var postFixId = start.date + "-" + start.month + "-" + start.year + "-" + start.hh + "-" + start.mm + "-" + end.hh + "-" + end.mm;
    return { date: strDate, postFixId: postFixId };
  };
  this.getDateTime = function(s, e) {
    var startDateTime = this.parseDate(s);
    var endDateTime = this.parseDate(e);
    return { start: startDateTime, end: endDateTime };
  };
  this.parseDate = function(dateString) {
    // Old
    // var m = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2})(?::([0-9]*)(\.[0-9]*)?)?(?:([+-])([0-9]{2}):([0-9]{2}))?/.exec(dateString);
            // 2014     - 08       - 18       T12        : 15       :00         .000         +    02       :00
    var m = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})\.([0-9]{3})([+-])([0-9]{2}):([0-9]{2})$/.exec(dateString);
    return { year: m[1], month: m[2], date: m[3], hh: m[4], mm: m[5], tzhh: m[9], tzmm: m[10] };
  }
  this.getSessionJSONFromId = function(findSessionId) {
    for(var type in this.retrievedScheduleData) {
      if(!this.retrievedScheduleData[type]) continue;
      var data = this.retrievedScheduleData[type].activities;
      if(!data) continue;
      var dataLen = data.length;
      if(!dataLen) continue;
      
      var skipTier = type === "plenary";
      for(var i = 0; i < dataLen; i++) {
        var dt = data[i];
        var dtShort = dt.teachingMethod.toLowerCase();
        var id = skipTier ? type : dtShort + "-" + dt.id;
        var sessions = [];
        var sequences = {};
        for(var j = 0, len = dt.sequences.length; j < len; j++) {
          var sequence = dt.sequences[j];
          var fixedResources = sequence.vrtxResourcesFixed;
          if(fixedResources && fixedResources.length) {
            sequences[sequence.id] = fixedResources;
          }
          sessions = sessions.concat(sequence.sessions);
        }
        for(j = 0, len = sessions.length; j < len; j++) {
          var session = sessions[j];
          var sessionDatePostFixId = this.getDateAndPostFixId(session.dtStart, session.dtEnd);
          var sessionId = id + "-" + session.id.replace(/\//g, "-") + "-" + sessionDatePostFixId.postFixId;
          
          if(findSessionId === sessionId) {
            return { id: id, session: session, sequences: sequences, type: type, skipTier: skipTier };
          }
        }
      }
    }
    return null;
  };
  this.enhanceSession = function(id, sessionId, contentElm) {
    var session = this.sessionsLookup[id][sessionId];
    if(session && !session.isEnhanced) { // If not already enhanced
      var multiples = session.multiples;
      var enhanceMultipleInputFieldsFunc = enhanceMultipleInputFields;
      for(var i = multiples.length; i--;) {
        var m = multiples[i];
        enhanceMultipleInputFieldsFunc(m.name + "-" + sessionId, m.movable, m.browsable, 50, m.json);
      }
      var rtEditors = session.rtEditors;
      for(i = rtEditors.length; i--;) {
        vrtxEditor.richtextEditorFacade.setup({
          name: rtEditors[i],
          isCompleteEditor: false,
          isWithoutSubSuper: false,
          defaultLanguage: vrtxAdmin.lang,
          cssFileList: cssFileList,
          simple: true
        });
      }
      session.isEnhanced = true;
    }
  };
  this.checkUnsavedChanges = function() {
    this.saveLastSession();
    for(var type in this.sessionsLookup) {
      for(var session in this.sessionsLookup[type]) {
        if(this.sessionsLookup[type][session].hasChanges) {
          return true;
        }
      }
    }
    return false;
  };
  this.saveLastSession = function() {
    if(this.lastElm) {
      this.saveSession(this.lastElm, this.lastId, this.lastSessionId);
    }
  };
  this.saveSession = function(sessionElms, id, sessionId) {
    saveMultipleInputFields(sessionElms, "$$$");

    var sessionLookup = this.sessionsLookup[id][sessionId];
    var rawOrig = sessionLookup.rawOrig;
    var rawPtr = sessionLookup.rawPtr;
    var descsPtr = sessionLookup.descsPtr;
    
    editorHtmlToJSON(sessionElms, descsPtr, rawOrig, rawPtr);

    sessionLookup.hasChanges = editorDetectChange(rawPtr, rawOrig);
  };
  this.saved = function() {
    for(var type in this.sessionsLookup) {
      for(var session in this.sessionsLookup[type]) {
        var sessionObj = this.sessionsLookup[type][session];
        if(sessionObj.hasChanges) {
          sessionObj.rawOrig = sessionObj.rawPtr;
          sessionObj.hasChanges = false;
        }
      }
    }
    this.sessionOnlyWindowClose();
  };
  this.sessionOnlyWindowClose = function() {
    if(onlySessionId) {
      window.close();
    }
  };

  var contents = $("#contents");
  if(onlySessionId) contents.find("#vrtx-editor-title-submit-buttons").hide();

  this.loadingUpdate(this.i18n.loadingRetrievingData);
  var cs = this;
  
  // GET JSON
  var retrievedScheduleDeferred = $.Deferred();
  vrtxAdmin.serverFacade.getJSON(url, {
    success: function(data, xhr, textStatus) {
      cs.retrievedScheduleData = data;
      cs.loadingUpdate(cs.i18n.loadingGenerating);
      retrievedScheduleDeferred.resolve();
    },
    error: function(xhr, textStatus) {
      if(textStatus === "parsererror") { // Running Vortikal or invalid JSON
        retrievedScheduleDeferred.resolve();
        vrtxAdmin.displayErrorMsg(textStatus);
      }
    }
  }, false);
  
  // Create/admin fixed resource folders
  $("#contents").on("click", ".create-fixed-resources-folder", function(e) {
    alert("TODO Create");
    e.preventDefault();
    e.stopPropagation();
  });
  $("#contents").on("click", ".admin-fixed-resources-folder", function(e) {
    var fixedResourcesWindow = openPopup(this.href, 1000, 600, "adminFixedResources");
    e.preventDefault();
    e.stopPropagation();
  });
  
  var editorProperties = vrtxEditor.editorForm.find(".properties");
  editorProperties.hide();
  
  initMultipleInputFields();

  $.when(retrievedScheduleDeferred, vrtxEditor.multipleFieldsBoxesDeferred).done(function() {
    var csRef = cs;
    
    if(csRef.retrievedScheduleData == null) {
      csRef.loadingUpdate("");
      editorProperties.prepend("<p>" + csRef.i18n.noData + "</p>");
      return;
    }
    
    // Remove - TODO: don't generate
    $(".vrtx-json").remove();

    if(onlySessionId) {
      var sessionOnly = csRef.getSessionOnlyHtml(onlySessionId);
      var html = sessionOnly.html;
      if(!html) html = "<p>" + csRef.i18n.noSessionData + "</p>";

      contents.find("#vrtx-editor-title-submit-buttons-inner-wrapper > h2")
              .text(csRef.i18n.editOnlySessionTitle);
      editorProperties.prepend("<h4 class='property-label'>" + sessionOnly.title + "</h4>" + html);
      
      csRef.enhanceSession(sessionOnly.id, onlySessionId, editorProperties);
      
      var editorSubmitButtons = vrtxEditor.editorForm.find(".submitButtons");
      var newButtonsHtml = "<input class='vrtx-focus-button vrtx-embedded-button' id='vrtx-embedded-save-button' type='submit' value='Lagre' />" +
                           "<input class='vrtx-button vrtx-embedded-button' id='vrtx-embedded-cancel-button' type='submit' value='Avbryt' />";
      editorSubmitButtons.prepend(newButtonsHtml);
      contents.find("#vrtx-editor-title-submit-buttons").show();
      
      /* Save and unlock */
      editorSubmitButtons.on("click", "#vrtx-embedded-save-button", function(e) {
        editorSubmitButtons.find("#saveAndViewButton").trigger("click");
        e.stopPropagation();
        e.preventDefault();
      });
      
      /* Cancel is unlock */
      editorSubmitButtons.on("click", "#vrtx-embedded-cancel-button", function(e) {
        var form = $("form[name='unlockForm']");
        var url = form.attr("action");
        var dataString = form.serialize();
        vrtxAdmin.serverFacade.postHtml(url, dataString, {
          success: function (results, status, resp) {
            vrtxEditor.needToConfirm = false;
            csRef.sessionOnlyWindowClose();
          }
        });
        e.stopPropagation();
        e.preventDefault();
      });
    } else {
      var html = "<div class='accordion-title'>" + csRef.i18n.titles.plenary + "</div>" +
                 csRef.getActivitiesForTypeHtml("plenary", true) +
                 "<div class='accordion-title'>" + csRef.i18n.titles.group + "</div>" +
                 csRef.getActivitiesForTypeHtml("group", false);
      
      // Add HTML to DOM
      editorProperties.prepend("<div class='vrtx-grouped'>" + html + "</div>"); 
       
      // Accordions - define at run-time
      var accordionOnActivateTier3 = function (id, e, ui, accordion) {
        if(ui.newHeader[0]) { // Enhance multiple fields in session on open
          var sessionId = ui.newHeader[0].id;
          var sessionElm = $(ui.newHeader).closest("div");
          var content = sessionElm.find("> .accordion-content");
        
          csRef.lastId = id;
          csRef.lastSessionId = sessionId;
          csRef.lastElm = content;
          
          csRef.enhanceSession(id, sessionId, content);
        } else { // Update session and accordion title on close
          var sessionId = ui.oldHeader[0].id;
          var sessionElm = $(ui.oldHeader).closest("div");
          var content = sessionElm.find("> .accordion-content");
        
          csRef.lastId = "";
          csRef.lastSessionId = "";
          csRef.lastElm = null;
        
          csRef.saveSession(content, id, sessionId);
          
          // Update title
          var session = csRef.sessionsLookup[id][sessionId];
          var titleElm = sessionElm.find("> .header > .header-title");
          var newTitle = content.find("> div:first-child input[type='text']");
          
          var cancelledElm = content.find("input[name='vrtxStatus']");
          if(cancelledElm.length) {
            session.isCancelled = cancelledElm[0].checked;
          }
          if(newTitle.length && newTitle.val() != "") {
            titleElm.html((session.isCancelled ? " <span class='header-status'>" + csRef.i18n.cancelled + "</span> - " : "") + newTitle.val());
          } else {
            titleElm.html((session.isCancelled ? " <span class='header-status'>" + csRef.i18n.cancelled + "</span> - " : "") + session.rawOrig.title);
          }
        }
      };  
    
      // Tier 2
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
      
      // Tier 1
      var accordionOnActivateTier1 = function (isTier1, e, ui, accordion) {
        if(ui.newHeader[0]) {
          var id = ui.newHeader[0].id;
          var contentWrp = $("#" + id).parent().find(".accordion-content");
          if(isTier1) { // Lookup and add sessions HTML to DOM
            if(!contentWrp.children().length) { // If not already added
              contentWrp.html("<div class='vrtx-grouped'>" + csRef.sessionsLookup["plenary"].html + "</div>");
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
                  contentWrp.html("<div class='vrtx-grouped'>" + csRef.sessionsLookup[id].html + "</div>");
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
    
      // Tier 0
      var optsH3 = {
        elem: editorProperties.find("> .vrtx-grouped"),
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
    }
    
    JSON_ELEMENTS_INITIALIZED.resolve();
    
    var waitALittle = setTimeout(function() {      
      csRef.loadingUpdate("");
      editorProperties.show();
    }, 50);
  });
}

function editorJSONToHtml(id, sessionId, session, fixedResources, descs, i18n) {
  var html = "";
  var multiples = [];
  var rtEditors = [];
  var vrtxEdit = vrtxEditor;
  for(var name in descs) {
    var desc = descs[name],
        descProps = jQuery.extend(true, [], desc.props),
        val = session[name] || fixedResources[name],
        origVal = "",
        propsVal = "",
        browsable = false,
        hasOrig = false,
        size = 40;
    
    var origName = name.split("vrtx")[1];
    if(origName) {
      var origVal = session[origName.toLowerCase()];
      if(origVal && origVal != "") {
        if(!val || !val.length) {
          val = origVal;
        }
        hasOrig = true;
      }
    }
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
              propsVal += (val[j][descProps[i].name] || "") + "###";
            }
            if(j < (propsLen - 1)) propsVal += "$$$";
          }
        }
        size = 20;
      case "string":
        val = (propsVal != "") ? propsVal : val;
        val = (desc.multiple && typeof val === "array") ? val.join(",") : val;
        if(desc.multiple) {
          multiples.push({
            name: name,
            json: descProps ? descProps : null, 
            movable: desc.multiple.movable,
            browsable: browsable
          });
        }
        html += vrtxEdit.htmlFacade.getStringField({ title: i18n[name],
                                                     name: (desc.autocomplete ? "vrtx-autocomplete-" + desc.autocomplete + " " : "") + name + "-" + sessionId,
                                                     id: name + "-" + sessionId,
                                                     val: val,
                                                     size: size
                                                   }, name);
        break;
      case "json-fixed":
        if(val) {
          var buttons = /* "<a class='vrtx-button create-fixed-resources-folder' id='" + sessionId + "-create-fixed-resources' href='javascript:void(0);'>Lag ressursmappe</a> "  */
                        "<a class='vrtx-button admin-fixed-resources-folder' href='" + val[0].url.replace(/[^\\/]*$/, "") + "?vrtx=admin&refreshparent=true" + "'>Last opp flere / administrer</a>";
          for(var j = 0, propsLen = val.length; j < propsLen; j++) {
            propsVal += "<a href='" + val[j].url + "'>" + val[j].title + "</a>";
          }
          html += "<div class='vrtx-simple-html'><label>" + i18n[name] + "</label><div class='preview-html'>" + propsVal + "</div>" + buttons + "</div>";
        }
        break;
      case "html":
        html += vrtxEdit.htmlFacade.getSimpleHtmlField({ title: i18n[name],
                                                           name: name + "-" + sessionId,
                                                           id: name + "-" + sessionId,
                                                           val: val
                                                         }, name + "-" + sessionId);
        rtEditors.push(name + "-" + sessionId);
        break;
      case "checkbox":
        if(!origVal || origVal !== "cancelled") {
          html += vrtxEdit.htmlFacade.getCheckboxField({ title: i18n[name],
                                                         name: name + "-" + sessionId,
                                                         id: name + "-" + sessionId,
                                                         checked: (val === "active" ? null : val),
                                                         tooltip: i18n.cancelledVortexTooltip
                                                       }, name);
        } else {
          html += "<abbr tabindex='0' class='tooltips cancelled-tp' title='" + i18n.cancelledTPTooltip + "'></abbr>";
        }
        break;
      default:
        break;
    }
  }
   
  return { html: html, multiples: multiples, rtEditors: rtEditors };
}

function editorHtmlToJSON(sessionElms, descs, rawOrig, rawPtr) {
  var vrtxEdit = vrtxEditor;
  var editorDetectChangeFunc = editorDetectChange;
  for(var name in descs) {
    var desc = descs[name],
        val = "";
    // XXX: support multiple CK-fields starting with same name
    if(descs[name].type === "html") {
      var elm = sessionElms.find("textarea[name^='" + name + "']");
    } else {
      var elm = sessionElms.find("input[name='" + name + "']");
    }
    if(!elm.length) continue; // This should not happen

    if(desc.type === "checkbox") {
      if(elm[0].checked) {
        val = "cancelled"; // TODO: Not very general
      }
    } else if(desc.type === "html") {
      val = vrtxEdit.richtextEditorFacade.getInstanceValue(elm.attr("name"));
    } else {
      val = elm.val(); // To string (string)
      if(desc.multiple && val.length) { // To array (multiple)
        val = val.split("$$$");
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
    if(val && val.length && editorDetectChangeFunc(val, rawOrig[name.split("vrtx")[1].toLowerCase()])) {
      rawPtr[name] = val;
    } else {
      delete rawPtr[name];
    }
  }
}

function editorDetectChange(o1, o2) {
  if(typeof o1 === "object" && typeof o2 === "object") {
    if(o1.length) { // Array
      if(o1.length !== o2.length) return true;
      for(var i = 0, len = o1.length; i < len; i++) {
        if(editorDetectChange(o1[i], o2[i])) return true;
      }
    } else {
      var propCount2 = 0;
      for(prop2 in o2) {
        propCount2++;
      }
      var propCount1 = 0;
      for(prop1 in o1) {
        if(editorDetectChange(o1[prop1], o2[prop1])) return true;
        propCount1++;
      }
      if(propCount1 !== propCount2) return true;
    }
  } else if(typeof o1 === "string" && typeof o2 === "string") {
    if(o1 !== o2) return true;
  } else if(typeof o1 === "number" && typeof o2 === "number") {
    if(o1 !== o2) return true;
  } else if(typeof o1 !== typeof o2) {
    return true;
  }
  return false;
}
   

/*-------------------------------------------------------------------*\
    8. Multiple fields and boxes
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
  var formFields = json && json.length ? inputFieldVal.split("$$$")
                                       : inputFieldVal.split(",");

  vrtxEditor.multipleFieldsBoxes[name] = { counter: 1, limit: limit };

  var addFormFieldFunc = addFormField, html = "";
  for (var i = 0, len = formFields.length; i < len; i++) {
    html += addFormFieldFunc(name, len, $.trim(formFields[i]), size, isBrowsable, isMovable, isDropdown, true, json);
  }
  html = $.parseHTML(html, document, true);
  $(html).insertBefore("#vrtx-" + name + "-add");
  inputFieldParent.find(".vrtx-multipleinputfield:first").addClass("first");
  
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

  removeButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("remove", " " + name, idstr, "", vrtxAdmin.multipleFormGroupingMessages.remove);
  if (isMovable) {
    if (i > 1 && len > 0) {
      moveUpButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("moveup", "", idstr, vrtxAdmin.multipleFormGroupingMessages.moveUp, "<img src='/vrtx/__vrtx/static-resources/themes/default/images/multiple-move-up.png' alt='' />");
    }
    if (i < len) {
      moveDownButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("movedown", "", idstr, vrtxAdmin.multipleFormGroupingMessages.moveDown, "<img src='/vrtx/__vrtx/static-resources/themes/default/images/multiple-move-down.png' alt='' />");
    }
  }
  if (isBrowsable) {
    browseButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("browse", "-resource-ref", idstr, "", vrtxAdmin.multipleFormGroupingMessages.browse);
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
        moveDownButton = vrtxEditor.htmlFacade.getMultipleInputfieldsInteractionsButton("movedown", "", idstr, vrtxAdmin.multipleFormGroupingMessages.moveDown, "<img src='/vrtx/__vrtx/static-resources/themes/default/images/multiple-move-down.png' alt='' />");
        last.append(moveDownButton);
      }
    }
    
    var addBtn = $("#vrtx-" + name + "-add");
    $($.parseHTML(html, document, true)).insertBefore(addBtn);
    
    fields = $("." + name + " div.vrtx-multipleinputfield");
    
    if(len === 0) {
      fields.filter(":first").addClass("first");
    }
    
    autocompleteUsername(".vrtx-autocomplete-username", idstr + i);
    
    var focusable = addBtn.prev().find("input[type='text'], select")
    if(focusable.length) {
      focusable[0].focus();
    }

    // Hide add button if limit is reached
    if((len == (vrtxEditor.multipleFieldsBoxes[name].limit - 1))) {
      $("<p class='vrtx-" + name + "-limit-reached'>" + vrtxAdmin.multipleFormGroupingMessages.limitReached + "</p>").insertBefore(addBtn);
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
    if(!firstField.hasClass("first")) {
      firstField.addClass("first");
    }
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
function saveMultipleInputFields(content, arrSeperator) {
  var multipleFields = (typeof content !== "undefined")
                       ? content.find(".vrtx-multipleinputfields")
                       : $(".vrtx-multipleinputfields");
  var arrSep = (typeof arrSeperator === "string") ? arrSeperator : ",";
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
        result += arrSep;
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
      jsonElm.append(vrtxEditor.htmlFacade.getJsonBoxesInteractionsButton("add", vrtxAdmin.multipleFormGroupingMessages.add, "<img src='/vrtx/__vrtx/static-resources/themes/default/images/multiple-add.png' alt='' />"))
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
  var removeButton = vrtxEditor.htmlFacade.getJsonBoxesInteractionsButton('remove', "", vrtxAdmin.multipleFormGroupingMessages.remove);

  var newElementId = "vrtx-json-element-" + j.name + "-" + vrtxEditor.multipleFieldsBoxes[j.name].counter;
  var newElementHtml = htmlTemplate + "<input type=\"hidden\" class=\"id\" value=\"" + vrtxEditor.multipleFieldsBoxes[j.name].counter + "\" \/>" + removeButton;
  if (!isImmovable && numOfElements > 0) {
    var moveUpButton = vrtxEditor.htmlFacade.getJsonBoxesInteractionsButton('move-up', vrtxAdmin.multipleFormGroupingMessages.moveUp, "<img src='/vrtx/__vrtx/static-resources/themes/default/images/multiple-move-up.png' alt='' />");
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
    var moveDownButton = vrtxEditor.htmlFacade.getJsonBoxesInteractionsButton('move-down', vrtxAdmin.multipleFormGroupingMessages.moveDown, "<img src='/vrtx/__vrtx/static-resources/themes/default/images/multiple-move-down.png' alt='' />");
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
  
  // Box picture
  initPictureAddJsonField(btn.closest(".vrtx-json").find(".vrtx-json-element:last"));

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
  getMultipleInputfieldsInteractionsButton: function (clazz, name, idstr, title, text) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["button"], {
      type: clazz,
      name: name,
      idstr: idstr,
      title: title,
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
      title: vrtxAdmin.multipleFormGroupingMessages.add,
      buttonText: "<img src='/vrtx/__vrtx/static-resources/themes/default/images/multiple-add.png' alt='' />",
      json: json
    });
  },
  getJsonBoxesInteractionsButton: function (clazz, title, text) {
    return vrtxAdmin.templateEngineFacade.render(vrtxEditor.multipleFieldsBoxesTemplates["add-remove-move"], {
      clazz: clazz,
      title: title,
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
      inputFieldName: inputFieldName,
      elemId: elem.id || inputFieldName,
      elemVal: elem.val
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
      elemTooltip: elem.tooltip,
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