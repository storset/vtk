<#ftl strip_whitespace=true>

<#--
  - File: audio.ftl
  - 
  - Description: A HTML page with an <object> tag to the audio file
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

    <h1><@property "author" /> - <@property "title" /></h1>

    Låt: <@property "title" /><br>
    Artist: <@property "author" /><br>
    Album: <@property "album" /><br>

    <p class="audio">
      <object type="audio/mpeg" width="200" height="50" data="${resourceContext.currentResource.URI}?showresource">
        <param name="src" value="${resourceContext.currentResource.URI}?showresource" />
        <param name="filename" value="${resourceContext.currentResource.URI}?showresource" />
        <param name="type" value="audio/mpeg" />
        Audio File
      </object>
    </p>
    <p>

      <a href="${resourceContext.currentResource.URI}?showresource">Last ned filen</a>
    </p>
  </body>
</html>

<#macro property name>
  <#compress>
    ${resourceContext.currentResource.getPropertyByPrefix(prefix, name).getStringValue()}
  </#compress>
</#macro>

