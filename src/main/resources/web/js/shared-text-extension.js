$(document).ready(function () {
	$(".vrtx-shared-text input").each(function(){
		var doctype = $("#resource-title").attr("class").split(" ")[0];
		var id = $(this).attr("id");
		var name = $(this).attr("name");
		var selected = $(this).val();	
		var path = "/vrtx/fellestekst/" + doctype + "/" + name + ".html?vrtx=source";

		var lang;
		if(CURRENT_RESOURCE_LANGAGE.indexOf("NY") > -1){
			lang = "nn";
		}else if(CURRENT_RESOURCE_LANGAGE.indexOf("no") > -1){
			lang = "no";
		}else{
			lang = "en";
		}
		
		var containerElement = $(this).parents(".vrtx-shared-text");
		var inputfieldContainer = $(this).parents(".inputfield");
		 
		$.getJSON(path, function(data) {  
			  
			if(!data){
				return;
			}
			  
			$(inputfieldContainer).html("<select id='" + id +"' name='" + name + "'></select>");
			var selectElement =  $("#" + id);
			$(selectElement).append("<option value=''>Ingen fellestekst</option>")
			$(containerElement).append("<div class='description' />");
			var valueList = data.properties["shared-text-box"];
			for(y in valueList){
				var s  = "";
				if(valueList[y].id  == selected){
					s = "selected";
					var d = $(containerElement).find(".description")
					$(d).html(valueList[y]['description-' + lang]);
				}
				$(selectElement).append("<option value=" + valueList[y].id + " " + s + ">" + valueList[y].title + "</option>");		 
			}
		
			/* Change selected text event */
			$(selectElement).change(function(){
				if($(selectElement).val().trim() == ""){
					 var d = $(containerElement).find(".description");
					 $(d).html("");
					 return;
				}
				for(y in valueList){
					if(valueList[y].id  == $(selectElement).val()){
						var d = $(containerElement).find(".description")
						$(d).html(valueList[y]['description-' + lang]);
						return;
					}
				}
			});	

		});  
	});
		
});