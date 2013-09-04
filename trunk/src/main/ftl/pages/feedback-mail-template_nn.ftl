<p>Tittel: ${title}</a></p>
<p>Uri: <a href="${uri?html}">${uri?html}</a></p>

<#if comment?exists && comment?has_content>
  <p>Kommentar:</p>
  <pre>${comment}</pre>
</#if>
