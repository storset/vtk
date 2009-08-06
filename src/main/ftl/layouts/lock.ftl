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

  <ul class="lock">
    <li>
      <#assign href = resourceContext.currentResource.lock.principal.name />
      <#if resourceContext.currentResource.lock.principal.URL?exists>
        <#assign href = '<a target="personsok" href="' + resourceContext.currentResource.lock.principal.URL + '">' + resourceContext.currentResource.lock.principal.name + '</a>' />
      </#if>
      <@vrtx.msg code="actions.lockedBy" default="Locked by" />&nbsp;${href}
      <#if lockUnlockActions?exists>
        <#list lockUnlockActions.items as item>
          <#if item.label = 'unlockResourceService' && item.url?exists>
            <#--
            ( <a href="${item.url?html}">${item.title}</a> )
             -->
            <form id="vrtx-unlock-resource-form" class="vrtx-admin-button"
                  action="${item.url?html}" method="post" >
              <input type="submit" id="vrtx-unlock-resource-form.submit" name="submit" value="${item.title?html}" />
            </form>
          </#if>
        </#list>
      </#if>
    </li>
  </ul>

</#if>
