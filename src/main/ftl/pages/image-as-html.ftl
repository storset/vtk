<#ftl strip_whitespace=true>
<#--
  - File: image-as-html.ftl
  - 
  - Description: TODO
  -
  - Optional model data:
  -   TODO
  -->
  
<#import "/lib/vortikal.ftl" as vrtx />
<#assign lang><@vrtx.requestLanguage/></#assign>

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
<body id="vrtx-image-preview">

<#if resource.name != title >
  <h1>${title}</h1>
</#if>

<#if src?exists>
  <div class="vrtx-introduction-image">
     <a href="${src}"><img src="${src}" alt="image" /></a>
  </div>
</#if>

<#assign pixelHeight = vrtx.propValue(resource, "pixelHeight") />
<#assign pixelWidth = vrtx.propValue(resource, "pixelWidth") />

<p id="vrtx-image-view-link">
  ${vrtx.getMsg('imageAsHtml.source')}: <a href="${src}">${resource.name?html}</a>
  <#if pixelHeight != "" && pixelWidth != "">
    &nbsp;(${pixelHeight}px x ${pixelWidth}px)
  </#if>
</p>

<#assign photographer = vrtx.propValue(resource, "photographer") />
<#if photographer?exists && photographer != "">
  <p>
    ${vrtx.getMsg('imageAsHtml.byline')}: ${photographer}
  </p> 
</#if>


<#if description?exists >
  <div id="vrtx-meta-description">
    ${description}
  </div>
</#if>

<p>
  <#if .vars["copyrightHelpURL." + lang]?exists>
  <#assign url = .vars["copyrightHelpURL." + lang] />
  <#if url?exists>${url}</#if>
  </#if>
</p>

</body>
</html>
