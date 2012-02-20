<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#if conf?exists>
  <div class="vrtx-folder-component">
    uri: ${conf.uri}
    maxItems: ${conf.maxItems}
    goToFolderLink: ${conf.goToFolderLink?string}
    folderTitle: ${conf.folderTitle?string}
  </div>
</#if>