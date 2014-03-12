<#ftl strip_whitespace=true>
<#import "../vortikal.ftl" as vrtx />

<#macro displayResearchGroupsAlphabetical researchGroupListing>
  <#list alpthabeticalOrdredResult?keys as key >
	<ul class="vrtx-alphabetical-research-group-listing">
      <li>${key}
        <ul>
		      <#list alpthabeticalOrdredResult[key] as researchGroup>
			      <#local title = vrtx.propValue(researchGroup.propertySet, 'title') />
			      <li><a href="${researchGroup.url?html}">${title}</a></li>
		      </#list>
		    </ul>
	  </li>
	</ul>
  </#list>
</#macro>

<#macro displayResearchGroups researchGroupListing>

  <#local researchGroups = researchGroupListing.entries />

  <#if (researchGroups?size > 0) >
    <div id="${researchGroupListing.name}" class="vrtx-resources vrtx-research-groups ${researchGroupListing.name}">
      <#if researchGroupListing.title?exists && researchGroupListing.offset == 0>
        <h2>${researchGroupListing.title?html}</h2>
      </#if>
      <#local locale = springMacroRequestContext.getLocale() />

      <#list researchGroups as researchGroupEntry>

       <#local researchGroup = researchGroupEntry.propertySet />

        <#local title = vrtx.propValue(researchGroup, 'title') />
        <#local introImg = vrtx.prop(researchGroup, 'picture')  />
        <#local intro = vrtx.prop(researchGroup, 'introduction')  />
        <#local caption = vrtx.propValue(researchGroup, 'caption')  />

        <div class="vrtx-resource vrtx-research-group">
          <#if introImg?has_content >
            <#local introImgURI = vrtx.propValue(researchGroup, 'picture') />
	        <#if introImgURI?exists>
	          <#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
	        <#else>
	    	  <#local thumbnail = "" />
	   	    </#if>
	   	    <#local introImgAlt = vrtx.propValue(researchGroup, 'pictureAlt') />
            <a class="vrtx-image" href="${researchGroupEntry.url?html}">
              <img src="${thumbnail?html}" alt="<#if introImgAlt?has_content>${introImgAlt?html}</#if>" />
            </a>
          </#if>
          <div class="vrtx-title">
            <a class="vrtx-title summary" href="${researchGroupEntry.url?html}">${title?html}</a>
	      </div>
          <#if intro?has_content && researchGroupListing.hasDisplayPropDef(intro.definition.name)>
            <div class="description introduction">
              <@vrtx.linkResolveFilter intro.value researchGroupEntry.url requestURL />
            </div>
          </#if>
          <div class="vrtx-read-more">
            <a href="${researchGroupEntry.url?html}" class="more">
              <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
            </a>
          </div>
        </div>
      </#list>
    </div>
  </#if>
</#macro>