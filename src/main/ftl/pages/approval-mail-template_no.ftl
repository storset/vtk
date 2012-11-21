<p>Hei!</p>

<#if resourceDetail?? && resourceDetail.hasWorkingCopy?? && resourceDetail.hasWorkingCopy>
  <p>Kan du sette denne arbeidsversjonen til gjeldende versjon?</p>
<#else>
  <p>Kan du publisere dette dokumentet?</p>
</#if>

<h2>${title}</h2>     

<#if comment?has_content>
<pre>${comment}</pre>
</#if>

<p>Lenke til dokument: <a href="${uri?html}">${uri?html}</a></p>
<p>Lenke til dokumentasjon: <a href="${uri?html}">${uri?html}</a></p>

<p>Med vennlig hilsen, ${mailFromFullName}</p>