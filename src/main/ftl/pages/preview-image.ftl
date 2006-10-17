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

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head><title>${(title.title)?default(resourceContext.currentResource.name)}</title>
</head>
  <body>
    <p class="preview">
      <img class="preview" src="${resourceReference}" alt="${resource.name}"/>
    </p>
  </body>
</html>


