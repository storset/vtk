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
      <#if templates?has_content>
        <ul class="radio-buttons">
          <@vrtx.formRadioButtons "createCollectionForm.sourceURI", templates, "<li>", "</li>" />
        </ul>
        <button id="initChangeTemplate" type="button" onclick="changeTemplate('${sourceURIBind?html}', true)"></button>
        
        <#-- If POST is not AJAX (otherwise it would be a funcComplete() in completeAsyncForm()) -->
        <script type="text/javascript"><!--
          $(document).ready(createFuncComplete);
        // -->
        </script>
      </#if>

      <@spring.bind "createCollectionForm" + ".title" />
      <#assign titleBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />
      <@spring.bind "createCollectionForm" + ".name" />
      <#assign nameBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />

      <h4 class="vrtx-admin-label"><@vrtx.msg code="actions.createCollectionService.title" default="Title" /></h4>
      <div class="vrtx-textfield" id="vrtx-textfield-collection-title">
        <input type="text" id="${titleBind?html}" name="${titleBind?html}" value="${newColTitle?html}" size="40" />
      </div>

      <h4 class="vrtx-admin-label"><@vrtx.msg code="actions.createCollectionService.collection-name" default="Folder name" /></h4>
      <div class="vrtx-textfield" id="vrtx-textfield-collection-name">
        <input type="text" id="${nameBind?html}" name="${nameBind?html}" value="${newColName?html}" size="15"  />
      </div>

      <@spring.bind "createCollectionForm" + ".hidden" />
      <#assign hiddenBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />

      <div class="vrtx-checkbox" id="vrtx-checkbox-hide-from-navigation">
        <input type="checkbox"  id="${hiddenBind?html}" name="${hiddenBind?html}" />
        <label for="hidden"><@vrtx.msg code="property.navigation:hidden" default="Hide from navigation" /></label>
        <abbr title="${vrtx.getMsg("actions.tooltip.hideFromNavigation")}" class="resource-prop-info"></abbr>
      </div>

      <@actionsLib.genOkCancelButtons "save" "cancelAction" "actions.createCollectionService.save" "actions.createCollectionService.cancel" />
    </form>
  </div>
</#if>

<#recover>
${.error}
</#recover>