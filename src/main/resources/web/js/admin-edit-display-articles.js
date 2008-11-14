// JavaScript Document
$( document ).ready( function() { 
  $( "#resource\\.recursive-listing\\.unspecified" ).bind( "click", showHide );
  $( "#resource\\.recursive-listing\\.false" ).bind( "click", showHide );
});

function showHide()
{
  if( $( this ).val() ){
	   $("#vrtx-resource\\.recursive-listing-subfolders").hide();
  }else {
	  $("#vrtx-resource\\.recursive-listing-subfolders").show();
  }
}
