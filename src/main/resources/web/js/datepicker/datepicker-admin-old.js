/*
 * Initialize Datepicker for old documenttypes and folders
 *
 */

function initDatePicker(language) {

  // i18n (default english)
  if (language == 'no') {
    $.datepicker.setDefaults($.datepicker.regional['no']);
  } else if (language == 'nn') {
    $.datepicker.setDefaults($.datepicker.regional['nn']);
  }

  $(".date").datepicker({
    dateFormat: 'yy-mm-dd'
  });

  if (!$("#resource\\.start-date").length || !$("#resource\\.end-date").length) {
    return;
  }

  var startDate = $("#resource\\.start-date").datepicker('getDate');
  if (startDate != null) {
    setDefaultEndDate();
  }

  $("#resource\\.start-date").change(function () {
    setDefaultEndDate();
  });
}

function setDefaultEndDate() {
  var endDate = $("#resource\\.end-date").val();
  var startDate = $("#resource\\.start-date").datepicker('getDate');
  if (endDate == "") {
    $("#resource\\.end-date").datepicker('option', 'defaultDate', startDate);
  }
}

/* ^ Initialize Datepicker for old documenttypes and folders */