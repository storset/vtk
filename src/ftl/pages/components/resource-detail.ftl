<#--
  - File: resource-detail.ftl
  - 
  - Description: Component that displays a table containing various 
  - resource properties, such as owner, contentType, language,
  - etc. with forms to edit these properties.
  - 
  - Required model data:
  -   resourceContext
  -   resourceDetail
  -  
  - Optional model data:
  -   ownershipForm
  -   editContentTypeForm
  -   editContentLanguageForm
  -   editCharacterEncodingForm
  -->

<#import "/lib/vortikal.ftl" as vrtx />

<#if !resourceContext?exists>
  <#stop "Unable to render model: required model data
  'resourceContext' missing">
</#if>

<#if !resourceDetail?exists>
  <#stop "Unable to render model: required model data
  'resourceDetail' missing">
</#if>

<#assign defaultHeader = vrtx.getMsg("resource.metadata.about", "About this resource") />
<div class="resourceInfoHeader" style="padding-top:0;padding-bottom:1.5em;">
  <h2 style="padding-top: 0px;float:left;">
    <@vrtx.msg
       code="resource.metadata.about.${resourceContext.currentResource.resourceType}"
       default="${defaultHeader}"/>
  </h2>
</div>

<table style="clear:both;" class="resourceInfo">

  <tr class="lastModified">
    <td class="key"><@vrtx.msg code="resource.lastModified" default="Last modified"/>:</td>
    <td class="value">
      ${resourceContext.currentResource.contentLastModified?datetime}
      <@vrtx.msg code="resource.lastModified.by" default="by" />
      <#if resourceContext.currentResource.contentModifiedBy.URL?exists>
        <a href="${resourceContext.currentResource.contentModifiedBy.URL}">${resourceContext.currentResource.contentModifiedBy.name}</a>
      <#else>
        ${resourceContext.currentResource.contentModifiedBy.name}
      </#if>
    </td>
  </tr>

  <tr class="owner">
    <td class="key">Created by:</td>
    <td class="value">${resourceContext.currentResource.createdBy.name}</td>
  </tr>

  <tr class="owner">
    <td class="key">Modified by:</td>
    <td class="value">${resourceContext.currentResource.modifiedBy.name}</td>
  </tr>

  <tr class="owner">
  <#if ownershipForm?exists && !ownershipForm.done>
  <td colspan="2" class="expandedForm">
  <form action="${ownershipForm.submitURL?html}" method="POST">
    <h3 class="nonul"><@vrtx.msg code="resource.owner" default="Owner"/>:</h3>
    <div class="nonul"><input type="text" size="12" name="owner" value="${ownershipForm.owner}"></div>
    <div><input type="submit" name="save" value="<@vrtx.msg code="resource.owner.save" default="Save"/>">
    <input type="submit" name="cancelAction" value="<@vrtx.msg code="resource.owner.cancel" default="Cancel"/>"></div>
  </form>
  </td>
  <#else>
    <td class="key"><@vrtx.msg code="resource.owner" default="Owner"/>:</td>
    <td class="value">${resourceContext.currentResource.owner.name}
       <#if resourceDetail.ownershipServiceURL?exists>
      (&nbsp;<a href="${resourceDetail.ownershipServiceURL?html}"><@vrtx.msg code="resource.owner.edit" default="edit"/></a>&nbsp;)
    </#if>
    </td>
  </#if>
  </tr>

  <#if resourceDetail.viewURL?exists>
  <tr class="viewURL">
    <td class="key"><@vrtx.msg code="resource.viewURL" default="Web address"/>:</td>
    <td class="value"><a href="${resourceDetail.viewURL?html}">${resourceDetail.viewURL}</a></td>
  </tr>
  </#if>

  <#if resourceDetail.webdavURL?exists>
  <tr class="webdavURL">
    <td class="key"><@vrtx.msg code="resource.webdavURL" default="WebDAV URL"/>:</td>
    <td class="value"><a href="${resourceDetail.webdavURL}">${resourceDetail.webdavURL}</a></td>
  </tr>
  </#if>

  <#if !resourceContext.currentResource.collection>
  <tr class="contentType">
  <#if editContentTypeForm?exists && !editContentTypeForm.done>
    <td colspan="2" class="expandedForm">
    <form action="${editContentTypeForm.submitURL?html}" method="POST">
      <h3 class="nonul"><@vrtx.msg code="resource.contentType" default="Content-type"/>:</h3>
      <div class="nonul"><input type="text" size="12" name="contentType" value="${editContentTypeForm.contentType?default("")}"></div>
      <div><input type="submit" name="save" value="<@vrtx.msg code="resource.contentType.save" default="Save"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="resource.contentType.cancel" default="Cancel"/>"></div>
    </form>
    </td>
  <#else>
     <td class="key"><@vrtx.msg code="resource.contentType" default="Content-type"/>:</td>
     <td class="value">${resourceContext.currentResource.contentType}
     <#if resourceDetail.setContentTypeServiceURL?exists>
      (&nbsp;<a href="${resourceDetail.setContentTypeServiceURL?html}"><@vrtx.msg code="resource.contentType.edit" default="edit"/></a>&nbsp;)</#if>
     </td>
  </#if>
  </tr>

  <tr class="contentLanguage">
  <#if editContentLanguageForm?exists && !editContentLanguageForm.done>
    <td colspan="2" class="expandedForm">
    <form action="${editContentLanguageForm.submitURL?html}" method="POST">
      <h3 class="nonul"><@vrtx.msg code="resource.contentLanguage" default="Content-language"/>:</h3>
      <div class="nonul">
        <select name="contentLanguage"> 
        <#list editContentLanguageForm.possibleLanguages as lang>
          <option value="${lang}" <#if editContentLanguageForm.contentLanguage?exists && editContentLanguageForm.contentLanguage = "${lang}">selected=""</#if>><@vrtx.msg code="resource.contentLanguage.${lang}" default="${lang}"/></option>
        </#list>
        </select>
      </div>
      <div><input type="submit" name="save" value="<@vrtx.msg code="resource.contentLanguage.save" default="Save"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="resource.contentLanguage.cancel" default="Cancel"/>"></div>
    </form>
    </td>
  <#else>
     <td class="key"><@vrtx.msg code="resource.contentLanguage" default="Content-language"/>:</td>
     <#assign langKey = 'resource.contentLanguage.' + (resourceContext.currentResource.contentLanguage)?default('unknown') />
     <td class="value"><@vrtx.msg code="${langKey}" default="unknown"/>
     <#if resourceDetail.setContentLanguageServiceURL?exists>
      (&nbsp;<a href="${resourceDetail.setContentLanguageServiceURL?html}"><@vrtx.msg code="resource.contentLanguage.edit" default="edit"/></a>&nbsp;)</#if>
     </td>
  </#if>
  </tr>

  <tr class="characterEncoding">
  <#if editCharacterEncodingForm?exists && !editCharacterEncodingForm.done>
    <td colspan="2" class="expandedForm">
    <form action="${editCharacterEncodingForm.submitURL?html}" method="POST">
      <h3 class="nonul"><@vrtx.msg code="resource.characterEncoding" default="Character-encoding"/>:</h3>
      <div class="nonul"><input type="text" size="12" name="characterEncoding" value="${editCharacterEncodingForm.characterEncoding?default("")}"></div>
      <div><input type="submit" name="save" value="<@vrtx.msg code="resource.characterEncoding.save" default="Save"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="resource.characterEncoding.cancel" default="Cancel"/>"></div>
    </form>
    </td>
  <#else>
     <td class="key"><@vrtx.msg code="resource.characterEncoding" default="Character-encoding"/>:</td>
     <td class="value">
       <#if resourceContext.currentResource.characterEncoding?exists>
	 ${resourceContext.currentResource.characterEncoding}
       <#else>
	 <@vrtx.msg code="resource.chacacterEncoding.notSet" default="Not set" />
       </#if>
     <#if resourceDetail.setCharacterEncodingServiceURL?exists>
      (&nbsp;<a href="${resourceDetail.setCharacterEncodingServiceURL?html}"><@vrtx.msg code="resource.characterEncoding.edit" default="edit"/></a>&nbsp;)</#if>
     </td>
  </#if>
  </tr>

  <tr>
     <td class="key"><@vrtx.msg code="resource.contentLength" default="Content-length"/>:</td>
     <td class="value">
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
  </tr>
  </#if>

</table>
