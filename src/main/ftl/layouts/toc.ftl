<#--
  - File: toc.ftl
  - 
  - Description: Displays a Table of Contents based on h2/h3 in the document
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<#if !url?exists>
  <#stop "Missing 'url' in model"/>
</#if>

<#assign tocHeader>'<span class="vrtx-toc-header"><@vrtx.msg code="decorating.tocComponent.header" default="Table of Contents"/><\/span>'</#assign>

<script type="text/javascript">document.write(${tocHeader});</script>

<div id="toc"></div>

<script type="text/javascript" src="${url?html}"></script>
