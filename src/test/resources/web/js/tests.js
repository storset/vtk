var utils = null;
module("Schedule.js", {
  setup: function() {
    utils = new scheduleUtils();
  }, teardown: function() {
    utils = null;
  }
});

test("DateTime parsing", function () {
  var datetime = utils.getDateTime("2014-08-18T12:15:00.000+02:00", "2014-08-18T14:00:00.000+02:00");
  equal(utils.getDateFormatted(datetime.startDateTime, datetime.endDateTime), "18.08.14", "Date");
  equal(utils.getTimeFormatted(datetime.startDateTime, datetime.endDateTime), "12:15&ndash;14:00",  "Time");
  equal(utils.getDayFormatted(datetime.startDateTime, datetime.endDateTime, {
    "d0": "Søndag",
    "d1": "Mandag",
    "d2": "Tirsdag",
    "d3": "Onsdag",
    "d4": "Torsdag",
    "d5": "Fredag",
    "d6": "Lørdag"
  }), "Mandag",  "Time");
});
