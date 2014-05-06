/*
 * Course schedule
 *
 */

var scheduleDeferred = $.Deferred();
$(document).ready(function() {
  var retrievedScheduleDeferred = $.Deferred();
  var retrievedScheduleData = null;
  $.getJSON(window.location.pathname + "?action=course-schedule", function(data, xhr, textStatus) {
    retrievedScheduleData = data;
    retrievedScheduleDeferred.resolve();
  }).fail(function(xhr, textStatus) {
    retrievedScheduleDeferred.resolve();
  });
  $.when(retrievedScheduleDeferred).done(function() {
    if(retrievedScheduleData == null) {
      $("#activities").html("Ingen data");
      scheduleDeferred.resolve();
      return;
    }
    var htmlPlenary = generateHTMLForType(retrievedScheduleData, "plenary");
    var htmlGroup = generateHTMLForType(retrievedScheduleData, "group");
    var html = htmlPlenary.tocHtml + htmlGroup.tocHtml + htmlPlenary.tablesHtml + htmlGroup.tablesHtml;
    if(html === "") {
      $("#activities").html("Ingen data");
    } else {
      $("#activities").html(html);
    }
    scheduleDeferred.resolve();
  });
});

function generateHTMLForType(json, type) {
  var jsonType = json[type],
      data = jsonType.data,
      now = new Date(),
      splitDateTimeFunc = splitDateTime,
      getDateFunc = getDate,
      getDayFunc = getDay,
      getTimeFunc = getTime,
      getTitleFunc = getTitle,
      getPlaceFunc = getPlace,
      getStaffFunc = getStaff,
      tocHtml = "",
      tablesHtml = "";
  
  if(!data) return { tocHtml: "", tablesHtml: "" };
  var dataLen = data.length;
  if(!dataLen) return { tocHtml: "", tablesHtml: "" };
  
  tocHtml += "<h2 class='accordion'>" + scheduleI18n["header-" + type] + "</h2>";
  tocHtml += "<ul>";
  for(var i = 0; i < dataLen; i++) {
    var dt = data[i];
    var sessions = dt.sessions;
    var newTM = dt.teachingmethod.toLowerCase();
    if(newTM !== "for" || i == 0) {
      var isFor = newTM === "for";
      var activityId = isFor ? newTM : newTM + "-" + dt.id;
      var caption = isFor ? scheduleI18n["table-for"] : sessions[0].title;
      if(i > 0) {
        tablesHtml += "</tbody></table>";
      }
      tocHtml += "<li><a href='#" + activityId + "'>" + caption + "</a></li>";
      tablesHtml += "<table id='" + activityId + "' class='course-schedule-table table-fixed-layout uio-zebra'><caption>" + caption + "</caption><thead><tr>";
        tablesHtml += "<th class='course-schedule-table-date'>" + scheduleI18n["table-date"] + "</th><th class='course-schedule-table-day'>" + scheduleI18n["table-day"] + "</th>";
        tablesHtml += "<th class='course-schedule-table-time'>" + scheduleI18n["table-time"] + "</th><th class='course-schedule-table-title'>" + scheduleI18n["table-title"] + "</th>";
        tablesHtml += "<th class='course-schedule-table-place'>" + scheduleI18n["table-place"] + "</th><th class='course-schedule-table-staff'>" + scheduleI18n["table-staff"] + "</th>";
      tablesHtml += "</tr></thead><tbody>";
    }
    for(var j = 0, len2 = sessions.length; j < len2; j++) {
      var session = sessions[j];

      var dateTime = splitDateTimeFunc(session.dtstart, session.dtend);
      var day = getDayFunc(dateTime.ed, dateTime.et);

      var classes = "";
      if(j % 2 == 1) classes = "even";
      if((session.status && session.status === "cancelled") ||
         (session["vrtx-status"] && session["vrtx-status"] === "cancelled")) { // Grey out
        if(classes !== "") classes += " ";
        classes += "cancelled-vortex";
      }
      if(day.endTime < now) {
        if(classes !== "") classes += " ";
        classes += "passed";
      }
      
      tablesHtml += classes !== "" ? "<tr class='" + classes + "'>" : "<tr>";
        tablesHtml += "<td>" + getDateFunc(dateTime.sd, dateTime.ed) + "</td>";
        tablesHtml += "<td>" + day.endDay + "</td>";
        tablesHtml += "<td>" + getTimeFunc(dateTime.st, dateTime.et) + "</td>";
        tablesHtml += "<td>" + getTitleFunc(session) + "</td>";
        tablesHtml += "<td>" + getPlaceFunc(session) + "</td>";
        tablesHtml += "<td>" + getStaffFunc(session) + "</td>";
      tablesHtml += "</tr>";
    }
  }
  if(i > 0) {
    tablesHtml += "</tbody></table>";
  }
  tocHtml += "</ul>";
  return { tocHtml: tocHtml, tablesHtml: tablesHtml };
}

function getDate(sd, ed) {
  if(sd[0] != ed[0] || sd[1] != ed[1] || sd[2] != ed[2]) {
    return sd[2] + "." + sd[1] + "." + sd[0] + "&ndash;" + ed[2] + "." + ed[1] + "." + ed[0];
  } else {
    return sd[2] + "." + sd[1] + "." + sd[0];
  }
}

function getDay(ed, et) {
  var endTime = new Date(ed[0], ed[1]-1, ed[2], et[0], et[1], 0, 0);
  return { endTime: endTime, endDay: scheduleI18n["d" + endTime.getDay()] };
}

function getTime(st, et) {
  return st[0] + ":" + st[1] + "&ndash;" + et[0] + ":" + et[1];
}

function getTitle(session) {
  return session["vrtx-title"] || session.title || session.id;
}

function getPlace(session) {
  var val = "";
  var room = session.room;
  if(room && room.length) {
    for(var i = 0, len = room.length; i < len; i++) {
      if(i > 0) val += "<br/>";
      val += room[i].buildingid + " " + room[i].roomid;
    }
  }
  return val;
}

function getStaff(session) {
  var val = "";
  var staff = session["vrtx-staff"] || session.staff;
  if(staff && staff.length) {
    for(var i = 0, len = staff.length; i < len; i++) {
      if(i > 0) val += "<br/>";
      val += staff[i];
    }
  }
  return val;
}

function splitDateTime(s, e) {
  var sd = s.split("T")[0].split("-");
  var st = s.split("T")[1].split(".")[0].split(":");
  var ed = e.split("T")[0].split("-");
  var et = e.split("T")[1].split(".")[0].split(":");
  return { sd: sd, st: st, ed: ed, et: et };
}