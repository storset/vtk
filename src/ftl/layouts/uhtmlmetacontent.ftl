<#ftl strip_whitespace=true>

<#--
  - File: uhtmlmetacontent.ftl
  - 
  - Description: Adds HTML <meta> tags for UHTML
  - 
  - Required model data:
  -  
  - Optional model data:
  -   configuredMetadata
  -   uhtmlResourceContext
  -
  -->

<#import "/lib/vortikal.ftl" as vrtx />


<#macro spraak spraak><#if spraak = "no_NO_NY">no-nyn<#elseif spraak = "en">en<#else>no-bok</#if></#macro>

<#if uhtmlResourceContext.currentResource?exists && configuredMetadata?exists>

  <#if uhtmlResourceContext.currentResource.getProperty("http://www.uio.no/vortex/custom-properties","visual-profile")?exists>
  <#compress>
    <#assign keys = configuredMetadata?keys />
    <#list keys as k>
        <meta name="${k?replace("_",".")}" content="${configuredMetadata[k]}" >
      </#list>

    <#-- Translates our represention of language to the represention in uhtml -->
    <#if uhtmlResourceContext.currentResource.contentLocale?exists>
      <meta name="spraak" content="<@spraak spraak="${uhtmlResourceContext.currentResource.contentLocale}"/>"> 
    <#else>
      <meta name="spraak" content="<@spraak spraak="No language available"/>"> 
    </#if>
      
    <#list uhtmlResourceContext.currentResource.properties as property>
      <#if property.namespace = "http://www.uio.no/visuell-profil/frie">
        <meta name="${property.name}" content="${property.value}">
      </#if>
    </#list>
  </#compress>
  </#if>

</#if>

