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
<#if resourceContext.currentResource.activeLocks?size &gt; 0>
  <#assign lock="locked" />
</#if>


<#if lock="locked">

  <ul class="lock">
    <li>
      <#assign href = resourceContext.currentResource.activeLocks[0].principal.name />
      <#if resourceContext.currentResource.activeLocks[0].principal.URL?exists>
        <#assign href = '<a target="personsok" href="' + resourceContext.currentResource.activeLocks[0].principal.URL + '">' + resourceContext.currentResource.activeLocks[0].principal.name + '</a>' />
      </#if>
      <@vrtx.msg code="actions.lockedBy" default="Locked by" />&nbsp;${href}
      <#if lockUnlockActions?exists>
        <#list lockUnlockActions.items as item>
          <#if item.label = 'unlockResourceService' && item.url?exists>
            ( <a href="${item.url?html}">${item.title}</a> )
          </#if>
        </#list>
      </#if>
    </li>
  </ul>

</#if>
