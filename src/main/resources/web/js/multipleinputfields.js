var MULTIPLE_INPUT_FIELD_NAMES = new Array();

function loadMultipleInputFields(name,addName, removeName) {
    var id = "#" + name;
    if ($(id).val() == null)
        return;

    if(MULTIPLE_INPUT_FIELD_NAMES.length > 0){
        MULTIPLE_INPUT_FIELD_NAMES[MULTIPLE_INPUT_FIELD_NAMES.length+1] = name; 
    }else{
        MULTIPLE_INPUT_FIELD_NAMES[0] = name; 
    }
    
    $(id).hide();
    $(id).after("<div id='vrtx-" + name + "-add'>" +
            "<button  onClick=\"addFormField('"+ name + "',null, '"+ removeName +  "'); return false;\">" + addName + "</button></div>");
    $(id).after("<input type='hidden' id='id-" + name + "' name='id-" + name + "' value='1' />");

    var listOfFiles = document.getElementById(name).value.split(",");
    for (i in listOfFiles) {
        addFormField(name,jQuery.trim(listOfFiles[i]), removeName);    
    }
}

function addFormField(name, value, removeName) {
    var idstr = "vrtx-" + name + "-";
    var i = document.getElementById("id-" + name).value;
    if (value == null)
        value = "";

    var deleteRow;
    if (removeName == null) {
        deleteRow = "";
    } else {
        deleteRow = "<button type='button' id='" + idstr + "remove' onClick='removeFormField(\"#" + idstr + "row-" + i + 
            "\"); return false;'>" + removeName + "</button>";
    }

    var browseServer = "";//"<button type=\"button\" id=\"" + idstr + "browse\" onclick=\"browseServer('" + idstr + id + "', '" + 
            //editorBase + "', '" + baseFolder + "', '" + editorBrowseUrl + "', 'File');\">" + browsName + "</button>";
    var classStr = " class='"  + idstr + "style' ";

    $("<p " + classStr + " id='"+ idstr + "row-" + i + "'><input value='" + value +  "' type='text' size='20' id='" + 
            idstr + i + "'> " + browseServer + deleteRow + "</p>").insertBefore("#vrtx-" + name + "-add");

    i++;
    document.getElementById("id-" + name).value = i;
}

function removeFormField(id) {
    $(id).remove();
}

function formatMultipleInputFields(name) {
    if ($( "#" + name ).val() == null)
        return;
    
    var test = $.find("input[id^='vrtx-" + name + "']");
    var result = "";
    for (i in test) {
        result += test[i].value;
        if (i < (test.length-1)) {
            result += ",";
        }
    }
    document.getElementById(name).value = result;
}

function saveMultipleInputFields(){
    for(i in MULTIPLE_INPUT_FIELD_NAMES){
        formatMultipleInputFields(MULTIPLE_INPUT_FIELD_NAMES[i]);
    }
}