<#--
  - File: view-collectionlisting.ftl
  - 
  - Description: 
  - 
  - Required model data:
  -  
  - Optional model data:
  -   
  -->

<#import "vortikal.ftl" as vrtx />


<#-- XXX: remove this when properties 'introduction' and 'description'
     are merged: -->
<#function getIntroduction resource>
  <#local introduction = vrtx.propValue(resource, "introduction") />
  <#if !introduction?has_content>
    <#local introduction = vrtx.propValue(resource, "description", "", "content") />
  </#if>
  <#return introduction />
</#function>

<#-- Function to get page -->
<#function getPage collectionListing>
  <#local page = "${collectionListing.page?html}" />
  <#return page />
</#function>

<#macro displayResources collectionListing>

  <#local resources=collectionListing.files />
  <#if resources?size &gt; 0>
    <div class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists>
      <h2>${collectionListing.title?html}</h2>
    </#if>
    
    <#list resources as r>
      <div class="vrtx-resource">

        <a class="vrtx-title" href="${collectionListing.urls[r.URI]?html}">${vrtx.propValue(r, "title", "", "")?html}</a>

        <#list collectionListing.displayPropDefs as displayPropDef>

          <#if displayPropDef.name = 'introduction'>
            <#assign val = getIntroduction(r) />
          <#elseif displayPropDef.type = 'IMAGE_REF'>
            <#assign val><img src="${vrtx.propValue(r, displayPropDef.name, "")}" /></#assign>
          <#elseif displayPropDef.name = 'lastModified'>
            <#assign val>
              <@vrtx.msg code="viewCollectionListing.lastModified"
                         args=[vrtx.propValue(r, displayPropDef.name, "long")] />
            </#assign>
          <#else>
            <#assign val = vrtx.propValue(r, displayPropDef.name, "long") /> <#-- Default to 'long' format -->
          </#if>

          <#if val?has_content>
            <div class="${displayPropDef.name}">
              ${val}
            </div>
          </#if>
        </#list>

      </div>
    </#list>
   </div>
  </#if>
  
  <#if collectionListing.prevURL?exists>
    <a class="vrtx-previous" href="${collectionListing.prevURL?html}"><@vrtx.msg code="viewCollectionListing.previous" /></a>&nbsp;
  </#if>
  <#if collectionListing.nextURL?exists>
    <a class="vrtx-next" href="${collectionListing.nextURL?html}"><@vrtx.msg code="viewCollectionListing.next" /></a>
  </#if>
  
</#macro>

<#macro displayArticles collectionListing displayMoreURLs=false>

  <#local resources=collectionListing.files />
  <#if resources?size &gt; 0>
    <div class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists>
      <h2>${collectionListing.title?html}</h2>
    </#if>

    <#local locale = springMacroRequestContext.getLocale() />
    <#list resources as r>
      <#local title = vrtx.propValue(r, 'title') />
      <#local introImg  = vrtx.prop(r, 'picture')  />
      <#local publishedDate  = vrtx.prop(r, 'published-date')  />
      <#local intro  = vrtx.prop(r, 'introduction')  />

      <div class="vrtx-resource">
        <a class="vrtx-title" href="${collectionListing.urls[r.URI]?html}">
        <#if introImg?has_content && collectionListing.displayPropDefs?seq_contains(introImg.definition)>
        <img src="${introImg.value?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
        </#if>
        ${title?html}</a> 

        <#if publishedDate?has_content && collectionListing.displayPropDefs?seq_contains(publishedDate.definition)> 
        <div class="published-date">
          <@vrtx.msg code="viewCollectionListing.publishedDate"
                     args=[publishedDate.getFormattedValue('long', locale)] />
        </div>

	</#if>

        <#if intro?has_content && collectionListing.displayPropDefs?seq_contains(intro.definition)>
        <div class="description introduction">${intro.value}</div>
        </#if>

        <#-- list collectionListing.displayPropDefs as displayPropDef>

          <#if displayPropDef.name = 'introduction'>
            <#assign val = getIntroduction(r) />
          <#elseif displayPropDef.type = 'IMAGE_REF'>
            <#assign val><img src="${vrtx.propValue(r, displayPropDef.name, "")}" /></#assign>
          <#else>
            <#assign val = vrtx.propValue(r, displayPropDef.name, "long") /> 
          </#if>

          <#if val?has_content>
            <div class="vrtx-prop ${displayPropDef.name}">
              ${val}
            </div>
          </#if>
        </#list -->

        <#if displayMoreURLs>
          <a href="${collectionListing.urls[r.URI]?html}" class="more">
            <@vrtx.msg code="viewCollectionListing.readMore" />
          </a>
        </#if>
      </div>
    </#list>
   </div>
  </#if>

  <#if collectionListing.prevURL?exists>
    <a class="vrtx-previous" href="${collectionListing.prevURL?html}"><@vrtx.msg code="viewCollectionListing.previous" /></a>
  </#if>
  <#if collectionListing.nextURL?exists>
    <a class="vrtx-next" href="${collectionListing.nextURL?html}"><@vrtx.msg code="viewCollectionListing.next" /></a>
  </#if>
  
</#macro>

<#macro displayEvents collectionListing displayMoreURLs=false>
  <#local resources=collectionListing.files />
  <#if resources?size &gt; 0>
    <div class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists>
      <h2>${collectionListing.title?html}</h2>
    </#if>
    <#local locale = springMacroRequestContext.getLocale() />
    <#list resources as r>
      <#local title = vrtx.propValue(r, 'title') />
      <#local introImg  = vrtx.prop(r, 'picture')  />
      <#local intro  = vrtx.prop(r, 'introduction')  />
      <#local location  = vrtx.prop(r, 'location')  />
      <div class="vrtx-resource vevent">
        <a class="vrtx-title summary" href="${collectionListing.urls[r.URI]?html}">
        <#if introImg?has_content && collectionListing.displayPropDefs?seq_contains(introImg.definition)>
        <img src="${introImg.value?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
        </#if>
        ${title?html}</a>

        <div class="time-and-place"> 
          <@coll.showTimeAndPlace r />
        </div>

        <#if intro?has_content && collectionListing.displayPropDefs?seq_contains(intro.definition)>
        <div class="description introduction">${intro.value}</div>
        </#if>

        <#if displayMoreURLs>
          <a href="${collectionListing.urls[r.URI]?html}" class="more" title="${title?html}">
            <@vrtx.msg code="viewCollectionListing.readMore" />
          </a>
        </#if>

      </div>
    </#list>
   </div>
  </#if>

  <#if collectionListing.prevURL?exists>
    <a class="vrtx-previous" href="${collectionListing.prevURL?html}"><@vrtx.msg code="viewCollectionListing.previous" /></a>
  </#if>
  <#if collectionListing.nextURL?exists>
    <a class="vrtx-next" href="${collectionListing.nextURL?html}"><@vrtx.msg code="viewCollectionListing.next" /></a>
  </#if>
  
</#macro>

<#--
 * Shows the start- and end-date of an event, seperated by a "-".
 * If the two dates are identical, only the time of enddate is shown.
 * 
 * @param resource The resource to evaluate dates from
-->
<#macro showTimeAndPlace resource>

  <#local start = vrtx.propValue(resource, "start-date") />
  <#local startiso8601 = vrtx.propValue(resource, "start-date", "iso-8601") />
  <#local startshort = vrtx.propValue(resource, "start-date", "short") />
  <#local end = vrtx.propValue(resource, "end-date") />
  <#local endiso8601 = vrtx.propValue(resource, "end-date", "iso-8601") />
  <#local endshort = vrtx.propValue(resource, "end-date", "short") />
  <#local endhoursminutes = vrtx.propValue(resource, "end-date", "hours-minutes") />
  <#local location = vrtx.propValue(resource, "location") />
  
  <#if start != ""><abbr class="dtstart" title="${startiso8601}">${start}</abbr></#if>
  <#t /><#if end != ""><span class="delimiter"> - </span>
    <#if startshort == endshort>
      <#t /><abbr class="dtend" title="${endiso8601}">${endhoursminutes}</abbr>
    <#else>
      <#t /><abbr class="dtend" title="${endiso8601}">${end}</abbr></#if><#rt />
  <#t/></#if>
  <#t /><#if location != "">, <span class="location">${location}</span></#if>
        
</#macro>



