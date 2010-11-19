// JavaScript Document	
var INITIAL_INPUT_FIELDS = new Array();
var NEED_TO_CONFIRM = true;
var UNSAVED_CHANGES_CONFIRMATION;

function initPropChange() {
    var i = 0;
    $("input").each(function() {
        INITIAL_INPUT_FIELDS[i++] = this.value;
    });
}

function unsavedChangesInEditor() {
    if (!NEED_TO_CONFIRM)
        return false;
    var dirtyState = false;
    currentStateOfInputFields = $("input");
    var INITIAL_INPUT_FIELDS_LENGTH = INITIAL_INPUT_FIELDS.length;
    for (i = 0; i < INITIAL_INPUT_FIELDS_LENGTH; i++) {
        if (currentStateOfInputFields[i].value != INITIAL_INPUT_FIELDS[i]) {
            dirtyState = true;
            break;
        }
    }
    $("textarea").each(function() {
        if (typeof (CKEDITOR) != "undefined") {
            if (getCkInstance(this.name) != null) { // defined in vrtx-json-javascript.ftl
                if (getCkInstance(this.name).checkDirty()) {
                    dirtyState = true;
                    return;
                }
            }
        }
    });
    return dirtyState;
}

function unsavedChangesInEditorMessage() {
    if (unsavedChangesInEditor()) {
        return UNSAVED_CHANGES_CONFIRMATION;
    }
}
