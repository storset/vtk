// JavaScript Document

var init = true;

$(document).ready(function() {
  //init
  showHide();
	
  init = false;
  
  //bind() click events
  $("#resource\\.recursive-listing\\.unspecified").bind("click", showHide);
  $("#resource\\.recursive-listing\\.false").bind("click", showHide);
  $("#resource\\.display-type\\.unspecified").bind("click", showHide);
  $("#resource\\.display-type\\.calendar").bind("click", showHide);
  

});

function showHide() {
  if($("#resource\\.recursive-listing\\.false:checked").val() == 'false'){
	$("#vrtx-resource\\.aggregation").slideUp(100);
	$("#vrtx-resource\\.recursiveAggregation").slideUp(100);
  } else {
	$("#vrtx-resource\\.aggregation").slideDown(100);
	$("#vrtx-resource\\.recursiveAggregation").slideDown(100);
  }
  if($("#resource\\.display-type\\.calendar:checked").val() != 'calendar') {
	$("#vrtx-resource\\.event-type-title").slideUp(100);
  } else {
	$("#vrtx-resource\\.event-type-title").slideDown(100);
  }
}
