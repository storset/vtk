<#macro printPropertyEditView title inputFieldName elem tooltip="" id="" inputFieldSize=20>
<div class="vrtx-json" id="${id}">
  <h2 class="vrtx-title">${title}</h2>
    <#local counter = 0 />

    <#if !elem.value?exists || !elem.valueIsList() >
    	  <div class="vrtx-json-element">
    	<#if !elem.valueIsList() >
    		blash!
    	</#if>
      <#list elem.description.attributes as jsonAttr>
        ${jsonAttr} : 
        <input type="text" name="${inputFieldName}.${jsonAttr}.${counter}"
               id="${inputFieldName}.${jsonAttr}.${counter}" />
      </#list>      
      	</div>
    <#else>
    	
      	<#list elem.value as map>
      	  <div class="vrtx-json-element" id="vrtx-json-element-${counter}">
      	  	<input type="button" class="vrtx-remove-button" value="Slett" onClick="$('#vrtx-json-element-${counter}').remove()" />
        <#list elem.description.attributes as jsonAttr>
	  		<#assign tmpName = inputFieldName + "." + jsonAttr + "." + counter />  
	          <#if map[jsonAttr]?exists >
				<@jizz elem.description.getType(jsonAttr) jsonAttr tmpName map[jsonAttr] elem />          
	          <#else>
	          	<@jizz elem.description.getType(jsonAttr) jsonAttr tmpName "" elem />    
	          </#if>
	        </#list>
        
        </div>
        <#local counter = counter + 1 />
      </#list>
      </#if>
  <div class="tooltip">${tooltip}</div>
</div>
</#macro>
<#macro jizz type jsonAttr tmpName value elem >
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
