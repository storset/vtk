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
    },
    highlight : function(value, term) {
      var splitValue = value.split("(");
      var valueArray = splitValue[0].split(" ");
      var termArray = term.split(" ");
      var returnValue = "";
      for (v in valueArray) {
        var val = valueArray[v];
        for (t in termArray) {
          val = val.replace(new RegExp("^(?![^&;]+;)(?!<[^<>]*)("
              + termArray[t].replace(/([\^\$\(\)\[\]\{\}\*\.\+\?\|\\])/gi, "\\$1") + ")(?![^<>]*>)(?![^&;]+;)", "gi"),
              "<strong>$1</strong>");
        }
        returnValue = (returnValue == "") ? val : (returnValue + " " + val);
      }
      if (splitValue.length > 1) {
        return returnValue + " (" + splitValue[1];
      }
      return returnValue;
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