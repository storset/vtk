<#ftl strip_whitespace=true>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<#assign htmlTitle = "${error.errorDescription}"/>

  <title>Error</title>
</head>
<body>

  <div class="error ${error.exception.class.name?replace('.', '-')}">
    <#include "/lib/error-detail.ftl" />
  </div>

</body>
</html>
