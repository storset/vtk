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

function initSchedule() {
  
  var url = window.location.href;
  if(/\/$/.test(url)) {
    url += "index.html";
  }
  url += "?action=course-schedule";
  // Debug: Local development
  url = "/vrtx/__vrtx/static-resources/js/tp-test.json";
  
  var retrievedScheduleData = null;
  var endAjaxTime = 0;
  
  // Get schedule view JSON
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
        $("#activities").html("<p>" + scheduleI18n.noData + "</p>");
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
      if(html === "") html = scheduleI18n.noData;

      $("#activities").html("<p>Total: " + (+new Date() - scheduleStartTime) + "ms <= ((DocReady: " + scheduleDocReadyEndTime +
                            "ms) || (AJAX-complete: " + endAjaxTime + "ms + Threads invoking/serializing: " + (endMakingThreadsTime + htmlPlenary.parseRetrievedJSONTime + htmlGroup.parseRetrievedJSONTime) +
                            "ms + (Plenary: " + htmlPlenary.time + "ms || Group: " + htmlGroup.time + "ms)))</p>" + html);
      
      // Toggle passed sessions
      $(document).on("click", ".course-schedule-table-toggle-passed", function(e) {
        var link = $(this);
        var table = link.next();
        table.toggleClass("hiding-passed"); 
        link.text(table.hasClass("hiding-passed") ? scheduleI18n.tableShowPassed : scheduleI18n.tableHidePassed);
        e.stopPropagation();
        e.preventDefault();
      });
      
      // Edit session
      if(schedulePermissions.hasReadWriteNotLocked) {
        $(document).on("mouseover mouseout", "tbody tr", function(e) {
          $(this).find(".course-schedule-table-row-edit").toggle();
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
          
          refreshRefocused();
          
          e.stopPropagation();
          e.preventDefault();
        });
      }
      scheduleDeferred.resolve();
    });
  });
}

function refreshRefocused() {
  var isVisible = false;
  var delayCheckVisibility = 450;
  var waitForVisibility = setTimeout(function() {
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
}

function startThreadGenerateHTMLForType(data, htmlRef, threadRef) {
  if(window.URL && window.URL.createObjectURL && typeof Blob === "function" && typeof Worker === "function") { // Use own thread
    var workerCode = function(e) {
      postMessage(generateHTMLForType(e.data));
    };
    var blob = new Blob(["onmessage = " + workerCode.toString() + "; " + scheduleUtils.toString() + " " + generateHTMLForType.toString()], {type : 'application/javascript;charset=utf-8'});
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

function scheduleUtils() { 
  /** Private */
  var now = new Date(),
  dateToISO = function(date) {
    var pad = function(number) {
      if ( number < 10 ) {
        return '0' + number;
      }
      return number;
    };
    return date.getUTCFullYear() +
          '-' + pad(date.getUTCMonth() + 1 ) +
          '-' + pad(date.getUTCDate() ) +
          'T' + pad(date.getUTCHours() ) +
          ':' + pad(date.getUTCMinutes() ) +
          ':' + pad(date.getUTCSeconds() ) +
          '.' + (date.getUTCMilliseconds() / 1000).toFixed(3).slice(2, 5) +
          'Z';
  },
  formatName = function(name) {
    var arr = name.split(" ");
    var arrLen = arr.length;
    if(!arrLen) return name;
    
    var val = "";
    for(var i = 0, len = arrLen-1; i < len; i++) {
       val += arr[i].substring(0,1) + ". ";
    }
    return val + arr[i];
  },
  linkAbbr = function(url, title, text) {
    val = "";
    if(url && title) {
      val += "<a title='" + title + "' href='" + url + "'>";
    } else if(url) {
      val += "<a href='" + url + "'>";
    } else if(title) {
      val += "<abbr title='" + title + "'>";
    }
    val += text;
    if(url) {
      val += "</a>";
    } else if(title) {
      val += "</abbr>";
    }
    return val;
  },
  jsonArrayToHtmlList = function(arr) {
    var val = "";
    var arrLen = arr.length;
    if(!arrLen) return val;
    
    if(arrLen > 1) val = "<ul>";
    for(var i = 0; i < arrLen; i++) {
      if(arrLen > 1) val += "<li>";
      var obj = arr[i];
      if(obj.name && obj.url) {
        val += "<a href='" + obj.url + "'>" + formatName(obj.name) + "</a>";
      } else if(obj.title && obj.url) {
        val += "<a href='" + obj.url + "'>" + obj.title + "</a>";
      } else if(obj.url) {
        val += "<a href='" + obj.url + "'>" + obj.url + "</a>";
      } else if(obj.name) {
        val += formatName(obj.name);
      } else if(obj.title) {
        val += obj.title;
      } else if(obj.id) {
        val += obj.id;
      }
      if(arrLen > 1) val += "</li>";
    }
    if(arrLen > 1) val += "</ul>";
    return val;
  },
  parseDate = function(dateString) {
    var m = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2})(?::([0-9]*)(\.[0-9]*)?)?(?:([+-])([0-9]{2}):([0-9]{2}))?/.exec(dateString);
    return { day: m[3], month: m[2], year: m[1], hh: m[4], mm: m[5], tzhh: m[9], tzmm: m[10] };
  };

  /** Public */
  this.now = now,
  this.getDateTime = function(s, e) {
    var startDateTime = parseDate(s);
    var endDateTime = parseDate(e);
    return { start: startDateTime, end: endDateTime };
  };
  this.getDateFormatted = function(dateStart, dateEnd) {
    return dateStart.day + "." + dateStart.month + "." + dateStart.year.substring(2,4);
  },
  this.getDayFormatted = function(dateStart, dateEnd, i18n) {
    // Server Date-string to Date with local timezone to UTC/GMT/Zulu Date-string
    var utcEnd = dateToISO(new Date(dateEnd.year, dateEnd.month - 1, dateEnd.day, dateEnd.hh, dateEnd.mm, 0, 0));
    // Parse Date-string to array
    var utcEndDateTime = parseDate(utcEnd);
    // new Date
    var utcDateEnd = new Date(utcEndDateTime.year, utcEndDateTime.month - 1, utcEndDateTime.day, utcEndDateTime.hh, utcEndDateTime.mm, 0, 0);
    // ms + server timezone
    utcDateEnd = new Date(+utcDateEnd + dateEnd.tzhh * 60000);
    return i18n["d" + utcDateEnd.getDay()];
  };
  this.getTimeFormatted = function(dateStart, dateEnd) {
    return dateStart.hh + ":" + dateStart.mm + "&ndash;" + dateEnd.hh + ":" + dateEnd.mm;
  };
  this.getPostFixId = function(dateStart, dateEnd) {
    return dateStart.day + "-" + dateStart.month + "-" + dateStart.year + "-" + dateStart.hh + "-" + dateStart.mm + "-" + dateEnd.hh + "-" + dateEnd.mm;
  };
  this.getTitle = function(session, isCancelled, i18n) {
    return (isCancelled ? "<span class='course-schedule-table-status'>" + i18n.tableCancelled + "</span>" : "") + (session.vrtxTitle || session.title || session.id);
  };
  this.getPlace = function(session) {
    var val = "";
    var rooms = session.rooms;
    if(rooms && rooms.length) {
      for(var i = 0, len = rooms.length; i < len; i++) {
        if(i > 0) val += "<br/>";
        var room = rooms[i]; 
        val += linkAbbr(room.buildingUrl, room.buildingName, (room.buildingAcronym || room.buildingId));
        val += " ";
        val += linkAbbr(room.roomUrl, room.roomName, room.roomId);
      }
    }
    return val;
  };
  this.getStaff = function(session) {
    var val = "";
    var staff = session.vrtxStaff || session.staff || [];
    var externalStaff = session.vrtxStaffExternal;
    if(externalStaff && externalStaff.length) {
      staff = staff.concat(externalStaff);
    }
    return jsonArrayToHtmlList(staff);
  };
  this.getResources = function(session) {
    var val = jsonArrayToHtmlList(session.vrtxResources || []);
    var resourcesText = session.vrtxResourcesText;
    if(resourcesText && resourcesText.length) {
      val += resourcesText;
    }
    return val;
  };
  this.getTableStartHtml = function(activityId, caption, isAllPassed, hasResources, hasStaff, i18n) {
    var html = "<div class='course-schedule-table-wrapper'>";
    html += "<a class='course-schedule-table-toggle-passed' href='javascript:void(0);'>" + i18n.tableShowPassed + "</a>";
    html += "<table id='" + activityId + "' class='course-schedule-table uio-zebra hiding-passed" + (isAllPassed ? " all-passed" : "") + (hasResources ? " has-resources" : "")  + (hasStaff ? " has-staff" : "") + "'><caption>" + caption + "</caption><thead><tr>";
      html += "<th class='course-schedule-table-date'>" + i18n.tableDate + "</th>";
      html += "<th class='course-schedule-table-day'>" + i18n.tableDay + "</th>";
      html += "<th class='course-schedule-table-time'>" + i18n.tableTime + "</th>";
      html += "<th class='course-schedule-table-title'>" + i18n.tableTitle + "</th>";
      if(hasResources) html += "<th class='course-schedule-table-resources'>" + i18n.tableResources + "</th>";
      html += "<th class='course-schedule-table-place'>" + i18n.tablePlace + "</th>";
      if(hasStaff)     html += "<th class='course-schedule-table-staff'>" + i18n.tableStaff + "</th>";
    html += "</tr></thead><tbody>";
    return html;
  };
  this.getTableEndHtml = function() {
    return "</tbody></table></div>";
  };
  this.splitThirds = function(arr, title) {
    var html = "<p>" + title + "</p>",
        len = arr.length,
        split1 = Math.ceil(len / 3),
        split2 = split1 + Math.ceil((len - split1) / 2);
    html += "<div class='course-schedule-thirds'><ul class='thirds-left'>";
    for(var i = 0; i < len; i++) {
      if(i === split1) html += "</ul><ul class='thirds-middle'>";
      if(i === split2) html += "</ul><ul class='thirds-right'>";
      html += arr[i];
    }
    html += "</ul></div>";
    return html;
  };
  this.editLink = function(clazz, html, displayEditLink, canEdit, i18n) {
    var startHtml = "<td class='" + clazz + "'>" + html;
    var endHtml = "</td>"
    if(!displayEditLink || !canEdit) return startHtml + endHtml;

    return startHtml + "<span class='course-schedule-table-row-edit' style='display: none'><a href='javascript:void'>" + i18n.tableEdit + "</a></span>" + endHtml;
  };
}

function generateHTMLForType(d) {
  var dta = JSON.parse(d),
  
      data = dta.data["activities"],
      type = dta.type,
      scheduleI18n = dta.i18n,
      canEdit = dta.canEdit,
      skipTier = type === "plenary",
      startGenHtmlForTypeTime = new Date(),
      utils = new scheduleUtils(),
      
      tocHtml = "",
      tablesHtml = "";
  
  if(!data) return { tocHtml: "", tablesHtml: "" };
  var dataLen = data.length;
  if(!dataLen) return { tocHtml: "", tablesHtml: "" };
  
  tocHtml += "<h2 class='accordion'>" + scheduleI18n["header-" + type] + "</h2>";
  if(skipTier) tocHtml += "<ul>";
  
  // Scope all variables to function (and outside loops)
  var i, j, len, dt, id, dtShort, lastDtShort = "", dtLong, forCode = "for", isFor,
      activityId, caption, sessions, sessionsPreprocessed, sessionsHtml, resourcesCount, staffCount, passedCount, sessionsCount,
      session, dateTime, staff, date, day, time, sessionId, classes, tocTime, tocTimeCount, tocTimeMax = 3,
      newTocTime, isCancelled, tocHtmlArr = [];

  for(i = 0; i < dataLen; i++) {
    dt = data[i];
    
    id = dt.id;
    dtShort = dt.teachingMethod.toLowerCase();
    dtLong = dt.teachingMethodName;
    isFor = dtShort === forCode;
    
    if(!isFor || i == 0) {
      activityId = isFor ? dtShort : dtShort + "-" + dt.id;
      sessionsHtml = "";
      resourcesCount = 0;
      staffCount = 0;
      passedCount = 0;
      sessionsCount = 0;
      sessions = [];
      sessionsPreprocessed = [];
      if(skipTier) {
        caption = dtLong;
      } else {
        groupCount = id.split("-")[1];
        caption = dtLong + " - " + scheduleI18n.groupTitle.toLowerCase() + " " + groupCount;
      }
      tocTime = "";
      tocTimeCount = 0;
    }
    
    // Add together sessions from sequences
    for(j = 0, len = dt.sequences.length; j < len; j++) {
      sessions = sessions.concat(dt.sequences[j].sessions);
    }

    if(!isFor || (isFor && (!data[i+1] || data[i+1].teachingMethod.toLowerCase() !== forCode))) {
      // Sort sessions
      sessions.sort(function(a,b) {
        return parseInt(a.dtStart.split("T")[0].split("-").join(""), 10) - parseInt(b.dtStart.split("T")[0].split("-").join(""), 10);
      });
      
      // Preprocess sessions and store what has been checked
      for(j = 0, len = sessions.length; j < len; j++) {
        session = sessions[j];
        sessionsPreprocessed[j] = {
          "staff": utils.getStaff(session),
          "resources": utils.getResources(session)
        };
        if(sessionsPreprocessed[j].staff)         staffCount++;
        if(sessionsPreprocessed[j].resources)     resourcesCount++;
      }
      
      // Generate sessions HTML
      for(j = 0, len = sessions.length; j < len; j++) {
        session = sessions[j];
        dateTime = utils.getDateTime(session.dtStart, session.dtEnd);
        sessionId = (skipTier ? type : dtShort + "-" + id) + "-" + session.id.replace(/\//g, "-") + "-" + utils.getPostFixId(dateTime.start, dateTime.end);
        isCancelled = (session.status && session.status === "cancelled") ||
                      (session.vrtxStatus && session.vrtxStatus === "cancelled");

        classes = (j & 1) ? "even" : "odd";     
        if(isCancelled) {
          if(classes !== "") classes += " ";
          classes += "cancelled";
        }
        /*
          if(dateTime.end < utils.now) {
            if(classes !== "") classes += " ";
            classes += "passed";
            passedCount++;
          }
        */
        sessionsCount++;
        
        date = utils.getDateFormatted(dateTime.start, dateTime.end);
        day = utils.getDayFormatted(dateTime.start, dateTime.end, scheduleI18n);
        time = utils.getTimeFormatted(dateTime.start, dateTime.end);
        
        sessionsHtml += classes !== "" ? "<tr id='" + sessionId + "' class='" + classes + "'>" : "<tr>";
          sessionsHtml += "<td class='course-schedule-table-date'>" + date + "</td>";
          sessionsHtml += "<td class='course-schedule-table-day'>" + day + "</td>";
          sessionsHtml += "<td class='course-schedule-table-time'>" + time + "</td>";
          sessionsHtml += "<td class='course-schedule-table-title'>" + utils.getTitle(session, isCancelled, scheduleI18n) + "</td>";
          if(resourcesCount) sessionsHtml += "<td class='course-schedule-table-resources'>" + sessionsPreprocessed[j].resources + "</td>";
          sessionsHtml += utils.editLink("course-schedule-table-place", utils.getPlace(session), !staffCount, canEdit, scheduleI18n);
          if(staffCount)     sessionsHtml += utils.editLink("course-schedule-table-staff", sessionsPreprocessed[j].staff, staffCount, canEdit, scheduleI18n);
        sessionsHtml += "</tr>";
      
        if(tocTimeCount < tocTimeMax) {
          newTocTime = day.toLowerCase().substring(0,3) + " " + time;
          if(tocTime.indexOf(newTocTime) === -1) {
            if(tocTimeCount > 0) tocTime += ", ";
            tocTime += newTocTime;
            tocTimeCount++;
          }
        }
      }
      tablesHtml += utils.getTableStartHtml(activityId, caption, (passedCount === sessionsCount), resourcesCount, staffCount, scheduleI18n) + sessionsHtml + utils.getTableEndHtml();
      
      // Generate ToC
      tocTime = tocTime.replace(/,([^,]+)$/, " " + scheduleI18n.and + "$1");
      if(skipTier) {
        tocHtml += "<li><a href='#" + activityId + "'>" + dtLong + "</a> - " + tocTime + "</li>";
      } else {
        tocHtmlArr.push("<li><a href='#" + activityId + "'>" + scheduleI18n.groupTitle + " " + groupCount + "</a> - " + tocTime + "</li>");
        if((dtShort !== lastDtShort && i > 0) || (i === (dataLen - 1))) {
          tocHtml += utils.splitThirds(tocHtmlArr, dtLong);
          tocHtmlArr = [];
        }
      }
    }
    
    lastDtShort = dtShort;
  }
  
  if(skipTier) tocHtml += "</ul>";
  
  return { tocHtml: tocHtml, tablesHtml: tablesHtml, time: (+new Date() - startGenHtmlForTypeTime) };
}