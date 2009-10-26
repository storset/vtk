<#if breadcrumb?exists>
<ul class="vrtx-breadcrumb-menu">
	<#list breadcrumb as elem >
		<#if (elem_has_next) >
			<li class="vrtx-ancestor"> <a href="${elem.URL}"><span>${elem.title}</span></a> </li>
		<#else>
			<#if (elem.URL?exists)  >
				<li class="vrtx-parent" ><a href="${elem.URL}"><span>${elem.title}</span></a>
			<#else>
				<li class="vrtx-parent" ><a class="vrtx-marked" href=""><span>${elem.title}</span></a>
			</#if>
		</#if>
	</#list>
     <ul>
     <#list children as c>
		<li class="vrtx-child"><a <#if (c.url?string = markedurl) >class="vrtx-marked"</#if> href="${c.url?html}"><span>${c.title?html}</span></a></li>
	</#list>
     </ul>
   </li>
</ul>
</#if>