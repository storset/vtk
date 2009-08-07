// JavaScript Document	
$(document).ready(function() {
    $(".date").each(
        function(){
            displayDateAsMultipleInputFields(this.name);
        }
    );
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
    var a = $.find("#" + name);  
   
    if(a[0].value != null){
        hours = extractHoursFromDate(a[0].value);
        minutes = extractMinutesFromDate(a[0].value)
        date = new String(a[0].value).split(" ");
    }
    
    dateField = "<input type='text' size='12' id='" + name  + "-date' name='" + name  + "-date' value='" + date[0] + "' class='date' />"; 
    hoursField = "<input type='text' size='2' id='" + name  + "-hours' name='" + name  + "-hours' value='" + hours + "' class='hours' />";  
    minutesField = "<input type='text' size='2' id='" + name  + "-minutes' name='" + name  + "-minutes' value='" + minutes + "' class='minutes' />"; 
    $("#" + name).hide();
    $("#" + name).after(dateField + hoursField + ":" + minutesField);
    $("#" + name + "-date").datepicker({dateFormat: 'yy-mm-dd'});
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
    if(b.length > 0){
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
    if(b.length > 0){
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
                var hours = $.find("#" + this.name + "-hours"); 
                var minutes = $.find("#" + this.name + "-minutes");
                var date = $.find("#" + this.name + "-date");

                if(date[0] != null){
                    this.value = date[0].value 
                    if(hours[0] != null){
                        this.value += " " + hours[0].value 
                        if(minutes[0] != null){
                            this.value += ":" + minutes[0].value;
                        }
                    }
                }
            }
    );
}
