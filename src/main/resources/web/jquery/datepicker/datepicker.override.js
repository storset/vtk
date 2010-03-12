/*
 * Specific behavior of datepicker for event listing
 * TODO: look at interchange between format '2010-4-2' and '2010-04-02'
 */

function eventListingCalendar(service, clickableDayTitle, notClickableDayTitle, language) {

  var allowedDates = new Array();
  
  var today = new Date();
  var activeDate = findActiveDate(today, true);
  var dateArray = findActiveDate(today, false).split('-');;
  var activeDateWithZeroesBeforeMonthAndDay = new Date(dateArray[0], dateArray[1]-1, dateArray[2]);
  
  // i18n (default english)
  if(language == 'no') {
    $.datepicker.setDefaults($.datepicker.regional['no']);
  } else if(language == 'nn') {
	$.datepicker.setDefaults($.datepicker.regional['nn']);
  }
  
  $("#datepicker").datepicker({
    dateFormat : 'yy-mm-dd',
    onSelect : function(dateText, inst) {
      location.href = location.href.split('?')[0] + "?date=" + dateText;
    },
    firstDay : 1,
    defaultDate : activeDateWithZeroesBeforeMonthAndDay,
    beforeShow : function(input, inst) {
    	
    },
    beforeShowDay : function(day) {
      var date_str = [ day.getFullYear(), day.getMonth() + 1, day.getDate()].join('-');
      if ($.inArray(date_str, allowedDates) != -1) {
        if (activeDate == date_str) {
          return [ true, 'state-active', clickableDayTitle ];
        } else {
          return [ true, '', clickableDayTitle ];
        }
      } else {
        return [ false, '', notClickableDayTitle ];
      }
    },
    onChangeMonthYear : function(year, month, inst) {
      if(month <= 9) {
        month = "0" + month;
      }
      allowedDates = queryAllowedDates (service, year, month);
    }
  });
}

function queryAllowedDates (service, year, month) {
  var allowedDates = new Array();
  $.ajax({
    type: 'GET',
    url: service + "&date=" + year + '-' + month,
    dataType: 'text',
    async: false,
    success: function(response) {
	  allowedDates = response.split(',');
    }
  });
  return allowedDates;
}

function findActiveDate(today, removeZeroesBeforeDayAndMonth) {
  var activeDate = [ today.getFullYear(), today.getMonth() + 1, today.getDate() ].join('-');
  if (location.href.indexOf('?date=') != -1) {
    var parameterDate = location.href.split('=');
    activeDate = parameterDate[(parameterDate.length - 1)];
    if(removeZeroesBeforeDayAndMonth) {
      activeDate = activeDate.replace(/-0/g, "-");
    }
  }
  return activeDate;
}