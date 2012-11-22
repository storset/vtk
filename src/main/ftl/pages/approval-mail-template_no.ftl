<p>Hei!</p>

<#if resourceDetail?? && resourceDetail.hasWorkingCopy?? && resourceDetail.hasWorkingCopy>
  <p>Kan du sette arbeidsversjonen for "${title}" til gjeldende versjon?</p>
<#else>
  <p>Kan du publisere "${title}"?</p>
</#if>

<p>Lenke: <a href="${uri?html}">${uri?html}</a></p>

<#if comment?has_content>
<pre>${comment}</pre>
</#if>

<p>Lenke til dokumentasjon: <a href="${uri?html}">${uri?html}</a></p>

<#t /><p>Med vennlig hilsen,<br/><#t />
<#t />${mailFromFullName}</p><#t />