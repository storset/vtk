<#--
  - File: pages/embed-media-player.ftl
  - 
  - Description: Web page display of embedded single local media resource.
  - 
  - This template requires "mediaResource" in model data and is not meant for display
  - of non-local media.
  -->

<#import "/lib/vortikal.ftl" as vrtx />
<#import "/layouts/media-player.ftl" as mediaPlayer />

<#assign title = vrtx.propValue(mediaResource, "title" , "flattened") />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>
  <style>
    body {background-color: #000; height: 100%; width: 100%; margin: 0; padding: 0;}
  </style>
</head>
<body>

<#assign dateStr = 0 />
<#if nanoTime?has_content><#assign dateStr = nanoTime?c /></#if>
<@mediaPlayer.mediaPlayer dateStr />

</body>
</html>
