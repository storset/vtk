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
    <#assign isImage = resource.contentType?exists && resource.contentType?starts_with("image/") />
    <#assign isAudio = resource.contentType?exists && resource.contentType?starts_with("audio/") />
    <#assign isVideo = resource.contentType?exists && resource.contentType?starts_with("video/") />
    <#assign supportedImageEditor = isImage && (resource.contentType == "image/jpeg" 
                                             || resource.contentType == "image/pjpeg"
                                             || resource.contentType == "image/png") />

    <#global baseFolder = "/" />
    <#if resourceContext.parentURI?exists>
      <#if isCollection>
        <#global baseFolder = resourceContext.currentURI?html />
      <#else>
        <#global baseFolder = resourceContext.parentURI?html />
      </#if>
     </#if>

    <script type="text/javascript"><!--
      $(window).load(function() {
        initDatePicker(datePickerLang);
      });
    
      $(document).ready(function() {
        var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;
      
        // Sticky bar shortcuts
        vrtxAdm.mapShortcut("#vrtx-save-view-shortcut", "#saveAndViewButton");
        vrtxAdm.mapShortcut("#vrtx-save-copy-shortcut", "#saveCopyButton");
        vrtxAdm.mapShortcut("#vrtx-save-shortcut", "#saveButton");
        vrtxAdm.mapShortcut("#vrtx-cancel-shortcut", "#cancel");
        _$("#editor").on("click", "#cancel", function(e) {
          performSave();
        });
        _$("#editor").on("click", "#saveAndViewButton, #saveCopyButton", function(e) {
          performSave();
          if(typeof vrtxImageEditor !== "undefined" && vrtxImageEditor.save && !vrtxImageEditor.savedImage) {
            vrtxImageEditor.save(_$.single(this).attr("id"));
            e.preventDefault();
          }
        });
        
        <#if !isCollection>
          vrtxAdm.interceptEnterKey('#resource\\.tags');
        </#if>
        
        // Multiple fields interaction
        registerMultipleInputFieldsClicks();

        if(_$("#resource\\.featured-articles").length) {
          loadMultipleInputFields("featured-articles",'${vrtx.getMsg("editor.add")}',
                                  '${vrtx.getMsg("editor.remove")}', '${vrtx.getMsg("editor.move-up")}',
                                  '${vrtx.getMsg("editor.move-down")}', '${vrtx.getMsg("editor.browseImages")}', true, true);
        }   
        if(_$("#resource\\.aggregation").length) {                  
          loadMultipleInputFields("aggregation", '${vrtx.getMsg("editor.add")}',
                                  '${vrtx.getMsg("editor.remove")}', '${vrtx.getMsg("editor.move-up")}',
                                  '${vrtx.getMsg("editor.move-down")}', '${vrtx.getMsg("editor.browseImages")}', false, false);
        } 
        if(_$("#resource\\.manually-approve-from").length) {     
          loadMultipleInputFields("manually-approve-from", '${vrtx.getMsg("editor.add")}',
                                  '${vrtx.getMsg("editor.remove")}', '${vrtx.getMsg("editor.move-up")}',
                                  '${vrtx.getMsg("editor.move-down")}', '${vrtx.getMsg("editor.browseImages")}', false, false);
          var manuallyApproveButton = $("#manually-approve-container-title");
          manuallyApproveButton.parent().find("> div:first-child").append(manuallyApproveButton.remove());
        }
      }); 

      UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.unsavedChangesConfirmation' />";
      COMPLETE_UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.completeUnsavedChangesConfirmation' />";
      window.onbeforeunload = unsavedChangesInEditorMessage;
      
      function performSave() {
        saveMultipleInputFields();
        NEED_TO_CONFIRM = false;
      } 
 
      <#if isCollection> // i18n
        var ajaxSaveText = "<@vrtx.msg code='editor.save-folder-ajax-loading-title' />";
      <#elseif isImage>
        var ajaxSaveText = "<@vrtx.msg code='editor.save-image-ajax-loading-title' />";   
      <#elseif isAudio>
        var ajaxSaveText = "<@vrtx.msg code='editor.save-audio-ajax-loading-title' />";         
      <#elseif isVideo>
        var ajaxSaveText = "<@vrtx.msg code='editor.save-video-ajax-loading-title' />";         
      <#else>
        var ajaxSaveText = "<@vrtx.msg code='editor.save-doc-ajax-loading-title' />";
      </#if>
      
      var approveGeneratingPage = "<@vrtx.msg code='editor.manually-approve.generating-page' />",
          approvePrev = "<@vrtx.msg code='imageListing.previous' />",
          approveNext = "<@vrtx.msg code='imageListing.next' />",
          approveShowing = "<@vrtx.msg code='editor.manually-approve.table-showing' />",
          approveOf = "<@vrtx.msg code='person-listing.of' />",
          approveTableTitle = "<@vrtx.msg code='proptype.name.title' />",
          approveTableSrc = "<@vrtx.msg code='resource.sourceURL' />",
          approveTablePublished = "<@vrtx.msg code='publish.permission.published' />",
          approveShowAll = "<@vrtx.msg code='editor.manually-approve.show-all' />",
          approveShowApprovedOnly = "<@vrtx.msg code='editor.manually-approve.show-approved-only' />",
          approveNoApprovedMsg = "<@vrtx.msg code='editor.manually-approve.no-approved-msg' />",
          browseBase = '${fckeditorBase.url?html}',
          browseBaseFolder = '${baseFolder}',
          browseBasePath = '${fckBrowse.url.pathRepresentation}';

      // Div container display in IE
      var cssFileList = [<#if fckEditorAreaCSSURL?exists>
                           <#list fckEditorAreaCSSURL as cssURL>
                             "${cssURL?html}" <#if cssURL_has_next>,</#if>
                           </#list>
                         </#if>]; 
     
      if (vrtxAdmin.isIE && vrtxAdmin.browserVersion <= 7) {
        cssFileList.push("/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css");
      }
     
    //-->
    </script>
    
    <script type="text/javascript" src="${jsBaseURL?html}/collectionlisting/manually-approve.js"></script>

    <@editor.addDatePickerScripts language true />

    <#if isImage>
      <!--[if lte IE 8]>
        <script type="text/javascript" src="${jsBaseURL?html}/image-editor/excanvas.compiled.js"></script>
      <![endif]-->
      <script type="text/javascript" src="${jsBaseURL?html}/image-editor/editor.js"></script>    
      <script type="text/javascript"><!--  
        var startCropText = '<@vrtx.msg code="editor.image.start-crop" default="Start cropping" />';
        var cropText = '<@vrtx.msg code="editor.image.crop" default="Crop" />';
        var widthText = '<@vrtx.msg code="imageListing.width" default="Width" />';
        var heightText = '<@vrtx.msg code="imageListing.height" default="Height" />';
        $(window).load(function() {
          var imageEditorElm = $("#vrtx-image-editor-wrapper");
          if(imageEditorElm.length) {
            vrtxImageEditor.init(imageEditorElm, "${imageURL}", "${supportedImageEditor?string}");
          }
        });
      // -->
      </script>
    </#if>
  </head>
  <body id="vrtx-editor">
    <#assign header><@vrtx.msg code="editor.edit" args=[vrtx.resourceTypeName(resource)?lower_case] /></#assign>

    <div id="vrtx-editor-title-submit-buttons">
      <div id="vrtx-editor-title-submit-buttons-inner-wrapper">
        <h2>${header}</h2>
        <div class="submitButtons submit-extra-buttons">
            <a class="vrtx-button" id="vrtx-save-view-shortcut" href="javascript:void(0)"><span>${vrtx.getMsg("editor.saveAndView")}</span></a>
          <#if supportedImageEditor>
            <a class="vrtx-button" id="vrtx-save-copy-shortcut" href="javascript:void(0)"><span>${vrtx.getMsg("editor.saveCopy")}</span></a>
          </#if>
          <span id="vrtx-save">
            <a class="vrtx-focus-button" id="vrtx-save-shortcut" href="javascript:void(0)"><span>${vrtx.getMsg("editor.save")}</span></a>
          </span>
            <a class="vrtx-button" id="vrtx-cancel-shortcut" href="javascript:void(0)"><span>${vrtx.getMsg("editor.cancel")}</span></a>
            <@genEditorHelpMenu resource.resourceType isCollection />
        </div>
      </div>
    </div>
    <form action="" method="post" id="editor">
      <div class="properties"<#if isImage> id="image-properties"</#if>>
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
 
     <#if isImage>
       <div id="vrtx-image-editor-wrapper">
         <h3 id="vrtx-image-editor-preview"><@vrtx.msg code="editor.image.preview-title" default="Preview" /></h3>
         <div id='vrtx-image-editor-inner-wrapper'>
           <canvas id="vrtx-image-editor"></canvas>
         </div>
       </div>
     </#if>
      
      <#-- Margin-bottom before save- cancel button for collections -->
      <#if isCollection>
        <div id="allowedValues"></div>
      </#if>

      <div id="submit" class="submitButtons save-cancel">
        <div class="vrtx-button">
          <input type="submit" id="saveAndViewButton" name="saveview"  value="${vrtx.getMsg("editor.saveAndView")}"  />
        </div>
        <#if supportedImageEditor>
           <div class="vrtx-button">
             <input type="submit" id="saveCopyButton" name="savecopy" value="${vrtx.getMsg("editor.saveCopy")}" />
           </div>  
        </#if>
        <div class="vrtx-focus-button">
          <input type="submit" id="saveButton" name="save" value="${vrtx.getMsg("editor.save")}" />
        </div>  
        <div class="vrtx-button">
          <input type="submit" id="cancel" name="cancel" value="${vrtx.getMsg("editor.cancel")}" />
        </div>
      </div>

     </form>
     
    </body>
</html>

<#macro propsForm propDefs>
    <#local locale = springMacroRequestContext.getLocale() />

    <#list propDefs as propDef>
      <#local name = propDef.name />
      
      <#-- HACKS 2012 start -->
      <#-- Wrap hide properties -->
      <#if !name?starts_with("hide") && startWrapHideProps?exists && startWrapHideProps = "true">
        </div><#assign startWrapHideProps = "false" />
      </#if>
      <#if name?starts_with("hide") && !startWrapHideProps?exists>
        <#assign startWrapHideProps = "true" />
        <div id="vrtx-resource.hide-props" class="hide property-item">
          <div class="resource.hide-props property-label"><@vrtx.msg code='editor.hide-props-title' /></div> 
      </#if>
      
      <#-- Title for aggregation and manually approve when recursive isn't present -->
      <#if name == "recursive-listing"><#assign recursivePresent = "true" /></#if>
      <#if name == "display-aggregation" && !recursivePresent?exists>
        <div id="vrtx-resource.recursive-listing" class="recursive-listing property-item">
          <div class="resource.recursive-listing property-label no-recursive-listing-prop"><@vrtx.msg code="proptype.name.${resource.resourceType}.recursive-listing" /></div>
        </div>
      </#if>
      <#-- HACKS 2012 end -->
      
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
              
              <#-- HACKS 2012 start -->
              <#-- Tooltip for aggregation and manually approve -->
              <#if name = "display-aggregation"><#-- only add once. TODO: generalize editor tooltip concept -->
                <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/plugins/jquery.vortexTips.js"></script>
                <script type="text/javascript"><!--
                  $(function() {
                    $("#editor").vortexTips("abbr", "#editor", 320, 300, 250, 300, 20, -30, false, false);
                  });
                // -->
                </script>
              </#if>
              <#if name = "display-aggregation" || name = "display-manually-approved">
                <abbr class="resource-prop-info" title="${vrtx.getMsg('editor.manually-approve-aggregation.info')}"></abbr>
              </#if>
              <#-- HACKS 2012 end -->
              
            <#else>
              <label class="resource.${name}">${allowedValues[0]?html}</label>
              <#if value == allowedValues[0]>
                <input name="resource.${name}" id="resource.${name}.${allowedValues[0]?html}" type="checkbox" value="${allowedValues[0]?html}" checked="checked" />
              <#else>
                <input name="resource.${name}" id="resource.${name}.${allowedValues[0]?html}" type="checkbox" value="${allowedValues[0]?html}" />
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
            <label class="tooltip">${vrtx.getMsg("editor.recursive-listing.featured-articles.hint")}</label>
          </#if>
          <#if description != "">
            <span class="input-description">(${description})</span>
          </#if>

          <#if name = 'manually-approve-from'>
            <div id="manually-approve-container-title">
              <a class="vrtx-button" id="manually-approve-refresh" href="."><span>
                <div id="manually-approve-refresh-icon"></div>${vrtx.getMsg("editor.manually-approve-refresh")}</span>
              </a>
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

<#macro genEditorHelpMenu type isCollection>
   
  <div id="editor-help-menu">
    <span id="editor-help-menu-header"><@vrtx.msg code="manage.help" default="Help" />:</span>
    <ul>
      <li> 
        <#assign lang><@vrtx.requestLanguage/></#assign>
        <#assign propKey = "helpURL.editor." + lang />
        <#if isCollection >
            <#assign propKey = "helpURL.editor.collection." + lang />            
        </#if>
        <#if type == "image" || type == "video" || type == "audio" >
            <#assign propKey = "helpURL.editor." + type + "." + lang />  
                    
        </#if>
        
        <#assign url = helpURL />
        <#if .vars[propKey]?exists>
          <#assign url = .vars[propKey] />
        </#if>
        <a href="${url?html}" target="_blank" class="help-link"><@vrtx.msg code="manage.help.editing" default="Help in editing" /></a>
      </li>
    </ul>
  </div>
</#macro>
