<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#assign resource = collection />
<#assign title = vrtx.propValue(resource, "title") />
<#if overriddenTitle?has_content>
  <#assign title = overriddenTitle />
</#if>

<#macro displayPrograms programListing>
  <#local programs=programListing.files />
  <#if (programs?size > 0) >
    <#if sort?exists && sort == "alphabetical">
      <ul id="${programListing.name}" class="vrtx-programs programListing.searchComponent ${programListing.name}">
    <#else>
      <div id="${programListing.name}" class="vrtx-programs programListing.searchComponent ${programListing.name}">
        <h2>${programListing.name?html}</h2>
    </#if>

    <#local locale = springMacroRequestContext.getLocale() />

    <#list programs as program>
      <#local title = vrtx.propValue(program, 'title') />
      <#local introImg = vrtx.prop(program, 'picture')  />

      <#if sort?exists && sort == "alphabetical">
        <#local intro = vrtx.prop(program, 'introduction')  />
        <li>
          <#if title?exists>
            <h2><a href="${programListing.urls[program.URI]?html}">${title?html}</a></h2>
          </#if>
          <#if intro?has_content && programListing.hasDisplayPropDef(intro.definition.name)>
            <div class="description introduction"><@vrtx.linkResolveFilter intro.value programListing.urls[program.URI]  requestURL /></div>
          </#if>
          <div class="vrtx-program-buttons">
            <a class="button vrtx-program-read-more" href="${programListing.urls[program.URI]?html}"><span>Mer om programmet</span></a>
            <a class="button vrtx-course-how-search" href="#"><span>Hvordan søke?</span></a>
          </div>
        </li>
      <#else>
        <#local idxPlusOne = program_index + 1 />
        <#if (idxPlusOne % 3 == 1)>
          <#local position = "left" />
          <div class="vrtx-program-row">
        <#elseif (idxPlusOne % 2 == 0)>
          <#local position = "middle" />
        <#else>
          <#local position = "right" />
        </#if>
        <div class="vrtx-frontpage-box white-box super-wide-picture third-box-${position}"> 
          <#if title?exists>
            <h3><a href="${programListing.urls[program.URI]?html}">${title?html}</a></h3>
          </#if>
          <#if introImg?has_content && programListing.hasDisplayPropDef(introImg.definition.name) >
            <div class="vrtx-frontpage-box-picture">
              <#local introImgURI = vrtx.propValue(program, 'picture') />
              <#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
              <a href="${programListing.urls[program.URI]?html}">
                <img src="${thumbnail?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
              </a>
            </div>
          </#if>
        </div>
        <#if (position == "right" || idxPlusOne == programs?size)>
          </div>
        </#if>
      </#if>
    </#list>
   <#if sort?exists && sort == "alphabetical">
     </ul>
   <#else>  
     </div>
   </#if>
  </#if>
</#macro>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<#if cssURLs?exists>
  <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" type="text/css" />
  </#list>
</#if>
<#if printCssURLs?exists>
  <#list printCssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" media="print" type="text/css" />
  </#list>
</#if>
<#if jsURLs?exists>
  <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
  </#list>
</#if> 
<#if alternativeRepresentations?exists && !(hideAlternativeRepresentation?exists && hideAlternativeRepresentation)>
  <#list alternativeRepresentations as alt>
   <link rel="alternate" type="${alt.contentType?html}" title="${alt.title?html}" href="${alt.url?html}" />
    </#list>
</#if>

<title>${title?html}</title>
</head>
<body id="vrtx-${resource.resourceType}">

<h1>${title?html}</h1>
<#if searchComponents?has_content>
    <#list searchComponents as searchComponent>
        <@displayPrograms searchComponent />
    </#list>
</#if>

</body>
</html>


