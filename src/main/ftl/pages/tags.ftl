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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

  <#if tagElements?exists && !scope.URI.root>
     <#assign title><@vrtx.msg code="tags.serviceTitle" args=[scope.title] /></#assign>
  <#elseif tagElements?exists>
  	<#assign title>${vrtx.getMsg("tags.noTagTitle")}</#assign>
  <#elseif scope?exists && !scope.URI.root>
    <#assign title><@vrtx.msg code="tags.scopedTitle" args=[scope.title, tag] /></#assign>
  <#else>
    <#assign title><@vrtx.msg code="tags.title" args=[repositoryID, tag] /></#assign>
  </#if>

  <title>${title?html}
    <#if page?has_content>
      <#if "${page}" != "1"> - <@vrtx.msg code="viewCollectionListing.page" /> ${page}</#if>
    </#if>
  </title>

  <#if cssURLs?exists>
    <#list cssURLs as cssUrl>
       <link href="${cssUrl}" type="text/css" rel="stylesheet"/>
    </#list>
  </#if>
  
  <#if alternativeRepresentations?exists>
  <#list alternativeRepresentations as alt>
    <link rel="alternate" type="${alt.contentType?html}" title="${alt.title?html}" href="${alt.url?html}" />
  </#list>
 </#if>
 
 <meta name="robots" content="noindex"/> 
 
</head>

<body id="vrtx-tagview">
  <h1>${title}
    <#if page?has_content>
      <#if "${page}" != "1"> - <@vrtx.msg code="viewCollectionListing.page" /> ${page}</#if>
    </#if>
  </h1>  

  <#if tagElements?exists>
    
	<div id="vrtx-tags-service">
	    <ul class="vrtx-tag">
	     <#assign i = 0>
	     <#list tagElements as element>     
		       <li class="vrtx-tags-element-${i}">
		         <a class="tags" href="${element.linkUrl?html}" rel="tags">${element.text?html}</a>
		       </li>
		       <#assign i = i+1>
	     </#list>
	    </ul>
	</div>

  <#else>

    <#if listing?exists && listing.hasContent()>
  

     <#-- List resources: -->
     <div class="tagged-resources vrtx-resources">
      <#assign resources=listing.getFiles() />
      <#assign urls=listing.urls />
      <#assign displayPropDefs=listing.displayPropDefs />
      <#assign i = 1 />
      <#assign displayMoreURLs = true />

              <#list resources as resource>
                  <#assign resourceTitle = resource.getPropertyByPrefix("","title").getFormattedValue() />
                  <#assign introImageProp = resource.getPropertyByPrefix("","picture")?default("") />
                  
                  
                  <div class="vrtx-resource" id="vrtx-result-${i}">
                     
                        <#if introImageProp != "">
                          <a href="${resource.getURI()?html}" class="vrtx-image">
        		          <#assign src = vrtx.propValue(resource, 'picture', 'thumbnail') />
                            <img src="${src?html}" />
                          </a>
                        </#if>
                        <div class="vrtx-title">
                        <a href="${resource.getURI()?html}" class="vrtx-title"> ${resourceTitle?html}</a>
                        </div>
                        <#list displayPropDefs as displayPropDef>
                          <#if displayPropDef.name = 'introduction'>
                            <#assign val = vrtx.getIntroduction(resource) />
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
                              <#if displayPropDef.name = 'introduction'>
                                  <#assign hasBody = vrtx.propValue(resource, 'hasBodyContent') == 'true' />
                                  <#if displayMoreURLs && hasBody>
                                  	  <div class="vrtx-read-more">
                                      <a href="${listing.urls[resource.URI]?html}" class="more">
                                      <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
                                      </a>
                                      </div>
                                  </#if>
                              </#if>
                              </div>
                          </#if> 
                        </#list>

                   </div> <!-- end class result -->
                  <#assign i = i + 1 />
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
	
	  <#if alternativeRepresentations?exists>
	    <#list alternativeRepresentations as alt>
	      <#if alt.contentType = 'application/atom+xml'>
	        <div class="vrtx-feed-link">
	          <a id="vrtx-feed-link" href="${alt.url?html}"><@vrtx.msg code="viewCollectionListing.feed.fromThis" /></a>
	        </div>
	        <#break />
	      </#if>
	    </#list>
	</#if>

    <#else> <#-- no resources found for tag -->
      <p>
        ${vrtx.getMsg("tags.notFound")} <span class="italic">${tag}</span>.
      </p>
    </#if>
  </#if>
</body>
</html>
