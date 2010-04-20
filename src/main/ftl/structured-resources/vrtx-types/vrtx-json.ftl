<#macro printPropertyEditView title inputFieldName elem tooltip="" id="" inputFieldSize=20>
<div class="vrtx-json">
  <div id="${id}" class="fieldset">
  <div class="header">${title}</div>
    <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
    <#local counter = 0 />
    <#local locale = springMacroRequestContext.getLocale() />
    
    <#-- Json property that has no value. We need to crate empty fields. -->
    <#if !elem.value?exists > 
      <div class="vrtx-json-element" id="vrtx-json-element-${inputFieldName}-${counter}">
        <#list elem.description.attributes as jsonAttr>
          <#assign attrName = jsonAttr.name />
          <#assign tmpName = inputFieldName + "." + attrName + "." + counter />
          <#assign jsonAttrLocalizedTitle = form.resource.getLocalizedMsg(attrName, locale, null) />
          <@printJsonProperyEditView jsonAttr.type jsonAttrLocalizedTitle tmpName "" elem attrName />                              
        </#list>
        <input type="button" class="vrtx-remove-button" value="${vrtx.getMsg("editor.remove")}" onClick="$('#vrtx-json-element-${inputFieldName}-${counter}').remove()" />
      </div>
    </#if>

    <#-- There is a value and the property is a list/multiple property. -->
    <#if elem.value?exists && elem.value?is_enumerable>
      <#list elem.value as map>
        <div class="vrtx-json-element" id="vrtx-json-element-${inputFieldName}-${counter}">
        <#list elem.description.attributes as jsonAttr>
          <#assign attrName = jsonAttr.name />
          <#assign tmpName = inputFieldName + "." + attrName + "." + counter />
          <#assign jsonAttrLocalizedTitle = form.resource.getLocalizedMsg(attrName, locale, null) />
          <#if map[jsonAttr]?exists >
            <@printJsonProperyEditView jsonAttr.type jsonAttrLocalizedTitle tmpName map[jsonAttr] elem jsonAttr />          
          <#else>
            <@printJsonProperyEditView jsonAttr.type jsonAttrLocalizedTitle tmpName "" elem jsonAttr />    
          </#if>
        </#list>
        <#assign arrayOfIds = "new Array(" />
        <#list elem.description.attributes as jsonAttr>
          <#assign tmpName = inputFieldName + '\\\\.' + jsonAttr + '\\\\.' />
          <#assign arrayOfIds = arrayOfIds + "'" + tmpName + "'" />
          <#if jsonAttr_has_next>
            <#assign arrayOfIds = arrayOfIds + "," />
          </#if>
        </#list>
        <#assign arrayOfIds = arrayOfIds + ")" />
        <input type="button" class="vrtx-remove-button" value="${vrtx.getMsg("editor.remove")}" onClick="$('#vrtx-json-element-${inputFieldName}-${counter}').remove()" />
        <#if (counter > 0) >
          <input type="button" class="vrtx-move-up-button" onClick="swapContent(${counter},${arrayOfIds},-1)"  value="&uarr; ${vrtx.getMsg("editor.move-up")}"  />	
        </#if>
        <#if map_has_next >
          <input type="button"  class="vrtx-move-down-button" onClick="swapContent(${counter},${arrayOfIds},1)"  value="&darr; ${vrtx.getMsg("editor.move-down")}"  />
        </#if>
        </div>
        <#local counter = counter + 1 />
      </#list>
    </#if>

    <#-- There is at least one value and the property is not a list/multiple -->
    <#if elem.value?exists && elem.value?is_hash >
      <#list elem.description.attributes as key>
        <#assign tmpName = inputFieldName + "." + key + "." + counter />
        <#if elem.value[key]?exists >
          <@printJsonProperyEditView elem.description.getType(key) jsonAttrLocalizedTitle tmpName elem.value[key] elem key />  
        <#else>
          <@printJsonProperyEditView elem.description.getType(key) jsonAttrLocalizedTitle tmpName "" elem key />  
        </#if>
      </#list>
    </#if>
  </div>
</div>
</#macro>

<#-- TODO: Duplicates some functionality in editor.ftl. Clean up later -->
<#macro printJsonProperyEditView type jsonAttr tmpName value elem key>
	<#switch type >
	 <#case "string">
        <#assign fieldSize="40" />
        <#if elem.description.edithints?exists && elem.description.edithints['size']?exists >
          <#if elem.description.edithints['size'] == "large" >
            <#assign fieldSize="60" />
          <#elseif elem.description.edithints['size'] == "small" >
            <#assign fieldSize="20"/>
          <#else>
            <#assign fieldSize=elem.description.edithints['size'] />
          </#if>
        </#if>
       <@vrtxString.printPropertyEditView 
         title=jsonAttr 
         inputFieldName=tmpName
         value=value
         classes=key
         inputFieldSize=fieldSize />
        <#break>
      <#case "simple_html">
        <#assign cssclass =  tmpName + " vrtx-simple-html" />
        <@vrtxHtml.printPropertyEditView 
          title=jsonAttr 
          inputFieldName=tmpName
          value=value
          editor="simple-ckeditor"
          classes=cssclass />
        <@fckEditor.insertEditor tmpName />
        <#break>
      <#case "html">
        <#if elem.description.edithints?exists>
           <#list elem.description.edithints?keys as hint>
             ${hint} <br />
           </#list>
       </#if>
        <@vrtxHtml.printPropertyEditView 
          title=jsonAttr 
          inputFieldName=tmpName
          value=value
          editor="ckeditor"
          classes="vrtx-html " + tmpName />
       
        <@fckEditor.insertEditor tmpName true false />
        <#break>
 	  <#case "boolean">
        <@vrtxBoolean.printPropertyEditView 
          title=jsonAttr
          inputFieldName=tmpName
          value=value
          classes=""  />
        <#break>
      <#case "image_ref">
        <@vrtxImageRef.printPropertyEditView 
          title=jsonAttr
          inputFieldName=tmpName
          value=value 
          baseFolder=resourceContext.parentURI
          classes="" />
        <#break>          
      <#case "media_ref">
        <@vrtxMediaRef.printPropertyEditView 
          title=jsonAttr
          inputFieldName=tmpName
          value=value
          baseFolder=resourceContext.parentURI
          classes=""  />
        <#break>
      <#case "datetime">
       <@vrtxDateTime.printPropertyEditView 
        title=jsonAttr
        inputFieldName=tmpName
        value=value
        classes=""  />
        <#break>   
      </#switch>
</#macro>
