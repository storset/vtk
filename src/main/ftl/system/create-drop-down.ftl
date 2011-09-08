<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" />
    </#list>
  </#if>
  
  <style type="text/css">
    html, body {
      background-color: transparent;    
    }
    body {
      min-width: 0;
      position: relative;
    }
  </style>
  
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  
 <script type="text/javascript"><!--
 
 // Notice parent about user actions
  
 $(document).ready(function () {
  $(".thickbox").click(function() {
    var hasPostMessage = window['postMessage'] && (!($.browser.opera && $.browser.version < 9.65));
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
});

</script>
</head>
<body>

<#assign docUrl = docUrl.url />
<#assign collUrl = collUrl.url />
<#assign upUrl = upUrl.url />

<ul class="manage-create"> 
  <li class="manage-create-drop first">
    <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.document" default="Choose where you would like to create document" />" href="${docUrl?html}">
      <@vrtx.msg code="manage.document" default="Create document" />
    </a>
  </li>
  <li class="manage-create-drop">
    <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.collection" default="Choose where you would like to create folder" />" href="${collUrl?html}">
      <@vrtx.msg code="manage.collection" default="Create folder" />
    </a>
  </li>
  <li class="manage-create-drop">
    <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.upload-file" default="Choose where you would like to upload file" />" href="${upUrl?html}">
      <@vrtx.msg code="manage.upload-file" default="Upload file" />
    </a>
  </li>
</ul>

</body>
</html>