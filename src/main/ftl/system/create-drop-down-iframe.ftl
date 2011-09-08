<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/iframe-admin.js"></script>

<#-- current URL to use in hash communication with iframe (Opera and IE 7) -->
<#assign origUrl = resourceContext.currentURI + "?vrtx=admin" />

<iframe id="create-iframe" src="" allowTransparency="true" height="50px" width="200px">
  [Du har ikke iframe]
</iframe>

<script type="text/javascript">
   var href = location.href;
   var iframe = document.getElementById("create-iframe");
   iframe.src = "${create.url}" + "#" + href;
</script>
