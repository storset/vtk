<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayCollection collectionListing>

  <#local resources=collectionListing.files />
  <#local editLinks = collectionListing.editLinkAuthorized />

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
		  <a class="vrtx-icon <@vrtx.iconResolver r.resourceType r.contentType />" href="${collectionListing.urls[r.URI]?html}"></a>
		</#if> 
      
		<div class="vrtx-title">
		  <#assign title = vrtx.propValue(r, "title", "", "") />
		  <#if !title?has_content>
		    <#assign title = vrtx.propValue(r, "solr.name", "", "") />
		  </#if>
          <a class="vrtx-title vrtx-title-link" href="${collectionListing.urls[r.URI]?html}">${title?html}</a>
          <#if editLinks?exists && editLinks[r_index]?exists && editLinks[r_index]>
            <a class="vrtx-resource-open-webdav" href="${vrtx.linkConstructor(uri, 'webdavService')}"><@vrtx.msg code="report.list-resources.edit" /></a>
          </#if>
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
            <#assign modifiedBy = vrtx.prop(r, 'modifiedBy').principalValue />
            <#if principalDocuments?exists && principalDocuments[modifiedBy.name]?exists>
              <#assign principal = principalDocuments[modifiedBy.name] />
              <#if principal.URL?exists>
                <#assign val = val + " <a href='${principal.URL}'>${principal.description}</a>" />
              <#else>
                <#assign val = val + " ${principal.description}" />
              </#if>
            <#else>
              <#assign val = val + " " + vrtx.propValue(r, 'modifiedBy', 'link') />
            </#if>
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