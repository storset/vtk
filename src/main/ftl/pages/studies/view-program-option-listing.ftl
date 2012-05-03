<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#assign resource = collection />
<#assign title = vrtx.propValue(resource, "title") />
<#if overriddenTitle?has_content>
  <#assign title = overriddenTitle />
</#if>

<#macro displayProgramOptions programOptionListing>
  <#local programOptions=programOptionListing.files />
  <#if (programOptions?size > 0) >
    <div id="${programOptionListing.name}" class="vrtx-programOptions articleListing.searchComponent ${programOptionListing.name}">
    
    <h2>${programOptionListing.name?html}</h2>
    
    <#local locale = springMacroRequestContext.getLocale() />
 
    <#list programOptions as programOption>
      <#local title = vrtx.propValue(programOption, 'title') />
      <#local introImg = vrtx.prop(programOption, 'picture')  />
      <#local intro = vrtx.prop(programOption, 'introduction')  />
      <div class="vrtx-default-article">
            <#if introImg?has_content && programOptionListing.hasdisplayPropDef(introImg.definition.name) >
                <#local introImgURI = vrtx.propValue(programOption, 'picture') />
                <#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
                <a class="vrtx-image" href="${programOptionListing.urls[programOption.URI]?html}">
                    <img src="${thumbnail?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
                </a>
            </#if>
            <div class="vrtx-title">
              <a class="vrtx-title summary" href="${programOptionListing.urls[programOption.URI]?html}">${title?html}</a>
            </div>
            <#if intro?has_content && programOptionListing.hasdisplayPropDef(intro.definition.name)>
              <div class="description introduction"><@vrtx.linkResolveFilter intro.value programOptionListing.urls[programOption.URI]  requestURL /></div>
            </#if>
             <div class="vrtx-read-more">
              <a href="${programOptionListing.urls[programOption.URI]?html}" class="more">
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
        <@displayProgramOptions searchComponent />
    </#list>
</#if>

</body>
</html>


