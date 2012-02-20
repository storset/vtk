<#ftl strip_whitespace=true>
<#import "vrtx-boolean.ftl" as vrtxBoolean />
<#import "vrtx-datetime.ftl" as vrtxDateTime />
<#import "vrtx-file-ref.ftl" as vrtxFileRef />
<#import "vrtx-html.ftl" as vrtxHtml />
<#import "vrtx-image-ref.ftl" as vrtxImageRef />
<#import "vrtx-media-ref.ftl" as vrtxMediaRef />
<#import "vrtx-radio.ftl" as vrtxRadio />
<#import "vrtx-string.ftl" as vrtxString />
<#import "vrtx-resource-ref.ftl" as vrtxResourceRef />

<#macro printPropertyEditView form elem locale>

  <#assign localizedTitle = form.resource.getLocalizedMsg(elem.name, locale, null) />

   

  <#switch elem.description.type>

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
      <#assign dropdown = false />
      <#if elem.description.edithints?exists && elem.description.edithints['dropdown']?exists >
        <#assign dropdown = true />
      </#if>
      <#if elem.description.edithints?exists && elem.description.edithints['class']?exists >
            <#assign classes= elem.description.edithints['class'] + ' ' + elem.name />
      <#else>
            <#assign classes = elem.name />
      </#if>
      <@vrtxString.printPropertyEditView
        title=localizedTitle
        inputFieldName=elem.name
        value=elem.getFormatedValue()
        classes=classes
        inputFieldSize=fieldSize
        tooltip=form.resource.getLocalizedTooltip(elem.name, locale)
        valuemap=elem.description.getValuemap(locale)
        dropdown=dropdown
        defaultValue=	elem.getDefaultValue()
      />
      <#break>

    <#case "simple_html">
      <#assign cssclass =  elem.name + " vrtx-simple-html" />
      <#if elem.description.edithints?exists && elem.description.edithints['size']?exists >
        <#assign cssclass = cssclass + "-" + elem.description.edithints['size'] />
      </#if>

      <@vrtxHtml.printPropertyEditView
        title=localizedTitle
        inputFieldName=elem.name
        value=elem.value
        classes=cssclass
        tooltip=form.resource.getLocalizedTooltip(elem.name, locale)
        editor=""
      />
      <@editor.createEditor elem.name />
      <#break>

    <#case "html">
      <#if elem.description.edithints?exists>
        <#list elem.description.edithints?keys as hint>
          ${hint} <br />
        </#list>
      </#if>

      <@vrtxHtml.printPropertyEditView
        title=localizedTitle
        inputFieldName=elem.name
        value=elem.value
        classes="vrtx-html " + elem.name
        tooltip=form.resource.getLocalizedTooltip(elem.name, locale)
        editor=""
      />
      <@editor.createEditor elem.name true false />
      <#break>

    <#case "boolean">
      <@vrtxBoolean.printPropertyEditView
        title=localizedTitle
        inputFieldName=elem.name
        value=elem.value
        classes=elem.name
        description=elem.description
        tooltip=form.resource.getLocalizedTooltip(elem.name, locale)
        defaultValue=elem.getDefaultValue()
      />
      <#break>

    <#case "image_ref">
      <#if elem.value?exists>
        <#local thumbnail = vrtx.relativeLinkConstructor(elem.value, 'displayThumbnailService') />
      <#else>
        <#local thumbnail = "" />
      </#if>
      
      <@vrtxImageRef.printPropertyEditView
        title=localizedTitle
        inputFieldName=elem.name
        value=thumbnail
        name=elem.value
        baseFolder=resourceContext.parentURI
        classes=elem.name
        tooltip=form.resource.getLocalizedTooltip(elem.name, locale)
      />
      <#break>

    <#case "resource_ref">
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

      <@vrtxResourceRef.printPropertyEditView
        title=localizedTitle
        inputFieldName=elem.name
        value=elem.getFormatedValue()
        name=elem.value
        baseFolder=resourceContext.parentURI
        classes=elem.name
        tooltip=form.resource.getLocalizedTooltip(elem.name, locale)
        inputFieldSize=fieldSize
      />
      <#break>

    <#case "media_ref">
      <@vrtxMediaRef.printPropertyEditView
        title=localizedTitle
        inputFieldName=elem.name
        value=elem.value
        baseFolder=resourceContext.parentURI
        classes=elem.name
        tooltip=form.resource.getLocalizedTooltip(elem.name, locale)
      />
      <#break>

    <#case "datetime">
      <@vrtxDateTime.printPropertyEditView
        title=localizedTitle
        inputFieldName=elem.name
        value=elem.value
        classes=elem.name
        tooltip=form.resource.getLocalizedTooltip(elem.name, locale)
      />
      <#break>

    <#case "json">
      <@printJSONPropertyEditView
        localizedTitle
        elem.name
        elem
        elem.name
        ""
        locale
        form.resource.getLocalizedTooltip(elem.name, locale)
      />
      <#break>

     <#default>
        No editor available for element type ${elem.description.type}

  </#switch>

</#macro>

<#macro printJSONPropertyEditView title inputFieldName elem id tooltip locale inputFieldSize=20>

  <div class="vrtx-json">
    <div id="${id}" class="fieldset">
      <div class="header">${title}</div>
    
      <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>

      <#local counter = 0 />
      <#local locale = springMacroRequestContext.getLocale() />
    
      <#-- JSON-property that has no value - we need to create empty fields -->

      <#if !elem.value?exists >
    	  <#assign arrayOfIds = "new Array(" />
          <div class="vrtx-json-element" id="vrtx-json-element-${inputFieldName}-${counter}">
            <#list elem.description.attributes as jsonAttr>
              <#assign attrName = jsonAttr.name />
              <#assign tmpName = inputFieldName + "." + attrName + "." + counter />
              <#assign jsonAttrLocalizedTitle = form.resource.getLocalizedMsg(attrName, locale, null) />

              <@printJsonProperyEditView jsonAttr.type jsonAttrLocalizedTitle tmpName "" elem attrName jsonAttr locale />

              <#assign arrayAttrName = inputFieldName + '\\\\.' + attrName + '\\\\.' />
              <#assign arrayOfIds = arrayOfIds + "'" + arrayAttrName + "'" />

              <#if jsonAttr_has_next>
                <#assign arrayOfIds = arrayOfIds + "," />
              </#if>

            </#list>

            <#assign arrayOfIds = arrayOfIds + ")" />
            <input type="hidden" class="id" value="${counter}" />

            <div class="vrtx-button vrtx-remove-button">
              <input type="button" value="${vrtx.getMsg("editor.remove")}" />
            </div>

            <script type="text/javascript"><!--
       	      $("#vrtx-json-element-${inputFieldName}-${counter}").find(".vrtx-remove-button").click(function(){
	            removeNode("${inputFieldName}", ${counter} ,  ${arrayOfIds} );
              });
            // -->
       	    </script>
          </div>
        </#if>

        <#-- JSON-property that has a value and is a list / multiple property -->

       <#if elem.value?exists && elem.value?is_enumerable>
         <#list elem.value as map>
           <div class="vrtx-json-element" id="vrtx-json-element-${inputFieldName}-${counter}">
             <#assign arrayOfIds = "new Array(" />
             <#list elem.description.attributes as jsonAttr>
               <#assign attrName = jsonAttr.name />
               <#assign tmpName = inputFieldName + "." + attrName + "." + counter />
               <#assign jsonAttrLocalizedTitle = form.resource.getLocalizedMsg(attrName, locale, null) />

               <#if map[attrName]?exists >
                 <@printJsonProperyEditView jsonAttr.type jsonAttrLocalizedTitle tmpName map[attrName] elem attrName jsonAttr locale />
               <#else>
                 <@printJsonProperyEditView jsonAttr.type jsonAttrLocalizedTitle tmpName "" elem attrName jsonAttr locale />
               </#if>

               <#assign arrayAttrName = inputFieldName + '\\\\.' + attrName + '\\\\.' />
               <#assign arrayOfIds = arrayOfIds + "'" + arrayAttrName + "'" />

               <#if jsonAttr_has_next>
                 <#assign arrayOfIds = arrayOfIds + "," />
               </#if>

             </#list>

             <#assign arrayOfIds = arrayOfIds + ")" />
       	     <input type="hidden" class="id" value="${counter}" />

       	     <div class="vrtx-button vrtx-remove-button">
               <input type="button" value="${vrtx.getMsg("editor.remove")}" />
             </div>

       	     <script type="text/javascript"><!--
       	       $("#vrtx-json-element-${inputFieldName}-${counter}").find(".vrtx-remove-button").click(function(){
                 removeNode("${inputFieldName}", ${counter},  ${arrayOfIds});
               });
    	     // -->
       	     </script>

             <#if (counter > 0) >
               <div class="vrtx-button vrtx-move-up-button">
                 <input type="button" value="&uarr; ${vrtx.getMsg("editor.move-up")}"  />
               </div>

               <script type="text/javascript"><!--
                 $("#vrtx-json-element-${inputFieldName}-${counter}").find(".vrtx-move-up-button").click(function(){
     		       swapContent(${counter}, ${arrayOfIds}, -1, "${inputFieldName}");
                 });
               // -->
               </script>
             </#if>

             <#if map_has_next >
               <div class="vrtx-button vrtx-move-down-button">
                 <input type="button" value="&darr; ${vrtx.getMsg("editor.move-down")}"  />
               </div>

               <script type="text/javascript"><!--
          	     $("#vrtx-json-element-${inputFieldName}-${counter}").find(".vrtx-move-down-button").click(function(){
	     	       swapContent(${counter}, ${arrayOfIds}, 1, "${inputFieldName}");
	     	     });
	     	   // -->
               </script>
             </#if>

           </div>
           <#local counter = counter + 1 />
         </#list>
       </#if>

       <#-- JSON-property has at least one value and is not a list / multiple -->

       <#if elem.value?exists && elem.value?is_hash >
         <#list elem.description.attributes as jsonAttr>
           <#assign attrName = jsonAttr.name />
           <#assign tmpName = inputFieldName + "." + attrName + "." + counter />
           <#if elem.value[attrName]?exists >
             <@printJsonProperyEditView jsonAttr.type jsonAttrLocalizedTitle tmpName elem.value[attrName] elem attrName jsonAttr locale />
           <#else>
             <@printJsonProperyEditView jsonAttr.type jsonAttrLocalizedTitle tmpName "" elem attrName jsonAttr locale />
           </#if>
         </#list>
       </#if>

    </div>
  </div>

</#macro>

<#macro printJsonProperyEditView type jsonAttr tmpName value elem key json locale >

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
      <#assign dropdown = false />

      <#if json.edithints?exists && json.edithints['dropdown']?exists >
        <#assign dropdown = true />
      </#if>

      <@vrtxString.printPropertyEditView
        title=jsonAttr
        inputFieldName=tmpName
        value=value
        classes=key
        inputFieldSize=fieldSize
        tooltip=""
        valuemap=json.getValuemap(locale)
        dropdown=dropdown />
      <#break>

    <#case "simple_html">
      <#assign cssclass =  tmpName + " vrtx-simple-html" />

      <@vrtxHtml.printPropertyEditView
        title=jsonAttr
        inputFieldName=tmpName
        value=value
        editor=""
        classes=cssclass />

      <@editor.createEditor tmpName />
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
        editor=""
        classes="vrtx-html " + tmpName />

      <@editor.createEditor tmpName true false />
      <#break>

    <#case "boolean">
      <@vrtxBoolean.printPropertyEditView
        title=jsonAttr
        inputFieldName=tmpName
        value=value
        classes=""  />
      <#break>

   <#case "image_ref">
     <#if value?has_content>
       <#local thumbnail =  vrtx.relativeLinkConstructor(value, 'displayThumbnailService') />
     <#else>
       <#local thumbnail = "" />
     </#if>

     <@vrtxImageRef.printPropertyEditView
       title=jsonAttr
       inputFieldName=tmpName
       value=thumbnail
       name=value
       baseFolder=resourceContext.parentURI
       classes="" />
     <#break>

   <#case "resource_ref">
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

    <@vrtxResourceRef.printPropertyEditView 
      title=jsonAttr
      inputFieldName=tmpName
      value=value
      name=value 
      baseFolder=resourceContext.parentURI
      classes=""
      inputFieldSize=fieldSize />
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
