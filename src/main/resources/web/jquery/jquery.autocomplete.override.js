/*
 * Override default behavior of autocomplete to satisfy our needs
 */

$(document).ready( function() {
  $("#userNames").result(function(event, data, formatted) {
    if (formatted) {
      var existingValue = document.getElementById("ac_${elementId}").value;
      if (existingValue != "") {
        document.getElementById("ac_${elementId}").value = existingValue + ", " + formatted;
      } else {
        document.getElementById("ac_${elementId}").value = formatted;
      }
    }
  });
});