<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<#-- XXX: remove hard-coded 'authTarget' parameter: -->

<#assign url = leaveAdmin.url />
<#if url?contains("?")>
  <#assign url = url + "&authTarget=http" />
<#else>
  <#assign url = url + "?authTarget=http" />
</#if>
<a href="${url?html}"><@vrtx.msg code="manage.leaveManageMode" default="Leave admin" /></a>