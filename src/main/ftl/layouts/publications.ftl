<#ftl strip_whitespace=true>
<#--
  - File: publications.ftl
  - 
  - Description: List publications from Frida
  -
  -->
  
<#assign counter = 1>
<#assign t1 = 0><#assign t2 = 0><#assign t3 = 0><#assign t4 = 0>

<#if selectedPublications?exists && selectedPublications != "">
   <#assign t1 = counter>
   <#assign counter = counter +1>
</#if>
<#if pBooks?exists && pBooks?size &gt; 0>
   <#assign t2 = counter>
   <#assign counter = counter +1>
</#if>
<#if pSciArtBookChapters?exists && pSciArtBookChapters?size &gt; 0>
   <#assign t3 = counter>
   <#assign counter = counter +1>
</#if>
<#if pOther?exists && pOther?size &gt; 0>
   <#assign t4 = counter>
</#if>



<ul>
    <#if t1 != 0>
      <li><a href="#tabs-${t1}">Utvalgte</a></li>
    </#if>
    <#if t2 != 0>
      <li><a href="#tabs-${t2}">Bøker</a></li>
    </#if>
    <#if t3 != 0>
      <li><a href="#tabs-${t3}">Vitenskapelige artikler og bokkapitler</a></li>
    </#if>
    <#if t4 != 0>
      <li><a href="#tabs-${t4}">Andre arbeider</a></li>
    </#if>
</ul> 

<#if t1 != 0>
  <div id="tabs-${t1}">
    ${selectedPublications}
  </div>
</#if>

<#if t2 != 0>
  <div id="tabs-${t2}">
    <ul class="vrtx-frida-publication">
      <#list pBooks as publication>
        <li>${publication.researchers}&nbsp;(${publication.year}). ${publication.title}</li>
      </#list>
    </ul>
  </div>
</#if>

<#if t3 != 0>
  <div id="tabs-${t3}">
    <ul class="vrtx-frida-publication">
      <#list pSciArtBookChapters as publication>
        <li>${publication.researchers}&nbsp;(${publication.year}). ${publication.title}</li>
      </#list>
    </ul>
  </div>
</#if>

<#if t4 != 0>
  <div id="tabs-${t4}">
    <ul class="vrtx-frida-publication">
      <#list pOther as publication>
        <li>${publication.researchers}&nbsp;(${publication.year}). ${publication.title}</li>
      </#list>
    </ul>
  </div>
</#if>