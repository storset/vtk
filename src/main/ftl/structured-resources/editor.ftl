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
  
  <script type="text/javascript" src="${jsBaseURL?html}/plugins/shortcut.js"></script>
  
  <#assign language = vrtx.getMsg("eventListing.calendar.lang", "en") />
  
  <script type="text/javascript"><!--
 
     
    var CURRENT_RESOURCE_LANGAGE = "${resourceLocaleResolver.resolveLocale(null)?string}";
    
    shortcut.add("Ctrl+S",function() {
        $(".vrtx-focus-button:last input").click();
    });

    $(window).load(function() {
        initDatePicker(datePickerLang);
    });
  
    UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.unsavedChangesConfirmation' />";
    COMPLETE_UNSAVED_CHANGES_CONFIRMATION = "<@vrtx.msg code='manage.completeUnsavedChangesConfirmation' />";
    window.onbeforeunload = unsavedChangesInEditorMessage;
 	
	$(document).ready(function() {
  	$("#app-content").delegate(".cke_button_maximize", "click", function(e) {
    	
		var stickyBar = $(".vrtx-sticky-editor-title-submit-buttons");
						
    	stickyBar.toggle();

    	var ckInject = $(this).closest(".cke_toolbar")
                          .find(".cke_toolbar_end");

    	if(!ckInject.find(".vrtx-focus-button").length) {
      	var shortcuts = stickyBar.find("#editor-shortcuts").html();
      		ckInject.append(shortcuts);
      		ckInject.children().not(".vrtx-focus-button")
                         .not("#editor-help-menu").remove();
    	} else {
      		ckInject.find(".vrtx-focus-button").toggle();
      		ckInject.find("#editor-help-menu").toggle();
    	}
  		});
	}); 

	function documentSave () {
		for (instance in CKEDITOR.instances) {
              CKEDITOR.instances[instance].updateElement();
        }    
        
        tb_show(saveDocAjaxText + "...", 
                   "/vrtx/__vrtx/static-resources/js/plugins/thickbox-modified/loadingAnimation.gif?width=240&height=20", 
                   false);
        
	 	performSave();
	 	$("#editor").ajaxSubmit({
              success: function () {},
              complete: function() {
                initDatePicker(datePickerLang);
                tb_remove();
              }
         });
	}

    function performSave() {
      saveDateAndTimeFields(); // js/datepicker/datepicker-admin.js
      if (typeof(MULTIPLE_INPUT_FIELD_NAMES) !== "undefined") {
        saveMultipleInputFields();  // js/editor-multipleinputfields.js
      }
      var boxUrlTextFields = $(".boxUrlText input");
      var i = boxUrlTextFields.length;
      while(i--) {
        var boxUrlTextField = $(boxUrlTextFields[i]);
        boxUrlTextField.val($.trim(boxUrlTextField.val()));
      }
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

  <@editor.addDatePickerScripts language />

  <#global baseFolder = "/" />
  <#if resourceContext.parentURI?exists>
    <#global baseFolder = resourceContext.parentURI?html />
  </#if>
  
  <#if form.resource.type.scripts?exists>
    <@scripts.includeScripts form.resource.type.scripts />
  </#if>

</head>
<body>

  <#assign locale = springMacroRequestContext.getLocale() />
  <#assign header = form.resource.getLocalizedMsg("header", locale, null) />
  
  <#if form.workingCopy>
    <div class="tabMessage-big">
      <@vrtx.rawMsg code="editor.workingCopyMsg" args=[versioning.currentVersionURL?html] />
    </div>
  </#if>
  
  <div id="vrtx-editor-title-submit-buttons">
    <div id="vrtx-editor-title-submit-buttons-inner-wrapper">
      <h2>${header}</h2>
      <div class="submitButtons submit-extra-buttons" id="editor-shortcuts">  
        <#if !form.published && !form.workingCopy>
          <a class="vrtx-button" href="javascript:void(0)" onclick="$('#saveAndViewButton').click()"><span>${vrtx.getMsg("editor.saveAndView")}</span></a>
          <a class="vrtx-focus-button" href="javascript:void(0)" onclick="$('#updateAction').click()"><span>${vrtx.getMsg("editor.save")}</span></a>
          <a class="vrtx-button" href="javascript:void(0)" onclick="$('#cancelAction').click()"><span>${vrtx.getMsg("editor.cancel")}</span></a>
          <@genEditorHelpMenu />
        <#elseif form.workingCopy>
          <ul id="editor-button-row">
            <li class="first"><a href="javascript:void(0)" onclick="$('#saveAndViewButton').click()">${vrtx.getMsg("editor.saveAndView")}</a></li>
            <li><a href="javascript:void(0)" onclick="$('#saveWorkingCopyAction').click()">${vrtx.getMsg("editor.save")}</a></li>
            <li class="last"><a href="javascript:void(0)" onclick="$('#cancelAction').click()">${vrtx.getMsg("editor.cancel")}</a></li>
          </ul>
          <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
          &nbsp;
          <div id="editor-menu-wrapper">
            <ul id="editor-menu">
              <li class="first"><a href="javascript:void(0)" onclick="$('#makePublicVersionAction').click()">${vrtx.getMsg("editor.makePublicVersion")}</a></li>
              <li class="last"><a href="javascript:void(0)" onclick="$('#deleteWorkingCopyAction').click()">${vrtx.getMsg("editor.deleteWorkingCopy")}</a></li>
            </ul>
          </div>
          <@genEditorHelpMenu />
        <#else>
          <ul id="editor-button-row">
            <li class="first"><a href="javascript:void(0)" onclick="$('#saveAndViewButton').click()">${vrtx.getMsg("editor.saveAndView")}</a></li>
            <li><a href="javascript:void(0)" onclick="$('#updateAction').click()">${vrtx.getMsg("editor.save")}</a></li>
            <li class="last"><a href="javascript:void(0)" onclick="$('#cancelAction').click()">${vrtx.getMsg("editor.cancel")}</a></li>
          </ul>
          <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
          &nbsp;
          <a class="vrtx-button" href="javascript:void(0)" onclick="$('#saveWorkingCopyAction').click()"><span>${vrtx.getMsg("editor.saveAsWorkingCopy")}</span></a>
          <@genEditorHelpMenu />
       </#if>
      </div>
    </div>
  </div> 

  <form action="${form.submitURL?html}" method="post" id="editor">
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
    
    <div class="submit submitButtons">
    <#if !form.published && !form.workingCopy>
      <div class="vrtx-button">
        <input type="button" id="saveAndViewButton" onclick="documentSave();" name="updateViewAction"  value="${vrtx.getMsg("editor.saveAndView")}">
      </div>
      <div class="vrtx-focus-button">
        <input type="button" id="updateAction" onclick="documentSave();" name="updateAction" value="${vrtx.getMsg("editor.save")}" />
      </div>
      <div class="vrtx-button">
        <input type="submit" onclick="performSave();" name="cancelAction" id="cancelAction" value="${vrtx.getMsg("editor.cancel")}" />
      </div>

    <#elseif form.workingCopy>
      <div class="vrtx-button">
        <input type="button" id="saveAndViewButton" onclick="documentSave();" name="updateViewAction"  value="${vrtx.getMsg("editor.saveAndView")}">
      </div>
      <div class="vrtx-focus-button">
        <input type="button" id="saveWorkingCopyAction" onclick="documentSave();" name="saveWorkingCopyAction" value="${vrtx.getMsg("editor.save")}" />
      </div>
      <div class="vrtx-button">
        <input type="submit" onclick="performSave();" name="cancelAction" id="cancelAction" value="${vrtx.getMsg("editor.cancel")}" />
      </div>

      <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
      &nbsp;
      <div class="vrtx-button">
        <input type="submit" id="makePublicVersionAction" onclick="performSave();" name="makePublicVersionAction" value="${vrtx.getMsg("editor.makePublicVersion")}" />
      </div>
      <div class="vrtx-button">
        <input type="submit" id="deleteWorkingCopyAction" onclick="performSave();" name="deleteWorkingCopyAction" value="${vrtx.getMsg("editor.deleteWorkingCopy")}" />
      </div>
      
    <#else>
      <div class="vrtx-button">
        <input type="submit" id="saveAndViewButton" onclick="documentSave();" name="updateViewAction"  value="${vrtx.getMsg("editor.saveAndView")}">
      </div>
      <div class="vrtx-focus-button">
        <input type="button" id="updateAction" onclick="documentSave();" name="updateAction" value="${vrtx.getMsg("editor.save")}" />
      </div>
      <div class="vrtx-button">
        <input type="submit" onclick="performSave();" name="cancelAction" id="cancelAction" value="${vrtx.getMsg("editor.cancel")}" />
      </div>

      <span id="buttons-or-text"><@vrtx.msg code="editor.orText" default="or" /></span>
      &nbsp;
      <div class="vrtx-button">
        <input type="submit" id="saveWorkingCopyAction" onclick="performSave();"  name="saveWorkingCopyAction" value="${vrtx.getMsg("editor.saveAsWorkingCopy")}" />
      </div>
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
