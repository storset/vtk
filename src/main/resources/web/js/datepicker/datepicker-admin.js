/*
 * Datepicker for new documenttypes
 *
 */

function initDatePicker(language) {

  // i18n (default english)
  if (language == 'no') {
    $.datepicker.setDefaults($.datepicker.regional['no']);
  } else if (language == 'nn') {
    $.datepicker.setDefaults($.datepicker.regional['nn']);
  }
  
  var dateFields = $(".date");
  for(var i = 0, len = dateFields.length; i < len; i++) {
    displayDateAsMultipleInputFields(dateFields[i].name);
  }

  // TODO !spageti && !run twice
  if (typeof UNSAVED_CHANGES_CONFIRMATION !== "undefined") {
    storeInitPropValues();
  }

  // Specific for start and end date
  if (!$("#start-date-date").length || !$("#end-date-date").length) {
    return;
  }
  var startDate = $("#start-date-date").datepicker('getDate');
  if (startDate != null) {
    setDefaultEndDate();
  }
  $("#start-date-date").change(function () {
    setDefaultEndDate();
  });
}

function displayDateAsMultipleInputFields(name) {
  var hours = "";
  var minutes = "";
  var date = [];
  var fieldName = name.replace(/\./g, '\\.');

  var elem = $("#" + fieldName);

  if (elem.length) {
    hours = extractHoursFromDate(elem[0].value);
    minutes = extractMinutesFromDate(elem[0].value)
    date = new String(elem[0].value).split(" ");
  }

  var dateField = "<div class='vrtx-textfield vrtx-date'><input type='text' size='12' id='" + name + "-date' value='" + date[0] + "' /></div>";
  var hoursField = "<div class='vrtx-textfield vrtx-hours'><input type='text' size='2' id='" + name + "-hours' value='" + hours + "' /></div>";
  var minutesField = "<div class='vrtx-textfield vrtx-minutes'><input type='text' size='2' id='" + name + "-minutes' value='" + minutes + "' /></div>";
  elem.parent().hide();
  elem.parent().after(dateField + hoursField + "<span class='vrtx-time-seperator'>:</span>" + minutesField);
  $("#" + fieldName + "-date").datepicker({
    dateFormat: 'yy-mm-dd'
  });
}

function setDefaultEndDate() {
  var endDate = $("#end-date-date").val();
  var startDate = $("#start-date-date").datepicker('getDate');
  if (endDate == "") {
    $("#end-date-date").datepicker('option', 'defaultDate', startDate);
  }
}

function extractHoursFromDate(datetime) {
  var a = new String(datetime);
  var b = a.split(" ");
  if (b.length > 1) {
    var c = b[1].split(":");
    if (c != null) {
      return c[0];
    }
  }
  return "";
}

function extractMinutesFromDate(datetime) {
  var a = new String(datetime);
  var b = a.split(" ");
  if (b.length > 1) {
    var c = b[1].split(":");
    if (c.length > 0) {
      var min = c[1];
      if (min != null) {
        return min;
      }
      // Hour has been specified, but no minutes.
      // Return "00" to properly display time.
      return "00";
    }
  }
  return "";
}

function saveDateAndTimeFields() {
  var dateFields = $(".date");
  for(var i = 0, len = dateFields.length; i < len; i++) {
    var dateFieldName = dateFields[i].name;
    if (!dateFieldName) return;

    var fieldName = dateFieldName.replace(/\./g, '\\.');

    var hours = $("#" + fieldName + "-hours")[0];
    var minutes = $("#" + fieldName + "-minutes")[0];
    var date = $("#" + fieldName + "-date")[0];

    var savedVal = "";
    
    if (date && date.value.toString().length) {
      savedVal = date.value;
      if (hours && hours.value.toString().length) {
        savedVal += " " + hours.value;
        if (minutes.value && minutes.value.toString().length) {
         savedVal += ":" + minutes.value;
        }
      }
    }
    
    dateFields[i].value = savedVal;
    
  }
}

/* ^ Datepicker for new documenttypes */