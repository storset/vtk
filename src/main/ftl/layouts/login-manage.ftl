<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#if options?has_content>
  <#assign type = "login-manage" />

  <#-- First option => visible dropdown toggler (and link if not principal desc.) -->
  <#list options?keys as opt>
    <#if opt_index == 1><#break /></#if>
    <#if opt = "principal-desc">
      <#assign title = principal.description />
      <#assign titleLink = "" />
    <#else>
      <#assign title = vrtx.getMsg("decoration.${type}.${opt?html}") />
      <#assign titleLink = options[opt] />
    </#if>
  </#list>
  
  <#-- Rest of options => dropdown list -->
  <#if (options?size > 1)>
    <!-- begin view dropdown js -->
    <script type="text/javascript" src="${jsUrl?html}"></script>
    <!-- end view dropdown js -->
    <script type="text/javascript"><!--
      $(function() {
        $(".vrtx-dropdown-form").addClass("hidden");
        $(".vrtx-dropdown-form-link").addClass("visible");
        $(document).off("click", ".vrtx-dropdown-form-link")
                    .on("click", ".vrtx-dropdown-form-link", function(e) {
          $(this).prev(".vrtx-dropdown-form").submit();
          e.stopPropagation();
          e.preventDefault();
        });
      });
    // -->
    </script>
  
    <@viewutils.displayDropdown type title titleLink false>
      <#list options?keys as opt>
        <#if (opt_index > 0)>
          <#assign classes = "" />
          <#if (opt_index == 1)>
            <#assign classes = classes + "vrtx-dropdown-first" />
          </#if>
          <#if (opt_index == (options?size - 1))>
            <#if classes != ""><#assign classes = classes + " " /></#if>
            <#assign classes = classes + "vrtx-dropdown-last" />
          </#if>
          <li<#if classes != ""> class="${classes}"</#if>>
            <#assign url = options[opt] />
            <#if opt = "logout">
              <form action="${url?html}" method="post" class="vrtx-dropdown-form">
                <@vrtx.csrfPreventionToken url=url />
                <button type="submit" id="logoutAction" name="logoutAction">
                  <@vrtx.msg code="decoration.${type}.${opt?html}" />
                </button>
              </form>
              <a href="javascript:void(0);" class="vrtx-${type}-${opt?html} vrtx-dropdown-form-link">
                <@vrtx.msg code="decoration.${type}.${opt?html}" />
              </a>
            <#else>
              <a href="${url?html}" class="vrtx-${type}-${opt?html}">
                <@vrtx.msg code="decoration.${type}.${opt?html}" />
              </a>
            </#if>
          </li>
        </#if>
      </#list>
    </@viewutils.displayDropdown>
    
  <#else>
  
    <div class="vrtx-${type}-component">
      <a href="<#if titleLink != ''>${titleLink?html}<#else>javascript:void(0)</#if>" class="vrtx-${type}-link">
        ${title?html}
      </a>
    </div>
    
  </#if>
  
</#if>