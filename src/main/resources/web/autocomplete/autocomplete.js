/*
 * Autocomplete
 */

function setAutoComplete(id, service, params) {
  // Default parameters, extended by 'params' if supplied
  var p = {
    minChars :2,
    multiple :true,
    selectFirst :true,
    max :20
  };
  if (params) {
    $.extend(p, params);
  }
  var objId = '#' + id;
  var serviceUrl = '/vrtx/__vrtx/app-resources/autocomplete?vrtx=admin&action=autocomplete&service=' + service;
  $(objId).autocomplete(serviceUrl, p);

}