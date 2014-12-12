<#ftl strip_whitespace=true />
<#import "/lib/vtk.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Collection listing</title>
    <#if cssURLs?exists>
      <#list cssURLs as cssUrl>
        <link href="${cssUrl}" type="text/css" rel="stylesheet" />
      </#list>
    </#if>
    <!--[if lte IE 8]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie8.css" type="text/css"/>
    <![endif]--> 
    <!--[if lte IE 7]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie7.css" type="text/css"/> 
    <![endif]-->
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
 *  With async actions a la regular admin
 *
 */

vrtxAdmin._$(document).ready(function () {
  var vrtxAdm = vrtxAdmin;
  
  /* View (frame busting) */
  $(document).on("click", "td.name a", function(e) {
    if (window != top) {
      window.parent.location.href = this.href;
      return false;
    }
  });
  
  /* Edit title action */
  vrtxAdm.getFormAsync({
    selector: ".edit-title-action",
    selectorClass: "propertyForm",
    insertAfterOrReplaceClass: "tr",
    isReplacing: true,
    findClosest: true,
    nodeType: "tr",
    simultanSliding: true,
    funcAfterComplete: updateIframeHeight
  });
  vrtxAdm.completeFormAsync({
    selector: "form#editPropertyService-form input[type=submit]",
    isReplacing: true,
    post: true,
    funcAfterComplete: updateListing
  });
  
  /* Delete action */
  vrtxAdm.getFormAsync({
    selector: ".delete-action",
    selectorClass: "globalmenu",
    insertAfterOrReplaceClass: "tr",
    isReplacing: true,
    findClosest: true,
    nodeType: "tr",
    simultanSliding: true,
    funcAfterComplete: updateIframeHeight
  });
  vrtxAdm.completeFormAsync({
    selector: "form#deleteResourceService-form input[type=submit]",
    isReplacing: true,
    post: true,
    funcAfterComplete: updateListing
  });
  
  /* Upload action */
  if (vrtxAdm.isIOS5) {
    $("#upload-action").hide();
  } else {
    vrtxAdm.getFormAsync({
      selector: "#upload-action",
      selectorClass: "vrtx-admin-form",
      insertAfterOrReplaceClass: "#directory-listing",
      nodeType: "div",
      focusElement: "",
      simultanSliding: true,
      funcBeforeComplete: function() {
        $("#upload-action").hide();
      },
      funcComplete: function (p) {
        vrtxAdm.initFileUpload();
      },
      funcAfterComplete: updateIframeHeight
    });
    vrtxAdm.completeFormAsync({
      selector: "form#fileUploadService-form input[type=submit]",
      errorContainer: "errorContainer",
      errorContainerInsertAfter: "h3",
      post: true,
      funcProceedCondition: function(opts) {
        updateIframeHeight(250);
        return ajaxUpload(opts);
      },
      funcAfterComplete: function() {
        $("#upload-action").show();
        updateListing();
      }
    });
    // Only if received upload parameter
    if(gup("upload", location.href) === "true") {
      $("#upload-action").click();
    } else {
      $(window).load(updateIframeHeight);
    }
  }
});

function updateListing() {
  vrtxAdmin.serverFacade.getHtml(location.href, {
    success: function (results, status, resp) {
      var html = $($.parseHTML(results)).filter("#directory-listing").html();
      vrtxAdmin.cachedBody.find("#directory-listing").html(html);
      updateIframeHeight();
    }
  }); 
}

function updateIframeHeight(minH) {
  if (window != top) {
    var minHeight = (typeof minH === "number") ? minH : 0;
    var parent = $(window.parent.document);
    if(!parent.find("html").hasClass("embedded")) {
      var iframes = parent.find(".session .accordion-content").filter(":visible").find("iframe");
    } else {
      var iframes = parent.find("iframe");
    }
    for (var i = 0, len = iframes.length; i < len; i++) {
      var iframe = iframes[i];
      if(window === iframe.contentWindow) {
        var iframeElm = $(iframe);
        if(iframeElm.filter(":visible").length) {
          try {
            var computedHeight = Math.max(minHeight, Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 15);
            computedHeight = (computedHeight - ($.browser.msie ? 4 : 0));
            iframe.style.height = computedHeight + 'px';
          } catch(ex) {}
        } else {
          var timerUpdateIframeHeight = setTimeout(arguments.callee, 150);
        }
        break;
      }
    }
  }
}
</script>
    
  </head>
  <body class="embedded2 forms-new">
    <table id="directory-listing" class="collection-listing">
      <tbody> 
      <#list entries as entry>
        <#assign url = (entry.actions['view'])?default('') />
        <#assign count = entry_index + 1 />
        <tr class="vrtx-directory-listing-${count} <#if entry_index % 2 == 0>odd<#else>even</#if> <@vrtx.resourceToIconResolver entry.resource /><#if entry.resource.collection> true</#if>">
          <td class="name"><a href="${url?html}">${entry.resource.title}</a></td>
          <td class="action">
          <#list entry.actions?keys as action>
            <#if action != "view">
              <a class="${action}-action" href="${entry.actions[action]?html}<#if action == "edit-title">&amp;default-value=${entry.resource.title?url("UTF-8")}</#if>">${vrtx.getMsg("embeddedListing.${action}")?lower_case}</a>&nbsp;&nbsp;&nbsp;
            </#if>
          </#list>
          </td>
        </tr>
      </#list>
      <tbody> 
    </table>

    <#list globalActions?keys as globalAction>
      <div class="globalaction">
        <a class="vrtx-button" id="upload-action" href="${globalActions[globalAction]?html}">${vrtx.getMsg("embeddedListing.${globalAction}")}</a>
      </div>
    </#list>
  </body>
</html>