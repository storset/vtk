var MULTIPLE_INPUT_FIELD_NAMES = new Array();
var COUNTER_FOR_MULTIPLE_INPUT_FIELD = new Array();

function loadMultipleInputFields(name,addName, removeName) {
    var id = "#" + name;
    if ($(id).val() == null)
        return;
    
    COUNTER_FOR_MULTIPLE_INPUT_FIELD[name] = 0;
    if(MULTIPLE_INPUT_FIELD_NAMES.length > 0){
        MULTIPLE_INPUT_FIELD_NAMES[MULTIPLE_INPUT_FIELD_NAMES.length+1] = name; 
    }else{
        MULTIPLE_INPUT_FIELD_NAMES[0] = name; 
    }
    
    var size = $(id).attr("size");  
    $(id).hide();
    $(id).after("<div id='vrtx-" + name + "-add'>" + "<button  onClick=\"addFormField('"+ 
                name + "',null, '"+ removeName + "','" + size + "'); return false;\">" + 
                addName + "</button></div>");

    var l = $(id).val().split(",");
    var lLen = l.length;
    for (var i = 0; i < lLen; i++) {
        addFormField(name,jQuery.trim(l[i]), removeName,size);    
    }
}

function addFormField(name, value, removeName, size) {
    var idstr = "vrtx-" + name + "-";
    var i = COUNTER_FOR_MULTIPLE_INPUT_FIELD[name];
    if (value == null){
        value = "";
    }
    var deleteRow = "";
    if (removeName != null) {
        removeButton = "<button type='button' id='" + idstr + "remove' onClick='removeFormField(\"#" + 
        idstr + "row-" + i + "\"); return false;'>" + removeName + "</button>";
    } 
    $("#vrtx-" + name + "-add").before("<div class='vrtx-multipleinputfield' id='"+ idstr + 
            "row-" + i + "'><input value='" + value +  "' type='text'  size='" + size +"' id='" + 
            idstr + i + "'> " + removeButton + "</div>");
    
    COUNTER_FOR_MULTIPLE_INPUT_FIELD[name]++;
}

function removeFormField(id) {
    $(id).remove();
}

function formatMultipleInputFields(name) {
    if ($( "#" + name ).val() == null)
        return;
    
    var allFields = $.find("input[id^='vrtx-" + name + "']");
    var result = "";
    var allFieldsLength = allFields.length;
    for (var i = 0; i < allFieldsLength; i++) {
        result += allFields[i].value;
        if (i < (allFieldsLength-1)) {
            result += ",";
        }
    }
    $("#" + name).val(result);
}

function saveMultipleInputFields(){
    for(i in MULTIPLE_INPUT_FIELD_NAMES){
        formatMultipleInputFields(MULTIPLE_INPUT_FIELD_NAMES[i]);
    }
}

