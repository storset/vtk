<#import "/lib/vortikal.ftl" as vrtx />
<#import "/layouts/media-player.ftl" as mediaPlayer />

<#assign title = vrtx.propValue(resourceContext.currentResource, "title" , "flattened") />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
      <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
</head>
<body>

<h1>${title}</h1>

<#if description?exists >
  <div id="vrtx-meta-description">
    ${description}
  </div>
</#if>

<@mediaPlayer.mediaPlayer />

</body>
</html>