NB: 

Filene 
 * /src/main/ext/kupu/v1.3.9/common/kupupopups/link.html 
 * /src/main/ext/kupu/v1.3.9/common/kupupopups/image.html 
 er endret i selve Kupu-pakken (i stedet for å bli overstyrt her i kupu-resources)

Følgende endring gjort (en referanse er lagt til i <head>):

<script type="text/javascript" src="../kupuhelpers.js"> </script>
(og inkludering av kupubaseresources.js for image.html)

Dette fordi filene kalles dynamisk, og for å overstyre på vanlig måte endringen må man overstyrt flere javascript-filer...
(dette er uansett en bug i Kupu, og som vi skal prøve å få commitet til Kupu-prosjektet)

I tillegg er 
 * /src/main/ext/kupu/v1.3.9/common/kupubasetools.js   (ca linje 580)
også foreløpig endret direkte i Kupu (dette skal overstyres når vi ser at det virker i alle browsere og ikke gir noen utilsiktede feil)  
