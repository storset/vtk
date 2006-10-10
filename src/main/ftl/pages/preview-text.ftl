<#ftl strip_whitespace=true>

<#--
  - File: preview-text.ftl
  - 
  - Description: A HTML page with the contents of a text resource
  - inside a <pre> tag.
  - 
  - Required model data:
  -   resourceString
  -   resourceContext
  -  
  - Optional model data:
  -   title
  -
  -->

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>${(title.title)?default(resourceContext.currentResource.name)}</title>
  </head>
  <body>
    <pre class="preview">${resourceString?html}</pre>
  </body>
</html>
