// Helper functions

function getCkValue(instanceName) {
    var oEditor = getCkInstance(instanceName);
    return oEditor.getData();
}

function getCkInstance(instanceName){
  	for(var i in CKEDITOR.instances) {
    	if(CKEDITOR.instances[i].name == instanceName){
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

// Prop change

var INITIAL_INPUT_FIELDS = new Array();
var NEED_TO_CONFIRM = true;
var UNSAVED_CHANGES_CONFIRMATION;

$(document).ready(function() {
 		initPropChange(); 
});

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
            return true;
        }
    }    
    $("textarea").each(function() {
        if (typeof (CKEDITOR) != "undefined") {
            if (getCkInstance(this.name) != null) {
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