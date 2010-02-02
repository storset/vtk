
<#import "/lib/vortikal.ftl" as vrtx />

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  </head>
  <body>
  
  <div class="vrtx-report-nav">
    <a href="${serviceURL}"><@vrtx.msg code="report.back" default="Back" /></a>
  </div>
  
  <div class="vrtx-report">
    <table cellpadding="3" border="1">
      <thead>
        <tr>
          <th><@vrtx.msg code="report.collection" default="Collection" /></th>
        </tr>
      </thead>
      <tbody>
      <#list report.collectionList as collection>
        <tr>
          <td><a href="${collection.URI}?vrtx=admin">${collection.URI}</a></td>
        </tr>
      </#list>
      </tbody>
    </table>
  </div>
  
  </body>
</html>
