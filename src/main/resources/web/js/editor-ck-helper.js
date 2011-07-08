/*
 * Check if inputfields or textareas (CK) have changes
 *
 */

var INITIAL_INPUT_FIELDS = [];
var NEED_TO_CONFIRM = true;
var UNSAVED_CHANGES_CONFIRMATION;

$(document).ready(function () {
  storeInitPropValues();
});

/* Store initial values in inputfields */
function storeInitPropValues() {
  var inputFields = $("input");
  for(var i = 0, len = inputFields.length; i < len;) {
    INITIAL_INPUT_FIELDS[i++] = $(inputFields[i]).val();
  }
}

function unsavedChangesInEditor() {
  if (!NEED_TO_CONFIRM) return false;

  // Textfields
  var currentStateOfInputFields = $("input");
  for (var i = 0, len = INITIAL_INPUT_FIELDS.length; i < len; i++) {
    if (currentStateOfInputFields[i].value !== INITIAL_INPUT_FIELDS[i]) {
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

// TODO: is this used anywhere(?)
function isCkEditor(instanceName) {
  var oEditor = getCkInstance(instanceName);
  return oEditor != null;
}

/* ^ Helper functions */

/* ^ Check if inputfields or textareas (CK) have changes */