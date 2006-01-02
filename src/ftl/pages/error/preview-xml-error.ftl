<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>Error</title>
  </head>
  <body>

  <div class="error" class="${error.exception.class.name?replace('.', '-')}">

    <p class="previewUnavailable">
      <@vrtx.msg code="preview.unavailable" default="The content cannot be previewed"/>
    </p>
    <p class="errorMessage">
      <@vrtx.msg code="xslt.errorMessage" default="Error message" />: 
      ${error.exception.cause.message}>
    </p>
  </div>
  </body>
</html>
