/*
 * Autocomplete
 */

function setAutoComplete(id, service) {
  var objId = '#' + id;
  var serviceUrl = '/vrtx/__vrtx/app-resources/autocomplete?vrtx=admin&action=autocomplete&service=' + service;
  $(objId).autocomplete(serviceUrl, {
    multiple :true,
    selectFirst :true
  });

}