<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<html>
<head>
<#if title?exists >
	<title>${title}</title>
</#if>
</head>
<body>
<#if title?exists >
	<h1>${title}</h2>
</#if>
<#if src?exists> 
<img src="${src}" />
</#if>
<#if description?exists >
	<div>
		${description}
	</div>
</#if>
</body>
</html>