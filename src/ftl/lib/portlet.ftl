<#--
  - File: portlet.ftl
  - 
  - Description: Macro for displaying portlet output in a consistent way.
  - 
  - Required model data:
  -   portlet
  -  
  - Optional model data:
  -   
  -
  -->


<#macro displayPortlet portlet displayHeader=true>
  <div class="portlet portlet_${portlet.name}">
    <#if displayHeader>
    <div class="portletHeader">
      <div class="portletTitle">${(portlet.title?html)?if_exists}</div>
      <div class="portletNavigation">
        <ul>
          <#if (portlet.editModeURL?html)?exists>
            <li><a href="${portlet.editModeURL?html}">edit</a>
          </#if>
          <#if (portlet.viewModeURL?html)?exists>
            <li><a href="${portlet.viewModeURL?html}">view</a>
          </#if>
          <#if (portlet.helpModeURL?html)?exists>
            <li><a href="${portlet.helpModeURL?html}">help</a>
          </#if>
        </ul>
      </div>
    </div>
    </#if>
    <div class="portletContent">
      <#if (portlet.content)?exists>
        ${portlet.content}
      </#if>
      <#if (portlet.error)?exists>
        <p>${portlet.error.class.name?html}:
          ${(portlet.error.message?default('No message'))?html}
        </p>
        <div class="exceptionStacktrace">
          Stacktrace:
          <ul class="stacktrace">
            <#list portlet.error.stackTrace as traceLine>
              <li>${traceLine?html}</li>
            </#list>
          </ul>
        </div>
        <#if (portlet.error.cause)?exists>
          <p>Caused by: ${portlet.error.cause.class.name?html}:
            ${(portlet.error.cause.message?default('No message'))?html}
          </p>
          <div class="exceptionStacktrace">
            Stacktrace:
            <ul class="stacktrace">
              <#list portlet.error.cause.stackTrace as traceLine>
                <li>${traceLine?html}</li>
              </#list>
            </ul>
          </div>
        </#if>      

      </#if>      
    </div>
  </div>
</#macro>
