/*
 * Specific behavior of autocomplete for permissions
 */

function splitAutocompleteUserNames() {
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
}