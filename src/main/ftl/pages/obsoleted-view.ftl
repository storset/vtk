<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#assign resource = resourceContext.currentResource />
<#assign title = vrtx.propValue(resource, "title") />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>${title} - <@vrtx.msg code="property.obsoleted" default="Obsoleted"/></title>
    <meta name="robots" content="noindex, nofollow" />
  </head>
  <body>
    <strong><@vrtx.msg code="property.obsoleted.value.true" default="This resource is obsoleted."/><strong>
  </body>
</html>