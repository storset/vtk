<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayCollection collectionListing>
  <#local resources=collectionListing.files />
  <#local i = 1 />
      
  <#if (resources?size > 0)>
    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
      <#if collectionListing.title?exists && collectionListing.offset == 0>
        <h2>${collectionListing.title?html}</h2>
      </#if>
      <#list resources as r>
        <#assign uri = vrtx.getUri(r) />
        <div id="vrtx-result-${i}" class="vrtx-resource">
		  <div class="vrtx-title">
		    <#assign title = vrtx.propValue(r, "title", "", "") />
		    <#if !title?has_content>
		      <#assign title = vrtx.propValue(r, "solr.name", "", "") />
		    </#if>
            <a class="vrtx-title" href="${uri?html}">${title?html}</a>
            <#if edit?exists && edit[r_index]?string = "true">
              <a class="vrtx-message-listing-edit" href="${vrtx.relativeLinkConstructor(uri, 'simpleMessageEditor')}"><@vrtx.msg code="report.collection-structure.edit" /></a>
            </#if> 
		  </div>
          <#local publishDate = vrtx.propValue(r, 'publish-date') />
          <div class="published-date">
            <span class="published-date-prefix"><@vrtx.localizeMessage code="viewCollectionListing.publishedDate" default="" args=[] locale=locale /></span>${publishDate}                
          </div>
          <div class="description introduction">
            <#assign message = vrtx.propValue(r, "message", "", "") />
            ${message}
          </div>
        </div>
        <#local i = i + 1 />
      </#list>
    </div>
  </#if>
</#macro>
