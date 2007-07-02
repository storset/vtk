<#ftl strip_whitespace=true>

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<!doctype html public "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>

<head>
<title>Vocabulary</title>
<link rel="stylesheet" type="text/css" href="http://developer.yahoo.com/yui/examples/treeview/css/screen.css">
<link rel="stylesheet" type="text/css" href="http://developer.yahoo.com/yui/examples/treeview/css/check/tree.css">

<script type="text/javascript" src="http://developer.yahoo.com/yui/build/yahoo/yahoo.js" ></script>
<script type="text/javascript" src="http://developer.yahoo.com/yui/build/event/event.js"></script>
<script type="text/javascript" src="http://developer.yahoo.com/yui/build/treeview/treeview.js" ></script>

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
      document.getElementById("expandcontractdiv").style.visibility = "visible";
      document.getElementById("insert").style.visibility = "visible";
	  tree = new YAHOO.widget.TreeView("treeDiv1");
      <@createTree nodes=rootNodes parent="tree.getRoot()" name="vra" selected=selected_nodes parentchecked=false />
      tree.draw();
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
	  <h3>Scientific disciplines</h3>
	  <p>Choose the relevant scientific disciplines and press submit</p>

	  <div style="visibility:hidden" id="expandcontractdiv">
		<a href="javascript:tree.expandAll()">Expand all</a>
		<a href="javascript:tree.collapseAll()">Collapse all</a>
		<a href="javascript:checkAll()">Check all</a>
		<a href="javascript:uncheckAll()">Uncheck all</a>
	  </div>
	  <div id="treeDiv1"><@listNodes nodes=rootNodes />
	   </div>

	</div>
	<input style="visibility:hidden" type="button" onclick="javascript:updateParent()" id="insert" name="save" value="Sett inn">
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
        <#assign displayName = vrtx.getMsg("property.scientific:disciplines.value." + node.entry?string, node.entry?string) />
 <li>${node.entry?string} - ${displayName}
        <#if node.children?exists><@listNodes nodes=node.children /></#if>
</li>
</#list>
 </ul>
 </#macro>
	<#macro createTree nodes parent name selected parentchecked>
	  <#list nodes as node>
	    <#assign checked=parentchecked />
	    <#if !checked>
	      <#assign checked=selected?seq_contains(node.entry) />
        </#if>
        <#assign displayName = vrtx.getMsg("property.scientific:disciplines.value." + node.entry?string, node.entry?string) />
        var ${name}_${node_index}_node = new YAHOO.widget.TaskNode("${displayName}",${parent}, "${node.entry}", false<#if checked>, ${checked?string}</#if>);
        <#if node.children?exists>
	      <@createTree nodes=node.children parent=name+"_"+node_index+"_node" name=name + "_" + node_index selected=selected parentchecked=checked />     	         
        </#if>	
      </#list>
	</#macro>

 