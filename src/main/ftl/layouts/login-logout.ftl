<#import "/lib/vortikal.ftl" as vrtx />

<#if loginURL?exists>
<div class="vrtx-login">
  <span class="vrtx-parenthesis">(&nbsp;</span><a href="${loginURL?html}"><@vrtx.msg code="decorating.authenticationComponent.login"
               default="log in" /></a><span class="vrtx-parenthesis">&nbsp;)</span>
</div>
<#elseif logoutURL?exists>
<div class="vrtx-logout">
  <span class="vrtx-user">${principal.name?html}</span>
  <span class="vrtx-parenthesis">(&nbsp;</span><a href="${logoutURL?html}"><@vrtx.msg code="decorating.authenticationComponent.logout"
               default="log in" /></a><span class="vrtx-parenthesis">&nbsp;)</span>
</div>
</#if>
