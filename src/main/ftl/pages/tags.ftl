<#ftl strip_whitespace=true>

<#--
  - File: tags.ftl
  - 
  - Description: Article view
  - 
  - Required model data:
  -   resource
  -   tag
-->

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />


<#-- XXX: remove this when properties 'introduction' and 'description'
     are merged: -->
<#function getIntroduction resource>
  <#local introduction = vrtx.propValue(resource, "introduction") />
  <#if !introduction?has_content>
    <#local introduction = vrtx.propValue(resource, "description", "", "content") />
  </#if>
  <#return introduction />
</#function>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#if error?exists>
    <#assign title = vrtx.getMsg("tags.noTagTitle") />
  <#elseif scope?exists>
    <#assign title><@vrtx.msg code="tags.scopedTitle" args=[scope.title,tag] /></#assign>
  <#else>
    <#assign title><@vrtx.msg code="tags.title" args=[tag] /></#assign>
  </#if>

  <title>${title}</title>

  <#if cssURLs?exists>
    <#list cssURLs as cssUrl>
       <link href="${cssUrl}" type="text/css" rel="stylesheet"/>
    </#list>
  </#if>
 
</head>

<body id="vrtx-tagview">
  <h1>${title}</h1>

  <#if error?exists>
    <p>${error}</p>

  <#else>

    <#if searchComponents?exists && searchComponents?has_content>
  

     <#-- List resources: -->
     <div class="tagged-resources">
      <#assign searchComponent=searchComponents[0]>
      <#assign resources=searchComponent.getFiles() />
      <#assign urls=searchComponent.urls />
      <#assign displayPropDefs=searchComponent.displayPropDefs />
      
      
      <#list resources as resource>
          <#assign resourceTitle = resource.getPropertyByPrefix("","title").getFormattedValue() />
          
          <#assign introImageProp = resource.getPropertyByPrefix("","picture")?default("") />
          
          
          <div class="result">
             
                <#if introImageProp != "">
                  <a id="${resource.name}" href="${resource.getURI()?html}">
                    <#assign src = introImageProp.formattedValue />
                    <#if !src?starts_with("/") && !src?starts_with("http://") && !src?starts_with("https://")>
                      <#assign src = resource.URI.getParent().extendAndProcess(src) />
                    </#if>
                    <img class="introduction-image" 
                         alt="IMG for '${resourceTitle?html}'"
                         src="${src?html}" />
                  </a>
                </#if>

                 <h2 class="title">
                  <a id="${resource.name}" href="${resource.getURI()?html}">
                    ${resourceTitle?html}
                  </a>
                </h2>
               
                <#list displayPropDefs as displayPropDef>
                  <#if displayPropDef.name = 'introduction'>
                    <#assign val = getIntroduction(resource) />
                  <#elseif displayPropDef.type = 'IMAGE_REF'>
                    <#assign val><img src="${vrtx.propValue(resource, displayPropDef.name, "")}" /></#assign>
                  <#elseif displayPropDef.name = 'lastModified'>
                    <#assign val>
                      <@vrtx.msg code="viewCollectionListing.lastModified"
                                 args=[vrtx.propValue(resource, displayPropDef.name, "long")] />
                    </#assign>
                  <#else>
                    <#assign val = vrtx.propValue(resource, displayPropDef.name, "long") /> <#-- Default to 'long' format -->
                  </#if>
        
                  <#if val?has_content>
                    <div class="${displayPropDef.name}">
                      ${val}
                    </div>
                  </#if>
                </#list>
                
    
           </div> <!-- end class result -->

        </#list>
      </div> <!-- end class tagged-resources -->
      
     <#-- Previous/next URLs: -->

     <#if prevURL?exists>
       <a class="vrtx-previous" href="${prevURL?html}"><@vrtx.msg code="viewCollectionListing.previous" /></a>
     </#if>
     <#if nextURL?exists>
       <a class="vrtx-next" href="${nextURL?html}"><@vrtx.msg code="viewCollectionListing.next" /></a>
     </#if>

    <#-- XXX: display first link with content type = atom: -->
    <#--
    <#list alternativeRepresentations as alt>
      <#if alt.contentType = 'application/atom+xml'>
        <div class="vrtx-feed-link">
          <a id="vrtx-feed-link" href="${alt.url?html}"><@vrtx.msg code="viewCollectionListing.feed.fromThis" /></a>
        </div>
        <#break />
      </#if>
    </#list>
    -->


      
      
      
    <#else>
      <p>
        ${vrtx.getMsg("tags.notFound")} <span class="italic">${tag}</span>.
      </p>
    </#if>
  </#if>
</body>
</html>
