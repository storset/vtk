/*
 * Multiple inputfields (new documenttypes)
 *
 */

var MULTIPLE_INPUT_FIELD_NAMES = [];
var COUNTER_FOR_MULTIPLE_INPUT_FIELD = [];
var LENGTH_FOR_MULTIPLE_INPUT_FIELD = [];

var debugMultipleInputFields = false;

function loadMultipleInputFields(name, addName, removeName, moveUpName, moveDownName, browseName) {
    var id = "#" + name;
    var inputField = $(id); // cache

    if (inputField.val() == null) { return; }

    var formFields = inputField.val().split(",");
    
    COUNTER_FOR_MULTIPLE_INPUT_FIELD[name] = 1; // 1-index
    LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] = formFields.length;
    MULTIPLE_INPUT_FIELD_NAMES.push(name);

    var size = inputField.attr("size");

    registerClicks(name);
    
    inputFieldParent = inputField.parent(); // another cache
    
    var isResourceRef = false;
    if(inputFieldParent.parent().attr("class").indexOf("vrtx-resource-ref-browse") != -1) {
      isResourceRef = true;	
    }
    
    if(inputFieldParent.next().hasClass("vrtx-button") && isResourceRef) {
      inputFieldParent.next().hide();
    }
    inputFieldParent.append("<div id='vrtx-" + name + "-add' class='vrtx-button'>"
		      + "<button onclick=\"addFormField('" + name + "',null, '"
		      + removeName + "','" + moveUpName + "','" + moveDownName + "','" + browseName + "','" + size + "'," + isResourceRef + "," + false + "); return false;\">"
		      + addName + "</button></div>").removeClass("vrtx-textfield");
		 
    inputField.hide();
    
    var addFormFieldFunc = addFormField;
    for (var i = 0; i < LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]; i++) {
       addFormFieldFunc(name, $.trim(formFields[i]), removeName, moveUpName, moveDownName, browseName, size, isResourceRef, true);
    }
    
    autocompleteUsernames(".vrtx-autocomplete-username");
}

function registerClicks(name) {
  var wrapper = $("." + name);  // cache

  wrapper.delegate(".remove", "click", function(){
	removeFormField(name, $(this));
  });
  wrapper.delegate(".moveup", "click", function(){
	moveUpFormField($(this));
  });
  wrapper.delegate(".movedown", "click", function(){
	moveDownFormField($(this));
  });
  wrapper.delegate(".browse-resource-ref", "click", function(){
	browseServer($(this).parent().parent().find('input').attr('id'), browseBase, browseBaseFolder, browseBasePath, 'File');
  });
}

function addFormField(name, value, removeName, moveUpName, moveDownName, browseName, size, isResourceRef, init) {
    if (value == null) { value = ""; }

    var idstr = "vrtx-" + name + "-";
    var i = COUNTER_FOR_MULTIPLE_INPUT_FIELD[name];
    var removeButton = "";
    var moveUpButton = "";
    var moveDownButton = "";
    var browseButton = "";

    if (removeName) {
        removeButton = "<div class='vrtx-button'><button class='remove' type='button' " + "id='" + idstr + "remove' >" + removeName + "</button></div>";
    }
    if (moveUpName && i > 1) {
    	moveUpButton = "<div class='vrtx-button'><button class='moveup' type='button' " + "id='" + idstr + "moveup' >"
    	+ "&uarr; " + moveUpName + "</button></div>";
    }
    if (moveDownName && i < LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]) {
    	moveDownButton = "<div class='vrtx-button'><button class='movedown' type='button' " + "id='" + idstr + "movedown' >"
    	+ "&darr; " + moveDownName + "</button></div>";
    }
    if(browseButton) {
        browseButton = "<div class='vrtx-button'><button type='button' class='browse-resource-ref'>" + browseName + "</button></div>";
    }
    
    if(!isResourceRef) {
      var html = "<div class='vrtx-multipleinputfield' id='" + idstr + "row-" + i + "'>"
               + "<div class='vrtx-textfield'><input value='" + value + "' type='text' size='" + size + "' id='" + idstr + i + "' /></div>"
               + removeButton + moveUpButton + moveDownButton + "</div>";
    } else {
      var html = "<div class='vrtx-multipleinputfield' id='" + idstr + "row-" + i + "'>"
               + "<div class='vrtx-textfield'><input value='" + value + "' type='text' size='" + size + "' id='" + idstr + i + "' /></div>"
               + browseButton + removeButton + moveUpButton + moveDownButton + "</div>";
    }
    
    $("#vrtx-" + name + "-add").before(html);
    
    if(!init) {
      if(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] > 0) {

        var fields = $("." + name + " div.vrtx-multipleinputfield"); // cache

        if(fields.eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).not("has:button.movedown")) {
          var theId = fields.eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).attr("id");
          moveDownButton = "<div class='vrtx-button'><button class='movedown' type='button' " + "id='" + idstr + "movedown' >"
                         + "&darr; " + moveDownName + "</button></div>";
          fields.eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).append(moveDownButton);
          //logMultipleInputFields("Added before-last movedown");
        }

      }
      LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]++;
      autocompleteUsername(".vrtx-autocomplete-username", idstr + i);
    }

    COUNTER_FOR_MULTIPLE_INPUT_FIELD[name]++;   
}

function removeFormField(name, that) {
    $(that).parent().parent().remove();
    //logMultipleInputFields("Fjerner felt");

    LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]--;
    COUNTER_FOR_MULTIPLE_INPUT_FIELD[name]--;
    //logMultipleInputFields("Number of inputfields: " + LENGTH_FOR_MULTIPLE_INPUT_FIELD[name]);
    //logMultipleInputFields("Next number for inputfield: " + COUNTER_FOR_MULTIPLE_INPUT_FIELD[name]);

    var fields = "." + name + " div.vrtx-multipleinputfield";

	if($(fields).eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).has("button.movedown")) {
	  $(fields).eq(LENGTH_FOR_MULTIPLE_INPUT_FIELD[name] - 1).find("button.movedown").parent().remove();
	  //logMultipleInputFields("Removed last movedown");
	}

	if($(fields).eq(0).has("button.moveup")) {
	  $(fields).eq(0).find("button.moveup").parent().remove();
	  //logMultipleInputFields("Removed first moveup");
	}
}

function moveUpFormField(that) {
  var thisInput = $(that).parent().parent().find("input");
  var prevInput = $(that).parent().parent().prev().find("input");
  var thisText = thisInput.val();
  var prevText = prevInput.val();
  $(thisInput).val(prevText);
  $(prevInput).val(thisText);
  //logMultipleInputFields("Moved up " + thisText + " and swapped with " + prevText);
}

function moveDownFormField(that) {
  var thisInput = $(that).parent().parent().find("input");
  var nextInput = $(that).parent().parent().next().find("input");
  var thisText = $(thisInput).val();
  var nextText = $(nextInput).val();
  $(thisInput).val(nextText);
  $(nextInput).val(thisText);
  //logMultipleInputFields("Moved down " + thisText + " and swapped with " + nextText);
}

function formatMultipleInputFields(name) {
    if ($( "#" + name ).val() == null) return;

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
  var formatMultipleInputFieldsFunc = formatMultipleInputFields;
  for(var i = 0; i < MULTIPLE_INPUT_FIELD_NAMES_LENGTH; i++){
    formatMultipleInputFields(MULTIPLE_INPUT_FIELD_NAMES[i]);
  }
}

function logMultipleInputFields(str) {
  if (typeof console !== "undefined" && console.log && debugMultipleInputFields) {
    console.log(str);
  }
}

/* ^ Multiple inputfields (new documenttypes) */