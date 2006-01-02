<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Error</title>
</head>
<body>

  <div class="error ${error.exception.class.name?replace('.', '-')}">

    <p class="xmlEditUnavailable"><@vrtx.msg code="xmledit.unavailable" default="The XML document cannot be edited"/></p>

    <p class="errorMessage"><@vrtx.msg code="xmledit.errorMessage" default="Error message" />: ${error.exception.message}>
      
      <#if (error.exception).cause?exists && ((error.exception).cause).message?exists >
        <p class="errorMessage"><@vrtx.msg code="xmledit.errorMessage.cause" default="Caused by" />: ${error.exception.cause.message}>
      </#if>
  </div>

</body>
</html>
