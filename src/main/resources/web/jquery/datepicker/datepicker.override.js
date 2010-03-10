/*
 * Specific behavior of datepicker for event listing
 */

function eventListingCalendar(service, clickableDayTitle, notClickableDayTitle) {

  var allowedDates = new Array();
  
  var today = new Date();
  var activeDate = findActiveDate(today);
  
  $("#datepicker").datepicker(
  {
    dateFormat :'yy-mm-dd',
    onSelect : function(dateText, inst) {
      location.href = location.href.split('?')[0] + "?date=" + dateText;
    },
    monthNames : [ 'Januar', 'Februar', 'Mars', 'April', 'Mai', 'Juni', 'Juli', 'August', 'September', 'Oktober',
        'November', 'Desember' ],
    dayNamesMin : [ 'Sø', 'Ma', 'Ti', 'On', 'To', 'Fr', 'Lø' ],
    firstDay :1,
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

function findActiveDate(today) {
  var activeDate = [ today.getFullYear(), today.getMonth() + 1, today.getDate() ].join('-');
  if (location.href.indexOf('?date=') != -1) {
    var parameterDate = location.href.split('=');
    activeDate = parameterDate[(parameterDate.length - 1)].replace(/-0/g, "-");
  }
  return activeDate;
}