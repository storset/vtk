/*
 * Autocomplete
 */

/**
 * @id Unique id of the field to hook up autocomplete to
 * @service Name of service to invoke for autocomplete suggestions
 * @params List of parameters to override/extend default behavior. Must be
 *         supplied as a name:value array, e.g. {minChars:1, selectFirst:false}
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