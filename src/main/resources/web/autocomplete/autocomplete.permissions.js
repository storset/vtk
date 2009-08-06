/*
 * Specific behavior of autocomplete for permissions
 */

function permissionsAutocomplete(id, service, params) {
  var p = {
    formatItem : function(data, i, n, value) {
      return value.split(';')[0] + ' (' + value.split(';')[1] + ')';
    },
    formatResult : function(data, value) {
      return value.split(';')[0];
    }
  };
  if (params) {
    $.extend(p, params);
  }
  setAutoComplete(id, service, p);
}

function splitAutocompleteSuggestion(id) {
  var fieldId = '#' + id;
  var hiddenValueId = 'ac_' + id;
  $(fieldId).result( function(event, data, formatted) {
    if (formatted) {
      var existingValue = document.getElementById(hiddenValueId).value;
      if (existingValue != '') {
        document.getElementById(hiddenValueId).value = existingValue + ', ' + formatted;
      } else {
        document.getElementById(hiddenValueId).value = formatted;
      }
    }
  });
}