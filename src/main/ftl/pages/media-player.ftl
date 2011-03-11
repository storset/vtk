
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/layouts/media-player.ftl" as mediaPlayer />

<#assign title = vrtx.propValue(resourceContext.currentResource, "title" , "flattened") />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<html>
<head>
	<title>${title}</title>
</head>
<body>

<h1>${title}</h2>

<@mediaPlayer.mediaPlayer />

<#if description?exists >
<div>
	${description}
</div>
</#if>
</body>
</html>
