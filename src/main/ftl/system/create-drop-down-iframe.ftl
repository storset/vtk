<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/iframe-admin.js"></script>
<style type="text/css">
  #create-iframe {
    height: 30px;
    width: 180px;
    overflow: visible;
    margin: 0;
    padding: 0;
    background-color: transparent;
  }
</style>

<iframe id="create-iframe" src="" allowTransparency="true" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" vspace="0" hspace="0">
  [Du har ikke iframe]
</iframe>

<script type="text/javascript">
   var href = location.href;
   var iframe = document.getElementById("create-iframe");
   iframe.src = "${create.url}" + "#" + href;
   
   // avoid flickering by set real height after 250ms
   setTimeout(function() {
     $(iframe).css("height", "100px");
   }, 250);
</script>
