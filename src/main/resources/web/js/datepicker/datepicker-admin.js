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
  var date = new Array("");
  var fieldName = name.replace(/\./g, '\\.');

  var a = $("#" + fieldName);

  if (a.length) {
    hours = extractHoursFromDate(a[0].value);
    minutes = extractMinutesFromDate(a[0].value)
    date = new String(a[0].value).split(" ");
  }

  dateField = "<div class='vrtx-textfield vrtx-date'><input type='text' size='12' id='" + name + "-date' name='" + name + "-date' value='" + date[0] + "' class='date' /></div>";
  hoursField = "<div class='vrtx-textfield vrtx-hours'><input type='text' size='2' id='" + name + "-hours' name='" + name + "-hours' value='" + hours + "' class='hours' /></div>";
  minutesField = "<div class='vrtx-textfield vrtx-minutes'><input type='text' size='2' id='" + name + "-minutes' name='" + name + "-minutes' value='" + minutes + "' class='minutes' /></div>";
  a.parent().hide();
  a.parent().after(dateField + hoursField + "<span class='vrtx-time-seperator'>:</span>" + minutesField);
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

    var hours = $("#" + fieldName + "-hours");
    var minutes = $("#" + fieldName + "-minutes");
    var date = $("#" + fieldName + "-date");

    dateFields[i].value = "";

    if (date[0] && date[0].value.toString().length) {
      dateFields[i].value = date[0].value
      if (hours[0] && hours[0].value.toString().length) {
        dateFields[i].value += " " + hours[0].value
        if (minutes[0].value && minutes[0].value.toString().length) {
         dateFields[i].value += ":" + minutes[0].value;
        }
      }
    }

    // Hack fix for editor.. .must be removed!!!
    if (typeof UNSAVED_CHANGES_CONFIRMATION !== "undefined") {
       $("#" + fieldName + "-hours").parent().remove();
       $("#" + fieldName + "-minutes").parent().remove();
       $("#" + fieldName + "-date").parent().remove();
       $(".vrtx-time-seperator").remove();
    }
  }
}

/* ^ Datepicker for new documenttypes */