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
<#import "view-utils.ftl" as viewutils />

<#macro displayResources collectionListing>

  <#local resources=collectionListing.files />
  <#if resources?size &gt; 0>
    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists && collectionListing.offset == 0>
      <h2>${collectionListing.title?html}</h2>
    </#if>
    
    <#list resources as r>
      <div class="vrtx-resource">
		<div class="vrtx-title">
        <a class="vrtx-title" href="${collectionListing.urls[r.URI]?html}">${vrtx.propValue(r, "title", "", "")?html}</a>
		</div>
        <#list collectionListing.displayPropDefs as displayPropDef>

          <#if displayPropDef.name = 'introduction'>
            <#assign val = vrtx.getIntroduction(r) />
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
  
</#macro>

<#macro displayArticles page collectionListings hideNumberOfComments displayMoreURLs=false >

  <#if collectionListings?size &gt; 0>
    <#assign i = 1 />
    
    <#local frontpageClass = "" />
    <#if page = 1>
      <#local frontpageClass = "vrtx-resources-frontpage" />
    </#if>
    
    <#--
      First of all, there is more than one searchcomponent, hence the list.
      Second, the searchcomponents aren't necessarily named "articleListing.searchComponent",
        but we wanna show the contents of them all in one common div.
      Thirdly, we don't know if there's any styling "out there" that uses this particular
        id. So we keep it...
    -->
    
    <div id="articleListing.searchComponent" class="vrtx-resources articleListing.searchComponent ${frontpageClass}">
    <#list collectionListings as articles>
      <#local resources=articles.files />
      <#if resources?size &gt; 0>
        <#list resources as r>
        
          <#local locale = springMacroRequestContext.getLocale() />
          <#if r.contentLocale?has_content>
            <#local locale = r.contentLocale />
          </#if>
          
          <#local title = vrtx.propValue(r, 'title') />
          <#local introImg  = vrtx.prop(r, 'picture')  />
          <#local publishedDate  = vrtx.prop(r, 'published-date')  />
          <#local intro  = vrtx.prop(r, 'introduction')  />
          <#local caption = vrtx.propValue(r, 'caption')  />
          
          <#-- Flattened caption for alt-tag in image -->
          <#local captionFlattened>
            <@vrtx.flattenHtml value=caption escape=true />
          </#local>
          
          <#local articleType = "vrtx-default-article" />
          <#if articles.name == "articleListing.featuredArticles">
            <#local articleType = "vrtx-featured-article" />
          </#if>
          
          <div id="vrtx-result-${i}" class="vrtx-resource ${articleType}"> 
            <#local src = vrtx.propValue(r, 'picture', 'thumbnail') />
            <#if introImg?has_content && articles.hasDisplayPropDef(introImg.definition.name)>
               <a class="vrtx-image" href="${articles.urls[r.URI]?html}">        
                 <#if caption != ''>
                    <img src="${src?html}" alt="${captionFlattened}" />
                  <#else>
                    <img src="${src?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
                  </#if>
               </a>
            </#if>
            <div class="vrtx-title">
            <a class="vrtx-title" href="${articles.urls[r.URI]?html}">${title?html}</a></div>
            <#if publishedDate?has_content && articles.hasDisplayPropDef(publishedDate.definition.name)> 
          	
              <div class="published-date">
                <span class="published-date-prefix"><@vrtx.localizeMessage code="viewCollectionListing.publishedDate" default="" args=[] locale=locale /></span>${publishedDate.getFormattedValue('long', locale)}                
              </div>
            </#if>
            
            <#if hideNumberOfComments?exists && !hideNumberOfComments >
               <#local numberOfComments = vrtx.prop(r, "numberOfComments") />
               <#if numberOfComments?has_content >	
                 <div class="vrtx-number-of-comments-add-event-container">
                   <@viewutils.displayNumberOfComments r locale />
                 </div>
               </#if>
          	</#if>
            <#if intro?has_content && articles.hasDisplayPropDef(intro.definition.name)>
              <div class="description introduction">${intro.value}</div>
            </#if>
			
            <#local hasBody = vrtx.propValue(r, 'hasBodyContent') == 'true' />
            <#if displayMoreURLs && hasBody>
            <div class="vrtx-read-more">
              <a href="${articles.urls[r.URI]?html}" class="more">
                <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
              </a>
            </div>
            </#if>
            
          </div>
          <#assign i = i + 1 />
        </#list>
      </#if>
    </#list>
    </div>
  </#if>

</#macro>

<#macro displayEvents collectionListing hideNumberOfComments displayMoreURLs=false>
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
