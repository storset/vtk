//
// TODO: combine with other show-hide functionality and/or move into admin-enhancements.js
//

// Add toggle functionality to properties

function setShowHide(name, parameters) {
  toggle(name, parameters);
  var objId = '[name=' + name + ']';
  $(objId).click( function() {
    toggle(name, parameters);
  });
}

function toggle(name, parameters) {
  $('#' + name + '-true').each( function() {
    if (this.checked) {
      var parametersLength = parameters.length;
      for (i = 0; i < parametersLength; i++) {
        $('div.' + parameters[i]).hide("fast");
      }
    }
  });
  $('#' + name + '-false').each( function() {
    if (this.checked) {
      var parametersLength = parameters.length;
      for (i = 0; i < parametersLength; i++) {
        $('div.' + parameters[i]).show("fast");
      }
    }
  });
}
