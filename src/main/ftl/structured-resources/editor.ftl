<html>
<head>
  <title>Edit structured resource</title>
</head>
<body>

<form action="${form.URL?html}" method="POST">
<#list form.formElements as elem>
  <p>
  ${elem.description.name} :
  <input type="text" size="40" name="${elem.description.name}" value="${elem.value}" />
  </p>
</#list>
<input type="submit" />
</form>
</body>
</html>
