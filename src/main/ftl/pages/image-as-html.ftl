<#import "/lib/vortikal.ftl" as vrtx />
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

<#if src?exists> 
  <img src="${src}" />
</#if>

<#assign photographer = vrtx.propValue(resource, "photographer") />
<#if photographer?exists && photographer != "">
	<div>Foto: ${photographer}</div>
</#if>

<h2>${vrtx.getMsg("","Kilde")}</h2>

<#assign pixelHeight = vrtx.propValue(resource, "pixelHeight") />
<#assign pixelWidth = vrtx.propValue(resource, "pixelWidth") />

<a href="${src}">${src}</a> (${pixelHeight}px x ${pixelWidth}px)

<#if description?exists >
  <h2>${vrtx.getMsg("","Beskrivelse")}</h2>
  <div id="vrtx-meta-description">
    ${description}
  </div>
</#if>

<#if uioWebCopyrightURL?exists >
	<h2>${vrtx.getMsg("uioWebCopyrightLinkText","Om bruk av bildet")}</h2>
	<a href="${uioWebCopyrightURL}?html">${vrtx.getMsg("","Opphavsrett på UiOs nettsider")}</a>
</#if>

</body>
</html>