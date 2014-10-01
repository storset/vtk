<p>Tittel: ${title}</a></p>
<p>Uri: <a href="${uri?html}">${uri?html}</a></p>

<#if comment?has_content>
  <p>Kommentar:</p>
  <pre>${comment}</pre>
</#if>
<#if userAgentViewport?has_content>
  <p>Klient/viewport: ${userAgentViewport?html}</p>
</#if>
