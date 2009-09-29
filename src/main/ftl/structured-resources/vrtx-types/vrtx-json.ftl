
<#macro printPropertyEditView title inputFieldName elem tooltip="" id="" inputFieldSize=20>

<div class="vrtx-json">
  <div id="${id}" class="fieldset">
  <div class="header">${title}</div>
    <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
    <#local counter = 0 />
    <#local locale = springMacroRequestContext.getLocale() />

    <#if !elem.value?exists>
	  <div class="vrtx-json-element">
      <#list elem.description.attributes as jsonAttr>
		<#assign tmpName = inputFieldName + "." + jsonAttr + "." + counter />
		<#assign jsonAttrLocalizedTitle = form.resource.getLocalizedMsg(jsonAttr, locale, null) />
		<@printJsonProperyEditView elem.description.getType(jsonAttr) jsonAttrLocalizedTitle tmpName "" elem />                              
      </#list>      
  	  </div>
    <#elseif !elem.valueIsList() >
    	<div class="vrtx-json-element">
        <#list elem.value?keys as jsonAttr >	
        	   <#assign tmpName = inputFieldName + "." + jsonAttr + "." + counter />
        	   <#assign jsonAttrLocalizedTitle = form.resource.getLocalizedMsg(jsonAttr, locale, null) />
               <@printJsonProperyEditView elem.description.getType(jsonAttr) jsonAttrLocalizedTitle tmpName elem.value[jsonAttr] elem /> 
        </#list>
      	</div>    
    <#else>
      	<#list elem.value as map>
      	  <div class="vrtx-json-element" id="vrtx-json-element-${inputFieldName}-${counter}">
        	<#list elem.description.attributes as jsonAttr>
	  		<#assign tmpName = inputFieldName + "." + jsonAttr + "." + counter />
	  		  <#assign jsonAttrLocalizedTitle = form.resource.getLocalizedMsg(jsonAttr, locale, null) />
	          <#if map[jsonAttr]?exists >
				<@printJsonProperyEditView elem.description.getType(jsonAttr) jsonAttrLocalizedTitle tmpName map[jsonAttr] elem />          
	          <#else>
	          	<@printJsonProperyEditView elem.description.getType(jsonAttr) jsonAttrLocalizedTitle tmpName "" elem />    
	          </#if>
	        </#list>
      	  	<input type="button" class="vrtx-remove-button" value="${vrtx.getMsg("editor.remove")}" onClick="$('#vrtx-json-element-${inputFieldName}-${counter}').remove()" />
        </div>
        <#local counter = counter + 1 />
      </#list>
      </#if>

</div>
</div>
</#macro>

<#-- TODO: Duplicates some functionality in editor.ftl. Clean up later -->
<#macro printJsonProperyEditView type jsonAttr tmpName value elem >
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
         classes=""
         inputFieldSize=fieldSize />
        <#break>
      <#case "simple_html">
        <#assign cssclass =  tmpName + " vrtx-simple-html" />
        <@vrtxHtml.printPropertyEditView 
          title=jsonAttr 
          inputFieldName=tmpName
          value=value
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
