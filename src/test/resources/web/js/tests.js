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
  equal(utils.getDateFormatted(datetime.start, datetime.end), "18.08.14", "Date");
  equal(utils.getTimeFormatted(datetime.start, datetime.end), "12:15&ndash;14:00",  "Time");
  equal(utils.getDayFormatted(datetime.start, datetime.end, {
    "d0": "Søndag",
    "d1": "Mandag",
    "d2": "Tirsdag",
    "d3": "Onsdag",
    "d4": "Torsdag",
    "d5": "Fredag",
    "d6": "Lørdag"
  }), "Mandag",  "Day +0200 - 12:15=>14:00");
  var datetime2 = utils.getDateTime("2014-11-03T23:15:00.000+01:00", "2014-11-03T23:59:00.000+01:00");
  equal(utils.getDayFormatted(datetime2.start, datetime2.end, {
    "d0": "Søndag",
    "d1": "Mandag",
    "d2": "Tirsdag",
    "d3": "Onsdag",
    "d4": "Torsdag",
    "d5": "Fredag",
    "d6": "Lørdag"
  }), "Mandag",  "Day +0100 - 23:15=>23:59");
  var datetime3 = utils.getDateTime("2014-11-03T00:15:00.000+01:00", "2014-11-03T01:59:00.000+01:00");
  equal(utils.getDayFormatted(datetime3.start, datetime3.end, {
    "d0": "Søndag",
    "d1": "Mandag",
    "d2": "Tirsdag",
    "d3": "Onsdag",
    "d4": "Torsdag",
    "d5": "Fredag",
    "d6": "Lørdag"
  }), "Mandag",  "Day +0100 - 00:15=>01:59");
});
