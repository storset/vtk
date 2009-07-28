<#ftl strip_whitespace=true />
<#import "/lib/vortikal.ftl" as vrtx />
<span class="principal">
<span class="name">${resourceContext.principal.description}</span>
<#if logout.url?exists>
  <form style="display:inline;" method="post" action="${logout.url?html}" id="logoutForm">
    <input type="hidden" name="useRedirectService" value="true" />
    <input style="display:inline;" type="submit" value="<@vrtx.msg code="manage.logout" default="logout"/>"
           id="logoutAction" name="logoutAction" />
  </form>
  <#-- Hide submit button, display a link instead: -->
  <script type="text/javascript" language="Javascript"><!--
    document.getElementById('logoutAction').style.display = 'none';
    document.write("(&nbsp;<a href=\"${logout.url?html}\" onclick=\"javascript:document.getElementById('logoutForm').submit();\"><@vrtx.msg code="manage.logout" default="logout"/></a>&nbsp;)");
    //-->
  </script>
</#if>
</span>
