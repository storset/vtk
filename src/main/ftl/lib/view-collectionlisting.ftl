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



<#macro displayResources model displayMoreURLs=false>

  <#local collectionListing = .vars[model] />
  <#local resources=collectionListing.files />
  <#if resources?size &gt; 0>
    <div class="vrtx-resources ${model}">
    <#if collectionListing.title?exists>
      <h2>${collectionListing.title?html}</h2>
    </#if>

    <#list resources as r>
      <div class="vrtx-resource">
        <h3><a class="vrtx-title" href="${collectionListing.urls[r.URI]?html}">${vrtx.propValue(r, "title", "", "")}</a></h3> 

        <#list collectionListing.displayPropDefs as displayPropDef>

          <#if displayPropDef.name = 'introduction'>
            <#assign val = getIntroduction(r) />
          <#elseif displayPropDef.type = 'IMAGE_REF'>
            <#assign val><img class="vrtx-prop" src="${vrtx.propValue(r, displayPropDef.name, "")}" /></#assign>
          <#else>
            <#assign val = vrtx.propValue(r, displayPropDef.name, "long") /> <#-- Default to 'long' format -->
          </#if>

          <#if val?has_content>
            <div class="vrtx-prop ${displayPropDef.name}">
              ${val}
            </div>
          </#if>
        </#list>
        <#if displayMoreURLs>
          <a href="${collectionListing.urls[r.URI]?html}" class="read-more">
            <@vrtx.msg code="viewCollectionListing.readMore" />
          </a>
        </#if>
      </div>
    </#list>
   </div>
  </#if>
  
  <#if collectionListing.prevURL?exists>
    <a href="${collectionListing.prevURL?html}">previous</a>&nbsp;
  </#if>
  <#if collectionListing.nextURL?exists>
    <a href="${collectionListing.nextURL?html}">next</a>
  </#if>
</#macro>




<#macro displayEvents model>
  <#local collectionListing = .vars[model] />
  <#local resources=collectionListing.files />
  <#if resources?size &gt; 0>
    <div class="vrtx-resources ${model}">
    <#if collectionListing.title?exists>
      <h2>${collectionListing.title?html}</h2>
    </#if>
    <#local locale = springMacroRequestContext.getLocale() />
    <#list resources as r>
      <#local title = vrtx.propValue(r, 'title') />
      <#local introImg  = vrtx.prop(r, 'picture')  />
      <#local intro  = vrtx.prop(r, 'introduction')  />
      <#local startDate  = vrtx.prop(r, 'start-date')  />
      <#local endDate  = vrtx.prop(r, 'end-date')  />
      <#local location  = vrtx.prop(r, 'location')  />
      <div class="vrtx-resource vevent">
        <a class="title" href="${collectionListing.urls[r.URI]?html}">${title}</a>
        <#if introImg?has_content && collectionListing.displayPropDefs?seq_contains(introImg.definition)>
        <img src="${introImg.value?html}" />
        </#if>
        <#if intro?has_content && collectionListing.displayPropDefs?seq_contains(intro.definition)>
        <span class="summary">${intro.value}</span>
        </#if>
        <abbr class="dtstart" title="${startDate.getFormattedValue('iso-8601', locale)}">${startDate.getFormattedValue('short', locale)}</abbr>
        <#if endDate?has_content && collectionListing.displayPropDefs?seq_contains(endDate.definition)>
        <span class="delimiter"> - </span>
        <abbr class="dtend" title="${endDate.getFormattedValue('iso-8601', locale)}">${endDate.getFormattedValue('short', locale)}</abbr>
        </#if>
        <#if location?has_content && collectionListing.displayPropDefs?seq_contains(location.definition)>
        <span class="location">${location.value}</span>
        </#if>
      </div>
    </#list>
   </div>
  </#if>

  <#if collectionListing.prevURL?exists>
    <a href="${collectionListing.prevURL?html}">previous</a>&nbsp;
  </#if>
  <#if collectionListing.nextURL?exists>
    <a href="${collectionListing.nextURL?html}">next</a>
  </#if>

</#macro>

