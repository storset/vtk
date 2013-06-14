<#ftl strip_whitespace=true>

<#assign numberofcolumns = (folders?size / resultSets)?ceiling>
<#list folders?sort as folder>

  <#assign i = 1>
  <#if (folder_index >= i)>
    <#assign i = i + 1>
  </#if>
  <div class="vrtx-list-articles">
    <div class="vrtx-list-articles-col vrtx-list-articles-col-${i}">
    <#if goToFolderLink?exists>
      <a href="${folder}">${folder?cap_first}</a>
    <#else>
      ${folder?cap_first}
    </#if>
    <ul>
    <#list .vars[folder] as result>
      <li><a href="${result.getURI()}">${result.getName()}</a></li>
    </#list>
    </ul>
    </div>
  </div>

</#list>