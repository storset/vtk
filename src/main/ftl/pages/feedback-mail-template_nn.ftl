<p>Tittel: ${title}</a></p>
<p>Uri: <a href="${uri?html}">${uri?string}</a></p>

<#if comment?exists && comment?has_content>
  <p>Kommentar:</p>
  <p>${comment}</p>
</#if>
