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
      <link rel="stylesheet" href="${cssURL}" />
      </#list>
    </#if>
  
    <!--[if IE 7]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie7.css" type="text/css"/> 
    <![endif]--> 
    <!--[if lte IE 6]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie6.css" type="text/css"/> 
    <![endif]--> 
  
    <style type="text/css">
      html, body {
        background-color: transparent !important;
      }
      body {
        min-width: 0;
        position: relative;
        text-align: left;
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
    
      // Notice parent about user actions
      $(document).ready(function () {
        $(".thickbox").click(function() {
          var vrtxAdminOrigin = "*"; // TODO: TEMP Need real origin of adm
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
          if(recievedData.replace) {
            var createDropdownOriginalTop = Number(recievedData.replace(/.*top=(\d+)(?:&|.*$)/, '$1' ));  
            var createDropdownOriginalLeft = Number(recievedData.replace(/.*left=(\d+)(?:&|$)/, '$1' ));
          }  
          $("ul.manage-create").css({
              "position": "absolute", 
              "top": createDropdownOriginalTop + "px",
              "left": createDropdownOriginalLeft + "px"
            });
        });    
        
        if(!hasPostMessage) {
          $("ul.manage-create").hide(0);
        }
        
      });
    // -->
    </script>
  </head>
  <body>
</#if>

<#if docUrl?exists && collUrl?exists && upUrl?exists>
  <ul class="manage-create"> 
    <li class="manage-create-drop first">
      <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.document" default="Choose where you would like to create document" />" href="${docUrl.url?html}">
        <@vrtx.msg code="manage.document" default="Create document" />
      </a>
    </li>
    <li class="manage-create-drop">
      <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.collection" default="Choose where you would like to create folder" />" href="${collUrl.url?html}">
        <@vrtx.msg code="manage.collection" default="Create folder" />
      </a>
    </li>
    <li class="manage-create-drop">
      <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.upload-file" default="Choose where you would like to upload file" />" href="${upUrl.url?html}">
        <@vrtx.msg code="manage.upload-file" default="Upload file" />
      </a>
    </li>
  </ul>
</#if>

<#if preview>
  </body>
  </html>
</#if>

</#macro>