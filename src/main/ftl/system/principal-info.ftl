<#ftl strip_whitespace=true />
<span class="principal"><span class="name">${resourceContext.principal}</span></principal>

<#if logoutURL?exists>
  (&nbsp;<a href="${logoutURL?html}"><@vrtx.msg code="manage.logout" default="logout"/></a>&nbsp;)
</#if>
