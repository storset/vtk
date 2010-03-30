/*
 * Specific behavior of datepicker for event listing
 * TODO: look at interchange between format '2010-4-2' and '2010-04-02'
 */

function eventListingCalendar(allowedDates, activeDate, clickableDayTitle, notClickableDayTitle, language) {

  var init = true;
  var activeDateForInit = makeActiveDateForInit(activeDate);

  // i18n (default english)
  if (language == 'no') {
    $.datepicker.setDefaults($.datepicker.regional['no']);
  } else if (language == 'nn') {
    $.datepicker.setDefaults($.datepicker.regional['nn']);
  }

  
  
  $("#datepicker").datepicker( {
    dateFormat : 'yy-mm-dd',
    onSelect : function(dateText, inst) {
      location.href = location.href.split('?')[0] + "?date=" + dateText;
    },
    firstDay : 1,
    showOtherMonths: true,
    defaultDate : activeDateForInit,
    beforeShowDay : function(day) {
      // Add classes and tooltip for dates with and without events
      var date_str = $.datepicker.formatDate("yy-m-d", new Date(day)).toString();
      if ($.inArray(date_str, allowedDates) != -1) {
        if ($.datepicker.formatDate("yy-m-d", new Date(activeDateForInit)).toString() == date_str) {
          return [ true, 'state-active', clickableDayTitle ];
        } else {
          return [ true, '', clickableDayTitle ];
        }
      } else {
          return [ false, '', notClickableDayTitle ];
      }
    },
    onChangeMonthYear : function(year, month, inst) {
      var date = $.datepicker.formatDate("yy-mm", new Date(year, month - 1)).toString();
      
      if (!init) {
        location.href = location.href.split('?')[0] + "?date=" + date;
      } else {
        init = false;
      }
      //wait.. and make month link
      setTimeout(function(){makeMonthLink(date)}, 100);
    }
  });
}

function makeMonthLink(date) {
  $(".ui-datepicker-month").html("<a href='" + location.href.split('?')[0] + "?date=" + date + "'>" 
  + $(".ui-datepicker-month").text() + ' ' + $(".ui-datepicker-year").remove().text() + "</a>");
}

// For init of calender / datepicker()
function makeActiveDateForInit(activeDate) {
  if(activeDate == "") { return new Date(); }
  var dateArray = activeDate.split('-');
  if (dateArray.length == 3) {
    return new Date(dateArray[0], dateArray[1] - 1, dateArray[2]);
  } else if (dateArray.length == 2) {
    return new Date(dateArray[0], dateArray[1] - 1);
  } else if (dateArray.length == 1) {
    return new Date(dateArray[0]);
  }
}