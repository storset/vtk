<#ftl strip_whitespace=true>

<#if breadcrumb?exists>
  <ul class="vrtx-breadcrumb-menu">

    <#list breadcrumb as elem >
      <#if children?exists && (children?size > 0)>
        <#if (elem_has_next)>
          <#if elem.URL?exists>
            <li class="vrtx-ancestor"> <a href="${elem.URL}"><span>${elem.title?html}</span></a></li>
          <#else>
            <li class="vrtx-ancestor"> <span class="vrtx-no-url"><span>${elem.title?html}</span></span></li>
          </#if>
        <#else>
          <#if (elem.URL.path = markedurl.path)>
            <li class="vrtx-parent" ><a class="vrtx-marked" href="${elem.URL}"><span>${elem.title?html}</span></a></li>
          <#else>
            <li class="vrtx-parent" ><a href="${elem.URL}"><span>${elem.title?html}</span></a></li>
          </#if>
        </#if>
      <#else>  
        <#-- If the user don't have access to the 'current' resource -->
        <#if (breadcrumb?size > elem_index + 2 )>
          <#if elem.URL?exists>
            <li class="vrtx-ancestor"> <a href="${elem.URL}"><span>${elem.title?html}</span></a></li>
          <#else>
            <li class="vrtx-ancestor"><span class="vrtx-no-url"><span>${elem.title?html}</span></span></li>
          </#if>
        <#else>
          <#if (breadcrumb?size > elem_index + 1)> 
            <#if elem.URL?exists>
              <li class="vrtx-parent" ><a href="${elem.URL}"><span>${elem.title?html}</span></a></li>
            <#else>
              <li class="vrtx-parent" ><span class="vrtx-no-url"><span>${elem.title?html}</span></span></li>
            </#if>
          <#else>
            <ul>
              <#if elem.URL?exists>
                <li class="vrtx-child" ><a class="vrtx-marked" href="${elem.URL}"><span>${elem.title?html}</span></a></li>
              <#else>
                <li class="vrtx-child" ><span class="vrtx-no-url vrtx-marked"><span>${elem.title?html}</span></span></li>
              </#if>
            </ul>
          </#if>
        </#if>
      </#if>
    </#list>

    <#if (children?exists && children?size > 0) >
      <ul>
        <#list children as c>
          <li class="vrtx-child"><a <#if (c.url.path = markedurl.path) >class="vrtx-marked"</#if> href="${c.url?html}"><span>${c.title?html}</span></a></li>
         </#list>
      </ul>
    </#if>

  </ul>
</#if>