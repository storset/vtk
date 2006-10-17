<#--
  - File: tabline.ftl
  - 
  - Description: Line under active tab
  - 
  - Optional model data:
  -   tabMenu1
  -   tabMessage
  -   tabMenu2
  -   tabHelpURL
  -
  -->
<#import "/lib/menu/list-menu.ftl" as listMenu />

<div class="activeTab clear">

  <#-- Use this when tabMenu1 is actually a menu.
   <#if tabMenu1?exists>
    <@listMenu.listMenu menu=tabMenu1 displayForms=true/>
   </#if>
   -->

  <#if tabMenu1?exists>
    <ul class="listMenu tabMenu1">
      <#if (tabMenu1.url)?exists>
      <li class="navigateToParentService">
        <a href="${tabMenu1.url?html}">
          <@vrtx.msg code="collectionListing.navigateToParent" default="Up"/>
        </a>
      </li>
      </#if>
    </ul>
  </#if>

  <#if tabMessage?exists>
    <div class="tabMessage">${tabMessage?html}</div>
  </#if>

  <#if tabMenu2?exists>
    <@listMenu.listMenu menu=tabMenu2 displayForms=true/>
  </#if>

  <#if tabHelpURL?exists>
    <a class="tabHelpURL" <#if tabHelpURL.target?exists> target="${tabHelpURL.target?html}"</#if>
       href="${tabHelpURL.url?html}">${tabHelpURL.description?html}</a>
  </#if>
  <#-- Because IE will not enclose floated elements -->
  <div style="clear:both"></div>
</div>
