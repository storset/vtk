<?xml version="1.0" encoding="utf-8" ?>
<Connector command="${command}" resourceType="File">
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
    <File name="${files[uri].resource.name}" size="${(files[uri].resource.contentLength/1000)?html}" />
  </#list>
  </Files>
  </#if>
</Connector>
