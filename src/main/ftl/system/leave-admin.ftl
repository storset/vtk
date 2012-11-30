<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<#-- XXX: remove hard-coded 'authTarget' parameter: -->

<a href="${leaveAdmin.url?html}?authTarget=http"><@vrtx.msg code="manage.leaveManageMode" default="Leave admin" /></a>
