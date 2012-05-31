<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/actions.ftl" as actionsLib />

<#if createDocumentForm?exists && !createDocumentForm.done>
  <div class="expandedForm vrtx-admin-form">
    <form name="createDocumentService" id="createDocumentService-form" action="${createDocumentForm.submitURL?html}"
          method="post" accept-charset="utf-8">
      <h3><@vrtx.msg code="actions.createDocumentService" default="Create Document"/></h3>
      <h4><@vrtx.msg code="actions.createDocumentService.subtitle" default="Choose a template"/></h4>
      <#compress>
        <@spring.bind "createDocumentForm" + ".sourceURI" /> 
        <@actionsLib.genErrorMessages spring.status.errorMessages />
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
      <@actionsLib.genErrorMessages spring.status.errorMessages />
      <div class="vrtx-textfield">
        <input type="text" name="${spring.status.expression}" value="${newDocName}" />
      </div>
      <@actionsLib.genOkCancelButtons "save" "cancelAction" "actions.createDocumentService.save" "actions.createDocumentService.cancel" />
    </form>
  </div>
</#if>
 
<#recover>
${.error}
</#recover>