<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if createDocumentForm?exists && !createDocumentForm.done>
  <#-- Need this div coz of IEs sucky boxmodel implementation -->
  <div style="clear:both;height:1px;visibility:hidden;"></div>
  <form class="createDocumentService" name="createDocumentForm" action="${createDocumentForm.submitURL?html}"
        method="POST" accept-charset="utf-8">
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

      <#-- assign templates = topTemplates?values?sort>
        ${templates[0]}
        <#list templates as template>
          ${template}
        </#list -->

      <#assign defaultTemplate = "">
        
      <#if topTemplates?exists>
        <ul>
          <@vrtx.formRadioButtons "createDocumentForm.sourceURI", topTemplates, "<li>", "</li>" />
        </ul>
        <#-- Need to assign a default filename -->
        <#assign defaultTemplate = topTemplates?values?sort[0]>
      </#if>
      <#if categoryTemplates?exists>
	<#assign categories = categoryTemplates?keys?sort>
	  <#list categories as category>
	    <h3>${category}:</h3>
            <#-- Need to assign a default filename -->
            <#if category_index = 0 && !topTemplates?exists>
              <#assign defaultTemplate = categoryTemplates[category]?values?sort[0]>
            </#if>
	    <ul>
	      <@vrtx.formRadioButtons "createDocumentForm.sourceURI", categoryTemplates[category],  "<li>", "</li>" />
	    </ul>
	  </#list>
          <#if !topTemplates?exists>
            <#assign d = categoryTemplates?keys?sort>
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
 
    <input type="text" name="${spring.status.expression}" value="${defaultTemplate}">

    <div id="submitButtons">
      <input type="submit" name="save" value="<@vrtx.msg code="actions.createDocumentService.save" default="Create"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createDocumentService.cancel" default="Cancel"/>"/>
    </div>

  </form>
  </#if>
<#recover>
${.error}
</#recover>
