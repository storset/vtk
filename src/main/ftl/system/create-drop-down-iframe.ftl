<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/iframe-admin.js"></script>
<style type="text/css">
  #create-iframe {
    height: 40px;
    width: 150px;
    overflow: visible;
    margin: 0;
    padding: 0;
    background-color: transparent;
  }
</style>

<iframe id="create-iframe" src="${create.url}#${resourceContext.currentServiceURL}" allowTransparency="true" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" vspace="0" hspace="0">
  [Du har ikke iframe]
</iframe>

<script type="text/javascript">
   // avoid flickering by set real height after 500ms
   setTimeout(function() {
     $("#create-iframe").css("height", "100px");
   }, 500);
</script>