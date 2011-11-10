<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayCollection collectionListing>

  <#local resources=collectionListing.files />
  <#if resources?size &gt; 0>
    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists && collectionListing.offset == 0>
      <h2>${collectionListing.title?html}</h2>
    </#if>
    
    <#list resources as r>
      <#assign url = r.URI />
      <#assign solrUrl = vrtx.propValue(r, "solr.url", "", "") />
      <#if solrUrl?exists && solrUrl?has_content>
        <#assign url = solrUrl>
      </#if>
      <div class="vrtx-resource">
		<div class="vrtx-title">
		  <#assign title = vrtx.propValue(r, "title", "", "") />
		  <#if !title?has_content && solrUrl?exists && solrUrl?has_content>
		    <#assign title = vrtx.propValue(r, "solr.name", "", "") />
		  </#if>
          <a class="vrtx-title" href="${url?html}">${title?html}</a>
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


