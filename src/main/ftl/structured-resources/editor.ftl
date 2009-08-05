<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/ping.ftl" as ping />
<#import "/lib/autocomplete.ftl" as autocomplete />

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
		saveDateAndTimeFields();
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
	
</script>
-->

  <link type="text/css" href="${themeBaseURL?html}/structured-resources/editor.css" rel="stylesheet" />
  
  <#-- XXX testing only! -->
  <@autocomplete.addAutoCompleteScripts srcBase="${webResources?html}"/>
  <script type="text/javascript">
    $(document).ready(function() {
      setAutoComplete('tags', 'tags', {minChars:1});
    });
  </script>
  
</head>
<body>

<form action="${form.submitURL?html}" method="POST">
<#assign locale = springMacroRequestContext.getLocale() />

<#list form.elements as elementBox>

  <#if elementBox.formElements?size &gt; 1>
    <#assign groupClass = "vrtx-grouped" />
    <#if elementBox.metaData['horizontal']?exists>
      <#assign groupClass = groupClass + "-horizontal" />
    </#if>	
    <div class=${groupClass}>
  </#if>

  <#list elementBox.formElements as elem>
  
    <#assign localizedTitle = form.resource.getLocalizedMsg(elem.description.name, locale, null) />
    
	<#switch elem.description.type>
	  <#case "string">
	  	<#assign fieldSize="20" />
	  	<#if elem.description.edithints?exists && elem.description.edithints['size']?exists >
	  		<#assign fieldSize=elem.description.edithints['size'] />
	  	</#if>
	 	<@vrtxString.printPropertyEditView 
	 		title=localizedTitle 
	 		inputFieldName=elem.description.name 
	 		value=elem.getFormatedValue()
	 		classes=elem.description.name
	 		inputFieldSize=fieldSize />
	    <#break>
	  <#case "simple_html">
	  	<#assign cssclass = "vrtx-simple-html" />
	 	<#if elem.description.edithints?exists && elem.description.edithints['size']?exists >
	  		<#assign cssclass = cssclass + "-" + elem.description.edithints['size'] />
	  	</#if>
	    <@vrtxHtml.printPropertyEditView 
	    	title=localizedTitle 
	    	inputFieldName=elem.description.name 
	    	value=elem.value 
	    	classes=cssclass />
	    <@fckEditor.insert elem.description.name />
	  	<#break>
	  <#case "html">
	  	<#if elem.description.edithints?exists>
		 		<#list elem.description.edithints?keys as hint>
		 			${hint} <br />
		 		</#list>
	 	</#if>
	    <@vrtxHtml.printPropertyEditView 
	    	title=localizedTitle 
	    	inputFieldName=elem.description.name 
	    	value=elem.value 
	    	classes="vrtx-html" />
	    <@fckEditor.insert elem.description.name true false />
	    <#break>
	  <#case "boolean">
	  	<@vrtxBoolean.printPropertyEditView 
	  		title=localizedTitle
	  		inputFieldName=elem.description.name 
	  		value=elem.value />
	  	<#break>
	  <#case "image_ref">
	  	<@vrtxImageRef.printPropertyEditView 
	  		title=localizedTitle
	  		inputFieldName=elem.description.name 
	  		value=elem.value 
	  		baseFolder=resourceContext.parentURI />
	  	<#break>          
	  <#case "media_ref">
	  	<@vrtxMediaRef.printPropertyEditView 
	  		title=localizedTitle
	  		inputFieldName=elem.description.name 
	  		value=elem.value 
	  		baseFolder=resourceContext.parentURI />
	  	<#break>
	  <#case "datetime">
		 <@vrtxDateTime.printPropertyEditView 
			title=localizedTitle
			inputFieldName=elem.description.name 
			value=elem.value 
			classes=elem.description.name  />
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
