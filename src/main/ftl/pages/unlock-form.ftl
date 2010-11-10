<#--
  - File: unlock-form.ftl
  - 
  - Description: Display (and if possible, autosubmit unlock form)
  - 
  - Required model data:
  -   form
  -   resourceContext
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<title>Unlock resource '${resourceContext.currentResource.name}'</title>
</head>
<body onload="document.forms[0].submit()">
<noscript>
  <h1>Unlock resource '${resourceContext.currentResource.name}'</h1>
</noscript>
<form method="post" action="${form.url?html}">
  <@vrtx.csrfPreventionToken url=form.url />
  <noscript>
    Your browser does not support Javascript, in order to unlock the
    resource please press the submit button:
    <input type="submit" value="unlock" />
  </noscript>
</form>
</body>
</html>
