<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#if relatedDocuments?? && (relatedDocuments?size > 0) >
  <div class="vrtx-${viewName} vrtx-frontpage-box">
    <h2><@vrtx.msg code="${viewName}.title" /></h2>
    <ul>
      <#list relatedDocuments as document>
        <#if document.url??>
          <li><a href="${document.url?html}">${document.title?html}</a></li>
        <#else>
          <li>${document.title?html}</li>
        </#if>
      </#list>
    </ul>
  </div>
</#if>
