<#if language == "no_NO">
<p>
Hei!
</p>
<p>
${serverHostname} har en artikkel jeg tror kan være interessant for deg:
</p>
<h2>${title}</h2>     
<p>
${comment}
</p> 
<p>       
Les hele artikkelen her:<br />
<a href="${articleFullUri}">${articleFullUri}</a>
</p>
<p>
Med vennlig hilsen,<br />
${mailFrom}
<p>

<p>
-------------------------------------------- <br />
Denne meldingen er sendt på oppfordring fra ${mailFrom}<br />
Din e-post adresse blir ikke lagret.<br />
Du vil ikke motta flere meldinger av denne typen,<br />
med mindre noen tipser deg om andre artikler på ${serverHostname}<br />
</p>
<#elseif language == "no_NO_NY">
<p>
Hei!
</p>
<p>
${serverHostname} har ein artikkel eg trur kan vera interessant for deg:
</p>
<h2>${title}</h2>
<p>        
${comment}
</p>
<p>        
Les heile artikkelen her: <br />
<a href="${articleFullUri}">${articleFullUri}</a>
</p>

<p>
Med vennlig helsing, <br />
${mailFrom}
</p>
<p>
--------------------------------------------<br />
Denne meldinga er sendt på oppfordring frå ${mailFrom}<br />
Di e-post adresse blir ikkje lagra.<br />
Du vil ikkje motta fleire meldingar som dette,<br />
med mindre nokon tipsar deg om andre artiklar på ${serverHostname}<br />
</p>
<#else>
<p>
Hi!
</p>
<p>
${serverHostname} has an article I believe you will find interesting:
</p>
<h2>${title}</h2>
<p>       
${comment}
</p>
<p>
Read the entire article here:<br />
<a href="${articleFullUri}">${articleFullUri}</a>
</p>

<p>
Best regards,<br />
${mailFrom}
</p>
<p>
--------------------------------------------<br />
This message is sent on behalf of ${mailFrom}<br />
Your emailaddress will not be saved.<br />
You will not receive further messages of this kind,<br />
unless someone tips you of other articles on ${serverHostname}<br />
</p>
</#if>
