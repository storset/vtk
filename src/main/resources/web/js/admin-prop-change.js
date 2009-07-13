// JavaScript Document	
var INITIAL_INPUT_FIELDS = new Array();
var NEED_TO_CONFIRM = true;
var PROP_CHANGE_CONFIRM_MSG;

$(document).ready(function() {
    var i = 0;
    $("input").each(function() {
        INITIAL_INPUT_FIELDS[i++] = this.value;
    });
});

function checkPropChange(){
    if(!NEED_TO_CONFIRM)
        return;
    var dirtyState = false;
    currentStateOfInputFeelds = $("input");
    for(i = 0; i < INITIAL_INPUT_FIELDS.length; i++){
        if(currentStateOfInputFeelds[i].value != INITIAL_INPUT_FIELDS[i]){
            dirtyState = true;
            break;
        }
    }
    $("textarea").each(function() {
        if(FCKeditorAPI.GetInstance(this.name).IsDirty()){
            dirtyState = true;
            return;
        }
     });
     if(dirtyState)
        return PROP_CHANGE_CONFIRM_MSG;
}