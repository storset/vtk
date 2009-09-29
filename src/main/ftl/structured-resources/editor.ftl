<#import "/lib/ping.ftl" as ping />

<#import "/lib/vortikal.ftl" as vrtx />
<#import "vrtx-types/vrtx-boolean.ftl" as vrtxBoolean />
<#import "vrtx-types/vrtx-datetime.ftl" as vrtxDateTime />
<#import "vrtx-types/vrtx-file-ref.ftl" as vrtxFileRef />
<#import "vrtx-types/vrtx-html.ftl" as vrtxHtml />
<#import "vrtx-types/vrtx-image-ref.ftl" as vrtxImageRef />
<#import "vrtx-types/vrtx-media-ref.ftl" as vrtxMediaRef />
<#import "vrtx-types/vrtx-radio.ftl" as vrtxRadio />
<#import "vrtx-types/vrtx-string.ftl" as vrtxString />
<#import "vrtx-types/vrtx-json.ftl" as vrtxJSON />
<#import "include/scripts.ftl" as scripts />

<#import "editor/fck.ftl" as fckEditor />
<#import "editor/vrtx-json-javascript.ftl" as vrtxJSONJavascript />
<html>
<head>
  <title>Edit structured resource</title>
  
  <@fckEditor.addFckScripts />
<@vrtxJSONJavascript.script />
  
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/admin-prop-change.js"></script>
  <script language="Javascript" type="text/javascript"><!--
    window.onbeforeunload = checkPropChange;
    PROP_CHANGE_CONFIRM_MSG = "<@vrtx.msg code='manage.unsavedChangesConfirmation' />";
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
  <script type="text/javascript" src="${jsBaseURL?html}/datepicker.js"></script>
  <link type="text/css" href="${themeBaseURL?html}/structured-resources/editor.css" rel="stylesheet" />
  <#if form.resource.type.scripts?exists>
    <@scripts.includeScripts form.resource.type.scripts />
  </#if>
</head>
<body>

<#assign locale = springMacroRequestContext.getLocale() />

<#assign header = form.resource.getLocalizedMsg("header", locale, null) />
<h2>${header}</h2>

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
  
    <#assign localizedTitle = form.resource.getLocalizedMsg(elem.name, locale, null) />
    
    <#switch elem.description.type>
      <#case "string">
        <#assign fieldSize="40" />
        <#if elem.description.edithints?exists && elem.description.edithints['size']?exists >
          <#if elem.description.edithints['size'] == "large" >
            <#assign fieldSize="60" />
          <#elseif elem.description.edithints['size'] == "small" >
            <#assign fieldSize="20"/>
          <#else>
            <#assign fieldSize=elem.description.edithints['size'] />
          </#if>
        </#if>
        <#assign dropdown = false />
        <#if elem.description.edithints?exists && elem.description.edithints['dropdown']?exists >
        	<#assign dropdown = true />
        </#if>
       <@vrtxString.printPropertyEditView 
         title=localizedTitle 
         inputFieldName=elem.name 
         value=elem.getFormatedValue()
         classes=elem.name
         inputFieldSize=fieldSize 
         tooltip=form.resource.getLocalizedTooltip(elem.name,locale)
         valuemap=elem.description.getValuemap(locale)
         dropdown=dropdown
         />
        <#break>
      <#case "simple_html">
        <#assign cssclass =  elem.name + " vrtx-simple-html" />
       <#if elem.description.edithints?exists && elem.description.edithints['size']?exists >
          <#assign cssclass = cssclass + "-" + elem.description.edithints['size'] />
        </#if>
        <@vrtxHtml.printPropertyEditView 
          title=localizedTitle 
          inputFieldName=elem.name 
          value=elem.value 
          classes=cssclass 
          tooltip=form.resource.getLocalizedTooltip(elem.name,locale)
          />
        <@fckEditor.insertEditor elem.name />
        <#break>
      <#case "html">
        <#if elem.description.edithints?exists>
           <#list elem.description.edithints?keys as hint>
             ${hint} <br />
           </#list>
       </#if>
        <@vrtxHtml.printPropertyEditView 
          title=localizedTitle 
          inputFieldName=elem.name 
          value=elem.value 
          classes="vrtx-html " + elem.name  
          tooltip=form.resource.getLocalizedTooltip(elem.name,locale)
          />
        <@fckEditor.insertEditor elem.name true false />
        <#break>
      <#case "boolean">
        <@vrtxBoolean.printPropertyEditView 
          title=localizedTitle
          inputFieldName=elem.name 
          value=elem.value
          classes=elem.name
          description=elem.description 
          tooltip=form.resource.getLocalizedTooltip(elem.name,locale)
          />
        <#break>
      <#case "image_ref">
        <@vrtxImageRef.printPropertyEditView 
          title=localizedTitle
          inputFieldName=elem.name 
          value=elem.value 
          baseFolder=resourceContext.parentURI
          classes=elem.name 
          tooltip=form.resource.getLocalizedTooltip(elem.name,locale)
          />
        <#break>          
      <#case "media_ref">
        <@vrtxMediaRef.printPropertyEditView 
          title=localizedTitle
          inputFieldName=elem.name 
          value=elem.value 
          baseFolder=resourceContext.parentURI
          classes=elem.name  
          tooltip=form.resource.getLocalizedTooltip(elem.name,locale)
          />
        <#break>
      <#case "datetime">
       <@vrtxDateTime.printPropertyEditView 
        title=localizedTitle
        inputFieldName=elem.name 
        value=elem.value 
        classes=elem.name  
        tooltip=form.resource.getLocalizedTooltip(elem.name,locale)
        />
        <#break>
      <#case "json">
       <@vrtxJSON.printPropertyEditView 
        title=localizedTitle
        inputFieldName=elem.name
        elem=elem
        id=elem.name  
        tooltip=form.resource.getLocalizedTooltip(elem.name,locale)
        />
        <#break>
      <#default>
        No editor available for element type ${elem.description.type}
    </#switch>
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
