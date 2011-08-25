<#ftl strip_whitespace=true />
<#import "/lib/vortikal.ftl" as vrtx />

<span class="principal">
  <span class="name">${resourceContext.principal.description}</span>
  <#if logout.url?exists>
    <form method="post" action="${logout.url?html}" id="logoutForm">
      <input type="hidden" name="useRedirectService" value="true" />
      <input type="submit" value="<@vrtx.msg code="manage.logout" default="logout"/>" id="logoutAction" name="logoutAction" />
    </form>
  </#if>
</span>
