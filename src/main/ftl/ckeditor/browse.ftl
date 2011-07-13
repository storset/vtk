<?xml version="1.0" encoding="utf-8" ?>
<Connector command="${command}" resourceType="${resourceType}">
  <CurrentFolder path="${currentFolder?html}" url="${currentFolder?html}" />
  <#if folders?exists>
  <Folders>
  <#list folders?keys as uri>
    <Folder name="${folders[uri].resource.name?html}" />
  </#list>
  </Folders>
  </#if>
  <#if files?exists>
  <Files>
  <#list files?keys as uri>
    <File name="${files[uri].resource.name?html}" size="${(files[uri].resource.contentLength/1000)?html}" />
  </#list>
  </Files>
  </#if>
  <#if error?exists>
  <#if customMessage?exists>
  <Error number="${error?html}" msg="${customMessage?html?if_exists}" />
  <#else>
  <Error number="${error?html}" />
  </#if>
  </#if>
</Connector>
