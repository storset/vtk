<#ftl strip_whitespace=true>
<#--
  - File: publications.ftl
  - 
  - Description: List publications from Frida
  -
  -->

<#if publications?exists && publications?size &gt; 0>
  <div class="vrtx-frida-publications">
  <#list publications as publication>
    <div class="vrtx-frida-publication">
      <p><b>${publication.publicationTitle}</b>, ${publication.researchers} (${publication.id})</p>
    </div>
  </#list>
  </div>
</#if>