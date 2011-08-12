<#import "../vortikal.ftl" as vrtx />

<#macro displayProjectsAlphabetical projectListing>
  <#list alpthabeticalOrdredResult?keys as key >
	<ul  class="vrtx-alphabetical-project-listing">
		<li>${key}
		<ul>
		<#list alpthabeticalOrdredResult[key] as project>
			<#local title = vrtx.propValue(project, 'title') />
			<li><a href="${projectListing.urls[project.URI]?html}">${title}</a></li>
		</#list>
		</ul>
		</li>
	</ul>
  </#list>
</#macro>

<#macro projectListingViewServiceURL >
  <#if viewAllProjectsLink?exists || viewOngoingProjectsLink?exists>
	<div id="vrtx-listing-completed-ongoing">
	  <#if viewAllProjectsLink?exists && displayAlternateLink?exists>
	  	<a href="${viewAllProjectsLink}">${vrtx.getMsg("projects.viewCompletedProjects")}</a>
	  </#if>
	  <#if viewOngoingProjectsLink?exists>
	    <a href="${viewOngoingProjectsLink}">${vrtx.getMsg("projects.viewOngoingProjects")}</a>
	  </#if>
    </div>
  </#if>
</#macro>

<#macro displayProjects projectListing>
  <#local projects=projectListing.files />
  <#if (projects?size > 0) >
    <div id="${projectListing.name}" class="vrtx-projects ${projectListing.name}">
    <#if projectListing.title?exists && projectListing.offset == 0>
      <h2>${projectListing.title?html}</h2>
    </#if>
    <#local locale = springMacroRequestContext.getLocale() />
    <#list projects as project>
      <#local title = vrtx.propValue(project, 'title') />
      <#local introImg = vrtx.prop(project, 'picture')  />
      <#local intro = vrtx.prop(project, 'introduction')  />
      <#local caption = vrtx.propValue(project, 'caption')  />
      <#-- Flattened caption for alt-tag in image -->
      <#local captionFlattened>
      <@vrtx.flattenHtml value=caption escape=true />
      </#local>
      <div class="vrtx-project">
            <#if introImg?has_content >
            <#local src = vrtx.propValue(project, 'picture', 'thumbnail') />
            <#local introImgURI = vrtx.propValue(project, 'picture') />
          	<#if introImgURI?exists>
    			<#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
    	 	<#else>
    			<#local thumbnail = "" />
   		   	</#if>
            	<a class="vrtx-image" href="${projectListing.urls[project.URI]?html}">
                <#if caption != ''>
                	<img src="${thumbnail?html}" alt="${captionFlattened}" />
                <#else>
                    <img src="${thumbnail?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
                </#if>
                </a>
            </#if>
            <div class="vrtx-title">
              <a class="vrtx-title summary" href="${projectListing.urls[project.URI]?html}">${title?html}</a>
			</div>
        	<#if intro?has_content && projectListing.hasDisplayPropDef(intro.definition.name)>
        	  <div class="description introduction"><@vrtx.linkResolveFilter intro.value projectListing.urls[project.URI]  requestURL /></div>
            </#if>
             <div class="vrtx-read-more">
              <a href="${projectListing.urls[project.URI]?html}" class="more">
                <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
              </a>
            </div>
      </div>
    </#list>
   </div>
  </#if>
</#macro>

<#macro completed >
	<#if viewOngoingProjectsLink?exists>
		<span>(${vrtx.getMsg("projects.completed")})</span>
	</#if>
</#macro>