<#if breadcrumb?exists>
<div class="menu-component">
  <ul class="vrtx-breadcrumb-menu">
		<#list breadcrumb as elem >
			<#if (elem_has_next) >
				<#if (elem.URL?exists)  >
					<li class="vrtx-ancestor"> <a href="${elem.URL}">${elem.title}</a> </li>
				<#else>
					<li class="vrtx-ancestor"> <a class="vrtx-marked" href="">${elem.title}</a> </li>
				</#if>
			<#else>
				<#if (elem.URL?exists)  >
					<li class="vrtx-parent" ><a href="${elem.URL}">${elem.title}</a>
				<#else>
					<li class="vrtx-parent" ><a class="vrtx-marked" href="">${elem.title}</a>
				</#if>
			</#if>
		</#list>
      <ul>
      	<#list children?keys as url>
			<li class="vrtx-child"  ><a <#if (url = markedurl) >class="vrtx-marked"</#if> href="${url}">${children[url]}</a></li>
		</#list>
      </ul>
    </li>
  </ul>
</div>
</#if>