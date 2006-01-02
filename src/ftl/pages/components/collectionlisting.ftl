<#--
  - File: collectionlisting.ftl
  - 
  - Description: Component that uses the collectionlisting-macro defined in
  - /lib/collectionlisting to render a collectionlisting. To be removed...
  - 
  - Required model data:
  -   resourceContext
  -   collectionListing
  -  
  - Optional model data:
  -
  -->

<#import "/lib/collectionlisting.ftl" as col />

<@col.listCollection/>
