/*
 * Course schedule
 *
 * - Two threaded parsing/generating of JSON to HTML if supported (one pr. type in addition to main thread)
 */

var scheduleDeferred = $.Deferred();
var scheduleDocumentReady = $.Deferred();
var scheduleStartTime = +new Date();
var scheduleDocReadyEndTime = 0;
$(document).ready(function() {
  scheduleDocumentReady.resolve();
  scheduleDocReadyEndTime = +new Date() - scheduleStartTime;
});

(function() {
  var retrievedScheduleData = null;

  var url = window.location.href;
  if(/\/$/.test(url)) {
    url += "index.html";
  }
  url += "?action=course-schedule";
  // Debug: local development
  url = "/vrtx/__vrtx/static-resources/js/tp-test.json";
  
  var endAjaxTime = 0;
  
  var retrievedScheduleDeferred = $.Deferred();
  $.getJSON(url, function(data, xhr, textStatus) {
    retrievedScheduleData = data;
  }).always(function() {
    retrievedScheduleDeferred.resolve();
    endAjaxTime = +new Date() - scheduleStartTime;
  });
  
  $.when(retrievedScheduleDeferred).done(function() {
    if(retrievedScheduleData == null) {
      $.when(scheduleDocumentReady).done(function() {
        $("#activities").html("<p>" + scheduleI18n["no-data"] + "</p>");
      });
      scheduleDeferred.resolve();
      return;
    }
    
    var startMakingThreadsTime = +new Date();
    
    var thread1Finished = $.Deferred(),
        thread2Finished = $.Deferred(),
        htmlPlenary = {},
        htmlGroup = {},
        plenaryStringified = JSON.stringify({
          data: retrievedScheduleData["plenary"].data,
          type: "plenary",
          i18n: scheduleI18n,
          canEdit: schedulePermissions.hasReadWriteNotLocked
        }),
        groupStringified = JSON.stringify({
          data: retrievedScheduleData["group"].data,
          type: "group",
          i18n: scheduleI18n,
          canEdit: schedulePermissions.hasReadWriteNotLocked
        });

    startThreadGenerateHTMLForType(plenaryStringified, htmlPlenary, thread1Finished);
    startThreadGenerateHTMLForType(groupStringified, htmlGroup, thread2Finished);
    
    var endMakingThreadsTime = +new Date() - startMakingThreadsTime;
    
    $.when(thread1Finished, thread2Finished, scheduleDocumentReady).done(function() {
      var html = htmlPlenary.tocHtml + htmlGroup.tocHtml + htmlPlenary.tablesHtml + htmlGroup.tablesHtml;
      if(html === "") html = scheduleI18n["no-data"];

      $("#activities").html("<p>Total: " + (+new Date() - scheduleStartTime) + "ms <= ((DocReady: " + scheduleDocReadyEndTime +
                            "ms) || (AJAX-complete: " + endAjaxTime + "ms + Threads invoking/serializing: " + (endMakingThreadsTime + htmlPlenary.parseRetrievedJSONTime + htmlGroup.parseRetrievedJSONTime) +
                            "ms + (Plenary: " + htmlPlenary.time + "ms || Group: " + htmlGroup.time + "ms)))</p>" + html);
      
      // Toggle passed sessions
      $(document).on("click", ".course-schedule-table-toggle-passed", function(e) {
        var link = $(this);
        var table = link.next();
        table.toggleClass("showing-passed"); 
        link.text(table.hasClass("showing-passed") ? scheduleI18n["table-hide-passed"] : scheduleI18n["table-show-passed"]);
        e.stopPropagation();
        e.preventDefault();
      });
      
      // Edit session
      if(schedulePermissions.hasReadWriteNotLocked) {
        $(document).on("mouseover mouseout", "tbody tr", function(e) {
          var row = $(this);
          var rowStaff = row.find(".course-schedule-table-row-staff");
          var rowEdit = rowStaff.next();
          rowStaff.toggle();
          rowEdit.toggle();
        });
        $(document).on("click", ".course-schedule-table-row-edit a", function(e) {
          var row = $(this).closest("tr");
          
          /*
          var futureSimpleDialogs = $.Deferred();
          if(typeof VrtxHtmlDialog === "undefined") {
            $.cachedScript('/vrtx/__vrtx/static-resources/js/vrtx-simple-dialogs.js').done(function() {
              futureSimpleDialogs.resolve();
            });
          } else {
            futureSimpleDialogs.resolve(); 
          }
          $.when(futureSimpleDialogs).done(function() { */

            var popupWindowInternal = function (w, h, url, name) {
              var screenWidth = window.screen.width;
              var screenHeight = window.screen.height;
              if (h > (screenHeight - 300)) {
                h = screenHeight - 300;
                w += 20;
              }
              var width = (screenWidth - w) / 2;
              var height = (screenHeight - h) / 2;
              var openedWindow = window.open(url, name, "status=no,height=" + h + ",width=" + w + ",resizable=no" + ",left=" + width + ",top=" + height + ",screenX=" + width + ",screenY=" + height + ",toolbar=no,menubar=no,scrollbars=yes,location=no,directories=no");
              openedWindow.focus();
              return openedWindow;
            };
            var openedEditActivityWindow = popupWindowInternal(850, 680, window.location.pathname + "?vrtx=admin&mode=editor&action=edit&embed&sessionid=" + row[0].id, "editActivity");
          // });
          e.stopPropagation();
          e.preventDefault();
        });
      }
      scheduleDeferred.resolve();
    });
  });
  
})();

function startThreadGenerateHTMLForType(data, htmlRef, threadRef) {
  if(window.URL && window.URL.createObjectURL && typeof Blob === "function" && typeof Worker === "function") { // Use own thread
    var workerCode = function(e) {
      postMessage(generateHTMLForType(e.data));
    };
    var blob = new Blob(["onmessage = " + workerCode.toString() + "; " + generateHTMLForType.toString()], {type : 'text/javascript'});
    var blobURL = window.URL.createObjectURL(blob);
    
    var worker = new Worker(blobURL);
    worker.onmessage = function(e) {
      finishedThreadGenerateHTMLForType(e.data, htmlRef, threadRef);
    };
    worker.postMessage(data);

    if(window.URL.revokeObjectURL) window.URL.revokeObjectURL(blobURL);
  } else { // Use main thread
    finishedThreadGenerateHTMLForType(generateHTMLForType(data), htmlRef, threadRef);
  }
}

function finishedThreadGenerateHTMLForType(data, htmlRef, threadRef) {
  var startFinishedCode = +new Date();
  var receivedData = data;
  htmlRef.tocHtml = receivedData.tocHtml;
  htmlRef.tablesHtml = receivedData.tablesHtml;
  htmlRef.time = receivedData.time;
  htmlRef.parseRetrievedJSONTime = (+new Date() - startFinishedCode);
  threadRef.resolve();
}

function generateHTMLForType(d) {
  var now = new Date(),
      startGenHtmlForTypeTime = +now,
      dta = JSON.parse(d),
      data = dta.data,
      type = dta.type,
      scheduleI18n = dta.i18n,
      canEdit = dta.canEdit,
      skipTier = type === "plenary",
      splitDateTimeFunc = function(s, e) {
        var sdt = s.split("T");
        var sd = sdt[0].split("-");
        var st = sdt[1].split(".")[0].split(":");
        var edt = e.split("T");
        var ed = edt[0].split("-");
        var et = edt[1].split(".")[0].split(":");
        return { sd: sd, st: st, ed: ed, et: et };
      },
      getDateFunc = function(sd, ed, st, et) {
        if(sd[0] != ed[0] || sd[1] != ed[1] || sd[2] != ed[2]) {
          var date = sd[2] + "." + sd[1] + "." + sd[0] + "&ndash;" + ed[2] + "." + ed[1] + "." + ed[0];
        } else {
          var date = sd[2] + "." + sd[1] + "." + sd[0];
        }
        return { date: date, postFixId: (sd[2] + "-" + sd[1] + "-" + sd[0] + "-" + st[0] + "-" + st[1] + "-" + et[0] + "-" + et[1]) };
      },
      getDayFunc = function(ed, et, i18n) {
        var endTime = new Date(ed[0], ed[1]-1, ed[2], et[0], et[1], 0, 0);
        return { endTime: endTime, day: i18n["d" + endTime.getDay()] };
      },
      getTimeFunc = function(st, et) {
        return st[0] + ":" + st[1] + "&ndash;" + et[0] + ":" + et[1];
      },
      getTitleFunc = function(session) {
        return session["vrtx-title"] || session.title || session.id;
      },
      getPlaceFunc = function(session) {
        var val = "";
        var room = session.room;
        if(room && room.length) {
          for(var i = 0, len = room.length; i < len; i++) {
            if(i > 0) val += "<br/>";
            val += room[i].buildingid + " " + room[i].roomid;
          }
        }
        return val;
      },
      getStaffFunc = function(session) {
        var val = "";
        var staff = session["vrtx-staff"] || session.staff;
        if(staff && staff.length) {
          for(var i = 0, len = staff.length; i < len; i++) {
            if(i > 0) val += "<br/>";
            val += staff[i].id;
          }
        }
        return val;
      },
      getTableStartHtml = function(activityId, caption, isAllPassed, i18n) {
        var html = "<div class='course-schedule-table-wrapper'>";
        html += "<a class='course-schedule-table-toggle-passed' href='javascript:void(0);'>" + i18n["table-show-passed"] + "</a>";
        html += "<table id='" + activityId + "' class='course-schedule-table table-fixed-layout uio-zebra" + (isAllPassed ? " all-passed" : "") + "'><caption>" + caption + "</caption><thead><tr>";
          html += "<th class='course-schedule-table-date'>" + i18n["table-date"] + "</th><th class='course-schedule-table-day'>" + i18n["table-day"] + "</th>";
          html += "<th class='course-schedule-table-time'>" + i18n["table-time"] + "</th><th class='course-schedule-table-title'>" + i18n["table-title"] + "</th>";
          html += "<th class='course-schedule-table-place'>" + i18n["table-place"] + "</th><th class='course-schedule-table-staff'>" + i18n["table-staff"] + "</th>";
        html += "</tr></thead><tbody>";
        return html;
      },
      getTableEndHtml = function() {
        return "</tbody></table></div>";
      },
      tocHtml = "",
      tablesHtml = "";
  
  if(!data) return { tocHtml: "", tablesHtml: "" };
  var dataLen = data.length;
  if(!dataLen) return { tocHtml: "", tablesHtml: "" };
  
  tocHtml += "<h2 class='accordion'>" + scheduleI18n["header-" + type] + "</h2>";
  tocHtml += "<ul>";
  var lastDtShort = "";
  for(var i = 0; i < dataLen; i++) {
    var dt = data[i];
    var sessions = dt.sessions;
    var id = dt.id;
    var dtShort = dt.teachingmethod.toLowerCase();
    if(dtShort !== "for" || i == 0) {
      if(lastDtShort === "for") {
        tablesHtml += getTableStartHtml(activityId, caption, (passedCount === sessionsCount), scheduleI18n) + sessionsHtml + getTableEndHtml();
      }
      var isFor = dtShort === "for";
      var activityId = isFor ? dtShort : dtShort + "-" + dt.id;
      var caption = isFor ? scheduleI18n["table-for"] : sessions[0].title;
      var sessionsHtml = "";
      var passedCount = 0;
      var sessionsCount = 0;
      tocHtml += "<li><a href='#" + activityId + "'>" + caption + "</a></li>";
    }
    for(var j = 0, len2 = sessions.length; j < len2; j++) {
      var session = sessions[j];

      var dateTime = splitDateTimeFunc(session.dtstart, session.dtend);
      var day = getDayFunc(dateTime.ed, dateTime.et, scheduleI18n);
      var date = getDateFunc(dateTime.sd, dateTime.ed, dateTime.st, dateTime.et);
      
      var sessionId = (skipTier ? type : dtShort + "-" + id) + "-" + session.id.replace(/\//g, "-") + "-" + date.postFixId;

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
        passedCount++;
      }
      sessionsCount++;
      
      sessionsHtml += classes !== "" ? "<tr id='" + sessionId + "' class='" + classes + "'>" : "<tr>";
        sessionsHtml += "<td>" + date.date + "</td>";
        sessionsHtml += "<td>" + day.day + "</td>";
        sessionsHtml += "<td>" + getTimeFunc(dateTime.st, dateTime.et) + "</td>";
        sessionsHtml += "<td>" + getTitleFunc(session) + "</td>";
        sessionsHtml += "<td>" + getPlaceFunc(session) + "</td>";
        sessionsHtml += "<td>";
          sessionsHtml += "<span class='course-schedule-table-row-staff'>" + getStaffFunc(session) + "</span>";
          sessionsHtml += (canEdit ? "<span class='course-schedule-table-row-edit' style='display: none'><a href='javascript:void'>" + scheduleI18n["table-edit"] + "</a></span>" : "");
        sessionsHtml += "</td>";
      sessionsHtml += "</tr>";
    }
    
    if(dtShort !== "for") {
      tablesHtml += getTableStartHtml(activityId, caption, (passedCount === sessionsCount), scheduleI18n) + sessionsHtml + getTableEndHtml();
    }
    lastDtShort = dtShort;
  }
  if(dtShort === "for") {
    tablesHtml += getTableStartHtml(activityId, caption, (passedCount === sessionsCount), scheduleI18n) + sessionsHtml + getTableEndHtml();
  }
  tocHtml += "</ul>";
  
  return { tocHtml: tocHtml, tablesHtml: tablesHtml, time: (+new Date() - startGenHtmlForTypeTime) };
}