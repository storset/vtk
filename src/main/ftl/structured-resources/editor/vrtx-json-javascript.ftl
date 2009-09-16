<#-- 
	Evil hack(s) alert! 
-->
<#macro script >
<script language="Javascript" type="text/javascript">
  
  	LIST_OF_JSON_ELEMENTS = new Array();
  
  	$(document).ready(function() {   
	   	<#assign i = 0 />
		<#list form.elements as elementBox>	
		  <#assign j = 0 />
		  <#list elementBox.formElements as elem>
		      <#if elem.description.type == "json" >
		      	
		LIST_OF_JSON_ELEMENTS[${i}] = new Object();
		LIST_OF_JSON_ELEMENTS[${i}].name = "${elem.name}";
		LIST_OF_JSON_ELEMENTS[${i}].type = "${elem.description.type}";
		LIST_OF_JSON_ELEMENTS[${i}].a = new Array();

			    <#list elem.description.attributes as jsonAttr>
		LIST_OF_JSON_ELEMENTS[${i}].a[${j}] = new Object();
		LIST_OF_JSON_ELEMENTS[${i}].a[${j}].name = "${jsonAttr}";	      
		LIST_OF_JSON_ELEMENTS[${i}].a[${j}].type = "${elem.description.getType(jsonAttr)}";
				    <#assign j = j + 1 />	
		  	    </#list>
		  		<#assign i = i + 1 />	
		  	   </#if>      
		 	 </#list>
		  </#list>
		
		for(i in LIST_OF_JSON_ELEMENTS){
  			$("#" + LIST_OF_JSON_ELEMENTS[i].name).after("<input type=\"button\" class=\"vrtx-add-button\" onClick=\"addNewJsonShit(LIST_OF_JSON_ELEMENTS[" + i + "])\" value=\"Flere\" />");  		
  		}		
  	});
  	
  	function getCounterForJson(inputFieldName,jsonAttr){
  		var i = 0;
  		while( $("#" + inputFieldName + "\\." + jsonAttr + "\\." + i).size() > 0 ){ 	
  			i++;
  		}
  		return i;
  	}
  	
	function addNewJsonShit(j){
	
	   var counter = getCounterForJson(j.name,j.a[0].name);
	   
	   var htmlTemplate = "";
	   for(i in j.a){
	   		var inputFieldName = j.name + "." + j.a[i].name + "." + counter;
	   		switch(j.a[i].type) {
	   			case "string":
	   				htmlTemplate += addStringField(j.a[i],inputFieldName); break
	   			case "html":
	   				htmlTemplate += addHtmlField(j.a[i],inputFieldName); break
	   			case "simple_html":
	   				htmlTemplate += addHtmlField(j.a[i],inputFieldName); break
	   			case "boolean":
	   				htmlTemplate += addBooleanField(j.a[i],inputFieldName); break
	   			case "image_ref":
	   				htmlTemplate += addImageRef(j.a[i],inputFieldName); break
	   			case "datetime":
	   				htmlTemplate += addDateField(j.a[i],inputFieldName); break
	   			case "media":
	   				htmlTemplate += addMediaRef(j.a[i],inputFieldName); break
	   			default:
	   				htmlTemplate += ""; break
	   		}
	   } 
	   
	   var deleteButton = "<input type=\"button\" class=\"vrtx-remove-button\" value=\"Slett\" onClick=\"$('#vrtx-json-element-" + counter + "').remove()\" \/>"
	   $("#" + j.name).append("<div class=\"vrtx-json-element\" id=\"vrtx-json-element-" + counter + "\">" + deleteButton +  htmlTemplate + "<\/div>");
	   
	   // Bootstrap f*@!ck.........
	   
	   for(i in j.a){
	   	var inputFieldName = j.name + "." + j.a[i].name + "." + counter;
	   	if(j.a[i].type == "simple_html"){	
		   	newEditor(inputFieldName,false,false,'${resourceContext.parentURI}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
	        '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', '');    
	   	}else if(j.a[i].type == "html"){
		   	newEditor(inputFieldName,true,false,'${resourceContext.parentURI}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
	        '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', '');
	   	}else if(j.a[i].type == "datetime"){
			displayDateAsMultipleInputFields(inputFieldName);
	   }
	   
	  }
	}
	

	function addStringField(elem,inputFieldName){
		var htmlTemplate = new String();
		htmlTemplate = '<div class=\"{classes}\">';
		htmlTemplate += '<label for=\"{inputFieldName}\">{title}<\/label>';
		htmlTemplate += '<div class=\"inputfield\">';
		htmlTemplate += '<input size=\"{inputFieldSize}\" type=\"text\" name=\"{inputFieldName}\" id=\"{inputFieldName}\" />';
		htmlTemplate +=	'<\/div>';
		htmlTemplate +=	'<div class=\"tooltip\">{tooltip}<\/div>';
		htmlTemplate +=	'<\/div>';
		
		return processHtmlTemplate(elem.name,"vrtx-string",htmlTemplate,inputFieldName);
	}
	
	function addHtmlField(elem,inputFieldName,counter){
		var htmlTemplate = new String();
		htmlTemplate = '<div class=\"{classes}\">';
        htmlTemplate += '<label for=\"{inputFieldName}\">{title}<\/label>';
        htmlTemplate += '<div>';
        htmlTemplate += '<textarea name=\"{inputFieldName}\" id=\"{inputFieldName}\" ';
        htmlTemplate += ' rows=\"4\" cols=\"20\" >{value}<\/textarea>';
        htmlTemplate += '<\/div><\/div>';
		
		if(elem.type == "simple_html"){
			var classes = "vrtx-simple-html";
		}else{
			var classes = "vrtx-html";
		}
		
		return processHtmlTemplate(elem.name,classes,htmlTemplate,inputFieldName);
	}
	
	function addBooleanField(elem,inputFieldName,counter){
		var htmlTemplate = new String();
		htmlTemplate = '<div class=\"{classes}\">';
  		htmlTemplate += '<div><label>{title}<\/label><\/div>';
  		htmlTemplate += '<div>';
      	htmlTemplate += '<input name=\"{inputFieldName}\" id=\"{inputFieldName}-true\" type=\"radio\" value=\"true\"  \/>';
      	htmlTemplate += '<label for=\"{inputFieldName}-true\">True<\/label> ';
      	htmlTemplate += '<input name=\"{inputFieldName}\" id=\"{inputFieldName}-false\" type=\"radio\" value=\"false\" \/>';
      	htmlTemplate +=	'<label for=\"{inputFieldName}-false\">False<\/label>';
  		htmlTemplate += '<\/div><\/div>';
  		
  		return processHtmlTemplate(elem.name,"vrtx-radio",htmlTemplate,inputFieldName);
	}
	
	function addImageRef(elem,inputFieldName,counter){
		var htmlTemplate = new String();
		htmlTemplate = '<div class=\"{classes}\">';
		htmlTemplate += '<div>';
		htmlTemplate += '<label for=\"{inputFieldName}\">{title}<\/label>';
		htmlTemplate += '<\/div><div>';
	  	htmlTemplate += '<input type=\"text\" id=\"{inputFieldName}\" name=\"{inputFieldName}\" value=\"{value?html}\" onblur=\"previewImage({inputFieldName});\" size=\"30\" \/>';
      	htmlTemplate += '<button type=\"button\" onclick=\"browseServer(\'{inputFieldName}\', \'${fckeditorBase.url}\', \'${resourceContext.parentURI}\',\'${fckBrowse.url.pathRepresentation}\');\"><@vrtx.msg code="editor.browseImages" /><\/button>';
		htmlTemplate += '<\/div>';
		htmlTemplate += '<div id=\"{inputFieldName}.preview\">';
		htmlTemplate += '<img src=\"{value}?vrtx=thumbnail\" alt=\"Preview image\" \/>';
		htmlTemplate += '<\/div><\/div>';
		
		return processHtmlTemplate(elem.name,"vrtx-image-ref",htmlTemplate,inputFieldName);
	}
	
	function addDateField(elem,inputFieldName,counter){
		var htmlTemplate = new String();
		htmlTemplate = '<div class=\"{classes}\">';
		htmlTemplate += '<label for=\"{inputFieldName}\">{title}<\/label>';
		htmlTemplate += '<div class=\"inputfield\">';
		htmlTemplate += '<input size=\"20\" type=\"text\" name=\"{inputFieldName}\" id=\"{inputFieldName}\" value=\"{value}\" class=\"date\" \/>';
		htmlTemplate += '<\/div>';
		htmlTemplate += '<div class=\"tooltip\">{tooltip}<\/div>';
		htmlTemplate += '<\/div>';
		
		return processHtmlTemplate(elem.name,"vrtx-string date",htmlTemplate,inputFieldName);
	}

	function addMediaRef(elem,inputFieldName,counter){
		var htmlTemplate = new String();
		htmlTemplate = '<div class=\"{classes}\">';
		htmlTemplate += '<div><label for=\"{inputFieldName}\">{title}<\/label>';
		htmlTemplate += '<\/div><div>';
		htmlTemplate += '<input type=\"text\" id=\"{inputFieldName}\" name=\"{inputFieldName}\" value=\"{value}\" onblur=\"previewImage({inputFieldName});\" size=\"30\"\/>';
		htmlTemplate += '<button type=\"button\" onclick=\"browseServer(\'{inputFieldName}\', \'${fckeditorBase.url}\', \'${resourceContext.parentURI}\',\'${fckBrowse.url.pathRepresentation}\',\'Media\');\"><@vrtx.msg code="editor.browseImages" /><\/button>';
		htmlTemplate += '<\/div><div id=\"{inputFieldName}.preview\">';
		htmlTemplate += '<img src=\"\" \/>';
		htmlTemplate += '<\/div><\/div>'
		
		return processHtmlTemplate(elem.name,"vrtx-media-ref",htmlTemplate,inputFieldName);
	}

	function processHtmlTemplate(name,classes,template,inputFieldName){
		var htmlTemplate = new String(template);
		htmlTemplate = htmlTemplate.replace(/{inputFieldName}/g,inputFieldName);
		htmlTemplate = htmlTemplate.replace(/{inputFieldSize}/g,"40");
		htmlTemplate = htmlTemplate.replace(/{tooltip}/g,"");
		htmlTemplate = htmlTemplate.replace(/{title}/g,name);
		htmlTemplate = htmlTemplate.replace(/{classes}/g,classes);
		htmlTemplate = htmlTemplate.replace(/{value}/g,"");
		
		return htmlTemplate;
	}

  </script>
</#macro>