<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/editor/common.ftl" as editor />

<!DOCTYPE html>
<html>
<head>
  <title>Simple structured resource editor</title>
  <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default.css" type="text/css" />
  <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/editor.css" type="text/css" />
  <style type="text/css">
    body {
      background: #fff;
    }
    #app-content {
      width: 620px;
      padding: 0 0 48px 0;
      margin: 0 25px;
      position: relative;
    }
    .vrtx-focus-button,
    #vrtx-message-cancel,
    #vrtx-message-delete {
      position: absolute;
      bottom: 0px;  
    }
    .vrtx-focus-button {
      left: 0px;
    }
    #vrtx-message-cancel {
      left: 75px;
    }
    #vrtx-message-delete {
      left: 175px;
    }
    #property-item-first {
      margin-top: 0px;
    }
  </style>
  <script type="text/javascript"><!--
    // Div container display in IE
    var cssFileList = [<#if fckEditorAreaCSSURL?exists>
                         <#list fckEditorAreaCSSURL as cssURL>
                           "${cssURL?html}" <#if cssURL_has_next>,</#if>
                         </#list>
                       </#if>]; 
     
    if (vrtxAdmin.isIE && vrtxAdmin.browserVersion <= 7) {
      cssFileList.push("/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css");
    }
  // -->
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
</head>
<body>
<div id="app-content">
<#if url?exists>
  <form method="POST" style="float:left;margin-top:10px">
    <@vrtx.csrfPreventionToken url />
    <div class="property-item" id="property-item-first">
      <div class="property-label">
        ${vrtx.getMsg("property.title")}
      </div> 
      <div class="vrtx-textfield">
        <input type="text" name="title" id="title"<#if properties?exists && properties.title?exists> value="${properties.title?html}"</#if>/>
      </div>
    </div>
    <div class="property-item">
      <div class="property-label">
        ${vrtx.getMsg("resourcetype.name.structured-message")}
      </div> 
      <textarea  id="message"  name="message"><#if properties?exists && properties.message?exists>${properties.message?html}</#if></textarea>
    </div>
    <div class="vrtx-focus-button">   
      <button type="submit" id="submit" name="submit" value="create" >${vrtx.getMsg("editor.save")}</button>
    </div> 
  </form>  
  <form method="POST" id="vrtx-message-cancel">
    <@vrtx.csrfPreventionToken url />
    <div class="vrtx-button">     
        <button type="submit" id="cancel" name="cancel" value="cancel" >${vrtx.getMsg("editor.cancel")}</button>
    </div>
  </form>
  <#if !isCollection>
    <form method="POST" id="vrtx-message-delete">        
      <@vrtx.csrfPreventionToken url />
      <input name="${url.path}" value="${url.path}" type="hidden" />
      <div class="vrtx-button">     
        <button type="submit" name="delete" >${vrtx.getMsg("tabMenuRight.deleteResourcesService")}</button>
      </div>
    </form>
  </#if>
</#if>
</div>
</body>
</html>