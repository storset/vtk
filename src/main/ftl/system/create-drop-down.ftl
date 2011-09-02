<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<#assign docUrl = createDropDown.url?html />
<#if docUrl?contains("?")>
  <#assign docUrl = docUrl + "&display=create-document-from-drop-down" />
<#else>
  <#assign docUrl = docUrl + "?vrtx=admin&display=create-document-from-drop-down" />
</#if>

<#assign colUrl = createDropDown.url?html />
<#if colUrl?contains("?")>
  <#assign colUrl = colUrl + "&display=create-collection-from-drop-down" />
<#else>
  <#assign colUrl = colUrl + "?vrtx=admin&display=create-collection-from-drop-down" />
</#if>

<#assign upUrl = createDropDown.url?html />
<#if upUrl?contains("?")>
  <#assign upUrl = upUrl + "&display=upload-file-from-drop-down" />
<#else>
  <#assign upUrl = upUrl + "?vrtx=admin&display=upload-file-from-drop-down" />
</#if>

<#-- Tmp. fix for getting 'https' page -->

<#if resourceContext.repositoryId != "localhost">
  <#if docUrl?contains("http:")>
    <#assign docUrl = docUrl?replace("http", "https:") />
  </#if>
  <#if colUrl?contains("http:")>
    <#assign colUrl = colUrl?replace("http", "https:") />
  </#if>
  <#if upUrl?contains("http:")>
    <#assign upUrl = upUrl?replace("http", "https:") />  
  </#if>
</#if>

<#-- ---------- -->

<ul class="manage-create"> 
  <li class="manage-create-drop first">
    <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.document" default="Choose where you would like to create document" />" href="${docUrl?html}">
      <@vrtx.msg code="manage.document" default="Create document" />
    </a>
  </li>
  <li class="manage-create-drop">
    <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.collection" default="Choose where you would like to create folder" />" href="${colUrl?html}">
      <@vrtx.msg code="manage.collection" default="Create folder" />
    </a>
  </li>
  <li class="manage-create-drop">
    <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.upload-file" default="Choose where you would like to upload file" />" href="${upUrl?html}">
      <@vrtx.msg code="manage.upload-file" default="Upload file" />
    </a>
  </li>
</ul>