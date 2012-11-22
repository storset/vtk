<p>Hei!</p>

<#if resourceDetail?? && resourceDetail.hasWorkingCopy?? && resourceDetail.hasWorkingCopy>
  <p>Kan du sette arbeidsversjonen for &laquo;${title}&raquo; til gjeldande versjon?</p>
<#else>
  <p>Kan du publisere &laquo;${title}&raquo;?</p>
</#if>

<p>Lenkje: <a href="${uri?html}">${uri?html}</a></p>

<#if comment?has_content>
<pre>${comment}</pre>
</#if>

<p>Lenkje til dokumentasjon: <a href="${uri?html}">${uri?html}</a></p>

<#t /><p>Med vennleg helsing,<br/><#t />
<#t />${mailFromFullName}</p><#t />