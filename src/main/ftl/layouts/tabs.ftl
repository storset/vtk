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
    
      <#assign tabClass =  tab.label />
      <#assign tabTitleKey = "describe." + tab.label>
      <#if tab.getAttribute("additionalLabels")?exists>
        <#assign tabClass = tabClass + " " + tab.getAttribute("additionalLabels")>
        <#assign tabTitleKey = tabTitleKey + "." + tab.getAttribute("additionalLabels")>
      </#if>
    
      <#if tab.active>
           <li class="current activeTab ${tabClass}">
             <a id="${tab.label}" href="${tab.url?html}" title="<@vrtx.msg code="${tabTitleKey}" default="${tab.title}"/>">${tab.title}</a>
           </li>
      <#else>
          <li class="${tabClass}"><a id="${tab.label}" href="${tab.url?html}" title="<@vrtx.msg code="${tabTitleKey}" default="${tab.title}"/>">${tab.title}</a></li>
      </#if>
    </#if>
  </#list>
</ul>
</div>


