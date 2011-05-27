<p>Hei!</p>

<p>${site} har en artikkel jeg tror kan være interessant for deg:</p>

<h2>${title}</h2>     

<#if comment?exists && comment?has_content>
<p>${comment}</p>
</#if>


<p>Les hele artikkelen her: <a href="${uri?html}">${uri?string}</a></p>

<p>Med vennlig hilsen ${mailFrom}</p>

<hr />

<p>
Denne meldingen er sendt på oppfordring fra ${mailFrom}.
Din e-postadresse blir ikke lagret.
Du vil ikke motta flere meldinger av denne typen,
med mindre noen tipser deg om andre artikler på ${site}.
</p>
