
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/layouts/subfolder-menu.ftl" as subfolder />

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
	<link href="http://www.uio.no/vrtx/css/view-components.css" type="text/css" rel="stylesheet">
  </head>
  <body>
  <div class="vrtx-report-nav">
    <div class="resourceInfo">
      <div class="back">
	  <a href="${serviceURL}"><@vrtx.msg code="report.back" default="Back" /></a>
	  </div>
	  <h2><@vrtx.msg code="report.collection-structure" /></h2>
	  <p>
	  <@vrtx.msg code="report.collection-structure.about" />
	  </p>
	  </div>
	  <div class="vrtx-report">
		<@subfolder.displaySubFolderMenu report.subFolderMenu true />
	  </div>
    </div>
  </body>
</html>
