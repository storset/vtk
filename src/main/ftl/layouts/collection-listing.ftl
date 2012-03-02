<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#if (conf.auth && list?exists && list?size > 0)>
  <div class="vrtx-collection-listing-component-wrapper">
    <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/include-jquery.js"></script>
    <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/open-webdav.js"></script>
    <div class="vrtx-collection-listing-component">
    <#if (conf.folderTitle && folderTitle?exists)>
      <h3>${folderTitle?string}</h3>
    </#if>
      <#if conf.compactView>
        <table class="vrtx-collection-listing-table vrtx-collection-listing-table-compact">
      <#else>
        <table class="vrtx-collection-listing-table">
      </#if>
        <thead>
          <tr>
            <th class="vrtx-collection-listing-title"><@vrtx.msg code="collectionListing.resourceTitle" default="Title" /></th>
          <#if !conf.compactView>
            <th class="vrtx-collection-listing-modified-by"><@vrtx.msg code="collectionListing.lastModifiedBy" default="Modified by" /></th>
            <th class="vrtx-collection-listing-last-modified"><@vrtx.msg code="collectionListing.lastModified" default="Last modified" /></th>
          </#if>
          </tr>
        </thead>
        <tbody>
        <#assign count = 1 />
        <#assign listSize = list?size />
        <#list list as res >
          <#assign title = vrtx.propValue(res, 'title') />
          <#assign lastModifiedTime = vrtx.propValue(res, 'lastModified') />
          <#assign modifiedBy = vrtx.propValue(res, 'modifiedBy', 'document-link') />
          <#assign uri = vrtx.getUri(res) />

          <#assign contentType = vrtx.propValue(res, 'contentType') />
          <#assign resourceType = res.resourceType />
        
          <#assign rowType = "odd" />
          <#if (res_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>

          <#assign firstLast = ""  />
          <#if (res_index == 0) && (res_index == (listSize - 1))>
            <#assign firstLast = " first last" />
          <#elseif (res_index == 0)>
            <#assign firstLast = " first" />
          <#elseif (res_index == (listSize - 1))>
            <#assign firstLast = " last" />     
          </#if>

          <tr class="${rowType} ${firstLast}">
            <#if !conf.compactView>
              <td class="vrtx-collection-listing-title first-col">
            <#else>
              <td class="vrtx-collection-listing-title first-col last-col">
            </#if>
              <a class="vrtx-icon <@vrtx.iconResolver resourceType contentType />" href="${uri?html}"></a>
              <a class="vrtx-title-link" href="${uri?html}">${title?html}</a>
            <#if (edit?exists && edit[res_index])>
              <a class="vrtx-resource-open-webdav" href="${vrtx.linkConstructor(uri, 'webdavService')}"><@vrtx.msg code="collectionListing.edit" /></a>
            </#if>
            <#if conf.compactView>
              <span>${lastModifiedTime?html}</span>
            </#if>
            </td>
          <#if !conf.compactView>
            <td class="vrtx-collection-listing-last-modified-by">${modifiedBy}</td>
            <td class="vrtx-collection-listing-last-modified last-col">${lastModifiedTime?html}</td>
          </#if>
          </tr>
        </#list>
        </tbody>
      </table>
      <#if (conf.goToFolderLink && goToFolderLink?exists)>
        <a href="${goToFolderLink?html}"><@vrtx.msg code="collectionListing.goTo" default="Go to" /> ${folderTitle}</a>
      </#if>
    </div>
  </div>
</#if>