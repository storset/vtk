<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>412 - precondition failed</title>
  <link href="http://www.uio.no/visuell-profil/css/uio.css" type="text/css" rel="stylesheet">
</head>
<body>

<h1>412 - precondition failed</h1>

<#if debugErrors?exists && debugErrors>
<hr style="width: 98%;">
<#include "/lib/error-detail.ftl" />
</#if>

<p>Server-administrator: <a href="mailto:${webmaster}">${webmaster}</a></p>

</body>
</html>
