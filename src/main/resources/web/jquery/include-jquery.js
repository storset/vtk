if (typeof jQuery == 'undefined') {
  var scriptNode = document.createElement("script");
  document.getElementsByTagName("head")[0].appendChild(scriptNode);
  scriptNode.setAttribute("type", "text/javascript", 0);
  scriptNode.setAttribute("src", "/vrtx/__vrtx/static-resources/jquery/jquery-1.4.2.min.js", 0);
}