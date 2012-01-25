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
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/autocomplete.ftl" as autocomplete />
<#import "/lib/editor/common.ftl" as editor />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Editor</title>
    <@editor.addCkScripts />

    <script type="text/javascript" src="${jsBaseURL?html}/plugins/shortcut.js"></script>
    
    <#assign language = vrtx.getMsg("eventListing.calendar.lang", "en") />
    <#assign isCollection = resource.resource.collection />
    <#assign simpleHTML = resource.resourceType = 'xhtml10trans' || resource.resourceType = 'html' />

    <#global baseFolder = "/" />
    <#if resourceContext.parentURI?exists>
      <#if isCollection>
        <#global baseFolder = resourceContext.currentURI?html />
      <#else>
        <#global baseFolder = resourceContext.parentURI?html />
      </#if>
     </#if>

    <script type="text/javascript">
    <!--
    
     shortcut.add("Ctrl+S",function() {
       $(".vrtx-focus-button:last input").click();
     });
     
     $(window).load(function() {
       if($("#resource\\.featured-articles").length) {
         loadMultipleDocuments(true, "resource\\.featured-articles", true, '${vrtx.getMsg("editor.add")}','${vrtx.getMsg("editor.remove")}','${vrtx.getMsg("editor.browse")}',
                             '${fckeditorBase.url?html}', '${baseFolder}', '${fckBrowse.url.pathRepresentation}');
       }   
       if($("#resource\\.aggregation").length) {                  
         loadMultipleDocuments(true, "resource\\.aggregation", false, '${vrtx.getMsg("editor.add")}','${vrtx.getMsg("editor.remove")}','${vrtx.getMsg("editor.browse")}',
                             '${fckeditorBase.url?html}', '${baseFolder}', '${fckBrowse.url.pathRepresentation}');
       } 
       if($("#resource\\.manually-approve-from").length) {                
         loadMultipleDocuments(false, "resource\\.manually-approve-from", false, '${vrtx.getMsg("editor.add")}','${vrtx.getMsg("editor.remove")}','${vrtx.getMsg("editor.browse")}',
                             '${fckeditorBase.url?html}', '${baseFolder}', '${fckBrowse.url.pathRepresentation}');
       }
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
      
      var cssFileList = [
      <#if fckEditorAreaCSSURL?exists>
        <#list fckEditorAreaCSSURL as cssURL>
          "${cssURL?html}" <#if cssURL_has_next>,</#if>
        </#list>
      </#if>];
      
      // Fix for div container display in IE
      if (vrtxAdmin.isIE && vrtxAdmin.browserVersion <= 7) {
       cssFileList.push("/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css");
      }
     
    //-->
    </script>

    <@editor.addDatePickerScripts language true />
    
    <#if !isCollection>
      <@autocomplete.addAutoCompleteScripts srcBase="${webResources?html}"/>
    <#else>
      <script type="text/javascript" src="${jsBaseURL?html}/collectionlisting/manually-approve.js"></script>
    </#if>

  </head>
  <body>
    <#assign header>
      <@vrtx.msg code="editor.edit" args=[vrtx.resourceTypeName(resource)?lower_case] />
    </#assign>
    <h2>${header}</h2>
    <div class="submitButtons submit-extra-buttons">
      <div class="vrtx-button">
        <input type="button" onclick="$('#saveAndViewButton').click()" value="${vrtx.getMsg("editor.saveAndView")}" />
      </div>
      <div class="vrtx-focus-button">
        <input type="button" onclick="$('#saveButton').click()"  value="${vrtx.getMsg("editor.save")}" />
      </div>
      <div class="vrtx-button">
        <input type="button" onclick="$('#cancel').click()"  value="${vrtx.getMsg("editor.cancel")}" />
      </div>
      <@genEditorHelpMenu />
    </div>
    <form action="" method="post" id="editor">

      <div class="properties"<#if (resource.contentType?exists && resource.contentType?starts_with("image"))> id="image-properties"</#if>>
        <@propsForm resource.preContentProperties />
      </div>

      <#if (resource.content)?exists>
        <div class="html-content">
          <label class="resource.content" for="resource.content"><@vrtx.msg code="editor.content" /></label> 
          <textarea name="resource.content" rows="7" cols="60" id="resource.content">${resource.bodyAsString?html}</textarea>
          <@editor.createEditor  'resource.content' true false simpleHTML />
        </div>
      </#if>

      <div class="properties">
        <@propsForm resource.postContentProperties />
      </div>
 
     <#if resource.contentType?exists && saveImageURL?exists
          && (resource.contentType == "image/jpg" || resource.contentType == "image/jpeg" || resource.contentType == "image/png")>
       <#assign theContentType = resource.contentType />
       <#if theContentType == "image/jpeg">
         <#assign theContentType = "image/jpg" />
       </#if>    
       <script type="text/javascript" src="${jsBaseURL?html}/image-editor/editor.js"></script>    
       <script type="text/javascript"><!--  
         $(function () {
           var imageEditorElm = $("#vrtx-image-editor-wrapper");
           // var url = "${resourceContext.currentURI}";
           var url = location.href;
           url = url.substring(0, url.indexOf("?"));
           url = url.replace("-adm", "");
           vrtxImageEditor.url = url;
           vrtxImageEditor.imageInAsBase64 = "data:${theContentType};base64,${imageAsBase64}";
           if(imageEditorElm.length) {
             vrtxImageEditor.init(imageEditorElm);   
           }
         });
       // -->
       </script>
       <div id="vrtx-image-editor-wrapper">
         <h3 id="vrtx-image-editor-preview">Forhåndsvisning</h3><output id="vrtx-image-editor-interpolation-complete"></output>
         <div id='vrtx-image-editor-inner-wrapper'>
           <canvas id="vrtx-image-editor"></canvas>
         </div>
         <div id='vrtx-image-editor-wrapper-loading-info'>
           <canvas id='vrtx-image-editor-preview-image'></canvas>
           <div id='vrtx-image-editor-wrapper-loading-info-text'>
             <span>Behandler bilde...</span>
           </div>
         </div>
       </div>
     </#if>
      
      <#-- Margin-bottom before save- cancel button for collections -->
      <#if isCollection>
        <div id="allowedValues"></div>
      </#if>

      <div id="submit" class="submitButtons save-cancel">
        <div class="vrtx-button">
          <input type="submit" id="saveAndViewButton" onclick="formatDocumentsData();performSave();" name="saveview"  value="${vrtx.getMsg("editor.saveAndView")}">
        </div>
        <div class="vrtx-focus-button">
          <input type="submit" id="saveButton" onclick="formatDocumentsData();performSave();" name="save" value="${vrtx.getMsg("editor.save")}">
        </div>
        <div class="vrtx-button">
          <input type="submit" id="cancel" onclick="performSave();" name="cancel" value="${vrtx.getMsg("editor.cancel")}">
        </div>
      </div>

     </form>
     
     <#if resource.contentType?exists && saveImageURL?exists
          && (resource.contentType == "image/jpg" || resource.contentType == "image/jpeg" || resource.contentType == "image/png")>
       <form enctype="multipart/form-data" id="vrtx-image-editor-save-image-form" action="${saveImageURL?html}" method="post" style="display: none;">
         <@vrtx.csrfPreventionToken url=saveImageURL />
       </form>
     </#if>
     
    </body>
</html>

<#macro propsForm propDefs>
    <#local locale = springMacroRequestContext.getLocale() />

    <#list propDefs as propDef>
      <#local name = propDef.name />
      
      <#if name = "display-aggregation" || name = "display-manually-approved">
        <#local localizedName>
          <@vrtx.msg code="proptype.name.${resource.resourceType}.${name}" />
        </#local>
      <#else>
        <#local localizedName = propDef.getLocalizedName(locale) />
      </#if>

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

        <textarea id="resource.${name}" name="resource.${name}" rows="7" cols="60">${value?html}</textarea>
        <@editor.createEditor  'resource.${name}' false false simpleHTML />
        
      <#elseif type = 'HTML' && name == 'caption'>
        <textarea id="resource.${name}" name="resource.${name}" rows="7" cols="60">${value?html}</textarea>
        <@editor.createEditor 'resource.${name}' false true />
        </div><#-- On the fly div STOP caption -->

      <#-- hack for setting collection titles: -->
      <#elseif isCollection && (name='userTitle' || name='navigationTitle')>

        <#if value = '' && name='userTitle'>
          <#local value = resource.title?html />
        </#if>  
        <#if name='userTitle'>  
          <div class="vrtx-textfield-big">
        <#else>
          <div class="vrtx-textfield">
        </#if>
            <input type="text" id="resource.${name}" name="resource.${name}" value="${value?html}" size="32" />
          </div>
        <#if description != "">
          <span class="input-description">(${description})</span>
        </#if>

      <#elseif name = 'media'>
        <div class="vrtx-textfield">
          <input type="text" id="resource.${name}"  name="resource.${name}" value="${value?html}" />
        </div>
        <div class="vrtx-button">
          <button type="button" onclick="browseServer('resource.${name}', '${fckeditorBase.url?html}', '${baseFolder}',
                  '${fckBrowse.url.pathRepresentation}', 'Media');"><@vrtx.msg code="editor.browseMediaFiles"/></button>
        </div>
        
      <#elseif type = 'IMAGE_REF'>
      
        <#if name == "picture">
        <div class="picture-and-caption">
          <div class="input-and-button-container">
            <div class="vrtx-textfield">
              <input type="text" id="resource.${name}" onblur="previewImage(id);" name="resource.${name}" value="${value?html}" />
            </div>
            <div class="vrtx-button">
              <button type="button" onclick="browseServer('resource.${name}', '${fckeditorBase.url?html}', '${baseFolder}',
                      '${fckBrowse.url.pathRepresentation}');"><@vrtx.msg code="editor.browseImages"/></button>
            </div>
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
        <#else>
        
          <div class="image-ref vrtx-image-ref.${name}">
            <div class="input-and-button-container.${name}">
              <div class="vrtx-textfield">
                <input type="text" id="resource.${name}" onblur="previewImage(id);" name="resource.${name}" value="${value?html}" />
              </div>
              <div class="vrtx-button">
                <button type="button" onclick="browseServer('resource.${name}', '${fckeditorBase.url?html}', '${baseFolder}',
                  '${fckBrowse.url.pathRepresentation}');"><@vrtx.msg code="editor.browseImages"/></button>
              </div>
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
          </div>
        </#if>
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
        <div class="vrtx-textfield vrtx-date">
          <input size="10" maxlength="10" type="text" class="date" id="resource.${name}" name="resource.${name}.date" value="${dateVal}" />
        </div>
        <div class="vrtx-textfield vrtx-hours">
          <input size="2" maxlength="2" type="text" class="hours" id="resource.${name}.hours" name="resource.${name}.hours" value="${hours}" />
        </div>
          <span class="colon">:</span>
        <div class="vrtx-textfield vrtx-minutes">
          <input size="2" maxlength="2" type="text" class="minutes" id="resource.${name}.minutes" name="resource.${name}.minutes" value="${minutes}" />
        </div>

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
          <div class="vrtx-textfield">
            <input type="text" id="resource.${name}" name="resource.${name}" value="${value?html}" size="32" />
          </div>

          <#if name = 'recursive-listing-subfolders'>
            <label>${vrtx.getMsg("editor.recursive-listing.featured-articles.hint")}</label>
          </#if>
          <#if description != "">
            <span class="input-description">(${description})</span>
          </#if>

          <#if name = 'manually-approve-from'>
            <div id="manually-approve-container-title">
              <a class="vrtx-button" id="manually-approve-refresh" href="."><span>${vrtx.getMsg("editor.manually-approve-refresh")}</span></a>
            </div>
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

<#macro genEditorHelpMenu>
  <div id="editor-help-menu">
    <span id="editor-help-menu-header"><@vrtx.msg code="manage.help" default="Help" />:</span>
    <ul>
      <li> 
        <#assign lang><@vrtx.requestLanguage/></#assign>
        <#assign url = helpURL />
        <#if .vars["helpURL.editor." + lang]?exists>
          <#assign url = .vars["helpURL.editor." + lang] />
        </#if>
        <a href="${url?html}" target="_blank" class="help-link"><@vrtx.msg code="manage.help.editing" default="Help in editing" /></a>
      </li>
    </ul>
  </div>
</#macro>
