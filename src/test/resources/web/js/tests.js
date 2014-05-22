var utils = null;
module("Schedule.js", {
  setup: function() {
    utils = new scheduleUtils();
  }, teardown: function() {
    utils = null;
  }
});
test("Padding", function () {
  equal(utils.addPadding(0), "00", "Add '0' to '0'");
  equal(utils.addPadding(9), "09", "Add '0' to '9'");
  equal(utils.addPadding(10), "10", "Don't add anything to '10'");
});

test("DateTime parsing", function () {
  var datetime = utils.getDateTime("2014-08-18T12:15:00.000+02:00", "2014-08-18T14:00:00.000+02:00");

  equal(datetime.startDateTimeUTC, "Mon Aug 18 2014 12:15:00 GMT+0200 (CET)", "Start server UTC");
  equal(datetime.endDateTimeUTC, "Mon Aug 18 2014 14:00:00 GMT+0200 (CET)", "End server UTC");
  equal(datetime.startDateTime, "Mon Aug 18 2014 14:15:00 GMT+0200 (CET)", "Start server with timezone");
  equal(datetime.endDateTime, "Mon Aug 18 2014 16:00:00 GMT+0200 (CET)", "End server with timezone");
  equal(utils.getDateFormatted(datetime.startDateTime, datetime.endDateTime), "18.08.14", "Start server date with timezone");
  equal(utils.getTimeFormatted(datetime.startDateTime, datetime.endDateTime), "14:15&ndash;16:00",  "Start => end server time with timezone");
});