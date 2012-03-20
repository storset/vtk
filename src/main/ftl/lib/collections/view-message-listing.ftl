<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayCollection collectionListing>

  <#local resources=collectionListing.files />
  <#if (resources?size > 0)>
    <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/include-jquery.js"></script>
    <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/open-webdav.js"></script>
    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists && collectionListing.offset == 0>
      <h2>${collectionListing.title?html}</h2>
    </#if>
    
    <#list resources as r>
      <#assign uri = vrtx.getUri(r) />
      
      <#if !hideIcon?exists>
        <div class="vrtx-resource vrtx-resource-icon">
      <#else>
        <div class="vrtx-resource">
      </#if>
        <#if !hideIcon?exists>
		  <a class="vrtx-icon <@vrtx.iconResolver r.resourceType r.contentType />" href="${uri?html}"></a>
		</#if> 
      
		<div class="vrtx-title">
		  <#assign title = vrtx.propValue(r, "title", "", "") />
		  <#if !title?has_content>
		    <#assign title = vrtx.propValue(r, "solr.name", "", "") />
		  </#if>
          <h2>${title?html}</h2>
          <#if edit?exists && edit[r_index]?string = "true">
          <div>
            <a class="vrtx-resource-open-webdav" href="${vrtx.relativeLinkConstructor(uri, 'simpleMessageEditor')}"><@vrtx.msg code="report.collection-structure.edit" /></a>
          </div>
          </#if> 
		</div>
        </div>
            <div>
            <#assign message = vrtx.propValue(r, "message", "", "") />
                ${message} 
            </div>
            <#local publishDate = vrtx.propValue(r, 'publish-date') />
            <div class="published-date">
              <span class="published-date-prefix"><@vrtx.localizeMessage code="viewCollectionListing.publishedDate" default="" args=[] locale=locale /></span>${publishDate}                
            </div>
    </#list>
   </div>
  </#if>
  
</#macro>