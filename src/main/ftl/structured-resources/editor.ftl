<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "include/scripts.ftl" as scripts />
<#import "/lib/editor/common.ftl" as editor />
<#import "editor/vrtx-json-javascript.ftl" as vrtxJSONJavascript />
<#import "vrtx-types/vrtx-json-common.ftl" as vrtxJSONCommon />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Edit structured resource</title>
  
  <@editor.addCkScripts />
  <@vrtxJSONJavascript.script />
  
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/plugins/jquery.hotkeys.js"></script> 
  
  <#assign language = vrtx.getMsg("eventListing.calendar.lang", "en") />
  
  <#global baseFolder = "/" />
  <#if resourceContext.parentURI?exists>
    <#global baseFolder = resourceContext.parentURI?html />
  </#if>
  
  <script type="text/javascript"><!--
    var DATE_PICKER_INITIALIZED = $.Deferred();
    $(window).load(function() {
      datepickerEditor = new VrtxDatepicker({
        language: datePickerLang,
        after: function() {
          DATE_PICKER_INITIALIZED.resolve();
        }
      });
    });

    $(document).ready(function() {
      var vrtxAdm = vrtxAdmin, _$ = vrtxAdm._$;

      // Sticky bar shortcuts
      vrtxAdm.mapShortcut("#vrtx-save-view-shortcut", "#saveAndViewButton");
      vrtxAdm.mapShortcut("#vrtx-save-shortcut", "#updateAction");
      vrtxAdm.mapShortcut("#vrtx-cancel-shortcut", "#cancelAction");
      vrtxAdm.mapShortcut("#vrtx-save-working-copy-shortcut", "#saveWorkingCopyAction");
      vrtxAdm.mapShortcut("#vrtx-make-public-version-shortcut", "#makePublicVersionAction");
      vrtxAdm.mapShortcut("#vrtx-delete-working-copy-shortcut", "#deleteWorkingCopyAction");
      vrtxAdm.mapShortcut("#vrtx-save-as-working-copy-shortcut", "#saveWorkingCopyAction");
      vrtxAdm.mapShortcut("#vrtx-send-to-approval-shortcut", "#vrtx-send-to-approval");
      
      // Cancel action
      _$("#editor").on("click", "#cancelAction", function(e) {
        vrtxEditor.needToConfirm = false;
      });
      
      // Save and versioning
      _$("#editor").on("click", "#saveAndViewButton, #saveWorkingCopyAction, #makePublicVersionAction, #deleteWorkingCopyAction", function(e) {
        var ok = performSave();
        if(!ok) return false;
      });
    });
    
    var CURRENT_RESOURCE_LANGAGE = "${resourceLocaleResolver.resolveLocale(null)?string}";
    UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.unsavedChangesConfirmation' />";
    COMPLETE_UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.completeUnsavedChangesConfirmation' />";
    window.onbeforeunload = unsavedChangesInEditorMessage;

    function performSave() {
      var ok = validTextLengthsInEditor(false);
      if(!ok) return false; 
      datepickerEditor.prepareForSave(); // js/datepicker/datepicker-admin.js
      saveMultipleInputFields();
      
      /* Trim box URLs */
      var boxUrlTextFields = vrtxAdmin._$(".boxUrlText input");
      var i = boxUrlTextFields.length;
      while(i--) {
        var boxUrlTextField = vrtxAdmin._$(boxUrlTextFields[i]);
        boxUrlTextField.val(vrtxAdmin._$.trim(boxUrlTextField.val()));
      }
      
      vrtxEditor.needToConfirm = false;
      
      return true;
    }

    // Async. save i18n 
    var ajaxSaveText = "<@vrtx.msg code='editor.save-doc-ajax-loading-title' />";

    // CKEditor CSS
    var cssFileList = [<#if fckEditorAreaCSSURL?exists>
                         <#list fckEditorAreaCSSURL as cssURL>
                           "${cssURL?html}" <#if cssURL_has_next>,</#if>
                         </#list>
                       </#if>];   
    
  //-->
  </script>

  <@editor.addCommonScripts language />
  
  <#if form.resource.type.scripts?exists>
    <@scripts.includeScripts form.resource.type.scripts />
  </#if>
</head>
<body id="vrtx-editor">

  <#assign locale = springMacroRequestContext.getLocale() />
  <#assign header = form.resource.getLocalizedMsg("header", locale, null) />
  
  <#if form.workingCopy>
    <div class="tabMessage-big">
      <@vrtx.rawMsg code="editor.workingCopyMsg" args=[versioning.currentVersionURL?html] />
    </div>
  </#if>

  <div id="vrtx-editor-title-submit-buttons">
    <div id="vrtx-editor-title-submit-buttons-inner-wrapper">
      <#if form.workingCopy || !form.onlyWriteUnpublished || !form.hasPublishDate>
      <h2>${header}</h2>
      <div class="submitButtons submit-extra-buttons" id="editor-shortcuts">
        <#if !form.hasPublishDate && !form.workingCopy>
          <a class="vrtx-button" href="javascript:void(0)" id="vrtx-save-view-shortcut"><span>${vrtx.getMsg("editor.saveAndView")}</span></a>
          <span id="vrtx-save">
            <a class="vrtx-focus-button" href="javascript:void(0)" id="vrtx-save-shortcut"><span>${vrtx.getMsg("editor.save")}</span></a>
          </span>
          <a class="vrtx-button" href="javascript:void(0)" id="vrtx-cancel-shortcut"><span>${vrtx.getMsg("editor.cancel")}</span></a>
          <#if form.onlyWriteUnpublished>
            <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
            &nbsp;
            <a class="vrtx-button" href="javascript:void(0)" id="vrtx-send-to-approval-shortcut"><span>${vrtx.getMsg('send-to-approval.title')}</span></a>
          </#if>
          <@genEditorHelpMenu />
        <#elseif form.workingCopy>
          <ul id="editor-button-row">
            <li class="first">
              <a href="javascript:void(0)" id="vrtx-save-view-shortcut">${vrtx.getMsg("editor.saveAndView")}</a>
            </li>
            <li><span id="vrtx-save">
              <a class="vrtx-focus-button" href="javascript:void(0)" id="vrtx-save-working-copy-shortcut">${vrtx.getMsg("editor.save")}</a>
              </span>
            </li>
            <li class="last">
              <a href="javascript:void(0)" id="vrtx-cancel-shortcut">${vrtx.getMsg("editor.cancel")}</a>
            </li>
          </ul>
          <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
          &nbsp;
          <div id="editor-menu-wrapper">
            <ul id="editor-menu">
              <li class="first">
                <#if (form.hasPublishDate && !form.onlyWriteUnpublished) || !form.hasPublishDate>
                  <a href="javascript:void(0)" id="vrtx-make-public-version-shortcut">${vrtx.getMsg("editor.makePublicVersion")}</a>
                <#else>
                  <a href="javascript:void(0)" id="vrtx-send-to-approval-shortcut">${vrtx.getMsg('send-to-approval.title')}</a>
                </#if>
              </li>
              <li class="last">
                <a href="javascript:void(0)" id="vrtx-delete-working-copy-shortcut">${vrtx.getMsg("editor.deleteWorkingCopy")}</a>
              </li>
            </ul>
          </div>
          <@genEditorHelpMenu />
        <#else>
          <ul id="editor-button-row">
            <li class="first">
              <a href="javascript:void(0)" id="vrtx-save-view-shortcut">${vrtx.getMsg("editor.saveAndView")}</a>
            </li>
            <li><span id="vrtx-save">
              <a class="vrtx-focus-button" href="javascript:void(0)" id="vrtx-save-shortcut">${vrtx.getMsg("editor.save")}</span></a>
            </li>
            <li class="last">
              <a href="javascript:void(0)" id="vrtx-cancel-shortcut">${vrtx.getMsg("editor.cancel")}</a>
            </li>
          </ul>
          <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
          &nbsp;
          <a class="vrtx-button" href="javascript:void(0)" id="vrtx-save-as-working-copy-shortcut">
            <span>${vrtx.getMsg("editor.saveAsWorkingCopy")}</span>
          </a>
          <@genEditorHelpMenu />
       </#if>
      </div>
      <#else>
        <h2>${vrtx.getMsg("editor.createWorkingCopy")}</h2>
      </#if>
    </div>
  </div>

  <#assign backupURL = vrtx.linkConstructor(".", 'copyBackupService') />
  <#assign backupViewURL = vrtx.relativeLinkConstructor("", 'viewService') />
  <form id="backupForm" action="${backupURL}" method="post" accept-charset="UTF-8">
    <@vrtx.csrfPreventionToken url=backupURL />
    <input type="hidden" name="uri" value="${backupViewURL}" />
  </form>
    
  <form action="${form.submitURL?html}" method="post" id="editor"<#if form.getResource().getType().getName()?exists> class="vrtx-${form.getResource().getType().getName()}"</#if>>
    <div class="properties <#if (!form.workingCopy && form.hasPublishDate && form.onlyWriteUnpublished)>hidden-props</#if>">
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
    </div>

    <div class="submit submitButtons">
    <#if !form.hasPublishDate && !form.workingCopy>
      <div class="vrtx-button vrtx-save-button">
        <input type="submit" id="saveAndViewButton" name="updateViewAction"  value="${vrtx.getMsg('editor.saveAndView')}" />
      </div>
      <div class="vrtx-focus-button vrtx-save-button"> 
        <input type="submit" id="updateAction" name="updateAction" value="${vrtx.getMsg('editor.save')}" />
      </div>
      <div class="vrtx-button">
        <input type="submit" id="cancelAction" name="cancelAction" value="${vrtx.getMsg('editor.cancel')}" />
      </div>
      <#if form.onlyWriteUnpublished>
        <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
        &nbsp;
        <a class="vrtx-button" title="${vrtx.getMsg('send-to-approval.title')}" id="vrtx-send-to-approval" href="?vrtx=admin&action=email-approval"><span>${vrtx.getMsg('send-to-approval.title')}</span></a>
      </#if>
    <#elseif form.workingCopy>
      <div class="vrtx-button vrtx-save-button">
        <input type="submit" id="saveAndViewButton" name="updateViewAction"  value="${vrtx.getMsg('editor.saveAndView')}" />
      </div>
      <div class="vrtx-focus-button vrtx-save-button">
        <input type="submit" id="saveWorkingCopyAction" name="saveWorkingCopyAction" value="${vrtx.getMsg('editor.save')}" />
      </div>
      <div class="vrtx-button">
        <input type="submit" id="cancelAction" name="cancelAction" value="${vrtx.getMsg('editor.cancel')}" />
      </div>
      <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
      &nbsp;
      <#if (form.hasPublishDate && !form.onlyWriteUnpublished) || !form.hasPublishDate>
        <div class="vrtx-button">
          <input type="submit" id="makePublicVersionAction" name="makePublicVersionAction" value="${vrtx.getMsg('editor.makePublicVersion')}" />
        </div>
      <#else>
        <a class="vrtx-button" title="${vrtx.getMsg('send-to-approval.title')}" id="vrtx-send-to-approval" href="?vrtx=admin&action=email-approval"><span>${vrtx.getMsg('send-to-approval.title')}</span></a>
      </#if>
      <div class="vrtx-button">
        <input type="submit" id="deleteWorkingCopyAction" name="deleteWorkingCopyAction" value="${vrtx.getMsg('editor.deleteWorkingCopy')}" />
      </div>
    <#else>
      <#if !form.onlyWriteUnpublished>
        <div class="vrtx-button vrtx-save-button">
          <input type="submit" id="saveAndViewButton" name="updateViewAction"  value="${vrtx.getMsg('editor.saveAndView')}" />
        </div>
        <div class="vrtx-focus-button vrtx-save-button">
          <input type="submit" id="updateAction" name="updateAction" value="${vrtx.getMsg('editor.save')}" />
        </div>
        <div class="vrtx-button">
          <input type="submit" id="cancelAction" name="cancelAction" value="${vrtx.getMsg('editor.cancel')}" />
        </div>

        <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
        &nbsp;
        <div class="vrtx-button">
          <input type="submit" id="saveWorkingCopyAction" name="saveWorkingCopyAction" value="${vrtx.getMsg('editor.saveAsWorkingCopy')}" />
        </div>
      <#else>
        <div class="vrtx-button">
          <input type="submit" id="saveWorkingCopyAction" name="saveWorkingCopyAction" value="${vrtx.getMsg('editor.createWorkingCopy')}" />
        </div>
      </#if>
    </#if>
    </div>
  </form>
</body>
</html>

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
      <li>
        <a class="help-link" href="${form.listComponentServiceURL?html}" target="new_window">
          <@vrtx.msg code="plaintextEdit.tooltip.listDecoratorComponentsService" />
        </a>
      </li>
    </ul>
  </div>
</#macro>
