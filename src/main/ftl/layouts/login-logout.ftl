<#import "/lib/vortikal.ftl" as vrtx />

<#if loginURL?exists>
<div class="vrtx-login"><a href="${loginURL?html}"><@vrtx.msg code="decorating.authenticationComponent.login" default="Log in"/></a></div>
<#elseif logoutURL?exists>
<div class="vrtx-logout">
  <span class="vrtx-user">${principal.description?html}</span>
  <form id="logoutForm" action="${logoutURL?html}" method="post" style="display:inline;">
    <button type="submit" id="logoutAction" name="logoutAction"><@vrtx.msg code="decorating.authenticationComponent.logout" default="logout"/></button>
  </form>
  <#-- Hide submit button, display a link instead: -->
  <script type="text/javascript" language="Javascript"><!--
    document.getElementById('logoutAction').style.display = 'none';
    document.write("(&nbsp;<a href=\"${logoutURL?html}\" onclick=\"javascript:document.getElementById('logoutForm').submit();\"><@vrtx.msg code="decorating.authenticationComponent.logout" default="logout"/></a>&nbsp;)");
    //-->
  </script>
</div>
</#if>
