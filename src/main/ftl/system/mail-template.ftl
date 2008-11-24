<#if language == "no_NO">
Hei!

${serverHostnameShort} har en artikkel jeg tror kan v&aelig;re interessant for deg:
<h2>${title}</h2>
        
${comment}
        
Les hele artikkelen her:
${articleFullUri}

Med vennlig hilsen,
${mailFrom}

<em>
--------------------------------------------
Denne meldingen er sendt på oppfordring fra ${mailFrom}
Din e-post adresse blir ikke lagret.
Du vil ikke motta flere meldinger av denne typen,
med mindre noen tipser deg om andre artikler p&aring; ${serverHostname}
    </em>
<#elseif language == "no_NO_NY">
Hei!

${serverHostnameShort} har en artikkel eg trur kan v&aelig;ra interessant for deg::
<h2>${title}</h2>
        
${comment}
        
Les heile artikkelen her:
${articleFullUri}

Med vennlig helsing,
${mailFrom}

<em>
--------------------------------------------
Denne meldinga er sendt på oppfordring fr&aring;  ${mailFrom}
Di e-post adresse blir ikkje lagra.
Du vil ikkje motta fleire meldingar som dette,
med mindre nokon tipsar deg om andre artiklar p&aring; ${serverHostname}
</em>
<#else>
Hi!

${serverHostnameShort} have an article I think you will find interesting:
<h2>${title}</h2>
        
${comment}
        
Read the entire article here:
${articleFullUri}

Best regards,
${mailFrom}

<em>
--------------------------------------------
This message is sent on behalf of ${mailFrom}
Your emailaddress will not be saved.
You will not receive more messages of this type,
unless someone tip you of other articles on ${serverHostname}
</em>
</#if>
