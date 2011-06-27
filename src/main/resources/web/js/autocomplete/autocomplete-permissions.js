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
      var desc = splitValue[1];
      var valueArray = splitValue[0].split(" ");
      var termArray = term.split(" ");
      var returnValue = "";
      for (v in valueArray) {
        var val = valueArray[v];
        for (t in termArray) {
          var regex = new RegExp("^(?![^&;]+;)(?!<[^<>]*)("
              + termArray[t].replace(/([\^\$\(\)\[\]\{\}\*\.\+\?\|\\])/gi, "\\$1") + ")(?![^<>]*>)(?![^&;]+;)", "gi");
          val = val.replace(regex, "<strong>$1</strong>");
          if (id.indexOf("userNames") != -1) {
            desc = desc.replace(regex, "<strong>$1</strong>");
          }
        }
        returnValue = (returnValue == "") ? val : (returnValue + " " + val);
      }
      return returnValue + " (" + desc;
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