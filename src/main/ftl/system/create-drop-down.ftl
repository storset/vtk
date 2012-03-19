<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<@genDropdown true />

<#macro genDropdown preview=false>

<#if preview>
  <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
  <html xmlns="http://www.w3.org/1999/xhtml">
  <head>

    <#if cssURLs?exists>
      <#list cssURLs as cssURL>
        <link rel="stylesheet" href="${cssURL}" type="text/css" />
      </#list>
    </#if>
  
    <!--[if IE 7]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie7.css" type="text/css" /> 
    <![endif]--> 
    <!--[if lte IE 6]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie6.css" type="text/css" /> 
    <![endif]--> 
  
    <style type="text/css">
      html, body {
        background-color: transparent !important;
      }
      body {
        min-width: 0;
        position: relative;
        text-align: left;
        overflow: hidden;
      }
      div.dropdown-shortcut-menu-container {
        left: auto !important;
        right: 0px !important;
        top: 28px !important;
      }
    </style>
  
    <#if jsURLs?exists>
      <#list jsURLs as jsURL>
        <script type="text/javascript" src="${jsURL}"></script>
      </#list>
    </#if>
  
    <script type="text/javascript"><!--
      var hasPostMessage = window['postMessage'] && (!($.browser.opera && $.browser.version < 9.65));
      var vrtxAdminOrigin = "*"; // TODO: TEMP Need real origin of adm
    
      // Notice parent about user actions
      $(document).ready(function () {
        $(".thickbox").click(function() { 
          if (parent) {
            // Pass our height to parent since it is typically cross domain (and can't access it directly)
            if(hasPostMessage) {
              parent.postMessage("fullsize", vrtxAdminOrigin);
            } else { // use the hash stuff in plugin from jQuery "Cowboy"
              var parent_url = decodeURIComponent(document.location.hash.replace(/^#/,''));
              $.postMessage({fullsize: true}, parent_url, parent);  
            }
          }
        });
        
        $.receiveMessage(function(e) {
          var recievedData = e.data;
          
          if(recievedData.indexOf) {
            if(recievedData.indexOf("collapsedsize") != -1) {
              $(".dropdown-shortcut-menu-container:visible").slideUp(100, "swing", function() {
                if (parent) {
                  // Pass our height to parent since it is typically cross domain (and can't access it directly)
                  if(hasPostMessage) {
                    parent.postMessage("collapsedsize", vrtxAdminOrigin);
                  } else { // use the hash stuff in plugin from jQuery "Cowboy"
                    var parent_url = decodeURIComponent(document.location.hash.replace(/^#/,''));
                    $.postMessage({collapsedsize: true}, parent_url, parent); 
                  }
                }
              });
            } else {
              try {
                if(recievedData.replace) {
                  var createDropdownOriginalTop = Number(recievedData.replace(/.*top=(\d+)(?:&|.*$)/, '$1' ));  
                  var createDropdownOriginalLeft = Number(recievedData.replace(/.*left=(\d+)(?:&|$)/, '$1' ));
                }  
                $("ul.manage-create").css({
                    "position": "absolute", 
                    "top": createDropdownOriginalTop + "px",
                    "left": createDropdownOriginalLeft + "px"
                  });
              } catch(e){
                if(typeof console !== "undefined" && console.log) {
                  console.log("Error parsing original position for create-iframe: " + e.message);
                }
              }
            }
          }
        });    
        
      });
    // -->
    </script>
  </head>
  <body>
</#if>

<#if docUrl?exists && collUrl?exists && upUrl?exists>
  <#if docUrl.url?exists && collUrl.url?exists && upUrl.url?exists>
    <#assign docFinalUrl = docUrl.url />
    <#assign collFinalUrl = collUrl.url />
    <#assign upFinalUrl = upUrl.url />  

    <#assign resourceType = resourceContext.currentResource.resourceType />

    <#-- TMP fix for image -- get service from folder instead of resource (you cant create on resource anyway) -->
    <#if resourceType == "image">
      <#assign docFinalUrl = docFinalUrl?substring(0, docFinalUrl?last_index_of("/"))
                           + "/" + docFinalUrl?substring(docFinalUrl?index_of("?"), docFinalUrl?length) />
      <#assign collFinalUrl = collFinalUrl?substring(0, collFinalUrl?last_index_of("/"))
                            + "/" + collFinalUrl?substring(collFinalUrl?index_of("?"), collFinalUrl?length) />
      <#assign upFinalUrl = upFinalUrl?substring(0, upFinalUrl?last_index_of("/"))
                          + "/" + upFinalUrl?substring(upFinalUrl?index_of("?"), upFinalUrl?length) />     
    </#if>

    <ul class="manage-create"> 
      <li class="manage-create-drop first">
        <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.document" default="Choose where you would like to create document" />" href="${docFinalUrl?html}">
          <@vrtx.msg code="manage.document" default="Create document" />
        </a>
      </li>
      <li class="manage-create-drop">
        <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.collection" default="Choose where you would like to create folder" />" href="${collFinalUrl?html}">
          <@vrtx.msg code="manage.collection" default="Create folder" />
        </a>
      </li>
      <li class="manage-create-drop">
        <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.upload-file" default="Choose where you would like to upload file" />" href="${upFinalUrl?html}">
          <@vrtx.msg code="manage.upload-file" default="Upload file" />
        </a>
      </li>
    </ul>
  </#if>
</#if>

<#if preview>
  </body>
  </html>
</#if>

</#macro>