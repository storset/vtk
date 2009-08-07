// Add toggle functionality to properties

function setShowHide(name, parameters) {
  toggle(name, parameters);
  var objId = '[name=' + name + ']';
  $(objId).click( function() {
    setShowHide(name, parameters);
  });
}

function toggle(name, parameters) {
  $('#' + name + '-true').each( function() {
    if (this.checked) {
      for (i = 0; i < parameters.length; i++) {
        $('div.' + parameters[i]).hide();
      }
    }
  });
  $('#' + name + '-false').each( function() {
    if (this.checked) {
      for (i = 0; i < parameters.length; i++) {
        $('div.' + parameters[i]).show();
      }
    }
  });
}