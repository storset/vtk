<#ftl strip_whitespace=true>
<#--
  - File: course-schedule.ftl
  - 
  - Description: Displays course schedule
  - 
  - Required model data:
  -   result
  -
  -->
  
<script type="text/javascript"><!--
  var canEdit = "${canEdit?string}";
// -->
</script>
  
<@generateType result "plenary" />
<@generateType result "group" />

<#macro generateType result type>
  <#local activities = result[type].activities />
  <#local skipTier = type == "plenary" />
  
  <#local sequences = {} />
  
  <#local sessions = [] />
  <#local isAllPassed = false />
  <#local hasResources = false />
  <#local hasStaff = false />
  
  <#list activities?sort_by("id") as activity>
    <#local id = activity.id />
    <#local dtShort = activity.teachingMethod?lower_case />
    <#local dtLong = activity.teachingMethodName />
    <#local isFor = dtShort == "for" />

    <#list activity.sequences as sequence>
      <#local sessions = sessions + sequence.sessions />
      
      <#if !hasStaff || !hasResources>
        <#list sequence.sessions as session>
          <#if !hasStaff &&
              ((session.staff?exists && session.staff?size > 0)
            || (session.vrtxStaff?exists) || (session.vrtxStaffExternal?exists))>
            <#local hasStaff = true />
          </#if>
          <#if !hasResources &&
              ((session.vrtxResources?exists && session.vrtxResources?size > 0)
            || (session.vrtxResourcesText?exists))>
            <#local hasResources = true />
          </#if>
          <#if hasStaff && hasResources><#break /></#if>
        </#list>
      </#if>
      
      <#if sequence.vrtxResourcesFixed?exists>
        <#local hasResources = true />
      </#if>
    </#list>
    <#if !isFor || (isFor && (!activity_has_next || activities[activity_index + 1].teachingMethod?lower_case != dtShort))>
      <#if isFor>
        <#local activityId = dtShort />
      <#else>
        <#local activityId = dtShort + "-" + id />
      </#if>
      
      <#local idSplit = id?split("-") />
      <#local groupCode = idSplit[0] />
      <#local groupNumber = idSplit[1] />
      <#if skipTier>
        <#local caption = dtLong />
      <#else>
        <#local caption = dtLong + " - " + "Gruppe"?lower_case + " " + groupNumber />
      </#if>
      
      <div class="course-schedule-table-wrapper">
        <table id="${activityId}" class="course-schedule-table uio-zebra hiding-passed<#if isAllPassed> all-passed</#if><#if hasResources> has-resources</#if><#if hasStaff> has-staff</#if>" >
          <caption>${caption}</caption>
          <thead>
          <tr>
            <th class="course-schedule-table-date">Dato</th>
            <th class="course-schedule-table-time">Tid</th>
            <th class="course-schedule-table-title">Aktivitet</th>
            <th class="course-schedule-table-place">Sted</th>
          <#if hasStaff>
            <th class="course-schedule-table-staff">Foreleser</th>
          </#if>
          <#if hasResources>
            <th class="course-schedule-table-resources">Ressurser/pensum</th>
          </#if>
          </tr>
          </thead>
          <tbody>

      <#local count = 0 />
      <#list sessions?sort_by("dtStart") as session>
        <#if !session.vrtxOrphan?exists>
        
        <#local count = count + 1 />
      
        <#local dateStart = session.dtStart?replace("T", " ")?date("yyyy-MM-dd hh:mm:ss") />
        <#local dateEnd = session.dtEnd?replace("T", " ")?date("yyyy-MM-dd hh:mm:ss") />
        
        <#if skipTier>
          <#local sessionId = type + "-" + session.id?replace("/", "-") + "-" + dateStart?string("dd-MM-yyyy-hh-mm") + "-" + dateEnd?string("hh-mm") />
        <#else>
          <#local sessionId = dtShort + "-" + id + "-" + session.id?replace("/", "-") + "-" + dateStart?string("dd-MM-yyyy-hh-mm") + "-" + dateEnd?string("hh-mm") />
        </#if>
        
        <#if session.vrtxTitle?exists>
          <#local title = session.vrtxTitle />
        <#elseif session.title?exists> 
          <#local title = session.title />
        <#else>
          <#local title = session.id />
        </#if>
        <#if (session.status == "cancelled") || (session.vrtxStatus?exists && session.vrtxStatus == "cancelled")>
          <#local title = "<span class='course-schedule-table-status'>AVLYST</span>" + title />
        </#if>
        
        <#if count % 2 == 0>
          <#local classes = "even" />
        <#else>
          <#local classes = "odd" />
        </#if>
        
        <#local hasNotStaffAndResources = !hasStaff && !hasResources />
        
        <#local place><@getPlace session /></#local>
        <#local staff><@getStaff session /></#local>
        <#local resources><@getResources session /></#local>
        
         <tr id="${sessionId}" class="${classes}"> 
           <td class='course-schedule-table-date'><span class='responsive-header'></span>${dateStart?string("d. MMM.")}</td>
           <td class='course-schedule-table-time'><span class='responsive-header'></span>${dateStart?string("hh:mm")}-${dateEnd?string("hh:mm")}</td>
           <td class='course-schedule-table-title'><span class='responsive-header'></span>${title}</td>
           <@editLink "course-schedule-table-place" "<span class='responsive-header'></span>${place}" hasNotStaffAndResources canEdit />
           <#if hasStaff><@editLink "course-schedule-table-staff" "<span class='responsive-header'></span>${staff}" !hasResources canEdit /></#if>
           <#if hasResources><@editLink "course-schedule-table-resources" "<span class='responsive-header'></span>${resources}" hasResources canEdit /></#if>
        </tr>
        
        </#if>
      </#list>
      </tbody>
      </table>
      </div>
      
      <#local sessions = [] />
      <#local isAllPassed = false />
      <#local hasResources = false />
      <#local hasStaff = false />
    </#if>
  </#list>
</#macro>

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
  <#local resourcesList = arrToList(resources, true) />
  <#local totTxtLen = resourcesList.txtLen />
  <#local val = resourcesList.val />
  <#local valAfter = resourcesList.valAfter />
  <#if session.vrtxResourcesText?exists>
    <#if (totTxtLen > resourcesTxtLimit)>
      <#local valAfter = valAfter + session.vrtxResourcesText />
    <#else>
      <#local val = val + session.vrtxResourcesText />
    </#if>
  </#if>
  
  ${val}
  
  <#if valAfter != "">
    <a href='javascript:void(0);' class='course-schedule-table-resources-after-toggle'>Vis mer...</a>
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
    ${title}
  <#else>
    ${text}
  </#if>
  <#if url != "">
    </a>
  <#else>
    </span>
  </#if>
</#macro>

<#function arrToList arr split>
  <#local resourcesTxtLimit = 70 />

  <#local val = "" />
  <#local valAfter = "" />
  <#local totTxtLen = 0 />
  <#local arrLen = arr?size />
  <#if (arrLen <= 0)> <#return { "val": val, "valAfter": valAfter, "txtLen": totTxtLen }> </#if>

  <#list arr as obj>
    <#if obj.name?exists && obj.url?exists>
      <#local txt><@formatName obj.name /></#local>
      <#local totTxtLen = totTxtLen + txt?length />
      <#local txt = "<a href='" + obj.url + "'>" + txt + "</a>" />
    <#elseif obj.title?exists && obj.url?exists>
      <#local txt = obj.title />
      <#local totTxtLen = totTxtLen + txt?length />
      <#local txt = "<a href='" + obj.url + "'>" + txt + "</a>" />
    <#elseif obj.url?exists>
      <#local txt = obj.url />
      <#local totTxtLen = totTxtLen + txt?length />
      <#local txt = "<a href='" + obj.url + "'>" + txt + "</a>" />
    <#elseif obj.name?exists>
      <#local txt><@formatName obj.name /></#local>
      <#local totTxtLen = totTxtLen + txt?length />
    <#elseif obj.title?exists>
      <#local txt = obj.title />
      <#local totTxtLen = totTxtLen + txt?length />
    <#elseif obj.id?exists>
      <#local txt = obj.id />
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
       <a class='button course-schedule-table-edit-link' href='javascript:void'><span>Rediger</span></a>
     </div>
    </#if>
  </td>  
</#macro>