/*
 * Course schedule
 *
 * - Two threaded parsing/generating of JSON to HTML if supported
 *   (one pr. type in addition to main thread)
 */

var scheduleDeferred = $.Deferred();
var startDoc = +new Date();
$(document).ready(function() {
  var activitiesElm = $("#activities");
  
  var retrievedScheduleDeferred = $.Deferred();
  var retrievedScheduleData = null;

  var startAjax = +new Date();
  var endDoc = startAjax - startDoc;

  $.getJSON("/vrtx/__vrtx/static-resources/js/tp-test.json", function(data, xhr, textStatus) {
    retrievedScheduleData = data;
  }).always(function() {
    retrievedScheduleDeferred.resolve();
  });
  
  $.when(retrievedScheduleDeferred).done(function() {
    if(retrievedScheduleData == null) {
      activitiesElm.html("Ingen data");
      scheduleDeferred.resolve();
      return;
    }
    
    var startParse = +new Date();
    var endAjax = startParse - startAjax;
    
    var thread1Finished = $.Deferred(),
        thread2Finished = $.Deferred(),
        htmlPlenary = {},
        htmlGroup = {},
        plenaryStringified = JSON.stringify({
          data: retrievedScheduleData,
          type: "plenary",
          i18n: scheduleI18n,
          canEdit: schedulePermissions.hasReadWriteNotLocked
        }),
        groupStringified = JSON.stringify({
          data: retrievedScheduleData,
          type: "group",
          i18n: scheduleI18n,
          canEdit: schedulePermissions.hasReadWriteNotLocked
        });

    startThreadGenerateHTMLForType(plenaryStringified, htmlPlenary, thread1Finished);
    startThreadGenerateHTMLForType(groupStringified, htmlGroup, thread2Finished);
    
    $.when(thread1Finished, thread2Finished).done(function() {
      var html = "<p>DEBUG: Doc-ready: " + endDoc + "ms. Ajax-request: " + endAjax + "ms. Parsed JSON to HTML: " + (+new Date() - startParse) + "ms.</p>" +
                 htmlPlenary.tocHtml + htmlGroup.tocHtml + htmlPlenary.tablesHtml + htmlGroup.tablesHtml;
      
      activitiesElm.html(html === "" ? "Ingen data" : html);
      
      // Edit session
      if(schedulePermissions.hasReadWriteNotLocked) {
        $(document).on("mouseover mouseout", "tbody tr", function(e) {
          var row = $(this);
          var rowStaff = row.find(".course-schedule-table-row-staff");
          var rowEdit = rowStaff.next();
          rowStaff.toggle();
          rowEdit.toggle();
        });
        $(document).on("click", ".course-schedule-table-row-edit", function(e) {
          var row = $(this).closest("tr");
          var futureSimpleDialogs = $.Deferred();
          if(typeof VrtxHtmlDialog === "undefined") {
            $.cachedScript('/vrtx/__vrtx/static-resources/js/vrtx-simple-dialogs.js').done(function() {
              futureSimpleDialogs.resolve();
            });
          } else {
            futureSimpleDialogs.resolve(); 
          }
          $.when(futureSimpleDialogs).done(function() {
            var d = new VrtxHtmlDialog({
              title: scheduleI18n["table-edit"] + " " + scheduleI18n["table-edit-activity"],
              html: "<iframe frameborder='0' style='width: 760px; height: 540px' src='" + window.location.pathname + "?vrtx=admin&mode=editor&action=edit&embed&sessionid=" + row[0].id + "'></iframe>",
              width: 760
            });
            d.open();
          });
          e.stopPropagation();
          e.preventDefault();
        });
      }
      scheduleDeferred.resolve();
    });
  });
  
});

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
  var receivedData = JSON.parse(data);
  htmlRef.tocHtml = receivedData.tocHtml;
  htmlRef.tablesHtml = receivedData.tablesHtml;
  threadRef.resolve();
}

function generateHTMLForType(d) {
  var dta = JSON.parse(d),
      type = dta.type,
      skipTier = type === "plenary",
      scheduleI18n = dta.i18n,
      canEdit = dta.canEdit,
      dtaType = dta.data[type],
      data = dtaType.data,
      now = new Date(),
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
      getDayFunc = function(ed, et) {
        var endTime = new Date(ed[0], ed[1]-1, ed[2], et[0], et[1], 0, 0);
        return { endTime: endTime, day: scheduleI18n["d" + endTime.getDay()] };
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
      var date = getDateFunc(dateTime.sd, dateTime.ed, dateTime.st, dateTime.et);
      
      var sessionId = (skipTier ? type : newTM + "-" + dt.id) + "-" + session.id.replace(/\//g, "-") + "-" + date.postFixId; 

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
      
      tablesHtml += classes !== "" ? "<tr id='" + sessionId + "' class='" + classes + "'>" : "<tr>";
        tablesHtml += "<td>" + date.date + "</td>";
        tablesHtml += "<td>" + day.day + "</td>";
        tablesHtml += "<td>" + getTimeFunc(dateTime.st, dateTime.et) + "</td>";
        tablesHtml += "<td>" + getTitleFunc(session) + "</td>";
        tablesHtml += "<td>" + getPlaceFunc(session) + "</td>";
        tablesHtml += "<td>";
          tablesHtml += "<span class='course-schedule-table-row-staff'>" + getStaffFunc(session) + "</span>";
          tablesHtml += (canEdit ? "<span class='course-schedule-table-row-edit' style='display: none'><a href='javascript:void' class='button'><span>" + scheduleI18n["table-edit"] + "</span></a></span>" : "");
        tablesHtml += "</td>";
      tablesHtml += "</tr>";
    }
  }
  if(i > 0) {
    tablesHtml += "</tbody></table>";
  }
  tocHtml += "</ul>";
  
  return JSON.stringify({ tocHtml: tocHtml, tablesHtml: tablesHtml });
}