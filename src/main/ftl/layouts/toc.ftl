<#--
  - File: toc.ftl
  - 
  - Description: displays a table of contents based on h2/h3 in the document
  - 
  -->

<#if !url?exists>
  <#stop "Missing 'url' in model"/>
</#if>

<div id="toc"></div>
<script type="text/javascript" src="http://www.uio.no/vrtx/javascript/toc.js"></script>
