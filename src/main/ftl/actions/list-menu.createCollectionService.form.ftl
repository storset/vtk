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
        <button id="initCreateChangeTemplate" type="button" onclick="createChangeTemplate(true)"></button>
        
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
        <input type="text" id="${nameBind?html}" name="${nameBind?html}" value="${newColName?html}" size="15" maxlength="50" />
      </div>

      <@spring.bind "createCollectionForm" + ".publish" />
      <#assign publishBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />

      <div class="vrtx-checkbox" id="vrtx-checkbox-hide-from-navigation">
        <input type="checkbox"  id="${publishBind?html}" name="${publishBind?html}" checked />
        <label for="publish"><@vrtx.msg code="property.navigation:publish" default="Publish" /></label>
        
        <#-- TODO: refactor with code in publish.ftl -->
        <#assign resource = resourceContext.currentResource />
        <#assign propResource = vrtx.getProp(resourceContext.currentResource,"unpublishedCollection")  />
        <#if resourceContext.parentResource?exists >
          <#assign propParent = vrtx.getProp(resourceContext.parentResource,"unpublishedCollection")  />
        </#if>
        <#assign notPublished = ((propResource?has_content || propParent?has_content) || !resource.published)  />
        <#assign resourceType = "folder">
        <#if propResource?has_content && propResource.inherited >
           <#if resource.published >
             <abbr class="tooltips" title="<@vrtx.msg code="publish.unpublished.published.info.${resourceType}" />"></abbr>
          <#else> 
            <abbr class="tooltips" title="<@vrtx.msg code="publish.unpublished.unpublishedCollection.info.${resourceType}" />"></abbr>
          </#if>
        <#elseif propResource?has_content && !propParent?has_content>  
          <abbr class="tooltips" title="<@vrtx.msg code="unpublishedCollection.info" />"></abbr>
        <#elseif propParent?has_content >
          <abbr class="tooltips" title="<@vrtx.msg code="publish.unpublished.unpublishedCollection.info.${resourceType}" />"></abbr>
        </#if>
      </div>

      <@actionsLib.genOkCancelButtons "save" "cancelAction" "actions.createCollectionService.save" "actions.createCollectionService.cancel" />
    </form>
  </div>
</#if>

<#recover>
${.error}
</#recover>