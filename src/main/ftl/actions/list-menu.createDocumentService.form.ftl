<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if createDocumentForm?exists && !createDocumentForm.done>
  <#-- Need this div coz of IEs sucky boxmodel implementation -->
  <div style="clear:both;height:1px;visibility:hidden;"></div>
  <form class="createDocumentService vrtx-admin-form"  name="createDocumentForm" action="${createDocumentForm.submitURL?html}"
        method="post" accept-charset="utf-8">
    <h3><@vrtx.msg code="actions.createDocumentService" default="Create Document"/>:</h3>
    <#compress>
    <@spring.bind "createDocumentForm" + ".sourceURI" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
		</ul>
      </#if>
      <#assign newDocName = "">
      <#-- Set the name of the new file to whatever the user already has supplied-->
      <#if createDocumentForm.name?exists>
        <#assign newDocName = createDocumentForm.name>
      </#if>
      <#if templates?exists && templates?size &gt; 0>
        <ul>
          <@vrtx.formRadioButtons "createDocumentForm.sourceURI", templates, "<li>", "</li>" />
        </ul>
        <#-- Assign a default filename if user has not yet entered anything -->
        <#if newDocName = "">
          <#assign newDocName = templates?values?sort[0]>
        </#if>
      </#if>
      </#compress>
      <@spring.bind "createDocumentForm" + ".name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
        </ul>
      </#if>
    <input type="text" name="${spring.status.expression}" value="${newDocName}">
    <div id="submitButtons">
      <input type="submit" name="save" value="<@vrtx.msg code="actions.createDocumentService.save" default="Create"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createDocumentService.cancel" default="Cancel"/>"/>
    </div>
  </form>
  </#if>
<#recover>
${.error}
</#recover>
