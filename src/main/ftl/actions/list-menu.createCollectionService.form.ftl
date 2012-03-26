<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if createCollectionForm?exists && !createCollectionForm.done>
  <div class="expandedForm vrtx-admin-form">
  <form name="createCollectionService" id="createCollectionService-form" action="${createCollectionForm.submitURL?html}" method="post">
    <h3 class="nonul"><@vrtx.msg code="actions.createCollectionService" default="Create collection"/></h3>
    <h4><@vrtx.msg code="actions.createCollectionService.subtitle" default="Choose a folder type"/></h4>
    <@spring.bind "createCollectionForm.name" /> 
    <@spring.bind "createCollectionForm" + ".sourceURI" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <div class="errorContainer">
          <ul class="errors">
            <#list spring.status.errorMessages as error> 
              <li>${error}</li> 
            </#list>
	      </ul>
	    </div>
      </#if>
      
      <#if templates?exists && templates?size &gt; 0>
        <ul class="radio-buttons">
          <@vrtx.formRadioButtons "createCollectionForm.sourceURI", templates, "<li>", "</li>" />
        </ul>
      </#if>
           <@spring.bind "createCollectionForm" + ".name" /> 
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
      <input type="text" name="name" />
    </div>
    <div id="submitButtons">
      <div class="vrtx-focus-button">
        <input type="submit" name="save" value="<@vrtx.msg code="actions.createCollectionService.save" default="Create"/>" />
      </div>
      <div class="vrtx-button">
        <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createCollectionService.cancel" default="Cancel"/>" />
      <div>
    </div>
  </form>

  </div>
  </#if>

<#recover>
${.error}
</#recover>
