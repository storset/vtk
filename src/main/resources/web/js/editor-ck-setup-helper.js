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

function newEditor(name, completeEditor, withoutSubSuper, baseFolder, baseUrl, baseDocumentUrl, browsePath, defaultLanguage, cssFileList, simpleHTML) {
  // File browser
  var linkBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=' + browsePath;
  var imageBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=' + browsePath;
  var flashBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=' + browsePath;

  var isCompleteEditor = completeEditor != null ? completeEditor : false;
  var isWithoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false;
  var isSimpleHTML = simpleHTML != null ? simpleHTML : false;

  // CKEditor configurations
  if (contains(name, "introduction")
   || contains(name, "resource.description")
   || contains(name, "resource.image-description")
   || contains(name, "resource.video-description")
   || contains(name, "resource.audio-description")) { // Introductions / descriptions
    setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, cssFileList, 150, 400, 40, inlineToolbar,
                      isCompleteEditor, false, baseDocumentUrl, isSimpleHTML);
  } else if (contains(name, "caption")) { // Caption
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
            || name == "content-study") {
      height = 400;
      maxHeight = 800;
      if (name == "resource.content") { // Old editor
        completeTB = completeToolbarOld;
      } 
      if (name == "content-study") { // Study toolbar
        completeTB = studyToolbar;
      }
    }
    setCKEditorConfig(name, linkBrowseUrl, imageBrowseUrl, flashBrowseUrl, defaultLanguage, cssFileList, height, maxHeight, 50, completeTB,
                      isCompleteEditor, true, baseDocumentUrl, isSimpleHTML);
  } else if (isWithoutSubSuper) {
    setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, null, 40, 400, 40, inlineToolbar, 
                      isCompleteEditor, true, baseDocumentUrl, isSimpleHTML);
  } else {
    setCKEditorConfig(name, linkBrowseUrl, null, null, defaultLanguage, null, 40, 400, 40, withoutSubSuperToolbar, 
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
    setCKEditorConfig("comments-text", null, null, null, null, null, 150, 400, 40, commentsToolbar, false, true, null);
  });
}

/*
 * Check if inputfields or textareas (CK) have changes
 *
 */

var INITIAL_INPUT_FIELDS = [];
var INITIAL_SELECTS = [];
var INITIAL_CHECKBOXES = [];
var INITIAL_RADIO_BUTTONS = [];

var NEED_TO_CONFIRM = true;
var UNSAVED_CHANGES_CONFIRMATION;

$(window).load(function () {
  storeInitPropValues();
});

/* Store initial values of inputfields */
function storeInitPropValues() {
  var contents = $("#contents");

  var inputFields = contents.find("input").not("[type=submit]").not("[type=button]")
                                          .not("[type=checkbox]").not("[type=radio]");
  for(var i = 0, len = inputFields.length; i < len; i++) {
    INITIAL_INPUT_FIELDS[i] = inputFields[i].value;
  }
  
  var selects = contents.find("select");
  for(var i = 0, len = selects.length; i < len; i++) {
    INITIAL_SELECTS[i] = selects[i].value;
  }
  
  var checkboxes = contents.find("input[type=checkbox]:checked");
  for(var i = 0, len = checkboxes.length; i < len; i++) {
    INITIAL_CHECKBOXES[i] = checkboxes[i].name;
  }
  
  var radioButtons = contents.find("input[type=radio]:checked");
  for(var i = 0, len = radioButtons.length; i < len; i++) {
    INITIAL_RADIO_BUTTONS[i] = radioButtons[i].name + " " + radioButtons[i].value;
  }
   
}

function validTextLengthsInEditor(isOldEditor) {
  var MAX_LENGTH = 1000, // Back-end limits it to 2048
      // NEW starts on wrapper and OLD starts on field (because of slightly different semantic/markup build-up)
      INPUT_NEW = ".vrtx-string, .vrtx-resource-ref, .vrtx-image-ref, .vrtx-media-ref",
      INPUT_OLD = "input[type=text]",
      CK_NEW = ".vrtx-simple-html, .vrtx-simple-html-small", // aka. textareas
      CK_OLD = "textarea:not(#resource\\.content)";

  var contents = $("#contents");
  
  // String textfields
  var currentInputFields = isOldEditor ? contents.find(INPUT_OLD) : contents.find(INPUT_NEW);
  for (var i = 0, textLen = currentInputFields.length; i < textLen; i++) {
    var strElm = $(currentInputFields[i]);
    var condition = isOldEditor ? strElm.val().length > MAX_LENGTH : strElm.find("input").val().length > MAX_LENGTH;
    if(condition) {
      if(typeof tooLongFieldPre !== "undefined" && typeof tooLongFieldPost !== "undefined") {
        $("html").scrollTop(0);
        vrtxAdmin.displayErrorMsg(tooLongFieldPre + (isOldEditor ? strElm.closest(".property-item").find(".property-label:first").text() : strElm.find("label").text()) + tooLongFieldPost);
      }
      return false;  
    }
  }
  
  // Textareas that are not content-fields (CK)
  var currentTextAreas = isOldEditor ? contents.find(CK_OLD) : contents.find(CK_NEW);
  for (i = 0, len = currentTextAreas.length; i < len; i++) {
    if (typeof CKEDITOR !== "undefined") {
      var txtAreaElm = $(currentTextAreas[i]);
      var txtArea = isOldEditor ? txtAreaElm : txtAreaElm.find("textarea");
      var ckInstance = getCkInstance(txtArea[0].name);
      if (ckInstance && ckInstance.getData().length > MAX_LENGTH) { // && guard
        if(typeof tooLongFieldPre !== "undefined" && typeof tooLongFieldPost !== "undefined") {
          $("html").scrollTop(0);
          vrtxAdmin.displayErrorMsg(tooLongFieldPre + (isOldEditor ? txtAreaElm.closest(".property-item").find(".property-label:first").text() : txtAreaElm.find("label").text()) + tooLongFieldPost);
        }
        return false;
      }
    }
  }
  
  return true;
}

function unsavedChangesInEditor() {
  if (!NEED_TO_CONFIRM) return false;
  
  var contents = $("#contents");

  // Inputfields (not submit and button)
  var currentStateOfInputFields = contents.find("input").not("[type=submit]").not("[type=button]")
                                                        .not("[type=checkbox]").not("[type=radio]");
  var textLen = currentStateOfInputFields.length;
  if(textLen != INITIAL_INPUT_FIELDS.length) { // if something is removed or added
    return true;
  }

  // Selects
  var currentStateOfSelects = contents.find("select");
  var selectsLen = currentStateOfSelects.length;
  if( selectsLen != INITIAL_SELECTS.length) { // if something is removed or added
    return true;
  }
  
  // Checkboxes
  var currentStateOfCheckboxes = contents.find("input[type=checkbox]:checked");
  var checkboxLen = currentStateOfCheckboxes.length;
  if(checkboxLen != INITIAL_CHECKBOXES.length) { // if something is removed or added
    return true;
  }
  
  // Radio buttons
  var currentStateOfRadioButtons = contents.find("input[type=radio]:checked");
  var radioLen = currentStateOfRadioButtons.length;
  if(radioLen != INITIAL_RADIO_BUTTONS.length) { // if something is removed or added
    return true;
  }
  
  // Check if values have changed
  
  for (var i = 0; i < textLen; i++) {
    if (currentStateOfInputFields[i].value !== INITIAL_INPUT_FIELDS[i]) {
      return true; // unsaved textfield
    }
  }
  
  for (var i = 0; i < selectsLen; i++) {
    if (currentStateOfSelects[i].value !== INITIAL_SELECTS[i]) {
      return true; // unsaved select value
    }
  }
  
  for (var i = 0; i < checkboxLen; i++) {
    if (currentStateOfCheckboxes[i].name !== INITIAL_CHECKBOXES[i]) {
      return true; // unsaved checked checkbox
    }
  }

  for (var i = 0; i < radioLen; i++) {
    var currentStateOfRadioButton = currentStateOfRadioButtons[i].name + " " + currentStateOfRadioButtons[i].value;
    if (currentStateOfRadioButton !== INITIAL_RADIO_BUTTONS[i]) {
      return true; // unsaved checked radio button
    }
  }
  
  //---

  // Textareas (CK->checkDirty())
  var currentStateOfTextFields = contents.find("textarea");
  for (i = 0, len = currentStateOfTextFields.length; i < len; i++) {
    if (typeof CKEDITOR !== "undefined") {
      var ckInstance = getCkInstance(currentStateOfTextFields[i].name);
      if (ckInstance && ckInstance.checkDirty()) {
        return true  // unsaved textarea
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
