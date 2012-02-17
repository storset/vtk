$(document).ready(function () {
	$(".vrtx-shared-text input").each(function(){
		var doctype = $("#resource-title").attr("class").split(" ")[0];
		//var lang = datePickerLang; 
		var id = $(this).attr("id");
		var name = $(this).attr("name");
		var selected = $(this).val();	
		var path = "/vrtx/fellestekst/" + doctype + "/" + name + ".html?vrtx=source";

		var containerElement = $(this).parents(".inputfield")
		 
		$.getJSON(path, function(data) {  
			  
			if(!data){
				return;
			}
			  
			$(containerElement).html("<select id='" + id +"' name='" + name + "'></select>");
			var selectElement =  $("#" + id);
			$(selectElement).append("<option value=''>Ingen fellestekst</option>")
			var properties = data.properties; 
			for(x in properties){
			  if(x == "shared-text-box"){
				  for (y in properties[x]){
					var s  = "";
					if(properties[x][y].id  == selected){
					  s = "selected";
					}
					$(selectElement).append("<option value=" + properties[x][y].id + " " + s + ">" + properties[x][y].title + "</option>");
					// $(selectElement).after("<div class='description-en'>" + properties[x][y]['description-en'] + "</div>");
					// $(selectElement).after("<div class='description-no'>" + properties[x][y]['description-no'] + "</div>");
					// $(selectElement).after("<div class='description-nn'>" + properties[x][y]['description-nn'] + "</div>");		 
					}
				}
			  }
		  });  
	
	});
});