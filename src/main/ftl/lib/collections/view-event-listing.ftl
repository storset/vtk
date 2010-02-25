<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayEvents collection searchComponent hideNumberOfComments=false displayMoreURLs=false >

  <#local displayType = vrtx.propValue(collection, 'display-type', '', 'el') />
  <#if !displayType?has_content>
    <@displayStandard searchComponent hideNumberOfComments displayMoreURLs />
  <#elseif displayType = 'calendar'>
    <@displayCalendar />
  </#if>
  
</#macro>>

<#macro displayStandard collectionListing hideNumberOfComments displayMoreURLs >
  <#local resources=collectionListing.files />
  <#if resources?size &gt; 0>

    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists && collectionListing.offset == 0>
      <h2>${collectionListing.title?html}</h2> 
    </#if>
    <#local locale = springMacroRequestContext.getLocale() />
    <#list resources as resource>
      
      <#local title = vrtx.propValue(resource, 'title') />
      <#local introImg = vrtx.prop(resource, 'picture')  />
      <#local intro = vrtx.prop(resource, 'introduction')  />
      <#local location  = vrtx.prop(resource, 'location')  />
      <#local caption = vrtx.propValue(resource, 'caption')  />
      <#local endDate = vrtx.prop(resource, 'end-date') />
      <#local hideEndDate = !endDate?has_content || !collectionListing.hasDisplayPropDef(endDate.definition.name) />
      <#local hideLocation = !location?has_content || !collectionListing.hasDisplayPropDef(location.definition.name) />
 

      <#-- Flattened caption for alt-tag in image -->
     <#local captionFlattened>
        <@vrtx.flattenHtml value=caption escape=true />
      </#local>
      <div class="vrtx-resource vevent">
        <#if introImg?has_content && collectionListing.hasDisplayPropDef(introImg.definition.name)>
          <#local src = vrtx.propValue(resource, 'picture', 'thumbnail') />
          <a class="vrtx-image" href="${collectionListing.urls[resource.URI]?html}">
            <#if caption != ''>
              <img src="${src?html}" alt="${captionFlattened}" />
            <#else>
              <img src="${src?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
            </#if>
          </a>
        </#if>
        <div class="vrtx-title">
          <a class="vrtx-title summary" href="${collectionListing.urls[resource.URI]?html}">${title?html}</a>
        </div>

        <div class="time-and-place"> 
          <@viewutils.displayTimeAndPlace resource title hideEndDate hideLocation hideNumberOfComments />
        </div>

        <#if intro?has_content && collectionListing.hasDisplayPropDef(intro.definition.name)>
        <div class="description introduction">${intro.value}</div>
        </#if>

        <#local hasBody = vrtx.propValue(resource, 'hasBodyContent') == 'true' />

        <#if displayMoreURLs && hasBody>
        <div class="vrtx-read-more">
          <a href="${collectionListing.urls[resource.URI]?html}" class="more" title="${title?html}">
            <@vrtx.msg code="viewCollectionListing.readMore" />
          </a>
          </div>
        </#if>

      </div>
    </#list>
   </div>
  </#if>

</#macro>

<#macro displayCalendar>

  <!-- XXX IMPLEMENT -->

  To be implemented...

</#macro>