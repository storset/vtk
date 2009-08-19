<#--
  - File: tag-cloud.ftl
  - 
  - Description: Simple rendering of a tag loud as a list. Feel free to improve.
  - 
  - Required model data:
  -     tagElements - List<org.vortikal.web.view.decorating.components.TagCloudComponent.TagElement>
  - 
  -->

<#if tagElements?exists>
  <div class="vrtx-tag-cloud"> 
    <#if tagElements?has_content>
      <ul class="vrtx-tag-cloud">
        <#list tagElements as element>
          <li class="tag-magnitude-${element.magnitude}">
            <a class="tag" href="${element.linkUrl?html}" rel="tag">${element.text?html}</a>
          </li>
        </#list>
      </ul>
    </#if>
  </div>
</#if>