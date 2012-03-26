<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if createDocumentForm?exists && !createDocumentForm.done>
  <div class="expandedForm vrtx-admin-form">
  <form name="createDocumentService" id="createDocumentService-form" action="${createDocumentForm.submitURL?html}"
        method="post" accept-charset="utf-8">
    <h3><@vrtx.msg code="actions.createDocumentService" default="Create Document"/></h3>
    <h4><@vrtx.msg code="actions.createDocumentService.subtitle" default="Choose a template"/></h4>
    <#compress>
    <@spring.bind "createDocumentForm" + ".sourceURI" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <div class="errorContainer">
          <ul class="errors">
            <#list spring.status.errorMessages as error> 
              <li>${error}</li> 
            </#list>
		  </ul>
		</div>
      </#if>
      <#assign newDocName = "">
      <#-- Set the name of the new file to whatever the user already has supplied-->
      <#if createDocumentForm.name?exists>
        <#assign newDocName = createDocumentForm.name>
      </#if>
      <#if templates?exists && templates?size &gt; 0>
        <ul class="radio-buttons">
          <@vrtx.formRadioButtons "createDocumentForm.sourceURI", templates, "<li>", "</li>", true />
        </ul>
        <#-- Assign a default filename if user has not yet entered anything -->
        <#if newDocName = "">
          <#assign newDocName = templates?values?sort[0]>
        </#if>
      </#if>
      </#compress>
      <@spring.bind "createDocumentForm" + ".name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <div class="errorContainer">
          <ul class="errors">
            <#list spring.status.errorMessages as error> 
              <li>${error}</li> 
            </#list>
          </ul>
        </div>
      </#if>
    <div class="vrtx-textfield">
      <input type="text" name="${spring.status.expression}" value="${newDocName}" />
    </div>
    <div id="submitButtons">
      <div class="vrtx-focus-button">
        <input type="submit" name="save" value="<@vrtx.msg code="actions.createDocumentService.save" default="Create"/>" />
      </div>
      <div class="vrtx-button">
        <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createDocumentService.cancel" default="Cancel"/>" />
      </div>
    </div>
  </form>
  
  </div>
  </#if>
  
<#recover>
${.error}
</#recover>
