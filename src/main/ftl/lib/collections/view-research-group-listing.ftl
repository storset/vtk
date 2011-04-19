<#import "../vortikal.ftl" as vrtx />

<#macro displayResearchGroupsAlphabetical researchGroupListing>

  <#list alpthabeticalOrdredResult?keys as key >
	<ul  class="vrtx-alphabetical-research-group-listing">
		<li>${key}
		<ul>
		<#list alpthabeticalOrdredResult[key] as researchGroup>
			<#local title = vrtx.propValue(researchGroup, 'title') />
			<li><a href="${researchGroupListing.urls[researchGroup.URI]?html}">${title}</a></li>
		</#list>
		</ul>
		</li>
	</ul>
  </#list>
</#macro>

<#macro displayResearchGroups researchGroupListing>
  <#local researchGroups=researchGroupListing.files />
  <#if (researchGroups?size > 0) >
    <div id="${researchGroupListing.name}" class="vrtx-research-groups ${researchGroupListing.name}">
    <#if researchGroupListing.title?exists && researchGroupListing.offset == 0>
      <h2>${researchGroupListing.title?html}</h2>
    </#if>
    <#local locale = springMacroRequestContext.getLocale() />
    <#list researchGroups as researchGroup>
      <#local title = vrtx.propValue(researchGroup, 'title') />
      <#local introImg = vrtx.prop(researchGroup, 'picture')  />
      <#local intro = vrtx.prop(researchGroup, 'introduction')  />
      <#local caption = vrtx.propValue(researchGroup, 'caption')  />
      <#-- Flattened caption for alt-tag in image -->
      <#local captionFlattened>
      <@vrtx.flattenHtml value=caption escape=true />
      </#local>
      <div class="vrtx-research-group">
            <#if introImg?has_content >
               <#local introImgURI = vrtx.propValue(researchGroup, 'picture') />
	           <#if introImgURI?exists>
	    			<#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
	    	   <#else>
	    			<#local thumbnail = "" />
	   		   </#if>
               <a class="vrtx-image" href="${researchGroupListing.urls[researchGroup.URI]?html}">
               <#if caption != ''>
                	<img src="${thumbnail?html}" alt="${captionFlattened}" />
               <#else>
                    <img src="${thumbnail?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
               </#if>
                </a>
            </#if>
            <div class="vrtx-title">
              <a class="vrtx-title summary" href="${researchGroupListing.urls[researchGroup.URI]?html}">${title?html}</a>
			</div>
        	<#if intro?has_content && researchGroupListing.hasDisplayPropDef(intro.definition.name)>
        	  <div class="description introduction">
        	  	<@vrtx.linkResolveFilter intro.value researchGroupListing.urls[researchGroup.URI] requestURL />
        	  </div>
            </#if>
             <div class="vrtx-read-more">
              <a href="${researchGroupListing.urls[researchGroup.URI]?html}" class="more">
                <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
              </a>
            </div>
      </div>
    </#list>
   </div>
  </#if>
</#macro>

