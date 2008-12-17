<#if language == "no_NO">
<pre>
Hei!

${serverHostname} har en artikkel jeg tror kan være interessant for deg:
</pre>
<h2>${title}</h2>     
<pre>
${comment}
        
Les hele artikkelen her:
<a href="${articleFullUri}">${articleFullUri}</a>

Med vennlig hilsen,
${mailFrom}
</pre>
<em>
<pre>
--------------------------------------------
Denne meldingen er sendt på oppfordring fra ${mailFrom}
Din e-post adresse blir ikke lagret.
Du vil ikke motta flere meldinger av denne typen,
med mindre noen tipser deg om andre artikler på ${serverHostname}
</pre>
</em>
<#elseif language == "no_NO_NY">
<pre>
Hei!

${serverHostname} har ein artikkel eg trur kan vera interessant for deg:
</pre>
<h2>${title}</h2>
<pre>        
${comment}
        
Les heile artikkelen her:
<a href="${articleFullUri}">${articleFullUri}</a>

Med vennlig helsing,
${mailFrom}
</pre>
<em><pre>
--------------------------------------------
Denne meldinga er sendt på oppfordring frå ${mailFrom}
Di e-post adresse blir ikkje lagra.
Du vil ikkje motta fleire meldingar som dette,
med mindre nokon tipsar deg om andre artiklar på ${serverHostname}
</pre></em>
<#else>
<pre>
Hi!

${serverHostname} has an article I believe you will find interesting:
</pre>
<h2>${title}</h2>
<pre>       
${comment}
        
Read the entire article here:
<a href="${articleFullUri}">${articleFullUri}</a>

Best regards,
${mailFrom}
</pre>
<em><pre>
--------------------------------------------
This message is sent on behalf of ${mailFrom}
Your emailaddress will not be saved.
You will not receive further messages of this kind,
unless someone tips you of other articles on ${serverHostname}
</pre></em>
</#if>
