/*
 * Check if inputfields or textareas (CK) have changes
 *
 */

var INITIAL_INPUT_FIELDS = [];
var INITIAL_SELECTS = [];
var NEED_TO_CONFIRM = true;
var UNSAVED_CHANGES_CONFIRMATION;

$(document).ready(function () {
  storeInitPropValues();
});

/* Store initial values of inputfields */
function storeInitPropValues() {
  var inputFields = $("input").not("[type=submit]").not("[type=button]");
  for(var i = 0, len = inputFields.length; i < len; i++) {
    INITIAL_INPUT_FIELDS[i] = inputFields[i].value;
  }
  
  var selects = $("select");
  for(var i = 0, len = selects.length; i < len; i++) {
    INITIAL_SELECTS[i] = selects[i].value;
  }
  
}

function unsavedChangesInEditor() {
  if (!NEED_TO_CONFIRM) return false;

  // Inputfields (not submit and button)
  var currentStateOfInputFields = $("input").not("[type=submit]").not("[type=button]");
  var len = INITIAL_INPUT_FIELDS.length;
  if(len != currentStateOfInputFields.length) { // if something is removed or added
    return true;
  }
  for (var i = 0; i < len; i++) {
    if (currentStateOfInputFields[i].value !== INITIAL_INPUT_FIELDS[i]) {
      return true; // unsaved textfield
    }
  }
  
  // Selects
  var currentStateOfSelects = $("select");
  len = INITIAL_SELECTS.length;
  if(len != currentStateOfSelects.length) { // if something is removed or added
    return true;
  }
  for (var i = 0; i < len; i++) {
    if (currentStateOfSelects[i].value !== INITIAL_SELECTS[i]) {
      return true; // unsaved textfield
    }
  }

  // Textareas (CK->checkDirty())
  var currentStateOfTextFields = $("textarea");
  for (i = 0, len = currentStateOfTextFields.length; i < len; i++) {
    if (typeof (CKEDITOR) !== "undefined") {
      if (getCkInstance(currentStateOfTextFields[i].name)) {
        if (getCkInstance(currentStateOfTextFields[i].name).checkDirty()) {
          return true  // unsaved textarea
        }
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