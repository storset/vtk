<#ftl strip_whitespace=true>
<#--
  - File: publications.ftl
  - 
  - Description: List publications from Frida
  -
  -->
  
<#assign counter = 1>
<#assign tab1 = 0><#assign tab2 = 0><#assign tab3 = 0><#assign tab4 = 0>

<#if selectedPublications?exists && selectedPublications != "">
   <#assign tab1 = counter>
   <#assign counter = counter +1>
</#if>
<#if pBooks?exists && pBooks?size &gt; 0>
   <#assign tab2 = counter>
   <#assign counter = counter +1>
</#if>
<#if pSciArtBookChapters?exists && pSciArtBookChapters?size &gt; 0>
   <#assign tab3 = counter>
   <#assign counter = counter +1>
</#if>
<#if pOther?exists && pOther?size &gt; 0>
   <#assign tab4 = counter>
</#if>



<ul>
    <#if tab1 != 0>
      <li><a href="#tabs-${tab1}">Utvalgte</a></li>
    </#if>
    <#if tab2 != 0>
      <li><a href="#tabs-${tab2}">Bøker</a></li>
    </#if>
    <#if tab3 != 0>
      <li><a href="#tabs-${tab3}">Vitenskapelige artikler og bokkapitler</a></li>
    </#if>
    <#if tab4 != 0>
      <li><a href="#tabs-${tab4}">Andre arbeider</a></li>
    </#if>
</ul> 

<#if tab1 != 0>
  <div id="tabs-${tab1}">
    ${selectedPublications}
  </div>
</#if>

<@listPublications tab2 pBooks />
<@listPublications tab3 pSciArtBookChapters />
<@listPublications tab4 pOther />

<#macro listPublications tabNumber publications>
  <#if tabNumber != 0>
    <div id="tabs-${tabNumber}">
      <ul class="vrtx-frida-publication">
        <#list publications as publication>
          <li>${publication.researchers}&nbsp;(${publication.year}). ${publication.title}</li>
        </#list>
      </ul>
    </div>
  </#if> 
</#macro>