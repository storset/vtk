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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${(title.title)?default(resourceContext.currentResource.name)}</title>
</head>
  <body>

    <h1><@property "author" /> - <@property "title" /></h1>

    Låt: <@property "title" /><br>
    Artist: <@property "author" /><br>
    Album: <@property "album" /><br>

    <p class="audio">
      <embed src="/vrtx/mp3player.swf" 
             flashvars="file=${resourceContext.currentResource.URI?html}%3Fshowresource&type=mp3" 
             type="application/x-shockwave-flash" 
             pluginspage="http://www.macromedia.com/go/getflashplayer" 
             height="20" 
             width="385" />
    </p>
    <p>

      <a href="${resourceContext.currentResource.URI?html}?showresource">Last ned filen</a>
    </p>
  </body>
</html>

<#macro property name>
  <#compress>
    ${resourceContext.currentResource.getPropertyByPrefix(prefix, name).getStringValue()}
  </#compress>
</#macro>

