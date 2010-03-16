<#if breadcrumb?exists>
<ul class="vrtx-breadcrumb-menu">
	<#list breadcrumb as elem >
	<#if children?exists && (children?size > 0)>
		<#if (elem_has_next) >
			<#if elem.URL?exists>
				<li class="vrtx-ancestor"> <a href="${elem.URL}"><span>${elem.title}</span></a> </li>
			<#else>
				<li class="vrtx-ancestor"> <span><span>${elem.title}</span></span></li>
			</#if>
		<#else>
			<#if (elem.URL?string = markedurl) && linkToMarkedURL >
				<li class="vrtx-parent" ><a class="vrtx-marked" href="${elem.URL}"><span>${elem.title}</span></a>
			<#elseif (elem.URL?string = markedurl) >
				<li class="vrtx-parent" ><span class="vrtx-marked"><span>${elem.title}</span></span>
			<#else>
				<li class="vrtx-parent" ><a href="${elem.URL}"><span>${elem.title}</span></a>
			</#if>
		</#if>
	<#else>	
		<#-- If the user don't have access to the 'current' resource -->
		<#if (breadcrumb?size > elem_index + 2 ) >
			<#if elem.URL?exists>
				<li class="vrtx-ancestor"> <a href="${elem.URL}"><span>${elem.title}</span></a> </li>
			<#else>
				<li class="vrtx-ancestor"><span><span>${elem.title}</span><span></li>
			</#if>
		<#else>
			<#if (breadcrumb?size > elem_index + 1) > 
				<#if elem.URL?exists >
					<li  class="vrtx-parent" ><a href="${elem.URL}"><span>${elem.title}</span></a>
				<#else>
					<li  class="vrtx-parent" ><span><span>${elem.title}</span></span>
				</#if>
			<#else>
				<ul>
				<#if elem.URL?exists && linkToMarkedURL>
					<li  class="vrtx-child" ><a class="vrtx-marked" href="${elem.URL}"><span>${elem.title}</span></a></li>
				<#else>
					<li  class="vrtx-child" ><span class="vrtx-marked"><span>${elem.title}</span></span></li>
				</#if>
				</ul>
			</#if>
		</#if>
	</#if>
	</#list>
	<#if children?exists >
    	<ul>
     	<#list children as c>
			<li class="vrtx-child">
			<#if (c.url?string = markedurl) && linkToMarkedURL>
				<a class="vrtx-marked" href="${c.url?html}"><span>${c.title?html}</span></a>
			<#elseif (c.url?string = markedurl) >
				<span class="vrtx-marked"><span>${c.title?html}</span></span>
			<#else>
				<a href="${c.url?html}"><span>${c.title?html}</span></a>
			</#if>
			</li>
	 	</#list>
     	</ul>
   	 </#if>
   	 </li>
</ul>
</#if>
