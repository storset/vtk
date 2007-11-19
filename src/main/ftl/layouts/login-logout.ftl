<#import "/lib/vortikal.ftl" as vrtx />

<#if loginURL?exists>
<div class="vrtx-login">
  ( <a href="${loginURL?html}"><@vrtx.msg code="decorating.authenticationComponent.login" default="Log in"/></a> )
</div>
<#elseif logoutURL?exists>
<div class="vrtx-logout">
  <span class="vrtx-user">${principal.name?html}</span>
  ( <a href="${logoutURL?html}"><@vrtx.msg code="decorating.authenticationComponent.logout" default="Log out"/></a> )
</div>
</#if>
