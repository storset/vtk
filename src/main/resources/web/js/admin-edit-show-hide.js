$(document).ready(function() {

  showHide(new Array("#resource\\.recursive-listing\\.unspecified", "#resource\\.recursive-listing\\.false"), 
		   "#resource\\.recursive-listing\\.false:checked", 
		   'false', 
		   new Array("#vrtx-resource\\.aggregation", "#vrtx-resource\\.recursiveAggregation"));
  
  showHide(new Array("#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"), 
		   "#resource\\.display-type\\.calendar:checked",
		   null, 
		   new Array("#vrtx-resource\\.event-type-title"));

});

/**
 * radioIds: Multiple id's for radiobuttons binding click events (Array)
 * conditionHide: Condition to be checked for hiding
 * conditionHideEqual: What it should equal 
 * showHideProps: Multiple props / id's / classnames to show / hide (Array)
 */
function showHide(radioIds, conditionHide, conditionHideEqual, showHideProps) {
  //init
  showHideProperties(conditionHide, conditionHideEqual, showHideProps);

  //bind() click() events
  for(var j = 0; j < radioIds.length; j++) {
	$(radioIds[j]).bind("click", function() {
	  showHideProperties(conditionHide, conditionHideEqual, showHideProps);
    });
  }
}

function showHideProperties(conditionHide, conditionHideEqual, showHideProps) {
  if($(conditionHide).val() == conditionHideEqual){
	for(var i = 0; i < showHideProps.length; i++) {
	  showHideProperty(showHideProps[i], true, false);
	}
  } else {
	for(var i = 0; i < showHideProps.length; i++) {
	  showHideProperty(showHideProps[i], true, true);
    }
  }
}

function showHideProperty(id, init, show) {
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
