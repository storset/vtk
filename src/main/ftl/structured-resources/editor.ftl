<#import "/lib/ping.ftl" as ping />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "include/scripts.ftl" as scripts />
<#import "editor/fck.ftl" as fckEditor />
<#import "editor/vrtx-json-javascript.ftl" as vrtxJSONJavascript />
<#import "vrtx-types/vrtx-json-common.ftl" as vrtxJSONCommon />

<html>
<head>
 <title>Edit structured resource</title>
  <@ping.ping url=pingURL['url'] interval=300 />
  <@fckEditor.addFckScripts />
  <@vrtxJSONJavascript.script />
  <script language="Javascript" type="text/javascript" src="${webResources?html}/jquery-plugins/jquery.hotkeys-0.7.9.min.js"></script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/admin-prop-change.js"></script>
  <script language="Javascript" type="text/javascript"><!--
  	
  	$(document).bind('keydown', 'ctrl+s', saveDocument);
  	
  	function saveDocument(){
  		$("#updateAction").click();
  	}
  
    window.onbeforeunload = unsavedChangesInEditorMessage;
    UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.unsavedChangesConfirmation' />";
    COMPLETE_UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.completeUnsavedChangesConfirmation' />";
    
    function performSave() {
        saveDateAndTimeFields();
        if (typeof(MULTIPLE_INPUT_FIELD_NAMES) != "undefined") {
            saveMultipleInputFields();
        }
        NEED_TO_CONFIRM = false;
    }
    function cSave() {
        document.getElementById("form").setAttribute("action", "#submit");
        performSave();
    }
    //-->
  </script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/imageref.js"></script>
  <!-- JQuery UI (used for datepicker) -->
  <link type="text/css" href="${webResources?html}/jquery-ui-1.7.1.custom/css/smoothness/jquery-ui-1.7.1.custom.css" rel="stylesheet" />
  <script type="text/javascript" src="${webResources?html}/jquery-ui-1.7.1.custom/js/jquery-ui-1.7.1.custom.min.js"></script>
  <script type="text/javascript" src="${webResources?html}/jquery-ui-1.7.1.custom/js/jquery.ui.datepicker-no.js"></script>
  <script type="text/javascript" src="${webResources?html}/jquery-ui-1.7.1.custom/js/jquery.ui.datepicker-nn.js"></script>
  <script type="text/javascript" src="${jsBaseURL?html}/datepicker.js"></script>
  
  <#assign language = vrtx.getMsg("eventListing.calendar.lang", "en") />

   <script type="text/javascript">
    <!--
      $(document).ready(function() {
          initDatePicker("${language}");
       });
    //-->
  </script>
  
  <#if form.resource.type.scripts?exists>
    <@scripts.includeScripts form.resource.type.scripts />
  </#if>
</head>
<body>

<#assign locale = springMacroRequestContext.getLocale() />

<#assign header = form.resource.getLocalizedMsg("header", locale, null) />
<h2>${header}</h2>

<div class="submit-extra-buttons">
    <input type="button" onClick="$('#updateQuitAction').click()" value="${vrtx.getMsg("editor.saveAndQuit")}" />
    <input type="button" onClick="$('#updateAction').click()"  value="${vrtx.getMsg("editor.save")}" />
    <input type="button" onClick="$('#cancelAction').click()"  value="${vrtx.getMsg("editor.cancel")}" />
</div>

<div id="help-links">
	<a href="${editorHelpURL?html}" target="new_window"><@vrtx.msg code="editor.help"/></a><br />
	<a href="${form.listComponentServiceURL?html}" target="new_window"><@vrtx.msg code="plaintextEdit.tooltip.listDecoratorComponentsService" /></a>
</div>	  



<form action="${form.submitURL?html}" method="post">




<#list form.elements as elementBox>

  <#if elementBox.formElements?size &gt; 1>
    <#assign groupClass = "vrtx-grouped" />
    <#if elementBox.metaData['horizontal']?exists>
      <#assign groupClass = groupClass + "-horizontal" />
    <#elseif elementBox.metaData['vertical']?exists>
      <#assign groupClass = groupClass + "-vertical" />
    </#if>
    <#if elementBox.name?exists>
      <#assign groupName = elementBox["name"] />
      <#assign groupClass = groupClass + " ${groupName?string}" />
       <div class="${groupClass}">
       <#assign localizedHeader = form.resource.getLocalizedMsg(elementBox.name, locale, null) />
       <div class="header">${localizedHeader}</div>
    <#else>
       <div class="${groupClass}">
    </#if>
  </#if>

  <#list elementBox.formElements as elem>
    <@vrtxJSONCommon.printPropertyEditView form elem locale />
  </#list>
  
  <#if elementBox.formElements?size &gt; 1>
    </div>
  </#if>
  
</#list>
<div class="submit">
    <input type="submit" id="updateQuitAction" onClick="performSave();" name="updateQuitAction" value="${vrtx.getMsg("editor.saveAndQuit")}" />
    <input type="submit" id="updateAction" onClick="performSave();" name="updateAction" value="${vrtx.getMsg("editor.save")}" />
    <input type="submit" onClick="performSave();" name="cancelAction" id="cancelAction" value="${vrtx.getMsg("editor.cancel")}" />
</div>
</form>
</body>
</html>
