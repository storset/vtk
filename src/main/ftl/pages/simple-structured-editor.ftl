<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/editor/common.ftl" as editor />

<!DOCTYPE html>
<html>
<head>
  <title>Simple structured resource editor</title>
  <script type="text/javascript">
    var cssFileList = "";
  </script>
      <#global baseFolder = "/" />
    <#if resourceContext.parentURI?exists>
      <#if isCollection?exists && isCollection>
        <#global baseFolder = resourceContext.currentURI?html />
      <#else>
        <#global baseFolder = resourceContext.parentURI?html />
      </#if>
     </#if>
     <#if jsURLs?exists>
      <#list jsURLs as jsURL>
        <script type="text/javascript" src="${jsURL}"></script>
      </#list>
    </#if>
  <@editor.addCkScripts />
  <@editor.createEditor 'message' true false />
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/forms.css" type="text/css" />
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default.css" type="text/css" />
</head>
<body>
    <div id="app-content">
<#if url?exists>
<form method="POST" style="float:left;margin-top:10px">
    <@vrtx.csrfPreventionToken url />
        <div class="property-label">
            ${vrtx.getMsg("property.title")}
        </div> 
        <div class="vrtx-textfield">
            <input type="text" name="title" id="title"<#if properties?exists && properties.title?exists> value="${properties.title?html}"</#if>/>
        </div>
            <div class="property-label">
                ${vrtx.getMsg("resourcetype.name.structured-message")}
            </div> 
        <textarea  id="message"  name="message"><#if properties?exists && properties.message?exists>${properties.message?html}</#if></textarea>
        <div class="vrtx-button" style="float:left;margin-right:10px;margin-top:10px">   
            <button type="submit" id="submit" name="submit" value="create" >${vrtx.getMsg("editor.save")}</button>
        </div> 
    
</form>  
<form method="POST" style="float:left;margin-top:10px">
    <@vrtx.csrfPreventionToken url />
    <div class="vrtx-button" style="float:left;margin-right:10px;margin-top:10px">     
        <button type="submit" id="cancel" name="cancel" value="cancel" >${vrtx.getMsg("editor.cancel")}</button>
    </div>
</form>
<#if !isCollection>
<form method="POST" style="float:left;margin-top:10px">        
     <@vrtx.csrfPreventionToken url />
     <input name="${url.path}" value="${url.path}" type="hidden" />
<div class="vrtx-button" style="float:left;margin-top:10px">     
     <button type="submit" name="delete" >${vrtx.getMsg("tabMenuRight.deleteResourcesService")}</button>
</div>
</form>
</#if>
</#if>
</div>
</body>
</html>