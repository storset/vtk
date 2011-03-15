<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/jquery-1.5.1.min.js"></script> 
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/iframe-view-not-another-iframe.js"></script> 
</head>
<body>

<h1>${title}</h1>

<#if description?exists >
  <div id="vrtx-meta-description">
    ${description}
  </div>
</#if>

<#if src?exists> 
  <img src="${src}" />
</#if>

</body>
</html>