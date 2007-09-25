<#ftl strip_whitespace=true>

<#--
  - File: preview-iframe.ftl
  - 
  - Description: A HTML page with a <iframe> tag to the previewed resource
  - 
  - Dynamic resizing of iframe only works in IE and Firefox. 
  - 
  - Required model data:
  -   resourceReference
  -   resourceContext
  -  
  - Optional model data:
  -   title
  -
  -->

<#if !resourceReference?exists>
  <#stop "Unable to render model: required submodel
  'resourceReference' missing">
</#if>
<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head><title>${(title.title)?default(resourceContext.currentResource.name)}</title>
    <script language="JavaScript" type="text/javascript"><!-- 
      function pageLoaded() {

          if (!window.frames['previewIframe'] || !window.frames['previewIframe'].document)
            return;

          var links = null;

          if (document.getElementsByTagName) {
             links = window.frames['previewIframe'].document.getElementsByTagName('a');
          } else {
             links = window.frames['previewIframe'].document.links;
          }                   

          if (!links) return; 

          for (i = 0; i < links.length; i++) {
              if (!links[i].href) continue;
              links[i].target = '_parent';
          }
      }
      // -->
    </script>
  </head>
  <body onload="pageLoaded()" onresize="dyniframesize()">

    <#if !previewRefreshParameter?exists>
      <#assign previewRefreshParameter = 'force-refresh' />
    </#if>

    <#assign constructor = "freemarker.template.utility.ObjectConstructor"?new() />
    <#assign dateStr = constructor("java.util.Date")?string("yyyymmddhhmmss") />    

    <#assign url = resourceReference />
    <#if url?contains("?")>
      <#assign url = url + "&amp;" + previewRefreshParameter + "=" + dateStr />
    <#else>
        <#assign url = url + "?" + previewRefreshParameter + "=" + dateStr />
    </#if>

    <iframe class="preview" name="previewIframe" id="previewIframe" src="${url}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" vspace="0" hspace="0" style="overflow:visible; width:100%; ">
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <A href="${resourceReference}">the related document.</A>]
    </iframe>

    <#-- iframe name="previewIframe" id="previewIframe" class="preview" src="${url}">
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <A href="${resourceReference}">the related document.</A>]
    </iframe -->

    <#--
    <noframes>
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <A href="${resourceReference}">the related document.</A>]
    -->


    <#-- We could also use the 'object' tag: -->
    <#-- 
    <object id="previewObject" class="preview" data="${resourceReference}"
            type="${resourceContext.currentResource.*contentType*}">
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <A href="${resourceReference}">the related document.</A>]
    </object>
    -->


  </body>
</html>


