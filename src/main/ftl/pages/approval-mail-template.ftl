<p>Hi!</p>

<#if resourceDetail?? && resourceDetail.hasWorkingCopy?? && resourceDetail.hasWorkingCopy>
  <p>Can you make this working copy into the public version?</p>
<#else>
  <p>Can you publish this document?</p>
</#if>

<h2>${title}</h2>

<#if comment?has_content>
<pre>${comment}</pre>
</#if>

<p>Link to document: <a href="${uri?html}">${uri?html}</a></p>
<p>Link til documentation: <a href="${uri?html}">${uri?html}</a></p>

<p>Best regards, ${mailFromFullName}</p>
