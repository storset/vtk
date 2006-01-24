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
<#import "/lib/version.ftl" as vrsn />

      </div>  <#-- End of <div class="content"> -->

    </div>  <#-- End of <div class="main"> -->

  </div>  <#-- End of <div class="body"> -->

  <div class="footer">
    <@vrsn.displayVersion version=version />
    <#include "language.ftl" />
  </div>
