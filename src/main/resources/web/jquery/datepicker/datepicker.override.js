/*
 * Specific behavior of datepicker for event listing
 * TODO: look at interchange between format '2010-4-2' and '2010-04-02'
 */

function eventListingCalendar(allowedDates, activeDate, clickableDayTitle, notClickableDayTitle, language) {

  var today = new Date();
  if (!activeDate) {
    activeDate = [ today.getFullYear(), today.getMonth() + 1, today.getDate() ].join('-');
  }
  var activeDateForInit = makeActiveDateForInit(activeDate);

  var init = true;

  // i18n (default english)
  if (language == 'no') {
    $.datepicker.setDefaults($.datepicker.regional['no']);
  } else if (language == 'nn') {
    $.datepicker.setDefaults($.datepicker.regional['nn']);
  }

  $("#datepicker").datepicker( {
    dateFormat :'yy-mm-dd',
    onSelect : function(dateText, inst) {
      location.href = location.href.split('?')[0] + "?date=" + dateText;
    },
    firstDay :1,
    defaultDate :activeDateForInit,
    beforeShow : function(input, inst) {

    },
    beforeShowDay : function(day) {
      var date_str = [ day.getFullYear(), day.getMonth() + 1, day.getDate() ].join('-');

      // Add classes and tooltip for dates with and without events
    if ($.inArray(date_str, allowedDates) != -1) {
      if (removeZeroesBeforeDayAndMonth(activeDate) == date_str) {
        return [ true, 'state-active', clickableDayTitle ];
      } else {
        return [ true, '', clickableDayTitle ];
      }
    } else {
      return [ false, '', notClickableDayTitle ];
    }
  },
  onChangeMonthYear : function(year, month, inst) {
    if (month <= 9) {
      month = "0" + month;
    }

    // If not init (when prev / next month click), refresh page with year and
    // month
    if (!init) {
      location.href = location.href.split('?')[0] + "?date=" + year + '-' + month;
    } else {
      init = false;
    }
  }
  });
}

function removeZeroesBeforeDayAndMonth(date) {
  return date.replace(/-0/g, "-");
}

// For init of calender / datepicker()
function makeActiveDateForInit(activeDate) {
  var dateArray = activeDate.split('-');
  if (dateArray.length == 3) {
    return new Date(dateArray[0], dateArray[1] - 1, dateArray[2]);
  } else if (dateArray.length == 2) {
    return new Date(dateArray[0], dateArray[1] - 1);
  } else if (dateArray.length == 1) {
    return new Date(dateArray[0]);
  } else {
    return new Date();
  }
}