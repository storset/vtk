<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel 'resourceContext' missing">
</#if>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Publishing status on document</title>
  </head>

  <#assign resource = resourceContext.currentResource />
  <#assign defaultHeader = vrtx.getMsg("publishing.header", "Publishing status on document") />

  <body>
    <div class="resourceInfo publishing">
      <h2>
        <@vrtx.msg code="publishing.header" default="${defaultHeader}"/>
      </h2>
    
    <p>
      <#assign isPublished = vrtx.propValue(resource, "published") == "true" />
      <#if isPublished>
        <@vrtx.msg code="publishing.published.${isPublished?string}" default="This document is published" args=[vrtx.propValue(resource, "publish-date")]/>
      <#else>
        <@vrtx.msg code="publishing.published.${isPublished?string}" default="This document is not published"/>
      </#if>
    </p>
    
    </div>
  </body>
  
</html>