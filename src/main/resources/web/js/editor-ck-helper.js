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

$(document).ready(function () {
  storeInitPropValues();
});

/* Store initial values of inputfields */
function storeInitPropValues() {
  var inputFields = $("input").not("[type=submit]").not("[type=button]")
                              .not("[type=checkbox]").not("[type=radio]");
  for(var i = 0, len = inputFields.length; i < len; i++) {
    INITIAL_INPUT_FIELDS[i] = inputFields[i].value;
  }
  
  var selects = $("select");
  for(var i = 0, len = selects.length; i < len; i++) {
    INITIAL_SELECTS[i] = selects[i].value;
  }
  
  var checkboxes = $("input[type=checkbox]:checked");
  for(var i = 0, len = checkboxes.length; i < len; i++) {
    INITIAL_CHECKBOXES[i] = checkboxes[i].name;
  }
  
  var radioButtons = $("input[type=radio]:checked");
  for(var i = 0, len = radioButtons.length; i < len; i++) {
    INITIAL_RADIO_BUTTONS[i] = radioButtons[i].name + " " + radioButtons[i].value;
  }
   
}

function unsavedChangesInEditor() {
  if (!NEED_TO_CONFIRM) return false;

  // Inputfields (not submit and button)
  var currentStateOfInputFields = $("input").not("[type=submit]").not("[type=button]")
                                            .not("[type=checkbox]").not("[type=radio]");
  var textLen = currentStateOfInputFields.length;
  if(textLen != INITIAL_INPUT_FIELDS.length) { // if something is removed or added
    return true;
  }

  // Selects
  var currentStateOfSelects = $("select");
  var selectsLen = currentStateOfSelects.length;
  if( selectsLen != INITIAL_SELECTS.length) { // if something is removed or added
    return true;
  }
  
  // Checkboxes
  var currentStateOfCheckboxes = $("input[type=checkbox]:checked");
  var checkboxLen = currentStateOfCheckboxes.length;
  if(checkboxLen != INITIAL_CHECKBOXES.length) { // if something is removed or added
    return true;
  }
  
  // Radio buttons
  var currentStateOfRadioButtons = $("input[type=radio]:checked");
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
    if (currentStateOfRadioButtons[i].name + " " + currentStateOfRadioButtons[i].value !== INITIAL_RADIO_BUTTONS[i]) {
      return true; // unsaved checked radio button
    }
  }
  
  //---

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