<#ftl strip_whitespace=true>

<#--
  - File: preview-image.ftl
  - 
  - Description: A HTML page with a <img> tag to the previewed image
  - 
  - Required model data:
  -   resource
  -   resourceReference
  -   resourceContext
  -  
  - Optional model data:
  -   title
  -
  -->

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${(title.title)?default(resourceContext.currentResource.name)}</title>
</head>
  <body>
  
   <#assign previewRefreshParameter = 'outer-iframe-refresh' />
    <#assign constructor = "freemarker.template.utility.ObjectConstructor"?new() />
    <#assign dateStr = constructor("java.util.Date")?string("yyyymmddhhmmss") />

    <#assign url = previewImage.URL />
    <#assign url = url + "&amp;" + previewRefreshParameter + "=" + dateStr />
  
      <iframe class="preview" name="previewIframe" id="previewIframe" src="${url?html}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" vspace="0" hspace="0" style="overflow:visible; width:100%; ">
        [Your user agent does not support frames or is currently configured
        not to display frames. However, you may visit
        <a href="${resourceReference}">the related document.</a>]
      </iframe>
      
  </body>
</html>


