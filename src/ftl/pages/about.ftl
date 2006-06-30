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

<#if !aboutItems?exists>
  <#stop "This template only works with 'aboutItems' model map supplied." />
</#if>

<#macro propertyItemIfExists propertyName>
  <#if aboutItems[propertyName]?exists>
    <@propList.editOrDisplayPropertyItem aboutItems[propertyName] />
  </#if>
</#macro>

<#macro propertyEditURLIfExists propertyName>
  <#if aboutItems[propertyName]?exists>
    <@propList.propertyEditURL aboutItems[propertyName] />
  </#if>
</#macro>


<#function shouldDisplayForm propertyName>
  <#if aboutItems[propertyName]?exists && form?exists
       && form.definition?exists
       && form.definition = aboutItems[propertyName].definition>
    <#return true />
  </#if>
  <#return false />
</#function>

<#macro displayForm propertyName>
  <@propList.propertyForm aboutItems[propertyName] />
</#macro>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>About</title>
</head>
<body>

<#assign resource = resourceContext.currentResource />
<#assign defaultHeader = vrtx.getMsg("resource.metadata.about", "About this resource") />

<#--
<#pre>
  <#list aboutItems?keys as key>
    ${key} = ${aboutItems[key]}
  </#list>
</pre>
 -->

  <div class="resourceInfoHeader">
    <h2>
      <@vrtx.msg
        code="resource.metadata.about.${resource.resourceType}"
        default="${defaultHeader}"/>
    </h2>
  </div>
  <p>Lorem ipsum dolere sit amet...</p>


  <h3 class="resourceInfoHeader">
    <@vrtx.msg
       code="resource.metadata.about.basic"
       default="Basic information"/>
  </h3>
  <table class="resourceInfo">
    <tr>
      <!-- Last modified -->
      <td class="key">
        <@vrtx.msg code="resource.lastModified" default="Last modified"/>:
      </td>
      <td>
        <#assign modifiedByStr = resource.modifiedBy.name />
        <#if resource.modifiedBy.URL?exists>
          <#assign modifiedByStr>
            <a href="${resource.modifiedBy.URL?html}">${resource.modifiedBy.name}</a>
          </#assign>
        </#if>
        <@vrtx.msg code = "resource.lastModifiedBy"
                   args = [ "${resource.lastModified?date}", "${modifiedByStr}" ]
                   default = "${resource.lastModified?date} by ${modifiedByStr}" />
      </td>
      <td>
        
      </td>
    </tr>
      <!-- Owner -->
      <@propertyItemIfExists propertyName = 'owner' />
    <tr>
      <!-- ResourceType -->
      <td class="key">
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
      <td class="key">
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
      <td class="key">
        <@vrtx.msg code="resource.webdavURL" default="WebDAV URL"/>:
      </td>
      <td>
        <a href="${resourceDetail.webdavURL}">${resourceDetail.webdavURL}</a>
      </td>
      <td>
        
      </td>
    </tr>

      <!-- Content language -->
      <@propertyItemIfExists propertyName = 'contentLocale' />

    <tr>
      <!-- Size -->
     <td class="key">
       <@vrtx.msg code="resource.contentLength" default="Content-length"/>:
     </td>
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



  <h3 class="resourceInfoHeader">
    <@vrtx.msg
       code="resource.metadata.about.technical"
       default="Technical details"/>
  </h3>
  <table class="resourceInfo">
      <!-- Content type -->
      <@propertyItemIfExists propertyName = 'contentType' />

    <tr>
      <!-- Character encoding -->
    <#if shouldDisplayForm('userSpecifiedCharacterEncoding')>
      <@displayForm propertyName = 'userSpecifiedCharacterEncoding' />
    <#else>
      <td class="key">
        <@vrtx.msg code="resource.characterEncoding" default="Character encoding"/>:
      </td>
      <td>
        <#if resource.userSpecifiedCharacterEncoding?has_content>
          ${resource.userSpecifiedCharacterEncoding}
        <#else>
          <@vrtx.msg code = "resource.characterEncoding.guessed"
                     args = [ "${resource.characterEncoding}" ]
                     default = "Guessed to be ${resource.characterEncoding}" />
        </#if>
      </td>
      <td>
        <@propertyEditURLIfExists propertyName = 'userSpecifiedCharacterEncoding' />
      </td>
    </#if>
    </tr>
  </table>



  <h3 class="resourceInfoHeader">
    <@vrtx.msg
       code="resource.metadata.about.misc"
       default="Miscellaneous information"/>
  </h3>
  <table class="resourceInfo">
    <tr>
      <!-- Last modified -->
      <td class="key">
        <@vrtx.msg code="resource.creattionTime" default="Created"/>:
      </td>
      <td>
        <#assign createdByStr = resource.createdBy.name />
        <#if resource.createdBy.URL?exists>
          <#assign createdByStr>
            <a href="${resource.createdBy.URL?html}">${resource.createdBy.name}</a>
          </#assign>
        </#if>
        <@vrtx.msg code = "resource.createdBy"
                   args = [ "${resource.creationTime?date}", "${createdByStr}" ]
                   default = "${resource.creationTime?date} by ${createdByStr}" />
      </td>
      <td>
        
      </td>
    </tr>

  </table>


</body>
</html>
