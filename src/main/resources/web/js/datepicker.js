// JavaScript Document	
$(document).ready(function() {
    $(".date").each(
        function(){
            displayDateAsMultipleInputFields(this.name);
        }
    );
    // TODO !spageti 
    if(requestFromEditor()){
        initPropChange();
    }
    
    // specific for start and end date
    if($("#start-date-date").length == 0 || $("#end-date-date").length == 0){
        return;
    }
	var startDate = $("#start-date-date").datepicker( 'getDate' );
	if(startDate != null){
		setDefaultEndDate();
	}
	$("#start-date-date").change(
		function(){
			setDefaultEndDate();					
		}
	);
});

// Stupid test to check if script is loaded from editor
// PROP_CHANGE_CONFIRM_MSG is defined in "structured-resource/editor.ftl"
function requestFromEditor(){
    return !(typeof(PROP_CHANGE_CONFIRM_MSG) == "undefined");
}

function displayDateAsMultipleInputFields(name){
    var hours = "";
    var minutes="";
    var date = new Array("");
    var fieldName = name.replace(/\./g,'\\.');

    var a = $.find("#" + fieldName);  
    
    if(a.length > 0){
        hours = extractHoursFromDate(a[0].value);
        minutes = extractMinutesFromDate(a[0].value)
        date = new String(a[0].value).split(" ");
    }
    
    dateField = "<input type='text' size='12' id='" + name  + "-date' name='" + name  + "-date' value='" + date[0] + "' class='date' />"; 
    hoursField = "<input type='text' size='2' id='" + name  + "-hours' name='" + name  + "-hours' value='" + hours + "' class='hours' />";  
    minutesField = "<input type='text' size='2' id='" + name  + "-minutes' name='" + name  + "-minutes' value='" + minutes + "' class='minutes' />"; 
    $("#" + fieldName).hide();
    $("#" + fieldName).after(dateField + hoursField + ":" + minutesField);
    $("#" + fieldName + "-date").datepicker({dateFormat: 'yy-mm-dd'});
}

function setDefaultEndDate(){
    var endDate = $("#end-date-date").val();
    var startDate = $("#start-date-date").datepicker( 'getDate' );
    if(endDate == ""){
        $("#end-date-date").datepicker('option', 'defaultDate', startDate);
    }               
}


function extractHoursFromDate(datetime){
    var a = new String(datetime);
    var b = a.split(" ");
    if(b.length > 1){
        var c = b[1].split(":");
        if(c != null){
            return c[0];
        }
    }
    return "";
}

function extractMinutesFromDate(datetime){
    var a = new String(datetime);
    var b = a.split(" ");
    if(b.length > 1){
        var c = b[1].split(":");
        if(c.length > 0){
            return c[1];
        }
    }
    return ""
}

function saveDateAndTimeFields(){
    $(".date").each(
            function(){
                if(!this.name)
                    return;
                var fieldName = this.name.replace(/\./g,'\\.');
                var hours = $.find("#" + fieldName + "-hours"); 
                var minutes = $.find("#" + fieldName + "-minutes");
                var date = $.find("#" + fieldName + "-date");
                if(requestFromEditor())
                    this.value = "";
                if(date[0] != null && date[0].value.toString().length > 0){
                    this.value = date[0].value 
                    if(hours[0] != null && hours[0].value.toString().length > 0){
                        this.value += " " + hours[0].value 
                        if(minutes[0].value != null && minutes[0].value.toString().length > 0){
                            this.value += ":" + minutes[0].value;
                        }
                    }
                }
                
                // Hack fix for editor.. .must be removed!!!
                if(requestFromEditor()){
                    $("#" + fieldName + "-hours").remove();
                    $("#" + fieldName + "-minutes").remove();
                    $("#" + fieldName + "-date").remove();
                }
            }
    );
}
