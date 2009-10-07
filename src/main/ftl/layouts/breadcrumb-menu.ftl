<div class="menu-component">
  <ul class="vrtx-breadcrumb-menu">
		<#list breadcrumb as elem >
			<#if (elem_has_next) >
				<li class="vrtx-ancestor"> <a <#if (elem.URL = markedurl) >class="vrtx-marked"</#if> href="${elem.URL}">${elem.title}</a> </li>
			<#else>
				<li class="vrtx-parent" ><a <#if (elem.URL = markedurl) >class="vrtx-marked"</#if> href="${elem.URL}">${elem.title}</a>
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