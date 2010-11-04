var MULTIPLE_INPUT_FIELD_NAMES = new Array();
var COUNTER_FOR_MULTIPLE_INPUT_FIELD = new Array();
var LENGTH_FOR_MULTIPLE_INPUT_FIELD = new Array();

var debugMultipleInputFields = false;

function loadMultipleInputFields(name, addName, removeName, moveUpName, moveDownName) {
    var id = "#" + name;
    if ($(id).val() == null) { return; }
    var formFields = $(id).val().split(",");
    
    COUNTER_FOR_MULTIPLE_INPUT_FIELD[name] = 1; // 1-index
    LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] = formFields.length;
    MULTIPLE_INPUT_FIELD_NAMES.push(name);
    
    var size = $(id).attr("size");

    $(id).hide();
    $(id).after("<div id='vrtx-" + name + "-add'>"
    		  + "<button  onClick=\"addFormField('" + name + "',null, '"
    		  + removeName + "','" + moveUpName + "','" + moveDownName + "','" + size + "'," + false + "); return false;\">"
    		  + addName + "</button></div>");
     
    for (var i = 0; i < LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]; i++) {
       addFormField(name, jQuery.trim(formFields[i]), removeName, moveUpName, moveDownName, size, true);    
    }
} 

function addFormField(name, value, removeName, moveUpName, moveDownName, size, init) {
	if (value == null) { value = ""; }
	
    var idstr = "vrtx-" + name + "-";
    var i = COUNTER_FOR_MULTIPLE_INPUT_FIELD[name];
    var removeButton = "";
    var moveUpButton = "";
    var moveDownButton = "";
    
    if (removeName != null) {
        removeButton = "<button type='button' "
        + "id='" + idstr + "remove' "
        + "onClick='removeFormField(\"" + name + "\",\"" + i + "\"); return false;'>"
        + removeName + "</button>";
    }
    if (moveUpName != null && i > 1) {
    	moveUpButton = "<button class='moveup' type='button' "
        + "id='" + idstr + "moveup' "
        + "onClick='moveUpFormField(\"#" + idstr + "row-" + i + "\"); return false;'>"
        + "&uarr; " + moveUpName + "</button>";
    } 
    if (moveDownName != null && i < LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]) {
    	moveDownButton = "<button class='movedown' type='button' "
    	+ "id='" + idstr + "movedown' "
    	+ "onClick='moveDownFormField(\"#" + idstr + "row-" + i + "\"); return false;'>"
    	+ "&darr; " + moveDownName + "</button>";
    }
    
    $("#vrtx-" + name + "-add")
      .before("<div class='vrtx-multipleinputfield' id='" + idstr + "row-" + i + "'>"
      + "<input value='" + value + "' type='text' size='" + size + "' id='" + idstr + i + "'>" 
      + removeButton + moveUpButton + moveDownButton + "</div>");
    
    if(!init) {
    	var fields = "." + name + " div.inputfield div";
        if($(fields).eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).not("has:button.movedown")) {
        	
        	var theId = $(fields).eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).attr("id");
        	
        	moveDownButton = "<button class='movedown' type='button' "
        	+ "id='" + idstr + "movedown' "
        	+ "onClick='moveDownFormField(\"#" + theId + "\"); return false;'>"
        	+ "&darr; " + moveDownName + "</button>";

        	$(fields).eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).append(moveDownButton);
        	logMultipleInputFields("Added before-last movedown");
        }
        LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]++;
      }
    
    COUNTER_FOR_MULTIPLE_INPUT_FIELD[name]++;
}

function removeFormField(name, i) {

	var id = "#vrtx-" + name + "-row-";

    $(id + i).remove();
    logMultipleInputFields("Fjerner :" + id + i);
    
    LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]--;
    COUNTER_FOR_MULTIPLE_INPUT_FIELD[name]--;
    logMultipleInputFields("Number of inputfields: " + LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]);
    logMultipleInputFields("Next number for inputfield: " + COUNTER_FOR_MULTIPLE_INPUT_FIELD[name]);
    
    var fields = "." + name + " div.inputfield div";
    
	if($(fields).eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).has("button.movedown")) {
	  $(fields).eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).find("button.movedown").remove();
	  logMultipleInputFields("Removed last movedown");
	}
			
	if($(fields).eq(0).has("button.moveup")) {
	  $(fields).eq(0).find("button.moveup").remove();
	  logMultipleInputFields("Removed first moveup");
	}
}

function moveUpFormField(id) {
  var thisText = $(id).find("input").val();
  var prevText = $(id).prev().find("input").val();
  $(id).find("input").val(prevText);
  $(id).prev().find("input").val(thisText);
  logMultipleInputFields("Moved up " + thisText + " and swapped with " + prevText);
}

function moveDownFormField(id) {
  var thisText = $(id).find("input").val();
  var nextText = $(id).next().find("input").val();
  $(id).find("input").val(nextText);
  $(id).next().find("input").val(thisText);
  logMultipleInputFields("Moved down " + thisText + " and swapped with " + nextText);
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
  var MULTIPLE_INPUT_FIELD_NAMES_LENGTH = MULTIPLE_INPUT_FIELD_NAMES.length;
  for(var i = 0; i < MULTIPLE_INPUT_FIELD_NAMES_LENGTH; i++){
    formatMultipleInputFields(MULTIPLE_INPUT_FIELD_NAMES[i]);
  }
}

function logMultipleInputFields(str) {
  if (typeof console != "undefined" && debugMultipleInputFields) {
    console.log(str);
  }
}