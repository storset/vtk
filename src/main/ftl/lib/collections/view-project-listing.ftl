<#import "../vortikal.ftl" as vrtx />

<#macro displayProjects projectListing>

  <#local projects=projectListing.files />
  <#if projects?size &gt; 0>
    <div id="${projectListing.name}" class="vrtx-resources ${projectListing.name}">
    <#if projectListing.title?exists && collectionListing.offset == 0>
      <h2>${projectListing.title?html}</h2>
    </#if>
    
    <#list projects as project>
      
      <#local title = vrtx.propValue(project, 'title') />
      <#local picture = vrtx.propValue(project, 'picture')  />
      
      <div class="vrtx-project">
        <div class="vrtx-project-title">
          <a class="vrtx-project-title-anchor" href="${projectListing.urls[project.URI]?html}">${title?html}</a>
        </div>
      </div>
    </#list>
  </#if>

</#macro>