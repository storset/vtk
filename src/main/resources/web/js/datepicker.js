// JavaScript Document	
$(document).ready(function() {
    $(".date").each(
        function(){
            displayDateAsMultipleInputFields(this.name);
        }
    );
    // TODO !spageti 
    initPropChange();
    if($("#resource\\.start-date").length == 0 || $("#resource\\.end-date").length == 0){
        return;
    }
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
    var endDate = $("#resource\\.end-date").val();
    var startDate = $("#resource\\.start-date").datepicker( 'getDate' );
    if(endDate == ""){
        $("#resource\\.end-date").datepicker('option', 'defaultDate', startDate);
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

                if(date.length > 0){
                    this.value = date[0].value 
                    if(hours.length > 0){
                        this.value += " " + hours[0].value 
                        if(minutes.length > 0){
                            this.value += ":" + minutes[0].value;
                        }
                    }
                }
                
                // Hack.. .must be fixed!!!
                $("#" + fieldName + "-hours").remove();
                $("#" + fieldName + "-minutes").remove();
                $("#" + fieldName + "-date").remove();

            }
    );
}
