<#ftl strip_whitespace=true>

<#--
  - File: visualprofile-edit.ftl
  - 
  - Description: HTML page that displays visual profile information
  - provides editing of those properties
  - 
  - Required model data:
  -   resourceContext
  -   resourceProperties
  -  
  - Optional model data:
  -   configuredMetadata
  -
  -->

<#import "/lib/vortikal.ftl" as vrtx />
<#import "components/properties-macro.ftl" as prop />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Visual profile edit</title>
</head>
<body>
  <div style="padding-left:0.5em;padding-right:0.5em;padding-bottom:1em;">
    <#include "components/uio-metadata-header.ftl"/>

    <#if resourceContext.currentResource.getProperty("http://www.uio.no/visuell-profil/arv","arv")?exists>
      <table class="resourceInfo" style="clear:both">
        <@prop.listproperties descriptors=resourceProperties.propertyDescriptors namespace="http://www.uio.no/visuell-profil" nullValue=true/>
      </table>
    <#elseif configuredMetadata?exists >
      <#assign keys = configuredMetadata?keys>

      <table class="resourceInfo" style="clear:both">
        <#if keys?size = 0>
          <div class="noMetadata"><@vrtx.msg code="visualProfile.noMetadataAvailable" default="Pre-defined metadata not available"/>.</div>
        </#if>
        <#list keys as k>
          <tr class="property">
            <td class="propertyName key">
              <#assign h = k?replace("_",".") >
              <@vrtx.msg code="property.http://www.uio.no/visuell-profil:${h}.description"
                   default="${h}"/>:
            </td>
            <td class="propertyValue value">
              ${configuredMetadata[k]}
            </td>
          </tr>
        </#list>
      </table>
    </#if>
    <div style="float:right;font-size:75%;color:grey;padding:5px;">Felt markert med * er obligatoriske</div>

    <h2 class="resourceInfoHeader" style="padding-top:15px;">
      <@vrtx.msg code="visualProfile.other.header" default="Other UiO-metadata"/>
    </h2>

    <table class="resourceInfo">
      <@prop.listproperties descriptors=resourceProperties.propertyDescriptors namespace="http://www.uio.no/visuell-profil/frie"/>
    </table> 

  </div>

</body>
</html>
