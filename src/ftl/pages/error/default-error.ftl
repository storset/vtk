<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
 <title>500 - Internal Server Error</title>
  <link href="http://www.uio.no/visuell-profil/css/uio.css" type="text/css" rel="stylesheet"/>
</head>
<body>

<DIV CLASS="navmarginbrod" ID="brodtekst">

<H2>500 - ${error.errorDescription}</H2>

Siden <STRONG>${(resourceContext.currentURI)?if_exists}</STRONG> kan
ikke vises fordi en feil har oppstått.

<P>
Feilmeldingen er: ${error.exception.message?default('Ingen feilmelding')?html}
<P>
Dersom du ønsker å rapportere feilen, kan du se <A
HREF="http://www.uio.no/hjelp/kontakt.htm">"Kven svarar ved UiO?"</A>.


 <HR STYLE="width: 98%;">

 <H2>500 - ${error.errorDescription}</H2>

The web page <STRONG>${(resourceContext.currentURI)?if_exists}</STRONG>
cannot be displayed due to an error.

<P>The error message is: ${error.exception.message?default('No message')?html}

<P>If you wish to report the error, you can check <A
HREF="http://www.uio.no/english/contact.html">"Who do I contact for
more information and how?"</A>.

<#if debug>
<HR STYLE="width: 98%;">
<#include "/lib/error-detail.ftl" />
</#if>

</DIV>

<DIV CLASS="navmarginline" ID="bunnnav"><BR>
<A HREF="http://www.uio.no/hjelp/kontakt.htm">Kontakt&nbsp;UiO</A>
&nbsp;&nbsp;&nbsp;<A HREF="http://www.uio.no/hjelp/">Hjelp</A>
<P>Server-administrator: <A HREF="mailto:webmaster@uio.no">webmaster@uio.no</A>
</DIV>
</BODY>
</HTML>
