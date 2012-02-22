<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<div class="vrtx-collection-listing-component-wrapper">
  <#if (list?exists && list?size > 0)>
    <div class="vrtx-collection-listing-component">
    <#if (conf.folderTitle?string = "true") && folderTitle?exists>
      <h2>${folderTitle?string}</h2>
    </#if>
      <table id="vrtx-collection-listing-component" class="vrtx-collection-listing">
        <thead>
          <tr>
            <th id="vrtx-collection-listing-title"><@vrtx.msg code="collectionListing.resourceTitle" default="Title" /></th>
            <th id="vrtx-collection-listing-modified-by"><@vrtx.msg code="report.modified-by" default="Modified by" /></th>
            <th id="vrtx-collection-listing-last-modified"><@vrtx.msg code="collectionListing.lastModified" default="Last modified" /></th>
          </tr>
        </thead>
        <tbody>
        <#assign count = 1 />
        <#assign collectionSize = list?size />
        <#list list as res >
          <#assign title = vrtx.propValue(res, 'title') />
          <#assign lastModifiedTime = vrtx.propValue(res, 'lastModified') />
          <#assign modifiedBy = vrtx.propValue(res, 'modifiedBy', 'name-link') />
          <#assign uri = vrtx.getUri(res) />

          <#assign contentType = vrtx.propValue(res, 'contentType') />
          <#assign resourceType = res.resourceType />
          <#assign isCollection = vrtx.propValue(res, 'collection') />
        
          <#assign rowType = "odd" />
          <#if (res_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>

          <#assign firstLast = ""  />
          <#if (res_index == 0) && (res_index == (collectionSize - 1))>
            <#assign firstLast = " first last" />
          <#elseif (res_index == 0)>
            <#assign firstLast = " first" />
          <#elseif (res_index == (collectionSize - 1))>
            <#assign firstLast = " last" />     
          </#if>

          <tr class="${rowType} ${firstLast}">
            <td>
              <a class="vrtx-icon <@vrtx.iconResolver resourceType contentType />" href="${uri?html}"></a>
            </td>
            <td class="vrtx-collection-listing-title">
              <a href="${uri?html}">${title?html}</a>
            <#if (resourceType == "doc" || resourceType == "xls" || resourceType == "ppt")>
              <a class="vrtx-resource-open-webdav" href="${vrtx.linkConstructor(uri, 'webdavService')}"><@vrtx.msg code="report.collection-structure.edit" /></a>
            </#if>
            </td>
            <td class="vrtx-collection-listing-last-modified-by">${modifiedBy}</td>
            <td class="vrtx-collection-listing-last-modified">${lastModifiedTime?html}</td>
          </tr>
        </#list>
        </tbody>
      </table>
      <#if (conf.goToFolderLink?string = "true") && goToFolderLink?exists>
        <a href="${goToFolderLink?html}"><@vrtx.msg code="collectionListing.goToFolder" default="Go to folder" /></a>
      </#if>
    </div>
  <#else>
    Remove div!
  </#if>
</div>