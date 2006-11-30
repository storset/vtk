NB: 

Filen /src/main/ext/kupu/v1.3.9/common/kupupopups/link.html er endret i selve Kupu-pakken (i stedet for å bli overstyrt her i kupu-resources)

Følgende endring gjort (en referanse er lagt til i <head>):

<script type="text/javascript" src="../kupuhelpers.js"> </script>

Dette fordi filen kalles dynamisk, og for å overstyre på vanlig måte endringen må man overstyrt flere javascript-filer...
(dette er uansett en bug i Kupu, og som vi skal prøve å få commitet til Kupu-prosjektet) 