<#ftl strip_whitespace=true>

<#--
  - File: webconsole.ftl
  - 
  - Description: 
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Web console</title>
</head>
<body>
  <#if commandForm?exists>
  <p>
    <form class="webConsole" action="${commandForm.submitURL}" method="POST">
      Enter command: <input type="text" name="command" size="60" />
    </form>
  </p>
  </#if>
  <#if commandForm.result?exists>
    <div class="webConsoleResults">
      ${commandForm.result?html}
    </div>
  </#if>

</body>
</html>
