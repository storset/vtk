<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#-- XXX: remove hard-coded 'authTarget' parameter: -->
<#assign url = leaveAdmin.url?html />
<#if url?contains("?")>
  <#assign url = url + "&amp;authTarget=http" />
<#else>
  <#assign url = url + "?authTarget=http" />
</#if>
<a href="${leaveAdmin.url?html}?authTarget=http"><@vrtx.msg code="manage.leaveManageMode" default="Leave admin" /></a>
