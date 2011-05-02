<#ftl strip_whitespace=true>
<#--
  - File: editor.ftl
  - 
  - Required model data:
  -  
  -  ckeditorBase.url
  -  ckSource.getURL
  -  ckCleanup.url
  -  ckBrowse.url
  -
  -->
<#import "/lib/ping.ftl" as ping />
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/autocomplete.ftl" as autocomplete />
<#import "/lib/editor/common.ftl" as editor />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Editor</title>
    <@ping.ping url=pingURL['url'] interval=300 />    
    <@editor.addCkScripts />

    <script type="text/javascript" src="${jsBaseURL?html}/tooltip.js"></script>
    
  	<script type="text/javascript" src="${jsBaseURL?html}/plugins/shortcut.js"></script>
    <script type="text/javascript" src="${jsBaseURL?html}/admin-ck-helper.js"></script>
    <script type="text/javascript" src="${jsBaseURL?html}/admin-prop-change.js"></script>
    
    <#assign language = vrtx.getMsg("eventListing.calendar.lang", "en") />
    <#assign isCollection = resource.resourceType = 'collection' || resource.resourceType?contains("-listing")/>

    <script type="text/javascript">
    <!--
   	  shortcut.add("Ctrl+S",function() {
 		$("#saveButton").click();
 	  });
    
      $(document).ready(function() {
          <#if !isCollection>
          interceptEnterKey('#resource\\.tags');
          setAutoComplete('resource\\.tags', 'tags', {minChars:1});
          </#if>
          initDatePicker("${language}");
       });

      UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.unsavedChangesConfirmation' />";
      COMPLETE_UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.completeUnsavedChangesConfirmation' />";
      window.onbeforeunload = unsavedChangesInEditorMessage;
      
      function performSave() {
        NEED_TO_CONFIRM = false;
      } 
     
    //-->
    </script>
    <script type="text/javascript" src="${jsBaseURL?html}/imageref.js"></script>
    
    <@editor.addDatePickerScripts true />
    
    <#if !isCollection>
    <@autocomplete.addAutoCompleteScripts srcBase="${webResources?html}"/>
    <#else>
    <script type="text/javascript" src="${jsBaseURL?html}/collectionlisting/manually-approve.js"></script>
    </#if>
    
   <#global baseFolder = "/" />
   <#if resourceContext.parentURI?exists>
    <#if isCollection>
      <#global baseFolder = resourceContext.currentURI?html />
    <#else>
      <#global baseFolder = resourceContext.parentURI?html />
    </#if>
   </#if>

  </head>
  <body onLoad="loadFeaturedArticles('${vrtx.getMsg("editor.add")}','${vrtx.getMsg("editor.remove")}','${vrtx.getMsg("editor.browse")}',
                  '${fckeditorBase.url?html}', '${baseFolder}', '${fckBrowse.url.pathRepresentation}');">

    <#assign header>
      <@vrtx.msg code="editor.edit" args=[vrtx.resourceTypeName(resource)?lower_case] />
    </#assign>
    <h2>${header}</h2>
	  <div class="submit-extra-buttons">
	  	<#include "/system/help.ftl" />
		<input type="button" onClick="$('#saveAndViewButton').click()" value="${vrtx.getMsg("editor.saveAndView")}" />
		<input type="button" onClick="$('#saveButton').click()"  value="${vrtx.getMsg("editor.save")}" />
		<input type="button" onClick="$('#cancel').click()"  value="${vrtx.getMsg("editor.cancel")}" />
	  </div>
    <form id="form" class="editor" action="" method="post">

      <@handleProps />

      <div class="properties">
        <@propsForm resource.preContentProperties />
      </div>

      <#if (resource.content)?exists>
      <div class="html-content">
      <label class="resource.content" for="resource.content"><@vrtx.msg code="editor.content" /></label> 
       <textarea name="resource.content" rows="8" cols="60" id="resource.content">${resource.bodyAsString?html}</textarea>
       <@editor.createEditor  'resource.content' true false />
      </div>
      </#if>

      <div class="properties">
        <@propsForm resource.postContentProperties />
      </div>
      
      <#-- Margin-bottom before save- cancel button for collection, event- or articlelisting -->
      <#if (resource.resourceType = 'event-listing' ||
               resource.resourceType = 'article-listing' || resource.resourceType = 'collection')>
      	<div id="allowedValues"></div>
      </#if>

      <div id="submit" class="save-cancel">
          <input type="submit" id="saveAndViewButton" onClick="formatFeaturedArticlesData();performSave();" name="saveview"  value="${vrtx.getMsg("editor.saveAndView")}">
          <input type="submit" id="saveButton" onClick="formatFeaturedArticlesData();performSave();" name="save" value="${vrtx.getMsg("editor.save")}">
          <input type="submit" id="cancel" onClick="performSave();" name="cancel" value="${vrtx.getMsg("editor.cancel")}">
      </div>

      <#--
      <#if (resource.content)?exists>
      <script type="text/javascript">
        <!--
          disableSubmit();
        // -->
      </script>
      <#-- <#else> -->
      <script type="text/javascript">
        <!--
          enableSubmit();
        // -->
      </script>
      <#-- </#if> -->
     </form>
    </body>
</html>

<#macro propChangeTests propDefs>
  <#list propDefs as propDef>
    <#local name = propDef.name />
    <#local value = resource.getValue(propDef) />

    <#local type = propDef.type />

    <#if type = 'HTML' && name='userTitle' && isCollection>
      <#local value = resource.title />
    </#if>
		
    <#if type = 'HTML'>
      var fck_${name} = CKEDITOR.instances.resource.${name};
	  	  
      if (fck_${name} && fck_${name}.editor.checkDirty()) {
        return true;
      } else if (!fck_${name} && '${value?js_string}' != document.getElementById('resource.${name}').value) {
        return true;
      }
    <#elseif type = 'DATE' || type = 'TIMESTAMP'>
      <#local dateVal = value />
      <#local hours = "" />
      <#local minutes = "" />
      <#if value != "">
        <#local d = resource.getProperty(propDef) />
        
        <#local dateVal = d.getFormattedValue('yyyy-MM-dd', springMacroRequestContext.getLocale()) />
        <#local hours = d.getFormattedValue('HH', springMacroRequestContext.getLocale()) />
        <#local minutes = d.getFormattedValue('mm', springMacroRequestContext.getLocale()) />
        <#if hours = "00" && minutes = "00">
          <#local hours = "" />
          <#local minutes = "" />
        </#if>
      </#if>

      if (document.getElementById('resource.${name}').value != null  && '${dateVal}' != document.getElementById('resource.${name}').value) {
        return true;
      }
      if (document.getElementById('resource.${name}.hours') != null  && '${hours}' != document.getElementById('resource.${name}.hours').value) {
        return true;
      }
      if (document.getElementById('resource.${name}.minutes') != null && '${minutes}' != document.getElementById('resource.${name}.minutes').value) {
        return true;
      }            
    <#else>
      <#if !(propDef.vocabulary)?exists><#--XXX we don't handle changes to properties with vocabularies for now-->
      if (document.getElementById('resource.${name}') != null && '${value?js_string}' != document.getElementById('resource.${name}').value) {
        return true;
      }
      </#if>

    </#if>
  </#list>
</#macro>

<#macro handleProps>
  <script type="text/javascript"><!--
    function propChange() {
      <@propChangeTests resource.preContentProperties />
      <@propChangeTests resource.postContentProperties />
      return false;
    }

  <#--assign url = unlockURL['url']?default("") />
  <#if url != "">
    function doUnlock(event) {
      if (needToConfirm) {
        var req;
        if (window.XMLHttpRequest) {
          req = new XMLHttpRequest();
        } else if (window.ActiveXObject) {
          req = new ActiveXObject("Microsoft.XMLHTTP");
        }
        var url = '${url}';
        if (req != null) {
          req.open('GET', url, false);
          req.send(null);
        }
      }
    }
	
    window.onunload = doUnlock;
   </#if-->
    // -->
  </script>
</#macro>

<#macro propsForm propDefs>
    <#local locale = springMacroRequestContext.getLocale() />

    <#list propDefs as propDef>
      <#local name = propDef.name />
      <#local localizedName = propDef.getLocalizedName(locale) />

      <#local value = resource.getValue(propDef) />

      <#local description = propDef.getDescription(locale)?default("") />

      <#local type = propDef.type />
      <#local error = resource.getError(propDef)?default('') />

      <#local useRadioButtons = false />
      <#if ((propDef.metadata.editingHints.radio)?exists)>
        <#local useRadioButtons = true />
      </#if>          

      <#local displayLabel = true />
      <#if ((propDef.metadata.editingHints.hideLabel)?exists)>
        <#local displayLabel = false />
      </#if>
      
      <div id="vrtx-resource.${name}" class="${name} property-item">
      <#if displayLabel>
        <div class="resource.${name} property-label">${localizedName}</div> 
      </#if>
      
      <#if type = 'HTML' && name != 'userTitle' && name != 'title' && name != 'caption'>

        <textarea id="resource.${name}" name="resource.${name}" rows="4" cols="60">${value?html}</textarea>
        <@editor.createEditor  'resource.${name}' false false />
        
      <#elseif type = 'HTML' && name == 'caption'>
        <textarea id="resource.${name}" name="resource.${name}" rows="1" cols="60">${value?html}</textarea>
        <@editor.createEditor 'resource.${name}' false true />
        </div><#-- On the fly div STOP caption -->

      <#-- hack for setting collection titles: -->
      <#elseif (type = 'HTML' && name='userTitle') || (type = 'STRING' && name='navigationTitle')
        && (resource.resourceType = 'event-listing' || resource.resourceType = 'article-listing' || resource.resourceType = 'collection' ||
            resource.resourceType = 'person-listing' || resource.resourceType = 'project-listing' || resource.resourceType = 'research-group-listing' ||
            resource.resourceType = 'blog-listing' || resource.resourceType = 'image-listing' || resource.resourceType = 'master-listing')>
  
        <#if name == 'navigationTitle'>
          <a class="show-tooltip" href="#" title="<@vrtx.msg code='editor.tooltip.navigation-title'/>">__</a>
        </#if>

        <#if value = '' && name='userTitle'>
          <#local value = resource.title?html />
        </#if>
        <input type="text" id="resource.${name}" name="resource.${name}" value="${value?html}" size="32" />
        <#if description != "">
          <span class="input-description">(${description})</span>
        </#if>

      <#elseif name = 'media'>
        <input type="text" id="resource.${name}"  name="resource.${name}" value="${value?html}" />
        <button type="button" onclick="browseServer('resource.${name}', '${fckeditorBase.url?html}', '${baseFolder}',
              '${fckBrowse.url.pathRepresentation}', 'Media');"><@vrtx.msg code="editor.browseMediaFiles"/></button>
        
      <#elseif type = 'IMAGE_REF'>
        <div id="picture-and-caption">
          <div id="input-and-button-container">
            <input type="text" id="resource.${name}" onblur="previewImage(id);" name="resource.${name}" value="${value?html}" />
            <button type="button" onclick="browseServer('resource.${name}', '${fckeditorBase.url?html}', '${baseFolder}',
              '${fckBrowse.url.pathRepresentation}');"><@vrtx.msg code="editor.browseImages"/></button>
          </div>
          <div id="resource.${name}.preview">
          
            <#local thumbnail = '' />
    		<#if value?exists && value != "">
    			<#if  vrtx.linkConstructor(value, 'displayThumbnailService')?exists >
					<#local thumbnail = vrtx.linkConstructor(value, 'displayThumbnailService').getPathRepresentation() />
				<#else>
					<#local thumbnail = value />
				</#if> 
    		</#if>          
            <#if thumbnail != ''>
              <img src="${thumbnail?html}" alt="preview" />
            <#else>
              <img src="" alt="no-image" style="visibility: hidden; width: 10px;" />
            </#if>
          </div>
      <#elseif type = 'DATE' || type = 'TIMESTAMP'>

        <#local dateVal = value />
        <#local hours = "" />
        <#local minutes = "" />
        <#if value != "">
          <#local d = resource.getProperty(propDef) />

          <#local dateVal = d.getFormattedValue('yyyy-MM-dd', springMacroRequestContext.getLocale()) />
          <#local year = d.getDateValue()?string("yyyy") />
          <#local month = d.getDateValue()?string("MM") />
          <#local date = d.getDateValue()?string("dd") />
          <#local hours = d.getFormattedValue('HH', springMacroRequestContext.getLocale()) />
          <#local minutes = d.getFormattedValue('mm', springMacroRequestContext.getLocale()) />
          <#if hours = "00" && minutes = "00">
            <#local hours = "" />
            <#local minutes = "" />
          </#if>
        </#if>

        <#local uniqueName = 'cal_' + propDef_index />
        
        <input size="10" maxlength="10" type="text" class="date" id="resource.${name}" name="resource.${name}.date"
            value="${dateVal}" />
        
        <input size="2" maxlength="2" type="text" class="hours" id="resource.${name}.hours" name="resource.${name}.hours"
            value="${hours}"><span class="colon">:</span><input size="2" maxlength="2" type="text" class="minutes"
            id="resource.${name}.minutes" name="resource.${name}.minutes" value="${minutes}">

      <#else>
      
        <#if (propDef.vocabulary)?exists>
          <#local allowedValues = propDef.vocabulary.allowedValues />

          <#if allowedValues?size = 1 && !useRadioButtons>

            <#if type = 'BOOLEAN' && !displayLabel>
              <#if value == allowedValues[0]>
                <input name="resource.${name}" id="resource.${name}.${allowedValues[0]?html}" type="checkbox" value="${allowedValues[0]?html}" checked="checked" />
              <#else>
                <input name="resource.${name}" id="resource.${name}.${allowedValues[0]?html}" type="checkbox" value="${allowedValues[0]?html}" />
              </#if>
              <label class="resource.${name}" for="resource.${name}.${allowedValues[0]?html}">${localizedName}</label>
            <#else>
              <label class="resource.${name}">${allowedValues[0]?html}</label>
              <#if value == allowedValues[0]>
                <input name="resource.${name}" id="resource.${name}.${allowedValues[0]?html}" type="checkbox" value="${allowedValues[0]?html}" checked="checked" />
              <#else>
                <input name="resource.${name}" id="resource.${name}.${allowedValues[0]?html}" type="checkbox" value="${allowedValues[0]?html}"/>
              </#if>
            </#if>

          <#elseif useRadioButtons>
          
            <#-- Special case for recursive listing... Jesus Christ... -->            
            <#if name == 'recursive-listing'>
              <@displayAllowedValuesAsRadioButtons propDef name allowedValues value />
              <@displayDefaultSelectedValueAsRadioButton propDef name />
            <#else>
              <@displayDefaultSelectedValueAsRadioButton propDef name />
              <@displayAllowedValuesAsRadioButtons propDef name allowedValues value />
            </#if>
            
          <#else>
            <select name="resource.${name}" id="resource.${name}">
              <#if !propDef.mandatory>
              <#attempt>
                <#local nullValue = propDef.valueFormatter.valueToString(nullArg, "localized", springMacroRequestContext.getLocale()) />
              <#recover>
                <#local nullValue = 'unspecified' />
              </#recover>
                
                <option value="">${nullValue?html}</option>
              </#if>
              <#list allowedValues as v>
                <#local localized = propDef.getValueFormatter().valueToString(v, 'localized', springMacroRequestContext.getLocale()) />
                <#if v == value>
                  <option selected="selected" value="${v?html}">${localized?html}</option>
                <#else>
                  <option value="${v?html}">${localized?html}</option>
                </#if>
              </#list>
            </select>
          </#if>
        <#else>

          <#if name = 'recursive-listing-subfolders'>
            <label>${vrtx.getMsg("editor.recursive-listing.featured-articles")}</label>
          </#if>

          <input type="text" id="resource.${name}" name="resource.${name}" value="${value?html}" size="32" /> 

          <#if name = 'recursive-listing-subfolders'>
            <label>${vrtx.getMsg("editor.recursive-listing.featured-articles.hint")}</label>
          </#if>
          <#if description != "">
            <span class="input-description">(${description})</span>
          </#if>
          
          <#if name = 'manually-approve-from'>
            <div id="manually-approve-container-title"><button id="manually-approve-refresh" href=".">Oppdater liste</a></button></div>
            <div id="manually-approve-container">
            </div>
          </#if>
        
        </#if>
      </#if>
      <#if error != ""><span class="error">${error}</span></#if> 
    </div>
    </#list>
</#macro>

<#macro displayDefaultSelectedValueAsRadioButton propDef name >
  <#if !propDef.mandatory>
    <#attempt>
      <#local nullValue = propDef.valueFormatter.valueToString(nullArg, "localized", springMacroRequestContext.getLocale()) />
    <#recover>
      <#local nullValue = 'unspecified' />
    </#recover>
    <#if !(resource.getProperty(propDef))?exists>
      <input name="resource.${name}" id="resource.${name}.unspecified" type="radio" value="" checked="checked" />
      <label class="resource.${name}" for="resource.${name}.unspecified">${nullValue?html}</label><br />
    <#else>
      <input name="resource.${name}" id="resource.${name}.unspecified" type="radio" value="" />
      <label class="resource.${name}" for="resource.${name}.unspecified">${nullValue?html}</label><br />
    </#if>
  </#if>
</#macro>

<#macro displayAllowedValuesAsRadioButtons propDef name allowedValues value >
  <#list allowedValues as v>
    <#local localized = v />
    <#if (propDef.valueFormatter)?exists>
      <#local localized = propDef.valueFormatter.valueToString(v, "localized", springMacroRequestContext.getLocale()) />
    </#if>
    <#if v == value>
      <input name="resource.${name}" id="resource.${name}.${v?html}" type="radio" value="${v?html}" checked="checked" />
      <label class="resource.${name}" for="resource.${name}.${v?html}">${localized?html}</label><br />
    <#else>
      <input name="resource.${name}" id="resource.${name}.${v?html}" type="radio" value="${v?html}" />
      <label class="resource.${name}" for="resource.${name}.${v?html}">${localized?html}</label><br />
    </#if>
  </#list>
</#macro>
