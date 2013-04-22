<#ftl strip_whitespace=true>

<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />
<#import "/lib/collections/view-event-listing.ftl" as events />
<#import "/lib/collections/view-project-listing.ftl" as projects />
<#import "/lib/collections/view-person-listing.ftl" as persons />

<#macro displayTagElements tagElements showOccurences=false splitInThirds=false limit=0 alphabeticalSeparation=false>
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
      <#if count = limit>
        <#break>
      </#if> 
      <#local count = count + 1 />
    </#list>
    </ul>
  <#else>
    <#list tagElements as element>
      <#local elementText = element.text />
        
      <#-- Alphabetical separation (not possible when split in thirds) -->
      <#if alphabeticalSeparation>
        <#if count = 1>
          <#if springMacroRequestContext.getLocale() = "en">
            <#local alphabet = ["a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
                                "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"] />
          <#else>
            <#local alphabet = ["a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o",
                                "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "æ", "ø", "å"] />
          </#if>
          <div id="vrtx-tags-alphabetical-tabs">
            <ul style="display: none">
             <#list alphabet?chunk(3) as alphaChunk>
               <#local alhaChunkSize = alphaChunk?size/>
               <#local alphaChunked>
                 <#list alphaChunk as alpha>
                   <#lt/><#if (alhaChunkSize == 2 && alpha_index = 1)>-</#if><#rt/>
                   <#lt/><#if (alhaChunkSize == 3 && alpha_index = 1)>-<#else>${alpha}</#if><#rt/>
                 </#list>
               </#local>
               <li><a href="#vrtx-tags-alphabetical-${alphaChunked}" name="vrtx-tags-alphabetical-${alphaChunked}">${alphaChunked?upper_case}</a></li>
             </#list>
           </ul>
           <div id="${count}"><#-- TODO tab containers -->
        </#if>
        
        <#local curChar><#if !curChar??>" "<#else>${curChar}</#if></#local>
        <#if !elementText?capitalize?starts_with(curChar)>
          <#local curChar = elementText?capitalize?substring(0,1) />
          <#if (count > 1)>
            </ul>
            </div>
            <div id="${count}"><#-- TODO tab containers -->
          </#if>
          <h2>${curChar}</h2>
          <ul class="vrtx-tag">
        </#if>
      <#elseif count = 1>
        <ul class="vrtx-tag">
      </#if>
      
      <#-- Tag element -->
      <li class="vrtx-tags-element-${count}">
        <a class="tags" href="${element.linkUrl?html}" rel="tags">${elementText?html}<#if showOccurences> (${element.occurences?html})</#if></a>
      </li>
      
      <#-- Limit -->
      <#if count = limit>
        <#break>
      </#if>
      
      <#local count = count + 1 />
    </#list>
    </ul>
    
    <#if alphaBeticalSeperation>
      </div>
      </div>
    </#if>
    
  </#if>
  
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