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
  var startDateElm = $("#start-date-date");
  var startDateHhElm = $("#start-date-hours");
  var startDateMmElm = $("#start-date-minutes");
  var endDateElm = $("#end-date-date");
  var endDateHhElm = $("#end-date-hours");
  var endDateMmElm = $("#end-date-minutes");
  
  if (!startDateElm.length || !endDateElm.length) {
    return;
  }
  var startDate = startDateElm.datepicker('getDate');
  if (startDate != null) {
    setDefaultEndDate(startDateElm, endDateElm);
  }
  
  $("#editor").on("change", "#start-date-date, #end-date-date", function () {
    setDefaultEndDate(startDateElm, endDateElm);
    preventInverseTimeline(startDateElm, startDateHhElm, startDateMmElm, endDateElm, endDateHhElm, endDateMmElm);
  });

  $("#editor").on("change", "#start-date-hours, #start-date-minutes", function () {
    verifyTime(startDateHhElm, startDateMmElm);
    preventInverseTimeline(startDateElm, startDateHhElm, startDateMmElm, endDateElm, endDateHhElm, endDateMmElm);
  });
  $("#editor").on("change", "#end-date-hours, #end-date-minutes", function () {
    verifyTime(endDateHhElm, endDateMmElm);
    preventInverseTimeline(startDateElm, startDateHhElm, startDateMmElm, endDateElm, endDateHhElm, endDateMmElm);
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

  dateField = "<div class='vrtx-textfield vrtx-date'><input type='text' size='12' id='" + name + "-date' name='" + name + "-date' value='" + date[0] + "' /></div>";
  hoursField = "<div class='vrtx-textfield vrtx-hours'><input type='text' size='2' id='" + name + "-hours' name='" + name + "-hours' value='" + hours + "' /></div>";
  minutesField = "<div class='vrtx-textfield vrtx-minutes'><input type='text' size='2' id='" + name + "-minutes' name='" + name + "-minutes' value='" + minutes + "' /></div>";
  a.parent().hide();
  a.parent().after(dateField + hoursField + "<span class='vrtx-time-seperator'>:</span>" + minutesField);
  $("#" + fieldName + "-date").datepicker({
    dateFormat: 'yy-mm-dd'
  });
}

function setDefaultEndDate(startDateElm, endDateElm) {
  var endDate = endDateElm.val();
  var startDate = startDateElm.datepicker('getDate');
  if (endDate == "") {
    endDateElm.datepicker('option', 'defaultDate', startDate);
  }
}

function verifyTime(hh, mm) {
  var hhVal = hh.val();
  var mmVal = mm.val();
  if(hhVal.length || mmVal.length) { // Don't trust Date/Systemtime blank filling (and we want it more robust)
    var newHhVal = parseInt(hhVal); // Correct hours
    if(isNaN(newHhVal)) {
      newHhVal = "00";
    } else {
      newHhVal = (newHhVal > 23) ? "00" : newHhVal;
      newHhVal = ((newHhVal < 10 && !newHhVal.length) ? "0" : "") + newHhVal;
    }
    
    var newMmVal = parseInt(mmVal); // Correct minutes
    if(isNaN(newMmVal)) {
      newMmVal = "00";
    } else {
      newMmVal = (newMmVal > 59) ? "00" : newMmVal;
      newMmVal = ((newMmVal < 10 && !newMmVal.length) ? "0" : "") + newMmVal;
    }
    
    if((newHhVal == "00" || newHhVal == "0") && (newMmVal == "00" || newMmVal == "0")) { // If all zeroes => remove time
      hh.val("");
      mm.val("");
    } else {
      if(hhVal != newHhVal) hh.val(newHhVal);
      if(mmVal != newMmVal) mm.val(newMmVal);
    }
  }
}

function preventInverseTimeline(startDateElm, startDateHhElm, startDateMmElm, endDateElm, endDateHhElm, endDateMmElm) {
  var startDateVal = startDateElm.val();
  var startDateHhVal = startDateHhElm.val();
  var startDateMmVal = startDateMmElm.val();
  var endDateVal = endDateElm.val();
  var endDateHhVal = endDateHhElm.val();
  var endDateMmVal = endDateMmElm.val();
  
  var start = new Date(startDateVal);
  var end = new Date(endDateVal);
  
  if(end <= start) {
    endDateElm.val(startDateVal);
    if(endDateHhVal <= startDateHhVal) {
      endDateHhElm.val(startDateHhVal);
      if(endDateMmVal < startDateMmVal) {
        endDateMmElm.val(startDateMmVal);
      }
    }
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
      dateFields[i].value = date[0].value;
      if (hours[0] && hours[0].value.toString().length) {
        dateFields[i].value += " " + hours[0].value;
        if (minutes[0].value && minutes[0].value.toString().length) {
         dateFields[i].value += ":" + minutes[0].value;
        }
      }
    }
  }
}

/* ^ Datepicker for new documenttypes */