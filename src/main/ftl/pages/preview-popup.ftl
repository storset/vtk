<#ftl strip_whitespace=true>

<#--
  - File: preview-popup.ftl
  - 
  - Description: Display a link to a popup window with a message
  - explaining the reason(s) why the resource cannot be previewed in
  - an iframe (would likely generate a mixed-content warning in the browser).
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Document cannot be previewed</title>
  </head>
  <body>
    <h2>Document cannot be previewed</h2>
    <p>But you can preview the web page if you want in a separate window.</p>
    <a class="vrtx-button" href="${resourceReference?html}" target="vrtx_preview_popup"><span>Open</span></a>.
    <hr />
    <p>Reasons that preview is not available:</p>
    <#assign prop = vrtx.getProp(resourceContext.currentResource, 'sslMixedMode') />
    <ul>
    <#list prop.values as v>
      <li>${v?html}</li>
    </#list>
    </ul>
  </body>
</html>
