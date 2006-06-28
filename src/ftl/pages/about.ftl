<#ftl strip_whitespace=true>

<#--
  - File: about.ftl
  - 
  - Description: A HTML page that displays resource info
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/propertyList.ftl" as propList />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>About</title>
</head>
<body>

<#assign resource = resourceContext.currentResource />
<#assign defaultHeader = vrtx.getMsg("resource.metadata.about", "About this resource") />

  <div class="resourceInfoHeader" style="padding-top:0;padding-bottom:1.5em;">
    <h2 style="padding-top: 0px;float:left;">
      <@vrtx.msg
        code="resource.metadata.about.${resource.resourceType}"
        default="${defaultHeader}"/>
    </h2>
  </div>
  <p>Lorem ipsum dolere sit amet...</p>


  <h3 class="resourceInfoHeader">
    <@vrtx.msg
       code="resource.metadata.about.basic-information"
       default="Basic information"/>
  </h3>
  <table style="clear: both;" class="resourceInfo">
    <tr>
      <!-- Last modified -->
      <td>
        <@vrtx.msg code="resource.lastModified" default="Last modified"/>:
      </td>
      <td>
        ${resource.lastModified}
        <@vrtx.msg code="resource.lastModified.by" default="by"/>
        <#assign modifiedBy = resource.lastModifiedBy />
        <#if modifiedBy.URL?exists>
          <a href="${modifiedBy.URL?html}">${modifiedBy.name}</a>
        <#else>
          ${modifiedBy.name}
        </#if>
      </td>
      <td>
        
      </td>
    </tr>
      <!-- Owner -->
      <@propertyItemIfExists aboutItems.owner />
    <tr>
      <!-- ResourceType -->
      <td>
        <@vrtx.msg code="resource.resourceType" default="Resource type"/>:
      </td>
      <td>
        <@vrtx.msg code="resource.resourceType.${resource.resourceType}" 
                   default="${resource.resourceType}" />
      </td>
      <td>
        
      </td>
    </tr>

    <tr>
      <!-- Web address -->
      <td>
        <@vrtx.msg code="resource.viewURL" default="Web address"/>
      </td>
      <td>
        <a href="${resourceDetail.viewURL?html}">${resourceDetail.viewURL}</a>
      </td>
      <td>
        
      </td>
    </tr>

    <tr>
      <!-- WebDAV address -->
      <td>
        <@vrtx.msg code="resource.webdavURL" default="WebDAV URL"/>:
      </td>
      <td>
        <a href="${resourceDetail.webdavURL}">${resourceDetail.webdavURL}</a>
      </td>
      <td>
        
      </td>
    </tr>

      <!-- Owner -->
      <@propertyItemIfExists aboutItems.owner />

  <tr>
      <!-- Size -->
     <td><@vrtx.msg code="resource.contentLength" default="Content-length"/>:</td>
     <td>
       <#if resourceContext.currentResource.contentLength?exists>
          <#if resourceContext.currentResource.contentLength <= 1000>
            ${resourceContext.currentResource.contentLength} B
          <#elseif resourceContext.currentResource.contentLength <= 1000000>
            ${(resourceContext.currentResource.contentLength / 1000)?string("0.#")} KB
          <#elseif resourceContext.currentResource.contentLength <= 1000000000>
            ${(resourceContext.currentResource.contentLength / 1000000)?string("0.#")} MB
          <#elseif resourceContext.currentResource.contentLength <= 1000000000000>
            ${(resourceContext.currentResource.contentLength / 1000000000)?string("0.#")} GB
          <#else>
            ${resourceContext.currentResource.contentLength} B
          </#if>
       <#else>
	 <@vrtx.msg code="resource.contentLength.unavailable" default="Not available" />
       </#if>
     </td>
      <td>
        
      </td>
  </tr>


  </table>

</body>
</html>
