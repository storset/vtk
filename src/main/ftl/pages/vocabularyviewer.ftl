<#ftl strip_whitespace=true>

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if propertyDefinition.namespace.prefix?exists>
  <#assign localePrefix =  propertyDefinition.namespace.prefix + ":" />
</#if>
<#assign localePrefix = "property." + localePrefix?if_exists + propertyDefinition.name />

<#assign title = vrtx.getMsg("vocabulary.title.prefix", "Vocabulary for")
         + " " + vrtx.getMsg(localePrefix, "Unknown")>

<!doctype html public "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>

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


<script type="text/javascript">

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
	var nodes = [];
	var nodeIndex;
	
	
	var nodes = new Array();
	
	function treeInit() {
	  <#if propertyDefinition.multiple>
      document.getElementById("expandcontractdiv").style.visibility = "visible";
      document.getElementById("insert").style.visibility = "visible";
	  tree = new YAHOO.widget.TreeView("treeDiv1");
      <@createTree nodes=rootNodes parent="tree.getRoot()" name="vra" selected=selected_nodes parentchecked=false />
      tree.draw();
      </#if>
	}

	function treeNode(i, c) {
		var item = i;
		var children = c;
	}
	
	var callback = null;

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
		<a href="javascript:checkAll()">${vrtx.getMsg("vocabulary.checkall", "Check all")}</a>
		<a href="javascript:uncheckAll()">${vrtx.getMsg("vocabulary.uncheckall", "Uncheck all")}</a>
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
        <#assign displayName = vrtx.getMsg(localePrefix + ".value." + node.entry?string, node.entry?string) />
        <li>${node.entry?string} - ${displayName}
          <#if node.children?exists><@listNodes nodes=node.children /></#if>
        </li>
      </#list>
    </ul>
  </#macro>
  
  <#macro listNodesForAC nodes>
      <#list nodes as node>
        <#assign displayName = vrtx.getMsg(localePrefix + ".value." + node.entry?string, node.entry?string) />
        ["${displayName}", "${node.entry?string}"], ["${node.entry?string}", "${displayName}"]<#if node.children?exists>, <@listNodesForAC nodes=node.children /></#if><#if node_has_next>, </#if>
      </#list>
  </#macro>
  
  <#macro createTree nodes parent name selected parentchecked>
    <#list nodes as node>
      <#assign checked=parentchecked />
      <#if !checked>
	<#assign checked=selected?seq_contains(node.entry) />
      </#if>
      <#assign displayName = vrtx.getMsg(localePrefix + ".value." + node.entry?string, node.entry?string) />
      var ${name}_${node_index}_node = new YAHOO.widget.TaskNode("${displayName}",${parent}, "${node.entry}", false<#if checked>, ${checked?string}</#if>);
      <#if node.children?exists>
	<@createTree nodes=node.children parent=name+"_"+node_index+"_node" name=name + "_" + node_index selected=selected parentchecked=checked />     	         
      </#if>	
    </#list>
  </#macro>
