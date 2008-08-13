<#ftl strip_whitespace=true />
<#import "/lib/vortikal.ftl" as vrtx />
<span class="principal"><span class="name">${resourceContext.principal}</span>
<#if logout.url?exists>
  (&nbsp;<a href="${logout.url?html}"><@vrtx.msg code="manage.logout" default="logout"/></a>&nbsp;)
</#if>
</span>
