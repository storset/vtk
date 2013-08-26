<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayCollection collectionListing>

  <#local entries=collectionListing.entries />

  <#if (entries?size > 0)>
    <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/include-jquery.js"></script>
    <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/open-webdav.js"></script>
    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
      <#if collectionListing.title?exists && collectionListing.offset == 0>
        <h2>${collectionListing.title?html}</h2>
      </#if>
    
      <#list entries as entry>

        <#-- The actual resource we are displaying -->
        <#local entryPropSet = entry.propertySet />
        <#assign url = entry.url />

        <#if !hideIcon?exists>
          <div class="vrtx-resource vrtx-resource-icon">
        <#else>
          <div class="vrtx-resource">
        </#if>
        <#if !hideIcon?exists>
		  <a class="vrtx-icon <@vrtx.resourceToIconResolver entryPropSet />" href="${url?html}"></a>
		</#if> 
      
		<div class="vrtx-title">
		  <#assign title = vrtx.propValue(entryPropSet, "title", "", "") />
		  <#if !title?has_content>
		    <#assign title = vrtx.propValue(entryPropSet, "solr.name", "", "") />
		  </#if>
          <a class="vrtx-title vrtx-title-link" href="${url?html}">${title?html}</a>

          <#--
            Only local resources are ever evaluated for edit authorization.
            Use prop set path (uri) and NOT full entry url for link construction.
            See open-webdav.js
          -->
          <#if entry.editAuthorized>
            <a class="vrtx-resource-open-webdav" href="${vrtx.linkConstructor(entryPropSet.URI, 'webdavService')}"><@vrtx.msg code="collectionListing.editlink" /></a>
          </#if>
		</div>

        <#list collectionListing.displayPropDefs as displayPropDef>
          <#if displayPropDef.name = 'introduction'>
            <#assign val = vrtx.getIntroduction(entryPropSet) />
          <#elseif displayPropDef.type = 'IMAGE_REF'>
            <#assign val><img src="${vrtx.propValue(entryPropSet, displayPropDef.name, "")}" /></#assign>
          <#elseif displayPropDef.name = 'lastModified'>
            <#assign val>
              <@vrtx.msg code="viewCollectionListing.lastModified"
                         args=[vrtx.propValue(entryPropSet, displayPropDef.name, "long")] />
            </#assign>
            <#assign modifiedBy = vrtx.prop(entryPropSet, 'modifiedBy').principalValue />
            <#if principalDocuments?exists && principalDocuments[modifiedBy.name]?exists>
              <#assign principal = principalDocuments[modifiedBy.name] />
              <#if principal.URL?exists>
                <#assign val = val + " <a href='${principal.URL}'>${principal.description}</a>" />
              <#else>
                <#assign val = val + " ${principal.description}" />
              </#if>
            <#else>
              <#assign val = val + " " + vrtx.propValue(entryPropSet, 'modifiedBy', 'link') />
            </#if>
          <#else>
            <#assign val = vrtx.propValue(entryPropSet, displayPropDef.name, "long") /> <#-- Default to 'long' format -->
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