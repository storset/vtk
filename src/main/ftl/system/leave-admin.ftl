<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<#-- XXX: remove hard-coded 'authTarget' parameter: -->

<#assign url = leaveAdmin.url?html />
<#if resourceContext.currentResource.isReadRestricted()>
  <#assign url = url + "?authTarget=https" />
<#else>
  <#assign url = url + "?authTarget=http" />
</#if>
<a href="${url?html}"><@vrtx.msg code="manage.leaveManageMode" default="Leave admin" /></a>
