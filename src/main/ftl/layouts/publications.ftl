<#ftl strip_whitespace=true>
<#--
  - File: publications.ftl
  - 
  - Description: List publications from Frida
  -
  - Required model data:
  -
  -->
  
<#import "/lib/vortikal.ftl" as vrtx />

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
      <ul class="vrtx-frida-publications">
        <#assign publicationNr = 0 />
        <#list publications as publication>
          <#assign publicationNr = publicationNr +1 />
          <li id="vrtx-frida-publication-${publicationNr}" class="vrtx-frida-publication">
             <#if publication.url != "">
               <#if publication.mainCategoryCode == "BOK">
                 ${publication.researchers}&nbsp;(${publication.year}).&nbsp;<em><a href="${publication.url}">${publication.title}</a></em>
               <#else>
                 ${publication.researchers}&nbsp;(${publication.year}).&nbsp;<a href="${publication.url}">${publication.title}</a>
               </#if>
             <#else>
               <#if publication.mainCategoryCode == "BOK">
                 ${publication.researchers}&nbsp;(${publication.year}). <em>${publication.title}</em>
               <#else>
                 ${publication.researchers}&nbsp;(${publication.year}).&nbsp;${publication.title}
               </#if>
             </#if>
             
             <#-- Add additional data if exists - logic and markup ported from SV-faculty JavaScript -->
             <#if publication.mainCategoryCode == "BOK">
                <ul><li>
                  <#if publication.publisherName != "">${publication.publisherName}.</#if>
                  <#if publication.isbn != "">&nbsp;ISBN&nbsp;${publication.isbn}.</#if>
                  <#if publication.numberOfPages != "">&nbsp;${publication.numberOfPages}&nbsp;s.</#if>
                </li></ul>
             <#elseif publication.mainCategoryCode == "BOKRAPPORTDEL">
                <ul><li>
                  <#if publication.titlePartOf != ""><em>${publication.titlePartOf}</em>,&nbsp;</#if>
                  <#if publication.publisherName != "">${publication.publisherName}.</#if>
                  <#if publication.isbn != "">&nbsp;ISBN&nbsp;${publication.isbn}.</#if>
                  <#if publication.chapter != "">&nbsp;${publication.chapter}.</#if>
                  <#if publication.pageFrom != "">&nbsp;s&nbsp;${publication.pageFrom}
                    <#if publication.pageTo != "">-&nbsp;${publication.pageTo}</#if>
                  </#if>
                </li></ul>
             <#else>
                <#if publication.mainCategoryCode == "TIDSSKRIFTPUBL">
                  <ul><li>
                    <#if publication.publisherUrl != "">
                      <em><a href="${publication.publisherUrl}">${publication.name}</a></em>. 
                    <#else>
                      <em>${publication.name}</em>.
                    </#if>
                    <#if publication.issn != "">&nbsp;ISBN&nbsp;${publication.issn}.</#if>
                    <#if publication.volume != ""><em>&nbsp;vol.&nbsp;${publication.volume}</em></#if>
                    <#if publication.hefte != "">&nbsp;(${publication.hefte})</#if>
                    <#if publication.pageFrom != "">
                      <#if publication.volume != "" || publication.hefte != "">,</#if>&nbsp;s&nbsp;${publication.pageFrom}     
                      <#if publication.pageTo != "">-&nbsp;${publication.pageTo}</#if>
                    </#if>
                  </li></ul
                </#if>
             </#if>
          </li>
        </#list>
      </ul>
      <p><a href="${publicationsUrl}">Se alle ${totalNumberOfPublications} publikasjoner</a></p>
    </div>
  </#if> 
</#macro>