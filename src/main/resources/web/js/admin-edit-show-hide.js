// JavaScript Document

//init
var init = true;
  
$(document).ready(function() {
  
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
	showHideProp("#vrtx-resource\\.aggregation", init, false);
	showHideProp("#vrtx-resource\\.recursiveAggregation", init, false);
  } else {
	showHideProp("#vrtx-resource\\.aggregation", init, true);
	showHideProp("#vrtx-resource\\.recursiveAggregation", init, true);
  }
  if($("#resource\\.display-type\\.calendar:checked").val() != 'calendar') {
	showHideProp("#vrtx-resource\\.event-type-title", init, false);
  } else {
	showHideProp("#vrtx-resource\\.event-type-title", init, true);
  }
}

function showHideProp(id, init, show) {
	if(init) {
	  if(show) {
		$(id).show();  
	  } else {
		$(id).hide();  
	  }
	} else {
	  if(show) {
	    $(id).slideDown(100);  
	  } else {
		$(id).slideUp(100); 
	  }
	}
}
