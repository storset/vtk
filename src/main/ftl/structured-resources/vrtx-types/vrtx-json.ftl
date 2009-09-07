<#macro printPropertyEditView title inputFieldName elem tooltip="" classes="" inputFieldSize=20>
<div class="vrtx-json ${classes}">
  <label for="${inputFieldName}">${title}</label>
  <div>

    <#local counter = 0 />

    <#if !elem.value?exists>
      <#list elem.description.attributes as jsonAttr>
        ${jsonAttr} : 
        <input type="text" name="${inputFieldName}.${jsonAttr}.${counter}"
               id="${inputFieldName}.${jsonAttr}.${counter}" />
      </#list>      
    <#else>
      <#list elem.value as map>
        <#list elem.description.attributes as jsonAttr>
          ${jsonAttr} : 
          <#if map[jsonAttr]?exists>

            <#-- XXX: check for different input types (all text for now): -->

            <input type="text" name="${inputFieldName}.${jsonAttr}.${counter}"
                   id="${inputFieldName}.${jsonAttr}.${counter}" value="${map[jsonAttr]?html}" />
          <#else>
            <input type="text" name="${inputFieldName}.${jsonAttr}.${counter}"
                   id="${inputFieldName}.${jsonAttr}.${counter}" />
          </#if>
        </#list>
        <#local counter = counter + 1 />
      </#list>
    </#if>
    <#-- XXX: add element below -->
    <input type="button" value="Add Element" />
  </div>
  <div class="tooltip">${tooltip}</div>
</div>
</#macro>
