<#ftl strip_whitespace=true>

<#--
  - File: preview-unavailable_nn.ftl
  - 
  - Description: A HTML page that displays a message that the
  - preview mode is not available for the current resource.
  - 
  - Required model data:
  -   resourceDetail
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
    <p class="previewUnavailable">Innholdet kan ikke forh&aring;ndsvises innenfor "Administrasjon av webdokumenter".</p> 
    <p>Men du kan se p&aring; eller laste ned dokumentet p&aring; webadressen:<br><a href="${resourceDetail.viewURL}">${resourceDetail.viewURL}</a></p>
  </body>
</html>
