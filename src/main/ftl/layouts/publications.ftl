<#ftl strip_whitespace=true>
<#--
  - File: publications.ftl
  - 
  - Description: List publications from Frida
  -
  -->

<#list publications as publication>
  <div class="vrtx-frida-publications">
    <p><b>${publication.publicationTitle}</b>, ${publication.researchers} (${publication.id})</p>
  </div>
</#list>