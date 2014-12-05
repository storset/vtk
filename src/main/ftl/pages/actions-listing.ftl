<#ftl strip_whitespace=true />
<#import "/lib/vtk.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Collection listing</title>
    <#if jsURLs?exists>
      <#list jsURLs as jsURL>
        <script type="text/javascript" src="${jsURL}"></script>
      </#list>
    </#if>
    <#include "/system/system.ftl" />
    <script type="text/javascript">
    /*
 *  Vortex Admin - Embedded listing
 *
 *  Async actions a la regular admin (but some different "wirering")
 *
 */
 
vrtxAdmin._$(document).ready(function () {
  var vrtxAdm = vrtxAdmin;

  /* Delete action */
  vrtxAdm.getFormAsync({
    selector: ".delete-action",
    selectorClass: "globalmenu",
    insertAfterOrReplaceClass: "#directory-listing",
    nodeType: "div",
    simultanSliding: true
  });
  vrtxAdm.completeFormAsync({
    selector: "form#deleteResourceService-form input[type=submit]",
    post: true,
    funcComplete: updateListing
  });
  
  /* Upload action */
  $("#upload-action").hide();
  if (vrtxAdm.isIOS5) {
  } else {
    vrtxAdm.getFormAsync({
      selector: "#upload-action",
      selectorClass: "vrtx-admin-form",
      insertAfterOrReplaceClass: "#upload-action-container span",
      nodeType: "div",
      focusElement: "",
      funcComplete: function (p) {
        vrtxAdm.initFileUpload();
      },
      simultanSliding: true
    });
    $(document).on("click", ".vrtx-file-upload", function () {
      $("#file").click();
    });
    
    // Auto-trigger Upload when have choosen files
    $(document).on("change", "#file", function () {
      $("form#fileUploadService-form .vrtx-focus-button").click();
    });
    vrtxAdm.completeFormAsync({
      selector: "form#fileUploadService-form .vrtx-focus-button",
      errorContainer: "errorContainer",
      errorContainerInsertAfter: "h3",
      post: true,
      funcProceedCondition: ajaxUpload,
      funcComplete: updateListing
    });
    $("#upload-action").click();
  }
});

function updateListing() {
  vrtxAdmin.serverFacade.getHtml(location.href, {
    success: function (results, status, resp) {
      var html = $($.parseHTML(results)).filter("#directory-listing").html();
      vrtxAdmin.cachedBody.find("#directory-listing").html(html);
      $("#upload-action").click();
    }
  }); 
}
    </script>
    <#if cssURLs?exists>
      <#list cssURLs as cssUrl>
        <link href="${cssUrl}" type="text/css" rel="stylesheet" />
      </#list>
    </#if>
  </head>
  <body class="embedded2 forms-new">
    <table id="directory-listing" class="collection-listing">
      <tbody> 
      <#list entries as entry>
        <#assign url = (entry.actions['view'])?default('') />
        <tr class="<#if entry_index % 2 == 0>odd<#else>even</#if> <@vrtx.resourceToIconResolver entry.resource /><#if entry.resource.collection> true</#if>">
          <td class="name"><a href="${url?html}">${entry.resource.title}</a></td>
          <#list [ "delete" ] as action>
            <#if !(entry.actions[action])?exists>
              <td></td>
            <#else>
              <td><a class="delete-action" href="${entry.actions[action]?html}">${action?html}</a></td>
            </#if>
          </#list>
        </tr>
      </#list>
      <tbody> 
    </table>

    <div id="upload-action-container">
      <span>&nbsp;</span>
    </div>

    <#list globalActions?keys as globalAction>
      <div class="globalaction">
        <a id="upload-action" href="${globalActions[globalAction]?html}">${globalAction?html}</a>
      </div>
    </#list>
  </body>
</html>