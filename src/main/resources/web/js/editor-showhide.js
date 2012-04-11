//
// TODO: combine with other show-hide functionality and/or move into admin-enhancements.js
//
// Add toggle functionality to properties


function setShowHide(name, parameters) {
  toggle(name, parameters);
  $("#editor").on("click", '[name=' + name + ']', function () {
    toggle(name, parameters);
  });
}

function toggle(name, parameters) {
  $('#' + name + '-true').each(function () {
    if (this.checked) {
      for (var i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
        $('div.' + parameters[i]).hide("fast");
      }
    }
  });
  $('#' + name + '-false').each(function () {
    if (this.checked) {
      for (var i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
        $('div.' + parameters[i]).show("fast");
      }
    }
  });
}