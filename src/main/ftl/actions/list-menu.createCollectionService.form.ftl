<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/actions.ftl" as actionsLib />

<#if createCollectionForm?exists && !createCollectionForm.done>
  <div class="expandedForm vrtx-admin-form">
    <form name="createCollectionService" id="createCollectionService-form" action="${createCollectionForm.submitURL?html}" method="post">
      <h3 class="nonul"><@vrtx.msg code="actions.createCollectionService" default="Create collection"/></h3>
      <h4><@vrtx.msg code="actions.createCollectionService.subtitle" default="Choose a folder type"/></h4>
      <@spring.bind "createCollectionForm" + ".sourceURI" /> 
      <#assign sourceURIBind = spring.status.value?default("")>
      <@actionsLib.genErrorMessages spring.status.errorMessages />
        <#assign newColTitle = "">
        <#assign newColName = "">
        <#-- Set the name of the new file to whatever the user already has supplied-->
        <#if createCollectionForm.title?exists>
          <#assign newColTitle = createCollectionForm.title>
        </#if>
        <#if createCollectionForm.name?exists>
          <#assign newColName = createCollectionForm.name>
        </#if>
      <#if templates?exists && templates?size &gt; 0>
        <ul class="radio-buttons">
          <@vrtx.formRadioButtons "createCollectionForm.sourceURI", templates, "<li>", "</li>" />
        </ul>
          <button id="initToggleShowDescription" type="button" onclick="toggleShowDescription('${templates[sourceURIBind]}', true)"></button> 
      </#if>

      <@spring.bind "createCollectionForm" + ".title" />
      <#assign titleBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />
      <@spring.bind "createCollectionForm" + ".name" />
      <#assign nameBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />

      <div class="vrtx-admin-label"><@vrtx.msg code="actions.createCollectionService.title" default="Title" /></div>
      <div class="vrtx-textfield" id="vrtx-textfield-collection-title">
        <input type="text" id="${titleBind}" name="${titleBind}" value="${newColTitle}" />
      </div>

      <div class="vrtx-admin-label"><@vrtx.msg code="actions.createCollectionService.collection-name" default="Folder name" /></div>
      <div class="vrtx-textfield" id="vrtx-textfield-collection-name">
        <input type="text" id="${nameBind}" name="${nameBind}" value="${newColName}"  />
      </div>

      <@spring.bind "createCollectionForm" + ".hidden" />
      <#assign hiddenBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />

      <div class="vrtx-checkbox">
        <input type="checkbox"  id="${hiddenBind}" name="${hiddenBind}" /> <@vrtx.msg code="actions.createCollectionService.hide" default="Hide from navigation" />(?)
      </div>

      <@actionsLib.genOkCancelButtons "save" "cancelAction" "actions.createCollectionService.save" "actions.createCollectionService.cancel" />
    </form>
  </div>
</#if>

<#recover>
${.error}
</#recover>
