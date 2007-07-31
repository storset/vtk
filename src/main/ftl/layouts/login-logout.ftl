<#import "/lib/vortikal.ftl" as vrtx />

<#if loginURL?exists>
<div class="vrtx-login">
  (&nbsp;<a href="${loginURL?html}">
    <@vrtx.msg code="decorating.authenticationComponent.login"
               default="log in" /></a>&nbsp;)
</div>
<#elseif logoutURL?exists>
<div class="vrtx-logout">
  <span class="vrtx-user">${principal.name?html}</span>
  (&nbsp;<a href="${logoutURL?html}">
    <@vrtx.msg code="decorating.authenticationComponent.logout"
               default="log in" /></a>&nbsp;)
</div>
</#if>
