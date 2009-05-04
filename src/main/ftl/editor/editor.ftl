<#ftl strip_whitespace=true>
<#--
  - File: editor.ftl
  - 
  - Required model data:
  -  
  -  fckeditorBase.url
  -  fckSource.getURL
  -  fckCleanup.url
  -  fckBrowse.url
  -  
  - Optional model data:
  -
  -  yuiBase.url
  -  autoCompleteBaseURL
  -
  -->
<#import "/lib/ping.ftl" as ping />
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/autocomplete.ftl" as autocomplete />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Editor</title>
    <@ping.ping url=pingURL['url'] interval=300 />    
    
    <!-- Main fck-editor js -->
    <script language="Javascript" type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
    <@setupFckEditor resource.resourceType />

    <!-- Yahoo YUI library: -->
    <link rel="stylesheet" type="text/css" href="${yuiBase.url?html}/build/calendar/assets/skins/sam/calendar.css" />
    
    <script language="Javascript" type="text/javascript" src="${yuiBase.url?html}/build/yahoo-dom-event/yahoo-dom-event.js"></script>
    <script language="Javascript" type="text/javascript" src="${yuiBase.url?html}/build/calendar/calendar-min.js"></script>
    <script language="Javascript" type="text/javascript" src="${jsBaseURL}/tooltip.js"></script>
    <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/imageref.js"></script>
    <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/serverbrowsedialog.js"></script>
    
    <@autocomplete.addAutoCompleteScripts srcBase="${yuiBase.url?html}"/>

    <!--[if IE 6]>
      <style type="text/css">
        div.start-date {
          padding-bottom: 1em;
        }
      </style>
    <![endif]-->

    <!--[if IE 7]>
     <style type="text/css">
         div.properties div.start-date, div.properties div.end-date, div.properties div.location {
           clear: both;
         }

         div.yui-calcontainer { 
           margin-left: 0em;
           margin-top: -16.5em; 
         }
        </style>
    <![endif]-->
    
  </head>
  
  <#assign baseFolder = "/" />
  <#if resourceContext.parentURI?exists>
    <#if resource.resourceType = 'article-listing' || resource.resourceType = 'event-listing'
         || resource.resourceType = 'collection'>
      <#assign baseFolder = resourceContext.currentURI?html />
    <#else>
      <#assign baseFolder = resourceContext.parentURI?html />
    </#if>
  </#if>
  
  <body onLoad="loadFeaturedArticles('${vrtx.getMsg("editor.add")}','${vrtx.getMsg("editor.remove")}','${vrtx.getMsg("editor.browse")}',
                  '${fckeditorBase.url?html}', '${baseFolder}', '${fckBrowse.url.pathRepresentation}');">
    <#assign header>
      <@vrtx.msg code="editor.edit" args=[resource.resourceTypeDefinition.getLocalizedName(springMacroRequestContext.getLocale())?lower_case] />
    </#assign>
    <h2>${header}</h2>

    <form id="form" class="editor" action="" method="post">
      <@handleProps />

      <div class="properties">
        <a id="help-link" href="${editorHelpURL?html}" target="new_window"><@vrtx.msg code="editor.help"/></a>
        <@propsForm resource.preContentProperties />
      </div>

      <#if (resource.content)?exists>
      <div class="html-content">
      <label class="resource.content" for="resource.content"><@vrtx.msg code="editor.content" /></label> 
       <textarea name="resource.content" rows="8" cols="60" id="resource.content">${resource.bodyAsString?html}</textarea>
       <@fck 'resource.content' true false />

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
          <input type="submit" id="saveButton" onClick="formatFeaturedArticlesData();cSave();" name="save" value="${vrtx.getMsg("editor.save")}">
          <input type="submit" id="saveAndQuitButton" onClick="formatFeaturedArticlesData();performSave();" name="savequit"  value="${vrtx.getMsg("editor.saveAndQuit")}">
          <input type="submit" onClick="performSave();" name="cancel" value="${vrtx.getMsg("editor.cancel")}">
      </div>

      <#if (resource.content)?exists>
      <script language="Javascript" type="text/javascript">
        <!--
          disableSubmit();
        // -->
      </script>
      <#else>
      <script language="Javascript" type="text/javascript">
        <!--
          enableSubmit();
        // -->
      </script>
      </#if>
     </form>
    </body>
</html>

<#macro setupFckEditor resourceType>
    <script language="Javascript" type="text/javascript">
      <!--
      function newEditor(name, completeEditor, withoutSubSuper) {

        var completeEditor = completeEditor != null ? completeEditor : false;
        var withoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false; 
        
        var fck = new FCKeditor( name ,'100%', 400) ;
        fck.BasePath = "${fckeditorBase.url?html}/";

        fck.Config['DefaultLanguage'] = '<@vrtx.requestLanguage />';

        fck.Config['CustomConfigurationsPath'] = '${fckeditorBase.url?html}/custom-fckconfig.js';

         if (completeEditor) {
            <#if resourceType = 'article' || resourceType = 'event'  >
              fck.ToolbarSet = 'Complete-article';
            <#else>
              fck.ToolbarSet = 'Complete'; 
            </#if> 
         } else {
            fck.ToolbarSet = 'Inline';
         }
     
         if(withoutSubSuper) {
           fck.ToolbarSet = 'Inline-S';
         }

         // File browser
         <#if resourceContext.parentURI?exists>
            <#if resourceType = 'article-listing' || resourceType = 'event-listing' || resourceType = 'collection'>              
              var baseFolder = "${resourceContext.currentURI?html}";
            <#else>
              var baseFolder = "${resourceContext.parentURI?html}";
            </#if>
         <#else>
         	var baseFolder = "/";
         </#if>
         fck.Config['LinkBrowserURL']  = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['ImageBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['FlashBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=${fckBrowse.url.pathRepresentation}';

         fck.Config.LinkUpload = false;
         fck.Config.ImageUpload = false;
         fck.Config.FlashUpload = false;

         // Misc setup
         fck.Config['FullPage'] = false;
         fck.Config['ToolbarCanCollapse'] = false;
         fck.Config['TabSpaces'] = 4;
         <#if resource.resourceTypeDefinition.name == 'xhtml10trans'>
           fck.Config['FontFormats'] = 'p;h1;h2;h3;h4;h5;h6;pre';
         <#else>
           fck.Config['FontFormats'] = 'p;h2;h3;h4;h5;h6;pre';
         </#if>

         fck.Config.EMailProtection = 'none';
         fck.Config.DisableFFTableHandles = false;
	 fck.Config.ForcePasteAsPlainText = false;

         fck.Config['SkinPath'] = fck.BasePath + 'editor/skins/silver/';
         fck.Config.BaseHref = '${fckeditorBase.documentURL?html}';

         var cssFileList = new Array(
         <#if fckEditorAreaCSSURL?exists>
           <#list fckEditorAreaCSSURL as cssURL>
             "${cssURL?html}",
           </#list>
         </#if>
         "");

         /* Fix for div contianer display in ie */
         var browser = navigator.userAgent;
         var ieversion = new Number(RegExp.$1)
         if(browser.indexOf("MSIE") > -1 && ieversion <= 7){
           cssFileList[cssFileList.length-1] = "/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css";
         }

         fck.Config['EditorAreaCSS'] = cssFileList;
         fck.ReplaceTextarea();
      }

      function FCKeditor_OnComplete(editorInstance) {
        // Get around bug: http://dev.fckeditor.net/ticket/1482
        editorInstance.ResetIsDirty();
        if ('resource.content' == editorInstance.Name) {
          enableSubmit();
        }
      }

      function disableSubmit() {
        document.getElementById("saveButton").disabled = true;
        document.getElementById("saveAndQuitButton").disabled = true;
        return true;
      }

      function enableSubmit() {
         document.getElementById("saveButton").disabled = false;
         document.getElementById("saveAndQuitButton").disabled = false;
         return true;
      }

      // -->
  </script>
</#macro>

<#macro propChangeTests propDefs>
  <#list propDefs as propDef>
    <#local name = propDef.name />
    <#local value = resource.getValue(propDef) />

    <#local type = propDef.type />

    <#if type = 'HTML' && name='userTitle' && (resource.resourceType = 'event-listing' ||
         resource.resourceType = 'article-listing' || resource.resourceType = 'collection')>
      <#local value = resource.title />
    </#if>
      

    <#if type = 'HTML'>
      var fck_${name} = FCKeditorAPI.GetInstance('resource.${name}');

      if (fck_${name} && fck_${name}.IsDirty()) {
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
  <script language="Javascript" type="text/javascript"><!--
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

    function doConfirm(event) {

    var f = propChange();
      if (!needToConfirm) {
        return;
      }
      var dirty = false;
      if (propChange()) dirty = true;
      <#if (resource.resourceType != 'event-listing' &&
         resource.resourceType != 'article-listing' && resource.resourceType != 'collection')>
      if (FCKeditorAPI.GetInstance('resource.content') .IsDirty()) dirty = true;
      </#if>
      
      if (dirty) {
        return '<@vrtx.msg code='manage.unsavedChangesConfirmation' />';
      }
    }

    window.onbeforeunload = doConfirm;
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
        <@fck 'resource.${name}' false false />
        
      <#elseif type = 'HTML' && name == 'caption'>
        <textarea id="resource.${name}" name="resource.${name}" rows="1" cols="60">${value?html}</textarea>
        <@fck 'resource.${name}' false true />
        </div><#-- On the fly div STOP caption -->

      <#-- hack for setting collection titles: -->
      <#elseif (type = 'HTML' && name='userTitle') || (type = 'STRING' && name='navigationTitle')
        && (resource.resourceType = 'event-listing' || resource.resourceType = 'article-listing' || resource.resourceType = 'collection')>
  
        <#if name == 'navigationTitle'>
               <a class="show-tooltip" href="#" title="<@vrtx.msg code='editor.tooltip.navigation-title'/>">____</a>
        </#if>

        <#if value = '' && name='userTitle'>
          <#local value = resource.title?html />
        </#if>
        <input type="text" id="resource.${name}" name="resource.${name}" value="${value?html}" size="32" />
        <#if description != "">
          <span class="input-description">(${description})</span>
        </#if>

      <#elseif name = 'media'>
        <input type="text" id="resource.${name}"  name="resource.${name}" value                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     ="${value?html}" />
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
            <#if value != ''>
              <img src="${value}" alt="preview" />
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
            value="${dateVal}" onblur="YAHOO.resource.${uniqueName}.calendar.cal1.syncDates()">
        
        <a class="calendar" id="${uniqueName}.calendar.href"><span>cal</span></a>
        <div id="resource.${name}.calendar" class="yui-skin-sam"></div>
        
        <input size="2" maxlength="2" type="text" class="hours" id="resource.${name}.hours" name="resource.${name}.hours"
            value="${hours}"><span class="colon">:</span><input size="2" maxlength="2" type="text" class="minutes"
            id="resource.${name}.minutes" name="resource.${name}.minutes" value="${minutes}">

        <script language="Javascript" type="text/javascript">
        <!--

          YAHOO.namespace("resource.${uniqueName}.calendar");
          var cal1 = YAHOO.resource.${uniqueName}.calendar.cal1;
          if (!cal1) {
            cal1 = YAHOO.resource.${uniqueName}.calendar.cal1 = 
            new YAHOO.widget.Calendar("cal1", "resource.${name}.calendar");
          }

          cal1.cfg.setProperty("iframe", true);
          <#if value != "">
            cal1.cfg.setProperty("selected", "${month}/${date}/${year}", false);
            cal1.cfg.setProperty("pagedate", "${month}/${year}", false);
          </#if>
          cal1.render();

          cal1.selectEvent.subscribe(function(type, dates) {
            var date = this._toDate(dates[0][0]);
            var year = date.getFullYear();
            var monthNumber = date.getMonth() + 1;
            var month = monthNumber < 10 ? '0' + monthNumber  : '' + monthNumber;
            var day = date.getDate(); if (day < 10 ) day = '0' + day;
            var dateStr =  year + '-' + month + '-' + day;

            document.getElementById('resource.${name}').value = dateStr;
            ${uniqueName}_hide();
          }, cal1, true);


          cal1.syncDates = function() {
            var input = document.getElementById('resource.${name}').value;
            var regexp = /(\d+)\-(\d\d)-(\d\d)/;
            var match = regexp.exec(input);
             
            if (match) {
              var d = new Date();
              d.setFullYear(match[1]);
              d.setMonth(match[2]);
              d.setDate(match[3]);

              this.cfg.setProperty("selected", d.getMonth() + "/" + d.getDate() + "/" + d.getFullYear(), false);
              this.cfg.setProperty("pagedate", d.getMonth() + "/" + d.getFullYear(), false);
              this.render();
            }
          }

          var ${uniqueName}_hidden = true;

          function ${uniqueName}_show() {
            var cal1 = YAHOO.resource.${uniqueName}.calendar.cal1;
            cal1.show();
            ${uniqueName}_hidden = false;
          }

          function ${uniqueName}_hide() {
            var cal1 = YAHOO.resource.${uniqueName}.calendar.cal1;
            cal1.hide();
            ${uniqueName}_hidden = true;
          }

          function ${uniqueName}_toggle() {
            if (${uniqueName}_hidden) {
              ${uniqueName}_show();
            } else {
              ${uniqueName}_hide();
            }
          }

          function ${uniqueName}_click(e) {
            if (!e) var e = window.event;
            var target;
            if (e.target) {
              target = e.target;
            } else if (e.srcElement) {
              target = e.srcElement;
            }
            var inCalendarRef = "${uniqueName}.calendar.href" == target.id;
            var inCalendar = false;
              
            for (t = target; t; t = t.parentNode) {
              if ("resource.${name}.calendar" == t.id) {
                inCalendar = true;
                  break;
              }
            }

            if (inCalendarRef) {
              ${uniqueName}_toggle();
            } else if (!${uniqueName}_hidden && !inCalendarRef && !inCalendar) {
              ${uniqueName}_hide();
            } else return true;
          }

          if (window.addEventListener) {
            window.addEventListener("click", ${uniqueName}_click, false);
          } else if (window.attachEvent) {
            document.attachEvent('onclick', ${uniqueName}_click);
          }
        //-->
        </script>
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

          <#-- AutoComplete only for the tags inputfield -->
          <#if name = 'tags'>
            <@autocomplete.createAutoCompleteInputField appSrcBase="${autoCompleteBaseURL}" fieldName="${name}" 
                    description="${description}" value="${value?html}" width="18" schema=["text"]/>
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
          </#if>
        
        </#if>
      </#if>
      <#if error != ""><span class="error">${error}</span></#if> 
    </div>
    </#list>
</#macro>

<#macro fck content completeEditor=false withoutSubSuper=false>
    <script language="Javascript" type="text/javascript"><!--
      var needToConfirm = true;

      newEditor('${content}', ${completeEditor?string}, ${withoutSubSuper?string});
      
      function cSave() {
        document.getElementById("form").setAttribute("action", "#submit");
        performSave();
      }

      function performSave() {
        needToConfirm = false;
        var oEditor = FCKeditorAPI.GetInstance('${content}');
        var srcxhtml = oEditor.GetXHTML();
        // var title = document.getElementById("title");

        // Title
        <#-- // srcxhtml = srcxhtml.replace(/<title.*<\/title>/i, "<title>" + title.value + "</title>"); -->
        document.getElementById('${content}').value = srcxhtml;
      }  
      // -->
    </script>

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
