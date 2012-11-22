<p>Hei!</p>

<#if resourceDetail?? && resourceDetail.hasWorkingCopy?? && resourceDetail.hasWorkingCopy>
  <p>Kan du sette arbeidsversjonen for &laquo;${title}&raquo; til gjeldende versjon?</p>
<#else>
  <p>Kan du publisere &laquo;${title}&raquo;?</p>
</#if>

<p>Lenke: <a href="${uri?html}">${uri?html}</a></p>

<#if comment?has_content>
<pre>${comment}</pre>
</#if>

<p>Lenke til dokumentasjon: <a href="${uri?html}">${uri?html}</a></p>

<#t /><p>Med vennlig hilsen,<br/><#t />
<#t />${mailFromFullName}</p><#t />