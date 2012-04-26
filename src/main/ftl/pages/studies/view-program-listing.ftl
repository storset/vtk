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
    <div id="${programListing.name}" class="vrtx-programs articleListing.searchComponent ${programListing.name}">
    
    <h2>${programListing.name?html}</h2>
    
    <#local locale = springMacroRequestContext.getLocale() />
 
    <#list programs as program>
      <#local title = vrtx.propValue(program, 'title') />
      <#local introImg = vrtx.prop(program, 'picture')  />
      <#local intro = vrtx.prop(program, 'introduction')  />
      <div class="vrtx-default-article">
            <#if introImg?has_content && programListing.hasDisplayPropDef(introImg.definition.name) >
                <#local introImgURI = vrtx.propValue(program, 'picture') />
                <#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
                <a class="vrtx-image" href="${programListing.urls[program.URI]?html}">
                    <img src="${thumbnail?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
                </a>
            </#if>
            <div class="vrtx-title">
              <a class="vrtx-title summary" href="${programListing.urls[program.URI]?html}">${title?html}</a>
            </div>
            <#if intro?has_content && programListing.hasDisplayPropDef(intro.definition.name)>
              <div class="description introduction"><@vrtx.linkResolveFilter intro.value programListing.urls[program.URI]  requestURL /></div>
            </#if>
             <div class="vrtx-read-more">
              <a href="${programListing.urls[program.URI]?html}" class="more">
                <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
              </a>
            </div>
      </div>
    </#list>
    
   </div>
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
        <!-- title master, bachelore jeje-->
        <@displayPrograms searchComponent />
    </#list>
</#if>

</body>
</html>


