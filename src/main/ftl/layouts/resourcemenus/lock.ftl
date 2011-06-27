<#--
  - File: lock.ftl
  - 
  - Description: Rendering lock info in header
  - 
  - Required model data:
  -   resourceContext
  -  
  - Optional model data:
  -   lockUnlockActions
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign lock=""/>
<#if resourceContext.currentResource.lock?exists>
  <#assign lock="locked" />
</#if>


<#if lock="locked">

  <div class="lock resource-menu">
    <#assign lockedBy = resourceContext.currentResource.lock.principal.name />
    <#if resourceContext.currentResource.lock.principal.URL?exists>
      <#assign lockedBy  = '<a href="' + resourceContext.currentResource.lock.principal.URL + '">' + resourceContext.currentResource.lock.principal.description + '</a>' />
    </#if>
    <h3><@vrtx.msg code="actions.lockedBy" default="Locked by" /></h3>
    <p>${lockedBy}</p>
    <#if lockUnlockActions?exists>
      <#list lockUnlockActions.items as item>
        <#if item.label = 'manage.unlockFormService' && item.url?exists>
          <p><a class="vrtx-button-small" href="${item.url?html}"><span>${item.title}</span></a></p>
        </#if>
      </#list>
    </#if>
  </div>

</#if>
