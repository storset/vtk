<p>Hei!</p>

<p>Kan du godkjenne denne?</p>

<h2>${title}</h2>     

<#if comment?has_content>
<pre>${comment}</pre>
</#if>


<p>Lenke til dokument: <a href="${uri?html}">${uri?html}</a><br />
Lenke til dokumentasjon:  <a href="${uri?html}">${uri?html}</a></p>

<p>Med vennlig hilsen,<br/>
${mailFrom}</p>

<hr />

<p>
Denne meldingen er sendt på oppfordring fra ${mailFrom}.
Din e-postadresse blir ikke lagret.
Du vil ikke motta flere meldinger av denne typen,
med mindre noen sender deg andre dokumenter for godkjenning.
</p>
