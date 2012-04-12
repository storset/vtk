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
  var trues = $('#' + name + '-true');
  for(var i = 0, truesLen = trues.length; i < truesLength; i++) {
    if (this.checked) {
      for (var k = 0, parametersLength = parameters.length; k < parametersLength; k++) {
        $('div.' + parameters[k]).hide("fast");
      }
    }
  }
  var falses = $('#' + name + '-false');
  for(i = 0, falsesLen = falses.length; i < falsesLength; i++) {
    if (this.checked) {
      for (k = 0, parametersLength = parameters.length; k < parametersLength; k++) {
        $('div.' + parameters[k]).show("fast");
      }
    }
  }
}