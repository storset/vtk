<#ftl strip_whitespace=true>

<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />
<#import "/lib/collections/view-event-listing.ftl" as events />
<#import "/lib/collections/view-project-listing.ftl" as projects />
<#import "/lib/collections/view-person-listing.ftl" as persons />

<#macro displayTagElements tagElements showOccurences=false splitInThirds=false limit=0>
  <div id="vrtx-tags-service">
  
  <#local count = 1 />
  
  <#-- Split in thirds -->
  <#if splitInThirds>
    <#assign tagElementsSize = tagElements?size />
    <#if (limit > 0 && tagElementsSize > limit)>
      <#assign tagElementsSize = limit />  
    </#if>
    <#local colOneCount = vrtx.getEvenlyColumnDistribution(tagElementsSize, 1, 3) />
    <#local colTwoCount = vrtx.getEvenlyColumnDistribution(tagElementsSize, 2, 3) />
    <#local colThreeCount = vrtx.getEvenlyColumnDistribution(tagElementsSize, 3, 3) />
    <ul class="vrtx-tag thirds-left">
    <#list tagElements as element>
    
      <#-- Tag element -->
      <li class="vrtx-tags-element-${count}">
        <a class="tags" href="${element.linkUrl?html}" rel="tags">${element.text?html}<#if showOccurences> (${element.occurences?html})</#if></a>
      </li>
      
      <#if (count = colOneCount && colTwoCount > 0)>
        </ul><ul class="vrtx-tag thirds-middle">
      </#if>
      <#if ((count = colOneCount + colTwoCount) && colThreeCount > 0)>
        </ul><ul class="vrtx-tag thirds-right">
      </#if>
      
      <#-- Limit -->
      <#if count = limit>
        <#break>
      </#if> 
      <#local count = count + 1 />
    </#list>
    </ul>
  <#else>
    <ul class="vrtx-tag">
    <#list tagElements as element>
    
      <#-- Tag element -->
      <li class="vrtx-tags-element-${count}">
        <a class="tags" href="${element.linkUrl?html}" rel="tags">${element.text?html}<#if showOccurences> (${element.occurences?html})</#if></a>
      </li>
      
      <#-- Limit -->
      <#if count = limit>
        <#break>
      </#if>
      <#local count = count + 1 />
    </#list>
    </ul>

  </#if>
  
  </div>
</#macro>

<#macro displayAlphabeticalTagElements alphabeticalTagElementsChunks showOccurences=false>
  <div id="vrtx-tags-service">
  
  <#local count = 1 />

  <div id="vrtx-tags-alphabetical-tabs">
    <ul style="display: none">
      <#list alphabeticalTagElementsChunks?keys as alphabeticalTagElementsChunk>
        <li><a href="#vrtx-tags-alphabetical-${alphabeticalTagElementsChunk}" name="vrtx-tags-alphabetical-${alphabeticalTagElementsChunk}">${alphabeticalTagElementsChunk?upper_case}</a></li>
      </#list>
    </ul>
    <#list alphabeticalTagElementsChunks?keys as alphabeticalTagElementsChunkKey>
      <div id="vrtx-tags-alphabetical-${alphabeticalTagElementsChunkKey}">
        <#local alphabeticalTagElementsChunk = alphabeticalTagElementsChunks[alphabeticalTagElementsChunkKey]>
        <#list alphabeticalTagElementsChunk?keys as alphabeticalTagElements>
          <h2>${alphabeticalTagElements?upper_case}}</h2>
          <ul class="vrtx-tag">
            <#list alphabeticalTagElementsChunk[alphabeticalTagElements] as element>
              <#-- Tag element -->
              <li class="vrtx-tags-element-${count}">
                <a class="tags" href="${element.linkUrl?html}" rel="tags">${element.text?html}<#if showOccurences> (${element.occurences?html})</#if></a>
              </li>
              <#local count = count + 1 />
            </#list>
          </ul>
        </#list>
      </div>
    </#list>
  </div>
  
  </div>
</#macro>

<#macro displayTagListing listing>
  
  <div class="tagged-resources vrtx-resources">
    <#if resourceType??>
      <#if resourceType = 'person'>
        <@persons.displayPersons listing />
      <#elseif resourceType = 'structured-project'>
        <@projects.displayProjects listing />
      <#elseif resourceType = 'event' || resourceType = 'structured-event'>
        <@events.displayEvents
          collection=collection hideNumberOfComments=hideNumberOfComments displayMoreURLs=true considerDisplayType=false />
      <#else>
        <@displayCommonTagListing listing />
      </#if>
    <#else>
      <@displayCommonTagListing listing />
    </#if>
  </div>
  
  <div class="vrtx-paging-feed-wrapper">
  	<#if pageThroughUrls?? >
		<@viewutils.displayPageThroughUrls pageThroughUrls page />
	</#if>
    
    <#if alternativeRepresentations??>
      <#list alternativeRepresentations as alt>
        <#if alt.contentType = 'application/atom+xml'>
          <div class="vrtx-feed-link">
            <a id="vrtx-feed-link" href="${alt.url?html}"><@vrtx.msg code="viewCollectionListing.feed.fromThis" /></a>
          </div>
          <#break />
        </#if>
      </#list>
     </#if>
  </div>
  
</#macro>

<#macro displayCommonTagListing listing>
  
  <#assign resources=listing.getFiles() />
  <#assign urls=listing.urls />
  <#assign displayPropDefs=listing.displayPropDefs />
  <#assign i = 1 />

  <#list resources as resource>
    <#assign resourceTitle = vrtx.prop(resource, "title", "").getFormattedValue() />
    <#assign introImageProp = vrtx.prop(resource, "picture", "")?default("") />
    <div class="vrtx-resource" id="vrtx-result-${i}">
      <#if introImageProp != "">
      <a href="${resource.getURI()?html}" class="vrtx-image">
        <#assign src = vrtx.propValue(resource, 'picture', 'thumbnail') /><img src="${src?html}" />
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
        <#elseif displayPropDef.name = 'publish-date'>
          <#assign val>
            <@vrtx.localizeMessage code="viewCollectionListing.publishedDate" default="" args=[] locale=locale />${vrtx.propValue(resource, displayPropDef.name)}
          </#assign>  
        <#else>
          <#assign val = vrtx.propValue(resource, displayPropDef.name, "long") />
        </#if>
        <#if val?has_content>
          <div class="${displayPropDef.name}">
            ${val} 
            <#if displayPropDef.name = 'introduction'>
              <#assign hasBody = vrtx.propValue(resource, 'hasBodyContent') == 'true' />
              <#if hasBody>
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
    </div>
    <#assign i = i + 1 />
  </#list>
  
</#macro>