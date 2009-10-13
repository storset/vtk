<#import "../vortikal.ftl" as vrtx />

<#macro displayProjects projectListing>

  <#local projects=projectListing.files />
  <#if projects?size &gt; 0>
    <div id="${projectListing.name}" class="vrtx-resources ${projectListing.name}">
    <#if projectListing.title?exists && collectionListing.offset == 0>
      <h2>${projectListing.title?html}</h2>
    </#if>
    
    <#list projects as project>
      <div class="vrtx-resource">
        <div class="vrtx-title">
          <a class="vrtx-title" href="${projectListing.urls[project.URI]?html}">${vrtx.propValue(project, "title", "", "")?html}</a>
        </div>
      </div>
    </#list>
  </#if>

</#macro>