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

<#function shouldDisplayForm propertyName>
  <#if aboutItems[propertyName]?exists && form?exists
       && form.definition?exists
       && form.definition = aboutItems[propertyName].definition>
    <#return true />
  </#if>
  <#return false />
</#function>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>About</title>
</head>
<body id="vrtx-about">
<#assign resource = resourceContext.currentResource />
<#assign defaultHeader = vrtx.getMsg("resource.metadata.about", "About this resource") />

  <div class="resourceInfo">
  <h2>
    <@vrtx.msg
       code="resource.metadata.about.${resource.resourceType}"
       default="${defaultHeader}"/>
  </h2>

  <table id="vrtx-resourceInfoMain" class="resourceInfo">

      <!-- Last modified -->
      <#assign modifiedByStr = modifiedBy.description />
      <#if modifiedBy.URL?exists>
        <#assign modifiedByStr>
          <a title="${modifiedBy.name?html}" href="${modifiedBy.URL?html}">${modifiedBy.description}</a>
        </#assign>
      </#if>
      <#assign modifiedStr>
        <@vrtx.rawMsg code = "property.lastModifiedBy"
                   args = [ vrtx.getPropValue(resource, "lastModified", "longlong"), "${modifiedByStr}" ]
                   default = "${resource.lastModified?date} by ${modifiedByStr}" />
      </#assign>

      <@propList.defaultPropertyDisplay
             propName = "lastModified"
             name = vrtx.getMsg("property.lastModified", "Last modified")
             value = modifiedStr />

      <!-- Created -->
      <#assign createdByStr = createdBy.description />
      <#if createdBy.URL?exists>
        <#assign createdByStr>
          <a title="${createdBy.name?html}" href="${createdBy.URL?html}">${createdBy.description}</a>
        </#assign>
      </#if>
      <#assign createdByStr>
        <@vrtx.rawMsg code = "property.createdBy"
                   args = [ vrtx.getPropValue(resource, "creationTime", "longlong"), "${createdByStr}" ]
                   default = "${resource.creationTime?date} by ${createdByStr}" />
      </#assign>
      <@propList.defaultPropertyDisplay
             propName = "creationTime"
             name = vrtx.getMsg("property.creationTime", "Created")
             value = createdByStr />


      <!-- Owner -->
      <#assign ownerItem = aboutItems['owner'] />
      <#assign msgPrefix = propList.localizationPrefix(ownerItem) />
      <tr>
        <td class="key">
          <@vrtx.msg code=msgPrefix default=ownerItem.definition.name />
        </td>
        <td class="value">
          <#if owner.URL?exists>
            <a title="${owner.name?html}" href="${owner.URL?html}">${owner.description?html}</a>
          <#else>
            ${owner.description?html}
          </#if>

          <#if ownerItem.toggleURL?exists>
            <#assign editAction>
              <@vrtx.msg code="propertyEditor.takeOwnership" default="take ownership" />
            </#assign>
            <#assign warning>
              <@vrtx.msg code="propertyEditor.takeOwnershipWarning"
                         default="Are you sure you want to take ownership of this resource?" />
            </#assign>
            <script type="text/javascript"><!--
              var confirmTakeOwnershipMsg = '${warning?js_string}',
                  confirmTakeOwnershipTitle = '${editAction}';
            // -->
            </script>
            
            <form id="vrtx-admin-ownership-form" action="${ownerItem.toggleURL?html}" method="post">
              <input class="vrtx-button-small" id="vrtx-admin-ownership-button" type="submit" 
                     name="confirmation" value="${editAction}" />
            </form>
          </#if>
        </td>
      </tr>

      <!-- ResourceType -->
      <@propList.defaultPropertyDisplay
             propName = "resourceType"
             name = vrtx.getMsg("property.resourceType", "Resource type")
             value = vrtx.resourceTypeName(resource) />

      <!-- Web address -->
      <#assign url><a id="vrtx-aboutWebAddress" href="${resourceDetail.viewURL?html}">${resourceDetail.viewURL?html}</a></#assign>
      <@propList.defaultPropertyDisplay
             propName = "viewURL"
             name = vrtx.getMsg("resource.viewURL", "Web address")
             value = url />

      <!-- WebDAV address -->
      <#assign url>${resourceDetail.webdavURL?html}</#assign>
      <@propList.defaultPropertyDisplay 
             propName = "webdavURL"
             name = vrtx.getMsg("resource.webdavURL", "WebDAV address")
             value = url />
      
      <!-- Source address -->
      <#if resourceDetail.getSourceURL?exists>
        <#assign url><a id="vrtx-aboutSourceAddress" href="${resourceDetail.getSourceURL?html}">${resourceDetail.getSourceURL?html}</a></#assign>
        <@propList.defaultPropertyDisplay
             propName = "sourceURL"
             name = vrtx.getMsg("resource.sourceURL")
             value = url />
      </#if>
      
      
      <#if resourceDetail.viewImageInfoService?exists>
        <#assign url><a href="${resourceDetail.viewImageInfoService?html}">${resourceDetail.viewImageInfoService?html}</a></#assign>
        <@propList.defaultPropertyDisplay
             propName = "viewAsWebpage"
             name = vrtx.getMsg("resource.viewAsWebpage")
             value = url />
      </#if>
      
      <#if resourceDetail.mediaPlayerService?exists>
        <#assign url><a href="${resourceDetail.mediaPlayerService?html}">${resourceDetail.mediaPlayerService?html}</a></#assign>
        <@propList.defaultPropertyDisplay
             propName = "viewAsWebpage"
             name = vrtx.getMsg("resource.viewAsWebpage")
             value = url />
      </#if>
      

      <!-- Content language -->
      <@propList.editOrDisplayProperty modelName='aboutItems' propertyName = 'contentLocale' displayMacro = 'languagePropertyDisplay' />

      <!-- Comments -->
      <@propList.editOrDisplayProperty modelName='aboutItems' propertyName = 'commentsEnabled' displayMacro = 'generalInheritedPropertyDisplay' />

      <#if !resource.collection>
        <!-- Size -->
        <#assign size>
         <#if resourceContext.currentResource.contentLength?exists>
            <@vrtx.calculateResourceSize resourceContext.currentResource.contentLength />
         <#else>
            <@vrtx.msg code="property.contentLength.unavailable" default="Not available" />
         </#if>
        </#assign>
        <@propList.defaultPropertyDisplay
               propName = "contentLength"
               name = vrtx.getMsg("property.contentLength", "Size")
               value = size />

        <!-- Editorial contacts for document (can only be inherited) -->
        <#if resourceDetail.propertyInheritanceMap["editorial-contacts"]?exists>
           <@propList.defaultPropertyDisplay propName = 'editorial-contacts' 
                                             name = vrtx.getMsg("property.editorial-contacts")
                                             value = vrtx.getPropValue(resource, "editorial-contacts") />
        </#if>
      <#else>
        <!-- Editorial contacts -->
        <@propList.editOrDisplayProperty modelName = 'aboutItems' propertyName = 'editorial-contacts' displayMacro = 'generalInheritedPropertyDisplay' />
      </#if>
  </table>

  <#if urchinStats?exists>
    <h3 id="resourceVisitHeader"><@vrtx.msg code="resource.metadata.about.visit" default="Visit count"/></h3>
    <!--[if lt IE 9]>
      <style type="text/css">
        .vrtx-resource-visit-stat {
          width: 20%;
        }
        #vrtx-resource-visit-stats {
          clear: left;
          width: 100%;
          margin: 10px 0 0 0;
          top: 0px;
        }
        #vrtx-resource-visit-info {
          bottom: 111px;
        }
      </style>
    <![endif]-->
    <script type="text/javascript"><!--
      $(function() {
        $("<div id='vrtx-resource-visit-wrapper'><span id='urchin-loading'></span></div>").insertAfter("#resourceVisitHeader"); 
        vrtxAdmin.serverFacade.getText("${urchinStats}", {
          success: function (results, status, resp) {
            $("#vrtx-resource-visit-wrapper").html(results);
          }
        }); 
      });
    // -->
    </script>
  </#if>

  <h3 class="resourceInfoHeader">
    <@vrtx.msg
       code="resource.metadata.about.content"
       default="Information describing the content"/>
  </h3>

  <#-- @propList.propertyList
       modelName = "aboutItems"
       itemNames =  [ 'title', 'navigation:hidden', 'navigation:importance', 
                      'content:description', 'content:verifiedDate',
                      'content:authorName', 'content:authorEmail', 
                      'content:authorURL' ] / -->

  <table id="vrtx-resourceInfoContent" class="resourceInfo">
    <!-- title -->
    <@propList.editOrDisplayPropertyItem item=aboutItems['userTitle'] defaultItem=aboutItems['title'] inputSize=40 />

    <!-- content:description -->
    <#if resource.resourceType != "audio" && resource.resourceType != "video" && resource.resourceType != "image" >
        <@propList.editOrDisplayProperty modelName='aboutItems' propertyName = 'content:description' inputSize=100 />
    </#if>

 </table>


  <h3 class="resourceInfoHeader">
    <@vrtx.msg
       code="resource.metadata.about.technical"
       default="Technical details"/>
  </h3>
  <table id="vrtx-resourceInfoTechnical" class="resourceInfo">

  <#if resource.collection>
    <!-- navigation:hidden
    <@propList.editOrDisplayProperty modelName='aboutItems' propertyName = 'navigation:hidden' />

    navigation:importance
    <@propList.editOrDisplayProperty modelName='aboutItems' propertyName = 'navigation:importance' /> -->

    <!-- Type of Collection -->

    <@propList.editOrDisplayProperty modelName='aboutItems' propertyName = 'collection-type' />


  <#else>
    <!-- Content type -->
    <@propList.editOrDisplayProperty modelName='aboutItems' propertyName = 'contentType' inputSize=50/>

    <!-- Character encoding -->
    <#if aboutItems['characterEncoding']?exists>
      <@propList.editOrDisplayPropertyItem item=aboutItems['userSpecifiedCharacterEncoding'] defaultItem=aboutItems['characterEncoding'] />
    </#if>

    <!-- Plaintext Edit on managed xml -->
    <@propList.editOrDisplayProperty modelName='aboutItems' propertyName = 'plaintext-edit' />

    <!-- Type of XHTML document -->
    <@propList.editOrDisplayProperty modelName='aboutItems' propertyName = 'xhtml10-type' />

    <!-- Media info -->
    <#if aboutItems['pixelWidth']?? && aboutItems['pixelWidth'].property??>
      <@propList.defaultPropertyDisplay propName='pixelWidth' name=vrtx.getMsg("proptype.name.pixelWidth") value=aboutItems['pixelWidth'].property.intValue />
    </#if>
    <#if aboutItems['pixelWidth']?? && aboutItems['pixelHeight'].property??>
      <@propList.defaultPropertyDisplay propName='pixelHeight' name=vrtx.getMsg("proptype.name.pixelHeight") value=aboutItems['pixelHeight'].property.intValue />
    </#if>
    <#if aboutItems['duration']?? && aboutItems['duration'].property??>
      <#assign timeValue>
        <@vrtx.displayTime aboutItems['duration'].property.intValue />
      </#assign>
      <@propList.defaultPropertyDisplay propName='duration' name=vrtx.getMsg("proptype.name.duration") value=timeValue />
    </#if>
  </#if>
  </table>
  </div>

</body>
</html>

<#macro languagePropertyDisplay propName name value prefix=false editURL="">
  <tr class="prop-${propName}">
    <td class="key">
      ${name}:
    </td>
    <td class="value">
      <#if prefix?is_string>
        ${prefix}
      </#if>
      <#compress>
      <#local l=vrtx.resourceLanguage()?string />
      
      <#if .vars['aboutItems'][propName].property?exists && .vars['aboutItems'][propName].property.inherited>
          <@vrtx.msg "resource.property.unset" "Not set"/>, <@vrtx.msg "language.inherits" "inherits"/> ${l?lower_case}
      <#else>
          ${l}
      </#if>
      </#compress>
      <#if editURL != "">
        ${editURL}
      </#if>
    </td>
  </tr>
</#macro>

<#-- XXX: this macro only works for props in aboutItems, which excludes props
          that are inherited, but do not belong the current resource type. -->
<#macro generalInheritedPropertyDisplay propName name value prefix=false editURL="">
  <tr class="prop-${propName}">
    <td class="key">
      ${name}:
    </td>
    <td class="value">
      <#if prefix?is_string>
        ${prefix}
      </#if>
      ${value?trim}
      <#compress>
      <#if .vars['aboutItems'][propName].property?exists && .vars['aboutItems'][propName].property.inherited>
         &nbsp;(<@vrtx.msg "resource.property.inherited"  "inherited" />)
      </#if>
      </#compress>
      <#if editURL != "">
        ${editURL}
      </#if>
    </td>
  </tr>
</#macro>