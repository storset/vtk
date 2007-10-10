<#ftl strip_whitespace=true>

<#--
  - File: plaintext-edit.ftl
  - 
  - Description: HTML page that displays a form for editing the
  - contents of a plain-text resource
  - 
  - Required model data:
  -  pingURL
  - Optional model data:
  -
  -->
<#import "/lib/ping.ftl" as ping />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Plain text edit</title>
  <@ping.ping url=pingURL['url'] interval=300/>
</head>
<body>

  <#include "components/plaintext-edit-form.ftl"/>

</body>
</html>
