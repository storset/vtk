<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/editor/common.ftl" as editor />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Simple structured resource editor</title>
  <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default.css" type="text/css" />
  <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/editor.css" type="text/css" />
  <style type="text/css">
    html {
      background: #999999;
    }
  </style>
  <script type="text/javascript"><!--
    // Div container display in IE
    var cssFileList = [<#if fckEditorAreaCSSURL?exists>
                         <#list fckEditorAreaCSSURL as cssURL>
                           "${cssURL?html}" <#if cssURL_has_next>,</#if>
                         </#list>
                       </#if>]; 
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
  <@editor.createEditor 'message' false false />
  <script type="text/javascript"><!--
    $(function() {
      $("#app-content").css("marginTop", (($(window).outerHeight() / 2) - $("#app-content").outerHeight()) + "px")
                       .on("click", ".vrtx-back a, #vrtx-close-simple-structured-editor", function(e) {
        $("#cancel").click();
        e.preventDefault();
      }); 
    });  
  // -->
  </script>
</head>
<body id="vrtx-simple-editor">
<div id="app-content">
  <div class="vrtx-back">
    <a href="javascript:void(0)">${vrtx.getMsg("report.back.to.diagram")}</a>
    <a href="javascript:void(0)" id="vrtx-close-simple-structured-editor"></a>
  </div>
  <#if url?exists>
    <form  action="" method="post">
      <@vrtx.csrfPreventionToken url />
      <div id="vrtx-resource.userTitle" class="userTitle property-item">
        <div class="property-label">
          ${vrtx.getMsg("property.title")}
        </div> 
        <div class="vrtx-textfield">
          <input type="text" name="title" id="title"<#if properties?exists && properties.title?exists> value="${properties.title?html}"</#if>/>
        </div>
      </div>
      <div id="vrtx-message" class="property-item">
        <div class="property-label">
          ${vrtx.getMsg("resourcetype.name.structured-message")}
        </div> 
        <textarea id="message" name="message"><#if properties?exists && properties.message?exists>${properties.message?html}</#if></textarea>
      </div>
      <div class="vrtx-focus-button">   
        <input type="submit" id="submit" name="submit" value="${vrtx.getMsg("editor.save")}" />
      </div> 
    </form>  
    <form action="" method="post" id="vrtx-message-cancel">
      <@vrtx.csrfPreventionToken url />
      <div class="vrtx-button">     
        <input type="submit" id="cancel" name="cancel" value="${vrtx.getMsg("editor.cancel")}" />
      </div>
    </form>
    <#if !isCollection>
      <form  action="" method="post" id="vrtx-message-delete">        
        <@vrtx.csrfPreventionToken url />
        <input name="${url.path}" value="${url.path}" type="hidden" />
        <div class="vrtx-button">     
          <input type="submit" name="delete" value="${vrtx.getMsg("tabMenuRight.deleteResourcesService")}" />
        </div>
      </form>
    </#if>
  </#if>
</div>
</body>
</html>