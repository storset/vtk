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
  // Debug: Local development
  url = "/vrtx/__vrtx/static-resources/js/tp-test.json";
  
  var endAjaxTime = 0;
  
  // Get schedule JSON
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
          data: retrievedScheduleData["plenary"],
          type: "plenary",
          i18n: scheduleI18n,
          canEdit: schedulePermissions.hasReadWriteNotLocked
        }),
        groupStringified = JSON.stringify({
          data: retrievedScheduleData["group"],
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
        table.toggleClass("hiding-passed"); 
        link.text(table.hasClass("hiding-passed") ? scheduleI18n["table-show-passed"] : scheduleI18n["table-hide-passed"]);
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
          var popupWindowInternal = function (w, h, url, name) {
            var screenWidth = window.screen.width;
            var screenHeight = window.screen.height;
            var left = (screenWidth - w) / 2;
            var top = (screenHeight - h) / 2;
            var openedWindow = window.open(url, name, "height=" + h + ", width=" + w + ", left=" + left + ", top=" + top + ", status=no, resizable=no, toolbar=no, menubar=no, scrollbars=yes, location=no, directories=no");
            openedWindow.focus();
            return openedWindow;
          };
          var openedEditActivityWindow = popupWindowInternal(850, 680, window.location.pathname + "?vrtx=admin&mode=editor&action=edit&embed&sessionid=" + row[0].id, "editActivity");
          
          // Refresh when gets refocused
          var isVisible = false;
          var delayCheckVisibility = 450;
          var waitVisibility = setTimeout(function() {
            if(document.addEventListener) {
              var detectVisibilityChange = function() {
                isVisible = !document.hidden;
                if(isVisible && document.removeEventListener) {
                  document.removeEventListener("visibilitychange", detectVisibilityChange);
                }
              }
              document.addEventListener("visibilitychange", detectVisibilityChange, false);
            }
          }, delayCheckVisibility);
          var waitForClose = setTimeout(function() {
            if(document.hasFocus() || isVisible) {
              window.location.reload(1);
            } else {
              setTimeout(arguments.callee, 50); 
            }
          }, delayCheckVisibility);
          
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
    var blob = new Blob(["onmessage = " + workerCode.toString() + "; " + generateHTMLForType.toString()], {type : 'application/javascript;charset=utf-8'});
    var blobURL = window.URL.createObjectURL(blob);

    try {
      var worker = new Worker(blobURL);
      worker.onmessage = function(e) {
        finishedThreadGenerateHTMLForType(e.data, htmlRef, threadRef);
      };
      worker.onerror = function(err) {
        if(/^Failed to load script/.test(err.message)) { // Firefox CPS
          finishedThreadGenerateHTMLForType(generateHTMLForType(data), htmlRef, threadRef); 
        } else {
          finishedThreadGenerateHTMLForType({ tocHtml: "", tablesHtml: "<p>" + err.message + "</p>", time: 0 }, htmlRef, threadRef);
        }
      };
      worker.postMessage(data);

      if(window.URL.revokeObjectURL) window.URL.revokeObjectURL(blobURL);
    } catch(err) { // IE10-IE11 SecurityError (https://connect.microsoft.com/IE/feedback/details/801810/web-workers-from-blob-urls-in-ie-10-and-11) 
      finishedThreadGenerateHTMLForType(generateHTMLForType(data), htmlRef, threadRef);
    }
  } else { // Use main thread
    finishedThreadGenerateHTMLForType(generateHTMLForType(data), htmlRef, threadRef);
  }
}

function finishedThreadGenerateHTMLForType(data, htmlRef, threadRef) {
  var startFinishedCode = +new Date();
  htmlRef.tocHtml = data.tocHtml;
  htmlRef.tablesHtml = data.tablesHtml;
  htmlRef.time = data.time;
  htmlRef.parseRetrievedJSONTime = (+new Date() - startFinishedCode);
  threadRef.resolve();
}

function generateHTMLForType(d) {
  var now = new Date(),
      startGenHtmlForTypeTime = +now,
      dta = JSON.parse(d),
      data = dta.data["activities"],
      type = dta.type,
      scheduleI18n = dta.i18n,
      canEdit = dta.canEdit,
      skipTier = type === "plenary",
      getDateTimeFunc = function(s, e, i18n) { // Supports session over multiple days
        var sdt = s.split("T");
        var sd = sdt[0].split("-");
        var st = sdt[1].split(".")[0].split(":");
        var edt = e.split("T");
        var ed = edt[0].split("-");
        var et = edt[1].split(".")[0].split(":");
        
        // TODO: +0100 => client timezone
        var endDateTime = new Date(ed[0], ed[1]-1, ed[2], et[0], et[1], 0, 0);
        if(sd[0] != ed[0] || sd[1] != ed[1] || sd[2] != ed[2]) {
          var date = sd[2] + "." + sd[1] + "." + sd[0] + "&ndash;" + ed[2] + "." + ed[1] + "." + ed[0];
          var startDateTime = new Date(sd[0], sd[1]-1, sd[2], st[0], st[1], 0, 0);
          var day = i18n["d" + startDateTime.getDay()] + "&ndash;" + i18n["d" + endDateTime.getDay()];
        } else {
          var date = sd[2] + "." + sd[1] + "." + sd[0];
          var day = i18n["d" + endDateTime.getDay()];
        }
        var time = st[0] + ":" + st[1] + "&ndash;" + et[0] + ":" + et[1];
        var postFixId = sd[2] + "-" + sd[1] + "-" + sd[0] + "-" + st[0] + "-" + st[1] + "-" + et[0] + "-" + et[1];
        
        return { date: date, endDateTime: endDateTime, day: day, time: time, postFixId: postFixId };
      },
      getTitleFunc = function(session, isCancelled, i18n) {
        return (isCancelled ? "<span class='course-schedule-table-status'>" + i18n["table-cancelled"] + "</span>" : "") + (session["vrtxTitle"] || session.title || session.id);
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
        var staff = session["vrtxStaff"] || session.staff;
        if(staff && staff.length) {
          for(var i = 0, len = staff.length; i < len; i++) {
            if(i > 0) val += "<br/>";
            val += staff[i].id;
          }
        }
        return val;
      },
      getResourcesFunc = function(session) {
        var val = "";
        var resources = session["vrtxResources"];
        if(resources && resources.length) {
          var resourcesLen = resources.length;
          if(resourcesLen > 1) val = "<ul>";
          for(var i = 0; i < resourcesLen; i++) {
            if(resourcesLen > 1) val += "<li>";
            if(resources[i].title && resources[i].url) {
              val += "<a href='" + resources[i].url + "'>" + resources[i].title + "</a>";
            } else if(resources[i].url) {
              val += "<a href='" + resources[i].url + "'>" + resources[i].url + "</a>";
            } else if(resources[i].title) {
              val += resources[i].title;
            }
            if(resourcesLen > 1) val += "</li>";
          }
          if(resourcesLen > 1) val += "</ul>";
        }
        return val;
      },
      getTableStartHtml = function(activityId, caption, isAllPassed, i18n) {
        var html = "<div class='course-schedule-table-wrapper'>";
        html += "<a class='course-schedule-table-toggle-passed' href='javascript:void(0);'>" + i18n["table-show-passed"] + "</a>";
        html += "<table id='" + activityId + "' class='course-schedule-table uio-zebra hiding-passed" + (isAllPassed ? " all-passed" : "") + "'><caption>" + caption + "</caption><thead><tr>";
          html += "<th class='course-schedule-table-date'>" + i18n["table-date"] + "</th><th class='course-schedule-table-day'>" + i18n["table-day"] + "</th>";
          html += "<th class='course-schedule-table-time'>" + i18n["table-time"] + "</th><th class='course-schedule-table-title'>" + i18n["table-title"] + "</th>";
          html += "<th class='course-schedule-table-resources'>" + i18n["table-resources"] + "</th><th class='course-schedule-table-place'>" + i18n["table-place"] + "</th>";
          html += "<th class='course-schedule-table-staff'>" + i18n["table-staff"] + "</th>";
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
  if(skipTier) tocHtml += "<ul>";
  
  // Scope all variables to function (and outside loops)
  var i, j, len2, k, len3, split1, split1, dt, sessions, id, dtShort, lastDtShort = "", dtLong, isFor, activityId, caption, sessionsHtml, passedCount, sessionsCount,
      session, dateTime, sessionId, classes, tocTime, tocTimeCount, tocTimeMax = 3, newTocTime, isCancelled, tocHtmlArr = [];
  for(i = 0; i < dataLen; i++) {
    dt = data[i];
    sessions = dt.sessions;
    id = dt.id;
    dtShort = dt.teachingmethod.toLowerCase();
    isFor = dtShort === "for";
    dtLong = dt.teachingmethodname;
    if(!isFor || i == 0) {
      if(lastDtShort === "for") {
        tablesHtml += getTableStartHtml(activityId, caption, (passedCount === sessionsCount), scheduleI18n) + sessionsHtml + getTableEndHtml();
      }
      activityId = isFor ? dtShort : dtShort + "-" + dt.id;
      sessionsHtml = "";
      passedCount = 0;
      sessionsCount = 0;
      if(skipTier) {
        caption = dtLong;
      } else {
        groupCount = id.split("-")[1];
        caption = dtLong + " - " + scheduleI18n["group-title"].toLowerCase() + " " + groupCount;
      }
      tocTime = "";
      tocTimeCount = 0;
    }
    // Generate sessions HTML
    for(j = 0, len2 = sessions.length; j < len2; j++) {
      session = sessions[j];
      dateTime = getDateTimeFunc(session.dtstart, session.dtend, scheduleI18n);
      sessionId = (skipTier ? type : dtShort + "-" + id) + "-" + session.id.replace(/\//g, "-") + "-" + dateTime.postFixId;
      isCancelled = (session.status && session.status === "cancelled") ||
                    (session["vrtxStatus"] && session["vrtxStatus"] === "cancelled");
      
      classes = "";
      if(j & 1) classes = "even";
      if(isCancelled) {
        if(classes !== "") classes += " ";
        classes += "cancelled";
      }
      if(dateTime.endDateTime < now) {
        if(classes !== "") classes += " ";
        classes += "passed";
        passedCount++;
      }
      sessionsCount++;
      
      sessionsHtml += classes !== "" ? "<tr id='" + sessionId + "' class='" + classes + "'>" : "<tr>";
        sessionsHtml += "<td class='course-schedule-table-date'>" + dateTime.date + "</td>";
        sessionsHtml += "<td class='course-schedule-table-day'>" + dateTime.day + "</td>";
        sessionsHtml += "<td class='course-schedule-table-time'>" + dateTime.time + "</td>";
        sessionsHtml += "<td class='course-schedule-table-title'>" + getTitleFunc(session, isCancelled, scheduleI18n) + "</td>";
        sessionsHtml += "<td class='course-schedule-table-resources'>" + getResourcesFunc(session) + "</td>";
        sessionsHtml += "<td class='course-schedule-table-place'>" + getPlaceFunc(session) + "</td>";
        sessionsHtml += "<td class='course-schedule-table-staff'>";
          sessionsHtml += "<span class='course-schedule-table-row-staff'>" + getStaffFunc(session)  + "</span>";
          sessionsHtml += (canEdit ? "<span class='course-schedule-table-row-edit' style='display: none'><a href='javascript:void'>" + scheduleI18n["table-edit"] + "</a></span>" : "");
        sessionsHtml += "</td>"
      sessionsHtml += "</tr>";
    
      if(tocTimeCount < tocTimeMax && !isCancelled) {
        newTocTime = dateTime.day.toLowerCase().substring(0,3) + " " + dateTime.time;
        if(tocTime.indexOf(newTocTime) === -1) {
          if(tocTimeCount > 0) tocTime += ", ";
          tocTime += newTocTime;
          tocTimeCount++;
        }
      }
    }
    if(!isFor) {
      tablesHtml += getTableStartHtml(activityId, caption, (passedCount === sessionsCount), scheduleI18n) + sessionsHtml + getTableEndHtml();
    }
    
    // Generate ToC HTML
    if(!isFor || (isFor && (!data[i+1] || data[i+1].teachingmethod.toLowerCase() !== "for"))) {
      tocTime = tocTime.replace(/,([^,]+)$/, " " + scheduleI18n["and"] + "$1");
      if(skipTier) {
        tocHtml += "<li><a href='#" + activityId + "'>" + dtLong + "</a> - " + tocTime + "</li>";
      } else {
        tocHtmlArr.push("<li><a href='#" + activityId + "'>" + scheduleI18n["group-title"] + " " + groupCount + "</a> - " + tocTime + "</li>");
        if((dtShort !== lastDtShort && i > 0) || (i === (dataLen - 1))) {
          len3 = tocHtmlArr.length;
          split1 = Math.ceil(len3 / 3);
          split2 = split1 + Math.ceil((len3 - split1) / 2);
          tocHtml += "<p>" + dtLong + "</p>";
          // TODO: fix .thirds-<pos> outside frontpage
          tocHtml += "<div class='course-schedule-thirds'><ul class='thirds-left'>";
          for(k = 0; k < len3; k++) {
            if(k === split1) tocHtml += "</ul><ul class='thirds-middle'>";
            if(k === split2) tocHtml += "</ul><ul class='thirds-right'>";
            tocHtml += tocHtmlArr[k];
          }
          tocHtml += "</ul></div>";
          tocHtmlArr = [];
        }
      }
    }
    
    lastDtShort = dtShort;
  }
  if(dtShort === "for") {
    tablesHtml += getTableStartHtml(activityId, caption, (passedCount === sessionsCount), scheduleI18n) + sessionsHtml + getTableEndHtml();
  }
  
  if(skipTier) tocHtml += "</ul>";
  
  return { tocHtml: tocHtml, tablesHtml: tablesHtml, time: (+new Date() - startGenHtmlForTypeTime) };
}