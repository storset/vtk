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
        <#assign sourceURIBind = spring.status.value?default("")>
        <@actionsLib.genErrorMessages spring.status.errorMessages />
        <#assign newDocTitle = "">
        <#assign newDocName = "">
        <#-- Set the name of the new file to whatever the user already has supplied-->
        <#if createDocumentForm.title?exists>
          <#assign newDocTitle = createDocumentForm.title>
        </#if>
        <#if createDocumentForm.name?exists>
          <#assign newDocName = createDocumentForm.name>
        </#if>
        <#if templates?exists && templates?size &gt; 0>
          <ul class="radio-buttons">
            <@vrtx.formRadioButtons "createDocumentForm.sourceURI", templates, "<li>", "</li>", descriptions, titles, true />
          </ul>
          <button id="initToggleShowDescription" style="none" type="button" onclick="toggleShowDescription('${templates[sourceURIBind]}', <#if (titles?has_content && titles[sourceURIBind]?exists)>${titles[sourceURIBind]?string}<#else>false</#if>)"></button> 
        </#if>
      </#compress>

      <@spring.bind "createDocumentForm" + ".title" /> 
      <#assign titleBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />
      <@spring.bind "createDocumentForm" + ".name" /> 
      <#assign nameBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />
      <@spring.bind "createDocumentForm" + ".isIndex" /> 
      <#assign isIndexBind = spring.status.expression>
      <@actionsLib.genErrorMessages spring.status.errorMessages />
      <div id="vrtx-div-file-title">
      <@vrtx.msg code="actions.createDocumentService.title" default="Title" />:
      <br />
      <div class="vrtx-textfield" id="vrtx-textfield-file-title">
        <input type="text" id="${titleBind}" name="${titleBind}" value="${newDocTitle}" onkeyup="userTitleKeyUp('${titleBind}', '${nameBind}', '${isIndexBind}')" />
      </div>
      </div>
      <div id="vrtx-div-file-name">
      <br />
      <@vrtx.msg code="actions.createDocumentService.filename" default="Filename" />:
      <br />
      <div class="vrtx-textfield" id="vrtx-textfield-file-name">
        <input type="text" id="${nameBind}" name="${nameBind}" value="${newDocName}" onkeyup="disableReplaceTitle('${nameBind}')" />.html
      </div>
      </div>
      <div class="vrtx-checkbox">
        <input type="checkbox"  id="${isIndexBind}" name="${isIndexBind}" onClick="isIndexFile('${nameBind}', '${isIndexBind}')" /> <@vrtx.msg code="actions.createDocumentService.index" default="Is index-file" />(?)
      </div>
      <@actionsLib.genOkCancelButtons "save" "cancelAction" "actions.createDocumentService.save" "actions.createDocumentService.cancel" />
    </form>
  </div>
</#if>
 
<#recover>
${.error}
</#recover>
