/*
 * Specific behavior of datepicker for event listing
 */

function eventListingCalendar(service, clickableDayTitle, notClickableDayTitle) {

  var activeDate = findActiveDate();
  
  var allowedDates = new Array();
  var date = new Date();
	  
  allowedDates = queryAllowedDates (service, date.getFullYear(), date.getMonth + 1);
  
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
    	
    }
  });
}

function queryAllowedDates (service, year, month) {
  var allowedDates = new Array();
  $.ajax({
    type: 'GET',
    url: service + month,
    dataType: 'text',
    async: false,
    success: function(response) {
	  allowedDates = response.split(',');
    }
  });
  return allowedDates;
}

function findActiveDate() {
  var activeDate = "";
  if (location.href.indexOf('?date=') != -1) {
    var dated = location.href.split('=');
    activeDate = dated[(dated.length - 1)].replace(/-0/g, "-");
  } else {
    var cDate = new Date();
    var activeDate = [ cDate.getFullYear(), cDate.getMonth() + 1, cDate.getDate() ].join('-');
  }
  return activeDate;
}