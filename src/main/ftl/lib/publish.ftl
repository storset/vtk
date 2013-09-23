<#import "/lib/vortikal.ftl" as vrtx />

<#macro publishMessage resourceContext>
<#local resource = resourceContext.currentResource />
<#local propResource = vrtx.getProp(resourceContext.currentResource,"unpublishedCollection")  />
<#if resourceContext.parentResource?exists >
    <#local propParent = vrtx.getProp(resourceContext.parentResource,"unpublishedCollection")  />
</#if>

<p>
<#local notPublished = ((propResource?has_content || propParent?has_content) || !resource.published)  />
<span class="<#if notPublished >unpublished<#else>published</#if>">
    <#if propResource?has_content && propResource.inherited >
        <#if resource.published >
            <@vrtx.msg code="publish.unpublished.published" /> 
        <#else> 
            <@vrtx.msg code="publish.unpublished.unpublishedCollection" />
        </#if>
   <#elseif propResource?has_content && !propParent?has_content>  
       <@vrtx.msg code="publish.permission.unpublished" /> 

    <#elseif propParent?has_content >
        <@vrtx.msg code="publish.unpublished.unpublishedCollection" />

    <#elseif resource.published>
        <@vrtx.msg code="publish.permission.published" /> 
    <#else>
        <@vrtx.msg code="publish.permission.unpublished" /> 
    </#if>
</span>
<#if propResource?has_content && propResource.inherited >
    <#if resource.published >
      <abbr class="tooltips delayed" title="<@vrtx.msg code="publish.unpublished.published.info" />" />
    <#else> 
      <abbr class="tooltips delayed" title="<@vrtx.msg code="publish.unpublished.unpublishedCollection.info" />" />
    </#if>
<#elseif propResource?has_content && !propParent?has_content>  
  <abbr class="tooltips delayed" title="<@vrtx.msg code="unpublishedCollection.info" />" />
<#elseif propParent?has_content >
  <abbr class="tooltips delayed" title="<@vrtx.msg code="publish.unpublished.unpublishedCollection.info" />" />
</#if> 
</p>
</#macro>