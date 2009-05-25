<#--
  - File: tabs.ftl
  - 
  - Description: Simple tabs implementation
  - 
  - Required model data:
  -   tabs
  - NOTE: this template wll be deprecated by list-menu.ftl
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#if !tabs?exists>
  <#stop "Unable to render model: required submodel 'tabs' missing">
</#if>
<div class="tabs">
<ul class="${tabs.label}">
  <#list tabs.items as tab>
    <#if tab.url?exists>
      <#if tab.active>
        
        <#if tab.readProcessedAll && tab.label == "permissionsService">
           <li class="current activeTab readProcessedAll ${tab.label}">
             <a id="${tab.label}" href="${tab.url?html}" title="<@vrtx.msg code="describe.${tab.label}" default="${tab.title}"/>">${tab.title}</a>
           </li>
        <#else>
           <li class="current activeTab ${tab.label}">
             <a id="${tab.label}" href="${tab.url?html}" title="<@vrtx.msg code="describe.${tab.label}" default="${tab.title}"/>">${tab.title}</a>
           </li>
        </#if>
      <#else>
       <#if tab.readProcessedAll && tab.label == "permissionsService">
          <li class="${tab.label} readProcessedAll"><a id="${tab.label}" href="${tab.url?html}" title="<@vrtx.msg code="describe.${tab.label}" default="${tab.title}"/>">${tab.title}</a></li>
       <#else>
         <li class="${tab.label}"><a id="${tab.label}" href="${tab.url?html}" title="<@vrtx.msg code="describe.${tab.label}" default="${tab.title}"/>">${tab.title}</a></li>
       </#if>
      </#if>
    </#if>
  </#list>
</ul>
</div>


