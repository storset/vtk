<#--
  - File: banner.ftl
  - 
  - Description: Default manage banner
  -  
  - Optional model data:
  -   resourceContext
  -   leaveAdmin
  -   logout
  -   helpURL
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<div class="banner <@vrtx.msg code="banner.logo" default="norsk" />">
  <#if resourceContext?exists && resourceContext.principal?exists>
     <div style="margin-top: -3px;">
     <#if leaveAdmin?exists && leaveAdmin.url?exists>
     <a href="${leaveAdmin.url}">
       <@vrtx.msg code="manage.leaveManageMode" default="Leave admin" /></a>
     </#if>
     <#if leaveAdmin?exists && leaveAdmin.url?exists && helpURL?exists>
       | <a href="${helpURL?html}" target="help"><@vrtx.msg code="manage.help" default="Help" /></a>
     </#if>
     <span class="principal"><span class="name">${resourceContext.principal.name}</span><#if leaveAdmin?exists && leaveAdmin.url?exists && logout?exists && logout.url?exists>&nbsp;(&nbsp;<a href="${logout.url}"><@vrtx.msg code="manage.logout" default="Log out" /></a>&nbsp;)</#if></span></div>
  </#if>
</div>

