<#import "../vortikal.ftl" as vrtx />

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
      <#local location  = vrtx.prop(project, 'location')  />
      <#local caption = vrtx.propValue(project, 'caption')  />
      <#local endDate = vrtx.prop(project, 'end-date') />
      <#local hideEndDate = !endDate?has_content || !projectListing.hasDisplayPropDef(endDate.definition.name) />
      <#local hideLocation = !location?has_content || !projectListing.hasDisplayPropDef(location.definition.name) />
      <#-- Flattened caption for alt-tag in image -->
      <#local captionFlattened>
      <@vrtx.flattenHtml value=caption escape=true />
      </#local>
      <div class="vrtx-project vevent">
            <#if introImg?has_content >
            <#local src = vrtx.propValue(project, 'picture', 'thumbnail') />
            	<a class="vrtx-image" href="${projectListing.urls[project.URI]?html}">
                <#if caption != ''>
                	<img src="${src?html}" alt="${captionFlattened}" />
                <#else>
                    <img src="${src?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
                </#if>
                </a>
            </#if>
            <div class="vrtx-title">
              <a class="vrtx-title summary" href="${projectListing.urls[project.URI]?html}">${title?html}</a>
			</div>
        	<#if intro?has_content && projectListing.hasDisplayPropDef(intro.definition.name)>
        	  <div class="description introduction">${intro.value}</div>
            </#if>
      </div>
    </#list>
   </div>
  </#if>
</#macro>