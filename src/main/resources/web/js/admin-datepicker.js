// JavaScript Document	
$(document).ready(function() {
    $(".date").datepicker({dateFormat: 'yy-mm-dd'});
	var startDate = $("#resource\\.start-date").datepicker( 'getDate' );
	if(startDate != null){
		setDefaultEndDate();
	}
	$("#resource\\.start-date").change(
		function(){
			setDefaultEndDate();					
		}
	);
});
	
function setDefaultEndDate(){
	var endDate = $("#resource\\.end-date").val();
	var startDate = $("#resource\\.start-date").datepicker( 'getDate' );
	if(endDate == ""){
		$("#resource\\.end-date").datepicker('option', 'defaultDate', startDate);
	}				
}