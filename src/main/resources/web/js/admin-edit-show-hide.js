// JavaScript Document

$(document).ready(function() {
  //init
  showHide();
	
  //bind() click events
  $("#resource\\.recursive-listing\\.unspecified").bind("click", showHide);
  $("#resource\\.recursive-listing\\.false").bind("click", showHide);
  $("#resource\\.display-type\\.unspecified").bind("click", showHide);
  $("#resource\\.display-type\\.calendar").bind("click", showHide);
  

});

function showHide() {
  if($("#resource\\.recursive-listing\\.false:checked").val() == 'false'){
	$("#vrtx-resource\\.aggregation").hide();
	$("#vrtx-resource\\.recursiveAggregation").hide();
  } else {
    $("#vrtx-resource\\.aggregation").show();
  	$("#vrtx-resource\\.recursiveAggregation").show();
  }
  if($("#resource\\.display-type\\.calendar:checked").val() != 'calendar') {
	$("#vrtx-resource\\.event-type-title").hide();
  } else {
	$("#vrtx-resource\\.event-type-title").show();
  }
}
