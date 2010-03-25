// JavaScript Document
  
$(document).ready(function() {
  
  showHide(new Array("#resource\\.recursive-listing\\.unspecified", "#resource\\.recursive-listing\\.false"), "#resource\\.recursive-listing\\.false:checked", 
		               'false', new Array("#vrtx-resource\\.aggregation", "#vrtx-resource\\.recursiveAggregation"));
  showHide(new Array("#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"), "#resource\\.display-type\\.calendar:checked",
		  null, new Array("#vrtx-resource\\.event-type-title"));

});

function showHide(radioIds, condition, conditionVal, showHideProps) {
  for(var j = 0; j < radioIds.length; j++) {
	  $(radioIds[j]).bind("click", function() {
	    if($(condition).val() == conditionVal){
	      for(var i = 0; i < showHideProps.length; i++) {
		    showHideProp(showHideProps[i], false, false);
	      }
	    } else {
	      for(var i = 0; i < showHideProps.length; i++) {
	        showHideProp(showHideProps[i], false, true);
	      }
	    }
	  });
  }
  
  //init
  //TODO: refactor with above code
  if($(condition).val() == conditionVal){
    for(var i = 0; i < showHideProps.length; i++) {
	  showHideProp(showHideProps[i], true, false);
    }
  } else {
    for(var i = 0; i < showHideProps.length; i++) {
      showHideProp(showHideProps[i], true, true);
    }
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
