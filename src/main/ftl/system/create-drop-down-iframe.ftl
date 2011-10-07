<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/system/create-drop-down.ftl" as dropdownUtils />

<#if preview>
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/iframe-admin.js"></script>

  <iframe id="create-iframe" src="${create.url?html}#${resourceContext.currentServiceURL?html}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0">
    [Du har ikke iframe]
  </iframe>

  <script type="text/javascript"><!--
     $(function() {
       $("#create-iframe")[0].allowTransparency = true;
     });
     setTimeout(function() { // avoid flickering by set real height after 500ms
       $("#create-iframe").css("height", "135px");
     }, 500);
     
  // -->
  </script>
<#else>
  <@dropdownUtils.genDropdown />
</#if>