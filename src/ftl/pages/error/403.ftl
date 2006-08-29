<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>403 - Forbidden</title>
  <link href="http://www.uio.no/visuell-profil/css/uio.css" type="text/css" rel="stylesheet">
</head>
<body>
<DIV CLASS="navmarginbrod" ID="brodtekst">

<H2>403 - Ikke lesetilgang til dokumentet</H2>

<P>Du er logget inn, men du (${resourceContext.principal.name?if_exists}) har ikke tilgang til å lese dokumentet: <br><STRONG>${resourceContext.currentURI?if_exists}</STRONG>.</P>

<P>
Hvis du mener at du bør ha tilgang til dette dokumentet, kan du se <A
HREF="http://www.uio.no/hjelp/kontakt.htm">"Kven svarar ved UiO?"</A>.
</P>

<HR STYLE="width: 98%;">

<H2>403 - Access denied</H2>

<P>The web page <STRONG>${resourceContext.currentURI?if_exists}</STRONG>
is restricted to a specific set of users.</P> 

<P>For more information check <A HREF="http://www.uio.no/english/contact.html">"Who do I contact for
more information and how?"</A>.</P>
</DIV>
<DIV CLASS="navmarginline" ID="bunnnav">
  <A HREF="http://www.uio.no/hjelp/kontakt.htm">Kontakt&nbsp;UiO</A>

  &nbsp;&nbsp;&nbsp;<A HREF="http://www.uio.no/hjelp/">Hjelp</A>
  <P>Server-administrator: <A HREF="mailto:webmaster@uio.no">webmaster@uio.no</A></P>
</DIV>

<#if debug>
<HR STYLE="width: 98%;">
<#include "/lib/error-detail.ftl" />
</#if>

</body>
</html>
