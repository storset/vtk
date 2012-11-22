<p>Hei!</p>

<#if resourceDetail?? && resourceDetail.hasWorkingCopy?? && resourceDetail.hasWorkingCopy>
  <p>Kan du sette arbeidsversjonen for "${title}" til gjeldande versjon?</p>
<#else>
  <p>Kan du publisere dokumentet "${title}"?</p>
</#if>

<#if comment?has_content>
<pre>${comment}</pre>
</#if>

<p>Lenkje til dokument: <a href="${uri?html}">${uri?html}</a></p>
<p>Lenkje til dokumentasjon: <a href="${uri?html}">${uri?html}</a></p>

<#t /><p>Med vennleg helsing,<br/><#t />
<#t />${mailFromFullName}</p><#t />