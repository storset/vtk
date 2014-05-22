module("Schedule.js");
test("Padding", function () {
  var utils = new scheduleUtils();
  equal(utils.addPadding(0), "00", "Add '0' to '0'");
  equal(utils.addPadding(9), "09", "Add '0' to '9'");
  equal(utils.addPadding(10), "10", "Don't add anything to '10'");
});