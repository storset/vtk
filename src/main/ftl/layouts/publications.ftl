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

<#assign counter = 1 />
<#assign tab1 = 0 /><#assign tab2 = 0 /><#assign tab3 = 0 /><#assign tab4 = 0 />

<#if selectedPublications?exists && selectedPublications != "">
   <#assign tab1 = counter />
   <#assign counter = counter +1 />
</#if>
<#if pBooks?exists && pBooks?size &gt; 0>
   <#assign tab2 = counter />
   <#assign counter = counter +1 />
</#if>
<#if pSciArtBookChapters?exists && pSciArtBookChapters?size &gt; 0>
   <#assign tab3 = counter />
   <#assign counter = counter +1 />
</#if>
<#if pOther?exists && pOther?size &gt; 0>
   <#assign tab4 = counter />
</#if>

<ul>
    <#if tab1 != 0>
      <li><a href="#tabs-${tab1}"><@vrtx.msg code="frida.publications.selected" default="Selected"/></a></li>
    </#if>
    <#if tab2 != 0>
      <li><a href="#tabs-${tab2}"><@vrtx.msg code="frida.publications.books" default="Books"/></a></li>
    </#if>
    <#if tab3 != 0>
      <li><a href="#tabs-${tab3}"><@vrtx.msg code="frida.publications.sciartbookchapters" default="Scientific articles and bookchapters"/></a></li>
    </#if>
    <#if tab4 != 0>
      <li><a href="#tabs-${tab4}"><@vrtx.msg code="frida.publications.other" default="Other works"/></a></li>
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
          
            <#assign date = "" />
            <#if publication.mainCategoryCode == "MEDIEBIDRAG" && publication.dato != "">
              <#assign date = parseDate(publication.dato) />
            </#if>
             <#if publication.url != "">
               <#if publication.mainCategoryCode == "BOK">
                 ${publication.researchers}&nbsp;(${publication.year}).&nbsp;<em><a href="${publication.url}">${publication.title}</a></em>.
               <#else>
                 <#if date != "">
                   ${publication.researchers}&nbsp;(${publication.year}&#44;&nbsp;${date}).&nbsp;<a href="${publication.url}">${publication.title}</a>
                 <#else>
                   ${publication.researchers}&nbsp;(${publication.year}).&nbsp;<a href="${publication.url}">${publication.title}</a>
                 </#if>
                 <#if publication.mainCategoryCode != "BOKRAPPORTDEL">.</#if>
               </#if>
             <#else>
               <#if publication.mainCategoryCode == "BOK">
                 ${publication.researchers}&nbsp;(${publication.year}).&nbsp;<em>${publication.title}</em>.
               <#else>
                 <#if date != "">
                   <#t />${publication.researchers}&nbsp;(${publication.year}&#44;&nbsp;${date}).&nbsp;${publication.title}
                 <#else>
                   <#t />${publication.researchers}&nbsp;(${publication.year}).&nbsp;${publication.title}
                 </#if>
                 <#t /><#if publication.mainCategoryCode != "BOKRAPPORTDEL">.</#if>
               </#if>
             </#if>
             <#t />
             <#-- Add additional data if exists - logic and markup ported from SV-faculty JavaScript -->
             <#if publication.mainCategoryCode == "BOK">
                  <#if publication.publisherName != "">${publication.publisherName}.<#else></#if>
                  <#if publication.isbn != "">&nbsp;ISBN&nbsp;${publication.isbn}.</#if>
                  <#if publication.numberOfPages != "">&nbsp;${publication.numberOfPages}&nbsp;s.</#if>
             <#elseif publication.mainCategoryCode == "BOKRAPPORTDEL">
            <#t /><#if publication.titlePartOf != "">&#44;&nbsp;<#if publication.sprak == "EN">In<#else>I:</#if>&nbsp;
                  <#t /><#if publication.researchersPartOf != "">${publication.researchersPartOf}&nbsp;<#if publication.sprak == "EN">(ed.)<#else>(red.)</#if>,&nbsp;</#if>
                    <em>${publication.titlePartOf}</em>.&nbsp;
                  <#else>
                  .&nbsp;
                  </#if>
                  <#if publication.publisherName != "">${publication.publisherName}.</#if>
                  <#if publication.isbn != "">&nbsp;ISBN&nbsp;${publication.isbn}.</#if>
                  <#if publication.chapter != "">&nbsp;${publication.chapter}.</#if>
                  <#if publication.pageFrom != "">&nbsp;s&nbsp;${publication.pageFrom}
                    <#if publication.pageTo != "">-&nbsp;${publication.pageTo}</#if>
                  </#if>
             <#else>
                <#if publication.mainCategoryCode == "TIDSSKRIFTPUBL">
                    <#if publication.publisherUrl != "">
                      <em><a href="${publication.publisherUrl}">${publication.name}</a></em>.
                    <#else>
                      <em>${publication.name}</em>.
                    </#if>
                    <#if publication.issn != "">&nbsp;ISSN&nbsp;${publication.issn}.</#if>
                    <#t /><#if publication.volume != ""><em>&nbsp;${publication.volume}</em></#if>
                    <#t /><#if publication.hefte != ""><#if publication.volume == "">&nbsp;</#if>(${publication.hefte})</#if>
                    <#t /><#if publication.pageFrom != "">
                      <#t /><#if publication.volume != "" || publication.hefte != "">&#44;</#if>&nbsp;s&nbsp;${publication.pageFrom}
                      <#t /><#if publication.pageTo != "">-&nbsp;${publication.pageTo}</#if>
                    <#t /></#if>
                    <#t /><#if publication.doi != "">
                      .&nbsp;doi:<#if !publication.doi?string?starts_with("http")>
                        <a href="http://dx.doi.org/${publication.doi}">${publication.doi}</a><#-- DOI lookup -->
                      <#else>
                        <a href="${publication.doi}">${publication.doi}</a>
                      </#if>
                    </#if>
                <#elseif publication.mainCategoryCode == "MEDIEBIDRAG">
                  <#if publication.mediumtype != "" && publication.mediumtype != "Avis">[${publication.mediumtype}].</#if>
                  <#if publication.medium != "">&nbsp;${publication.medium}.</#if>
                <#elseif publication.mainCategoryCode == "PRODUKT">
                  <#if publication.produkttype != "" || publication.produktformat != "">
                    [<#if publication.produkttype != "">${publication.produkttype}<#if publication.produktformat != "">&nbsp;/&nbsp;</#if>
                    </#if><#if publication.produktformat != "">${publication.produktformat}</#if>].
                  </#if>
                </#if>
             </#if>
          </li>
        </#list>
      </ul>
      <#if publicationsUrl?exists && totalNumberOfPublications?exists>
        <p><a href="${publicationsUrl}"><@vrtx.msg code="frida.publications.url" default="View all publications" args=[totalNumberOfPublications] /></a></p>
      </#if>
    </div>
  </#if> 
</#macro>

<#function parseDate date>
  <#assign month = "" />
  <#switch date?string?split("-")[1]>
    <#case "01"><#assign month = "januar" /><#break>
    <#case "02"><#assign month = "februar" /><#break>
    <#case "03"><#assign month = "mars" /><#break>
    <#case "04"><#assign month = "april" /><#break>
    <#case "05"><#assign month = "mai" /><#break>
    <#case "06"><#assign month = "juni" /><#break>
    <#case "07"><#assign month = "juli" /><#break>
    <#case "08"><#assign month = "august" /><#break>
    <#case "09"><#assign month = "september" /><#break>
    <#case "10"><#assign month = "oktober" /><#break>
    <#case "11"><#assign month = "november" /><#break>
    <#case "12"><#assign month = "desember" /><#break>
    <#default><#assign month = "" />
  </#switch>
  <#return date?string?split("-")[2] + ". " + month />
</#function>