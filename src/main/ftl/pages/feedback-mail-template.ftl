<p>Title: ${title}</a></p>
<p>Uri: <a href="${uri?html}">${uri?html}</a></p>

<#if comment?exists && comment?has_content>
  <p>Comment:</p>
  <pre>${comment}</pre>
</#if>