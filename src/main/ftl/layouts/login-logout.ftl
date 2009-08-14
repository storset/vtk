<#import "/lib/vortikal.ftl" as vrtx />
<#if loginURL?exists>
<div class="vrtx-login"><a href="${loginURL?html}"><@vrtx.msg code="decorating.authenticationComponent.login" default="Log in"/></a></div>
<#elseif logoutURL?exists>
<div class="vrtx-logout" id="vrtx-logout">
  <span class="vrtx-user">${principal.description?html}</span>
  <form id="logoutForm" action="${logoutURL?html}" method="post" style="display:inline;">
    <button type="submit" id="logoutAction" name="logoutAction"><@vrtx.msg code="decorating.authenticationComponent.logout" default="logout"/></button>
  </form>
  <#-- Hide submit button, display a link instead: -->
  <script type="text/javascript" language="Javascript">
	   	document.getElementById('logoutAction').style.display = 'none';
		var logoutLink = document.createElement('a');
		logoutLink.setAttribute('href',"${logoutURL?html}");
		logoutLink.setAttribute('onclick',"javascript:document.getElementById('logoutForm').submit();");
		logoutLink.innerHTML = "<@vrtx.msg code="decorating.authenticationComponent.logout" default="logout"/>"
		
		var lp = document.createElement('span');
		lp.innerHTML = "( ";
		var rp = document.createElement('span');
		rp.innerHTML = " )";
		document.getElementById('vrtx-logout').appendChild(lp);
		document.getElementById('vrtx-logout').appendChild(logoutLink);
		document.getElementById('vrtx-logout').appendChild(rp);
  </script>
</div>
</#if>
