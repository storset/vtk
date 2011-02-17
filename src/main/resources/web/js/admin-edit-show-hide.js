$(document).ready(function() {

  showHide(new Array("#resource\\.recursive-listing\\.false", "#resource\\.recursive-listing\\.unspecified"), 
		   "#resource\\.recursive-listing\\.false:checked", 
		   'false', 
		   new Array("#vrtx-resource\\.recursive-listing-subfolders"));
  
  showHide(new Array("#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"), 
		   "#resource\\.display-type\\.calendar:checked",
		   null, 
		   new Array("#vrtx-resource\\.event-type-title"));
  
  showHide(new Array("#resource\\.display-type\\.unspecified", "#resource\\.display-type\\.calendar"), 
		   "#resource\\.display-type\\.calendar:checked",
		   'calendar', 
		   new Array("#vrtx-resource\\.hide-additional-content"));

});

/**
 * Show and hide properties
 * 
 * @param radioIds: Multiple id's for radiobuttons binding click events (Array)
 * @param conditionHide: Condition to be checked for hiding
 * @param conditionHideEqual: What it should equal
 * @param showHideProps: Multiple props / id's / classnames to show / hide (Array)
 */
function showHide(radioIds, conditionHide, conditionHideEqual, showHideProps) {
  var showHidePropertiesFunc = showHideProperties;

  //init
  showHidePropertiesFunc(true, conditionHide, conditionHideEqual, showHideProps);

  //bind() click() events
  var radioIdsLength = radioIds.length;
  for(var j = 0; j < radioIdsLength; j++) {
	$(radioIds[j]).bind("click", function() {
	  showHidePropertiesFunc(false, conditionHide, conditionHideEqual, showHideProps);
    });
  }
}

function showHideProperties(init, conditionHide, conditionHideEqual, showHideProps) {
  var showHidePropsLength = showHideProps.length;
  var conditionHideVal = $(conditionHide).val();
  var showHidePropertyFunc = showHideProperty;
  for(var i = 0; i < showHidePropsLength; i++) {
    showHidePropertyFunc(showHideProps[i], init, conditionHideVal == conditionHideEqual ? false : true);
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
