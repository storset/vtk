<#ftl strip_whitespace=true>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<#assign htmlTitle = "${error.errorDescription}"/>

  <title>304 - not modified</title>
</head>
<body>

  <div class="error ${error.exception.class.name?replace('.', '-')}">
    <#include "/lib/error-detail.ftl" />
  </div>

<p>Server-administrator: <a href="mailto:${webmaster}">${webmaster}</a></p>

</body>
</html>
