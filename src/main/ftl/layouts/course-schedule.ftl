<#ftl strip_whitespace=true>
<#--
  - File: course-schedule.ftl
  - 
  - Description: Displays course schedule
  -
  - Model data:
  -   canEdit
  -   sequences
  -   result
  -->
  
<#import "/lib/vortikal.ftl" as vrtx/>

<script type="text/javascript"><!--
  var canEdit = "${canEdit?string}";
// -->
</script>

<#if result?has_content>
  <div id="activities">
    <#-- Generate HTML -->
    <#if result.plenary?has_content>
      <#assign plenaryHtml = generateType(result, "plenary") />
    </#if>
    <#if result.group?has_content>
      <#assign groupHtml = generateType(result, "group") />
    </#if>
    
    <#-- Display ToC -->
    <#if plenaryHtml?exists>
      <h2 class="course-schedule-toc-title accordion">${vrtx.getMsg("course-schedule.header-plenary")}</h2>
      <div class="course-schedule-toc-content"><ul>${plenaryHtml.tocHtml}</ul></div>
    </#if>
    <#if groupHtml?exists>
      <h2 class="course-schedule-toc-title accordion">${vrtx.getMsg("course-schedule.header-group")}</h2>
      <div class="course-schedule-toc-content">${groupHtml.tocHtml}</div>
    </#if>
    
    <#-- Display tables -->
    <#if plenaryHtml?exists>
      ${plenaryHtml.tablesHtml}
    </#if>
    <#if groupHtml?exists>
      ${groupHtml.tablesHtml}
    </#if>
  </div>
<#else>
  <p>${vrtx.getMsg("course-schedule.no-data")}</p>
</#if>

<#function generateType result type>
  <#local activities = result[type] />
  <#local skipTier = type == "plenary" />
  
  <#local sessions = [] />
  
  <#local now = .now?date />
  <#local isAllPassed = false />
  
  <#local tablesHtml = "" />
  <#local tocHtml = "" />
  <#local tocHtmlMiddle = "" />
  <#local tocTimeNo = false />
  <#local tocTime = "" />
  <#local tocTimeCount = 0 />
  <#if skipTier>
    <#local tocTimeMax = 3 />
  <#else>
    <#local tocTimeMax = 2 />
  </#if>
  
  <#local groupCount = 0 />
  <#list activities as activity>
    <#local id = activity.id />
    <#local title = activity.title />
    <#local groupNumber = activity.groupNumber />
    <#local dtShort = activity.dtShort />
    <#local dtLong = activity.dtLong />
    <#local hasResources = activity.hasResources />
    <#local hasStaff = activity.hasStaff />
    <#local isFor = dtShort == "for" />

    <#if skipTier>
      <#local activityId = dtShort />
    <#else>
      <#local activityId = dtShort + "-" + id />
    </#if>
      
    <#local tablesHtmlStart>
    <div class="course-schedule-table-wrapper">
      <table id="${activityId}" class="course-schedule-table <#if isAllPassed> all-passed</#if><#if hasResources> has-resources</#if><#if hasStaff> has-staff</#if>" >
        <caption>${title?html}</caption>
        <thead>
        <tr>
          <th class="course-schedule-table-date">${vrtx.getMsg("course-schedule.table-date")}</th>
          <th class="course-schedule-table-time">${vrtx.getMsg("course-schedule.table-time")}</th>
          <th class="course-schedule-table-title">${vrtx.getMsg("course-schedule.table-title")}</th>
          <th class="course-schedule-table-place">${vrtx.getMsg("course-schedule.table-place")}</th>
          <#if hasStaff>
            <th class="course-schedule-table-staff">${vrtx.getMsg("course-schedule.table-staff")}</th>
          </#if>
          <#if hasResources>
            <th class="course-schedule-table-resources">${vrtx.getMsg("course-schedule.table-resources")}</th>
          </#if>
        </tr>
        </thead>
        <tbody>
     </#local>

     <#local count = 0 />
     <#local sessions = activity.sessions />
     <#list sessions as session>
       <#if !session.vrtxOrphan?exists>
        
       <#local count = count + 1 />
       <#local dateStart = session.dtStart?replace("T", " ")?date("yyyy-MM-dd HH:mm:ss") />
       <#local dateEnd = session.dtEnd?replace("T", " ")?date("yyyy-MM-dd HH:mm:ss") />
       <#local isPassed = (now > dateEnd) />
       <#local sessionId = dtShort + "-" + id + "-" + session.id?replace("/", "-") + "-" + dateStart?string("dd-MM-yyyy-HH-mm") + "-" + dateEnd?string("HH-mm") />
        
       <#if session.vrtxTitle?exists>
         <#local title = session.vrtxTitle />
       <#elseif session.title?exists> 
         <#local title = session.title />
       <#else>
         <#local title = session.id />
       </#if>
       <#if (session.status == "cancelled") || (session.vrtxStatus?exists && session.vrtxStatus == "cancelled")>
         <#local title = "<span class='course-schedule-table-status'>" + vrtx.getMsg("course-schedule.table-cancelled") + "</span>" + title?html />
       </#if>
        
       <#if count % 2 == 0>
         <#local classes = "even" />
       <#else>
         <#local classes = "odd" />
       </#if>
       <#if isPassed>
         <#local classes = classes + " passed" />
       </#if>
        
       <#local hasNotStaffAndResources = !hasStaff && !hasResources />
       <#local hasNotResources = !hasResources />
       
       <#local day>${dateStart?string("EEE")?capitalize}</#local>
       <#local time><span>${dateStart?string("HH:mm")}-</span><span>${dateEnd?string("HH:mm")}</span></#local>
       <#local date>${day?substring(0, 2)}. ${dateStart?string("d. MMM.")}</#local>
       <#local place><@getPlace session /></#local>
       <#local staff><@getStaff session /></#local>
       <#local resources><@getResources session /></#local>
        
       <#local placeHeader = vrtx.getMsg("course-schedule.table-place") />
       <#local staffHeader = vrtx.getMsg("course-schedule.table-staff") />
       <#local resourcesHeader = vrtx.getMsg("course-schedule.table-resources") />
         
       <#local tablesHtmlMiddle>
         <tr class="course-schedule-table-tr-header accordion"><td><span class="tr-header-date">${date}</span><span class="tr-header-time">${time}</span></td></tr>
         <tr id="${sessionId}" class="${classes}"> 
           <td class='course-schedule-table-date'><span class='responsive-header'>${vrtx.getMsg("course-schedule.table-date")}</span>${date}</td>
           <td class='course-schedule-table-time'><span class='responsive-header'>${vrtx.getMsg("course-schedule.table-time")}</span>${time}</td>
           <td class='course-schedule-table-title'><span class='responsive-header'>${vrtx.getMsg("course-schedule.table-title")}</span>${title}</td>
           <@editLink "course-schedule-table-place" "<span class='responsive-header'>${placeHeader}</span>${place}" hasNotStaffAndResources canEdit />
           <#if hasStaff><@editLink "course-schedule-table-staff" "<span class='responsive-header'>${staffHeader}</span>${staff}" hasNotResources canEdit /></#if>
           <#if hasResources><@editLink "course-schedule-table-resources" "<span class='responsive-header'>${resourcesHeader}</span>${resources}" hasResources canEdit /></#if>
         </tr>
       </#local>
       <#local tablesHtmlStart = tablesHtmlStart + tablesHtmlMiddle />
        
       <#if !tocTimeNo>
         <#local newTocTime = day?lower_case + " " + time />
         <#if !tocTime?contains(newTocTime)>
           <#if (tocTimeCount < tocTimeMax)>
             <#if (tocTimeCount > 0)>
               <#local tocTime = tocTime + ", " />
               <#local tocTime = tocTime + "<span>" />
             </#if>
             <#local tocTime = tocTime + newTocTime + "</span>" />
           </#if>
           <#local tocTimeCount = tocTimeCount + 1 />
           <#if (tocTimeCount > tocTimeMax && !skipTier)>
             <#local tocTimeNo = true />
           </#if>
         </#if>
       </#if>
        
       </#if>
     </#list>
      
     <#local tablesHtmlEnd>
       </tbody>
       </table>
       </div>
     </#local>
      
     <#local tablesHtml = tablesHtml + tablesHtmlStart + tablesHtmlEnd />
     
     <#local groupCount = groupCount + 1 />
       
     <#if !skipTier>
       <#local tocHtmlTime = "" />
       <#if (tocTimeCount <= tocTimeMax && !tocTimeNo)>
         <#local tocHtmlTime = " - " + tocTime?replace(",([^,]+)$", " " + vrtx.getMsg("course-schedule.and") + " $1", "r") />
       </#if>
       
       <#if tocHtmlMiddle != "">
         <#local tocHtmlMiddle = tocHtmlMiddle + "####" />
       </#if>
       <#local tocHtmlMiddle = tocHtmlMiddle + "<span><a href='#" + activityId + "'>" + vrtx.getMsg("course-schedule.group-title") + " " + groupNumber + "</a>" + tocHtmlTime />
       
       <#if (!activity_has_next || activities[activity_index + 1].dtShort != dtShort)>
          <#local tocHtml = tocHtml + "<span class='display-as-h3'>" + dtLong + "</span>" />
          <#local tocHtmlMiddleArr = tocHtmlMiddle?split("####") />
          <#local colOneCount = vrtx.getEvenlyColumnDistribution(tocHtmlMiddleArr?size, 1, 3) />
          <#local colTwoCount = vrtx.getEvenlyColumnDistribution(tocHtmlMiddleArr?size, 2, 3) />
          <#local colThreeCount = vrtx.getEvenlyColumnDistribution(tocHtmlMiddleArr?size, 3, 3) />
          <#local count = 0 />
          <#local tocHtmlMiddleProcessed>
            <div class="course-schedule-toc-thirds">
            <ul class="thirds-left">
            <#list tocHtmlMiddleArr as li>
              <#if (count = colOneCount && colTwoCount > 0)>
                </ul><ul class="thirds-middle">
              </#if>
              <#if ((count = colOneCount + colTwoCount) && colThreeCount > 0)>
                </ul><ul class="thirds-right">
              </#if>
              <#if (groupCount > 30)>
                <li>${li?replace(".*(<a[^>]+>[^<]+<\\/a>).*", "$1", "r")}</li>
              <#else>
                <li>${li}</li>
              </#if>
              <#local count = count + 1 />
            </#list>
            </ul>
            </div>
          </#local>
          <#local tocHtmlMiddle = "" />
          <#local tocHtml = tocHtml + tocHtmlMiddleProcessed />
          <#local groupCount = 0 />
       </#if>
     <#else>
       <#local tocHtml = tocHtml + "<li><span><a href='#" + activityId + "'>" + dtLong?html + "</a> - " + tocTime?replace(",([^,]+)$", " " + vrtx.getMsg("course-schedule.and") + " $1", "r") + "</li>" />
       <#local groupCount = 0 />
     </#if>
      
     <#local isAllPassed = false />
     <#local hasResources = false />
     <#local hasStaff = false />
     <#local tocTime = "" />
     <#local tocTimeCount = 0 />
  </#list>
  
  <#return { "tocHtml": tocHtml, "tablesHtml": tablesHtml }>
</#function>

<#macro getPlace session>
  <#local val = "" />
  <#if session.rooms?exists>
    <#local len = session.rooms?size />
    <#if (len > 1)><ul></#if>
    <#list session.rooms as room>
      <#if (len > 1)><li></#if>
        <#if room.buildingAcronym?exists>
          <#local buildingText = room.buildingAcronym />
        <#else>
          <#local buildingText = room.buildingId />
        </#if>
        <@linkAbbr room.buildingUrl room.buildingName buildingText /> <@linkAbbr room.roomUrl room.roomName room.roomId />
      <#if (len > 1)></li></#if>
    </#list>
    <#if (len > 1)></ul></#if>
  </#if>
</#macro>

<#macro getStaff session>
  <#local staff = [] />
  <#if session.vrtxStaff?exists>
    <#local staff = session.vrtxStaff />
  <#elseif session.staff?exists>
    <#local staff = session.staff />
  </#if>
  <#if session.vrtxStaffExternal?exists>
    <#local staff = staff + session.vrtxStaffExternal />
  </#if>
  <#local staffList = arrToList(staff, false).val />
  ${staffList}
</#macro>

<#macro getResources session>
  <#local resourcesTxtLimit = 70 />
  
  <#local resources = [] />
  <#if session.vrtxResources?exists>
    <#local resources = session.vrtxResources />
  </#if>

  <#local sequenceIdSplit = session.id?split("/") />
  <#if (sequenceIdSplit?size == 2)>
    <#local sequenceId = sequenceIdSplit[0] />
  <#elseif (sequenceIdSplit?size == 3)>
    <#local sequenceId = sequenceIdSplit[1] />
  <#elseif (sequenceIdSplit?size == 1 || !sequenceIdSplit?size)>
    <#local sequenceId = session.id />
  </#if>
  <#if sequences[sequenceId]?exists>
    <#local resources = resources + sequences[sequenceId].resources />
    <#local resourcesList = arrToList(resources, true, sequences[sequenceId].folderUrl) />
  <#else>
    <#local resourcesList = arrToList(resources, true) />
  </#if>
  
  <#local totTxtLen = resourcesList.txtLen />
  <#local val = resourcesList.val />
  <#local valAfter = resourcesList.valAfter />
  <#if session.vrtxResourcesText?exists>
    <#if (totTxtLen > resourcesTxtLimit)>
      <#local valAfter = valAfter + session.vrtxResourcesText />
    <#else>
      <#-- http://stackoverflow.com/questions/19244318/javascript-split-messes-up-my-html-tags -->
      <#local htmlSplitted = session.vrtxResourcesText?matches("<\\s*(\\w+\\b)(?:(?!<\\s*\\/\\s*\\1\\b)[\\s\\S])*<\\s*\\/\\s*\\1\\s*>|\\S+") />
      <#local totExtraTxtLen = 0 />
      <#list htmlSplitted as m>
        <#local totExtraTxtLen = totExtraTxtLen + m?replace("(<([^>]+)>)", "", "r")?length /> <#-- http://css-tricks.com/snippets/javascript/strip-html-tags-in-javascript/ -->
        <#if ((totTxtLen + totExtraTxtLen) > resourcesTxtLimit)>
          <#local valAfter = valAfter + m />
        <#else>
          <#local val = val + m />
        </#if>
      </#list>
    </#if>
  </#if>
  
  ${val}
  
  <#if valAfter != "">
    <a href='javascript:void(0);' class='course-schedule-table-resources-after-toggle'>${vrtx.getMsg("course-schedule.showMore")}...</a>
    <div class='course-schedule-table-resources-after'>${valAfter}</div>
  </#if>
</#macro>

<#macro linkAbbr url="" title="" text="">

  <#-- Short -->
  <#if url != "" && title != "">
    <a class='place-short' title='${title}' href='${url}'>
  <#elseif url != "">
    <a class='place-short' href='${url}'>
  <#elseif title?exists>
    <abbr class='place-short' title='${title}'>
  </#if>
  ${text}
  <#if url != "">
    </a>
  <#elseif title != "">
    </abbr>
  </#if>
  
  <#-- Long -->
  <#if url != "">
    <a class='place-long' href='${url}'>
  <#else>
    <span class='place-long'>
  </#if>
  <#if title != "">
    ${title?html}
  <#else>
    ${text?html}
  </#if>
  <#if url != "">
    </a>
  <#else>
    </span>
  </#if>
</#macro>

<#function arrToList arr split url="">
  <#local resourcesTxtLimit = 70 />

  <#local val = "" />
  <#local valAfter = "" />
  <#local totTxtLen = 0 />
  <#local arrLen = arr?size />
  <#if (arrLen <= 0)> <#return { "val": val, "valAfter": valAfter, "txtLen": totTxtLen }> </#if>

  <#list arr as obj>
    <#if obj.title?exists && obj.name?exists>
      <#local txt = obj.title />
      <#local totTxtLen = totTxtLen + txt?length />
      <#local txt = "<a href='" + url + "/" + obj.name + "'>" + txt?html + "</a>" />
    <#elseif obj.name?exists && obj.url?exists>
      <#local txt><@formatName obj.name /></#local>
      <#local totTxtLen = totTxtLen + txt?length />
      <#local txt = "<a href='" + obj.url + "'>" + txt?html + "</a>" />
    <#elseif obj.title?exists && obj.url?exists>
      <#local txt = obj.title />
      <#local totTxtLen = totTxtLen + txt?length />
      <#local txt = "<a href='" + obj.url + "'>" + txt?html + "</a>" />
    <#elseif obj.url?exists>
      <#local txt = obj.url />
      <#local totTxtLen = totTxtLen + txt?length />
      <#local txt = "<a href='" + obj.url + "'>" + txt?html + "</a>" />
    <#elseif obj.name?exists>
      <#local txt><@formatName obj.name /></#local>
      <#local txt = txt?html />
      <#local totTxtLen = totTxtLen + txt?length />
    <#elseif obj.title?exists>
      <#local txt = obj.title?html />
      <#local totTxtLen = totTxtLen + txt?length />
    <#elseif obj.id?exists>
      <#local txt = obj.id?html />
      <#local totTxtLen = totTxtLen + txt?length />
    </#if>
      
      <#if (arrLen > 1)>
        <#local midVal = "<li>" />
      <#else>
        <#local midVal = "<p>" />
      </#if>
      <#local midVal = midVal + txt />              
      <#if (arrLen > 1)>   
        <#local midVal = midVal + "</li>" />     
      <#else>
        <#local midVal = midVal + "</p>" />
      </#if> 
         
      <#if (split && totTxtLen > resourcesTxtLimit)>
        <#local valAfter = valAfter + midVal />
      <#else>
        <#local val = val + midVal />
      </#if>
    </#list>
    
    <#if (arrLen > 1)>
      <#local val = "<ul>" + val + "</ul>" />
      <#if valAfter != "">
        <#local valAfter = "<ul>" + valAfter + "</ul>" />
      </#if>
    </#if>
    
    <#return { "val": val, "valAfter": valAfter, "txtLen": totTxtLen } >
</#function>

<#macro formatName name>
  <#local words = name?word_list />
  <#list words as word>
    <#if !word_has_next>${word}
    <#else>${word?substring(0,1)}. </#if>
  </#list>
</#macro>

<#macro editLink class html displayEditLink canEdit=false>
  <td class="${class}<#if displayEditLink && canEdit> course-schedule-table-edit-cell</#if>">
    <#if !displayEditLink || !canEdit>
      ${html}
    <#else>
     <div class='course-schedule-table-edit-wrapper'>
       ${html}
       <a class='button course-schedule-table-edit-link' href='javascript:void'><span>${vrtx.getMsg("course-schedule.table-edit")}</span></a>
     </div>
    </#if>
  </td>  
</#macro>
