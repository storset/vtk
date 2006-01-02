<#--
  - File: footer.ftl
  - 
  - Description: Default manage footer 
  - 
  - Optional model data:
  -   version
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

      </div>  <#-- End of <div class="content"> -->

    </div>  <#-- End of <div class="main"> -->

  </div>  <#-- End of <div class="body"> -->

  <div class="footer">
    <#if version?exists>
    <div class="versionInfo">
      <@vrtx.msg "version.shortDisplaystring",
      "version", [version.frameworkTitle, version.version]/>

      <!-- (<@vrtx.msg "version.displaystring",
      "version", [version.frameworkTitle, version.version,
      version.buildDate?datetime?string.long, version.buildHost] /> ) -->
      <!-- (Vortikal ${version.vortikalVersion}) -->
    </div>
    </#if>
    <#include "language.ftl" />
  </div>
