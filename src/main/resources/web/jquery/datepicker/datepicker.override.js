/*
 * Specific behavior of datepicker for event listing
 */

function eventListingCalendar(service, clickableDayTitle, notClickableDayTitle, language) {

  var allowedDates = new Array();
  
  var today = new Date();
  var activeDate = findActiveDate(today, true);
  
  //i18n - default english
  $.datepicker.setDefaults($.datepicker.regional['']);
  if(language == 'no') {
    $.datepicker.setDefaults($.datepicker.regional['no']);
  }
  
  $("#datepicker").datepicker({
    dateFormat : 'yy-mm-dd',
    onSelect : function(dateText, inst) {
      location.href = location.href.split('?')[0] + "?date=" + dateText;
    },
    firstDay : 1,
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
  
  //keep month onSelect event
  var date = new Date(findActiveDate(today, false));
  $("#datepicker").datepicker('setDate', date);
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