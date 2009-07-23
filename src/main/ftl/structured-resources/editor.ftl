<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/ping.ftl" as ping />

<#import "vrtx-types/vrtx-boolean.ftl" as vrtxBoolean />
<#import "vrtx-types/vrtx-datetime.ftl" as vrtxDateTime />
<#import "vrtx-types/vrtx-file-ref.ftl" as vrtxFileRef />
<#import "vrtx-types/vrtx-html.ftl" as vrtxHtml />
<#import "vrtx-types/vrtx-image-ref.ftl" as vrtxImageRef />
<#import "vrtx-types/vrtx-media-ref.ftl" as vrtxMediaRef />
<#import "vrtx-types/vrtx-radio.ftl" as vrtxRadio />
<#import "vrtx-types/vrtx-string.ftl" as vrtxString />

<#import "editor/fck.ftl" as fckEditor />
<html>
<head>
  <title>Edit structured resource</title>
  <@fckEditor.setup />
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/admin-prop-change.js"></script>
  <script language="Javascript" type="text/javascript">
	window.onbeforeunload = checkPropChange;
	PROP_CHANGE_CONFIRM_MSG = "<@vrtx.msg code='manage.unsavedChangesConfirmation' />";
	function performSave(){
		NEED_TO_CONFIRM = false;
	}
	function cSave() {
	    document.getElementById("form").setAttribute("action", "#submit");
	    performSave();
    }
  </script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/imageref.js"></script>
  <!-- JQuery UI (used for datepicker) -->
  <link type="text/css" href="${webResources?html}/jquery-ui-1.7.1.custom/css/smoothness/jquery-ui-1.7.1.custom.css" rel="stylesheet" />
  <script type="text/javascript" src="${webResources?html}/jquery-ui-1.7.1.custom/js/jquery-ui-1.7.1.custom.min.js"></script>
  <script type="text/javascript" src="${jsBaseURL?html}/admin-datepicker.js"></script>
<#--
 <script language="javascript">	
	$(document).ready(function() {
		
		setLang("<@vrtx.requestLanguage />");
		
		var fields1 = new Array("title","firstName","surname","postalAddress","visitingAddress","email","webPage","officeNumber");
		var fields2 = new Array("scientificInformation");
		
		showHide("getExternalPersonInfo", fields1);
		showHide("getExternalScientificInformation", fields2);
			
	    $("input[name='getExternalPersonInfo']").click(
	        function(){       	
	        	showHide("getExternalPersonInfo", fields1);
	        }
	    );
	    
	    $("input[name='getExternalScientificInformation']").click(
	        function(){   	
	        	showHide("getExternalScientificInformation", fields2);
	        }
	    );
	 
	});
	
	function showHide(name,fields){
        var checkSelect = $("input[name='" + name + "']:checked").val();
        if(checkSelect == "false"){
        	for(i in fields)
            	$("." + fields[i]).show();
        }else{
        	for(i in fields)
            	$("." + fields[i]).hide();
        }           
	}
	
	/* TODO: Remove */
	function setLang(lang){
		var lang_index = 0;
		if(lang == "en")
			lang_index = 1;
		
		 var elmTitles = new Array();
		 
		 elmTitles['username'] = new Array("Brukernavn","Username");
		 elmTitles['title'] = new Array("Tittel","Title");
		 elmTitles['firstName'] = new Array("Fornavn","First name");
		 elmTitles['surname'] = new Array("Etternavn","Surname");
		 elmTitles['postalAddress'] = new Array("Postadresse","Postal Address");
		 elmTitles['visitingAddress'] = new Array("Besøksadresse","Visisting Address");
		 elmTitles['webPage'] = new Array("Hjemmeside","Web page");
		 elmTitles['officeNumber'] = new Array("Kontornummer","Office number");
		 elmTitles['getExternalPersonInfo'] = new Array("Hent ekstern person data","Get external person info");
		 elmTitles['availableHours'] = new Array("Tilgjengelig i tidsrom","Available hours");
		 elmTitles['picture'] = new Array("Bilde","Picture");
		 elmTitles['tags'] = new Array("Emneord","Tags");
		 elmTitles['getExternalScientificInformation'] = new Array("Hent ekstern informasjon om vitenskaplige arbeider"
		 											,"Get external scientific information");
		 elmTitles['content'] = new Array("Innhold","Content");
		 elmTitles['scientificInformation'] = new Array("Vitenskaplig informasjon","Scientific information");
		 elmTitles['email'] = new Array("E-post","E-mail");
		 											
		for(key in elmTitles)
			$("label[for=" + key +"]").text(elmTitles[key][lang_index]);
	}
</script>
-->

  <link type="text/css" href="${themeBaseURL?html}/structured-resources/editor.css" rel="stylesheet" />
  
</head>
<body>

<form action="${form.submitURL?html}" method="POST">

<#list form.elements as elementBox>

  <#if elementBox.formElements?size &gt; 1>
    <#assign groupClass = "vrtx-grouped" />
    <#if elementBox.metaData['horisontal']?exists>
      <#assign groupClass = groupClass + "-horisontal" />
    </#if>
    <div class=${groupClass}>
  </#if>

  <#list elementBox.formElements as elem>
	<#switch elem.description.type>
	  <#case "string">
	  	<#assign fieldSize="20" />
	  	<#if elem.description.edithints?exists && elem.description.edithints['size']?exists >
	  		<#assign fieldSize=elem.description.edithints['size'] />
	  	</#if>
	 	<@vrtxString.printPropertyEditView 
	 		title=elem.description.name 
	 		inputFieldName=elem.description.name 
	 		value=elem.value 
	 		classes=elem.description.name
	 		inputFieldSize=fieldSize />
	    <#break>
	  <#case "simple_html">
	  	<#if elem.description.edithints?exists>
		 		<#list elem.description.edithints?keys as hint>
		 			${hint} <br />
		 		</#list>
	 	</#if>
	    <@vrtxHtml.printPropertyEditView 
	    	title=elem.description.name 
	    	inputFieldName=elem.description.name 
	    	value=elem.value 
	    	classes=elem.description.name />
	    <@fckEditor.insert elem.description.name />
	  	<#break>
	  <#case "html">
	  	<#if elem.description.edithints?exists>
		 		<#list elem.description.edithints?keys as hint>
		 			${hint} <br />
		 		</#list>
	 	</#if>
	    <@vrtxHtml.printPropertyEditView 
	    	title=elem.description.name 
	    	inputFieldName=elem.description.name 
	    	value=elem.value 
	    	classes=elem.description.name />
	    <@fckEditor.insert elem.description.name true false />
	    <#break>
	  <#case "boolean">
	  	<@vrtxBoolean.printPropertyEditView 
	  		title=elem.description.name 
	  		inputFieldName=elem.description.name 
	  		value=elem.value />
	  	<#break>
	  <#case "image_ref">
	  	<@vrtxImageRef.printPropertyEditView 
	  		title=elem.description.name 
	  		inputFieldName=elem.description.name 
	  		value=elem.value 
	  		baseFolder=resourceContext.parentURI />
	  	<#break>          
	  <#case "media_ref">
	  	<@vrtxMediaRef.printPropertyEditView 
	  		title=elem.description.name 
	  		inputFieldName=elem.description.name 
	  		value=elem.value 
	  		baseFolder=resourceContext.parentURI />
	  	<#break>
	  <#case "datetime">
		 <@vrtxDateTime.printPropertyEditView 
			title=elem.description.name 
			inputFieldName=elem.description.name 
			value=elem.value 
			classes=elem.description.name />
	  	<#break>
	  <#default>
	    ny type property ${elem.description.type}
	</#switch>
  </#list>
  
  <#if elementBox.formElements?size &gt; 1>
    </div>
  </#if>
  
</#list>
<div class="submit">
	  <input type="submit" id="updateQuitAction" onClick="performSave();" name="updateQuitAction"  value="${vrtx.getMsg("editor.saveAndQuit")}" />
	  <input type="submit" id="updateAction" onClick="performSave();" name="updateAction" value="${vrtx.getMsg("editor.save")}" />
	  <input type="submit" onClick="performSave();" name="cancelAction" id="cancelAction" value="${vrtx.getMsg("editor.cancel")}" />
</div>
</form>
</body>
</html>
