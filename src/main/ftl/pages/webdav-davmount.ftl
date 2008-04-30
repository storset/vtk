<?xml version="1.0" encoding="utf-8"?>
<dm:mount xmlns:dm="http://purl.org/NET/webdav/mount">
  <dm:url>${webdavService.url?html}</dm:url>
  <#if (resourceContext.principal)?exists>
  <dm:username>${resourceContext.principal.name?html}</dm:username>
  </#if>
</dm:mount>
