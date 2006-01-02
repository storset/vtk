<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#assign htmlTitle = "${error.errorDescription}"/>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Error</title>
</head>
<body>
  <div class="error" class="${error.exception.class.name?replace('.', '-')}">

    <p>
      <@vrtx.msg code="xslt.unableToTransform"
                 default="This document cannot be transformed." />
    </p>
    <p>
      <@vrtx.msg code="xslt.errorMessage" default="Error message" />:
      ${error.exception.message}
    </p>
  </div>
</body>
</html>
