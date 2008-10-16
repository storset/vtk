<#ftl strip_whitespace=true>

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if propertyDefinition.namespace.prefix?exists>
  <#assign localePrefix =  propertyDefinition.namespace.prefix + ":" />
</#if>
<#assign localePrefix = "property." + localePrefix?if_exists + propertyDefinition.name />

<#assign title = vrtx.getMsg("vocabulary.title.prefix", "Vocabulary for")
         + " " + vrtx.getMsg(localePrefix, "Unknown")>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<title>${title}</title>

<#if cssURLs?exists>
  <#list cssURLs as cssURL>
    <link rel="stylesheet" type="text/css" href="${cssURL}">
  </#list>
</#if>

<#if jsURLs?exists>
  <#list jsURLs as jsURL>
  <script type="text/javascript" src="${jsURL}"></script>
  </#list>
</#if>


<script type="text/javascript"><!--

    var Dom = YAHOO.util.Dom,
        Event = YAHOO.util.Event,
        Lang = YAHOO.lang,
        Widget = YAHOO.widget;

   function updateParent() {
     var rootNodes = tree.getRoot().children;
     var v = '';
     for (i in rootNodes) {
       v = getCheckedAsStringValue(rootNodes[i], v);
     }
     
     opener.document.getElementById("value").value = v;

     self.close();
     return false;
   }

   function getCheckedAsStringValue(node, result) {

     if (node.checkState == 2) {
       if (result == '') {
         return node.taskCode;
       } else {
         return result + ', ' + node.taskCode;
       }
     } else if (node.checkState == 1 && node.children != null) {
   	var r = result;
       for(var i=0; i<node.children.length; ++i) {

   	  r = getCheckedAsStringValue(node.children[i], r);	  
       }    
       return r;
     } else {
       return result;
     }
   }

   var tree;
   
   function treeInit() {
       document.getElementById("expandcontractdiv").style.visibility = "visible";
       document.getElementById("insert").style.visibility = "visible";
       selected = '${selected}';
       tree = new YAHOO.widget.TreeView("treeDiv1");
       tree.draw();
   }

   var callback = null;

  <#if propertyDefinition.multiple>
    function checkAll() {
        var topNodes = tree.getRoot().children;
        for(var i=0; i<topNodes.length; ++i) {
            topNodes[i].check();
        }
    }

    function uncheckAll() {
        var topNodes = tree.getRoot().children;
        for(var i=0; i<topNodes.length; ++i) {
            topNodes[i].uncheck();
        }
    }
  </#if>

  // -->
</script>

</head>
  
<body onload="treeInit()">

<div id="container">
  <div id="containerTop">
    <div id="main">
  <div id="content">
    <form name="mainForm">
	<div class="newsItem">
	  <h3>${title}</h3>

	  <p>${vrtx.getMsg("vocabulary.description", "Choose the relevant values and press submit")}</p>

	  <div style="visibility:hidden" id="expandcontractdiv">
		<a href="javascript:tree.expandAll()">${vrtx.getMsg("vocabulary.expandall", "Expand all")}</a>
		<a href="javascript:tree.collapseAll()">${vrtx.getMsg("vocabulary.collapseall", "Collapse all")}</a>
  <#if propertyDefinition.multiple>
		<a href="javascript:checkAll()">${vrtx.getMsg("vocabulary.checkall", "Check all")}</a>
		<a href="javascript:uncheckAll()">${vrtx.getMsg("vocabulary.uncheckall", "Uncheck all")}</a>
  </#if>
	  </div>
<br>
	  <div id="treeDiv1"><@listNodes nodes=rootNodes />
	   </div>

	</div>
<br>
        <#assign submit=vrtx.getMsg("vocabulary.submit", "Submit") />
	<input style="visibility:hidden" type="button" onclick="javascript:updateParent()" id="insert" name="save" value="${submit}">
	</form>
  </div>
    </div>
  </div>
</div>
  </body>
</html>

  <#macro listNodes nodes>
    <ul>
      <#list nodes as node>
        <#assign displayName = propertyDefinition.vocabulary.valueFormatter.valueToString(node.entry, "localized", springMacroRequestContext.locale) />

        <li id="${node.entry?string}">${node.entry?string} - ${displayName}
          <#if node.children?exists><@listNodes nodes=node.children /></#if>
        </li>
      </#list>
    </ul>
  </#macro>
