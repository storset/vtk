<p>Title: ${title}</a></p>
<p>Uri: <a href="${uri?html}">${uri?string}</a></p>

<#if comment?exists && comment?has_content>
  <p>Comment:</p>
  <p>${comment}</p>
</#if>
