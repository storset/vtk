$(document).ready(function () {
	$(".vrtx-shared-text input").each(function(){
		var doctype = $("#resource-title").attr("class").split(" ")[0];
		var lang = "no"; 
		var id = $(this).attr("id");
		var name = $(this).attr("name");
		var selected = $(this).val();	
		var path = "/vrtx/fellestekst/" + doctype + "/" + name + ".html?vrtx=source";

		var containerElement = $(this).parents(".vrtx-shared-text");
		var inputfieldContainer = $(this).parents(".inputfield");
		 
		$.getJSON(path, function(data) {  
			  
			if(!data){
				return;
			}
			  
			$(inputfieldContainer).html("<select id='" + id +"' name='" + name + "'></select>");
			var selectElement =  $("#" + id);
			$(selectElement).append("<option value=''>Ingen fellestekst</option>")
			$(containerElement).append("<div class='description'>test</div>");
			var properties = data.properties; 
			for(x in properties){
			  if(x == "shared-text-box"){
				  for (y in properties[x]){
					var s  = "";
					if(properties[x][y].id  == selected){
					  s = "selected";
					  var d = $(containerElement).find(".description")
					  $(d).html(properties[x][y]['description-' + lang]);
					}
					$(selectElement).append("<option value=" + properties[x][y].id + " " + s + ">" + properties[x][y].title + "</option>");		 
				  }
				}
			  }
	
			/* Change selected text event */
			$(selectElement).change(function(){
				
				if($(selectElement).val().trim() == ""){
					 var d = $(containerElement).find(".description");
					 $(d).html("");
					 return;
				}
				
				for(x in properties){
					  for (y in properties[x]){
							if(properties[x][y].id  == $(selectElement).val()){
								  var d = $(containerElement).find(".description")
								  $(d).html(properties[x][y]['description-' + lang]);
								  return;
							}
					   }
				}

			});	

		});  
	});
		
});