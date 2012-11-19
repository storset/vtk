<p>Hei!</p>

<p>Kan du godkjenna denne?</p>

<h2>${title}</h2>

<#if comment?exists && comment?has_content>
<pre>${comment}</pre>
</#if>


<p>Lenkje til dokument: <a href="${uri?html}">${uri?html}</a></p>

<p>Med vennleg helsing ${mailFrom}</p>

<hr />
<p>
Denne meldinga er sendt på oppmoding frå ${mailFrom}.
Di e-postadresse vert ikkje lagra.
Du vil ikkje få tilsendt fleire meldinger som denne,
med mindre nokon sender deg andre dokumentar for godkjenning.
</p>
