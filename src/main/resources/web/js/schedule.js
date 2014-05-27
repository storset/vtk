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
  var retrievedScheduleDeferred = $.Deferred();
  var retrievedScheduleData = null;
  var endAjaxTime = 0;
  // Don't cache if has returned from editing an activity
  var hasEditedKey = "hasEditedSession";
  var useCache = true;
  if(window.localStorage && window.localStorage.getItem(hasEditedKey)) {
    window.localStorage.removeItem(hasEditedKey);
    useCache = false;
  }
  
  var url = location.protocol + "//" + location.host + location.pathname;
  if(/\/$/.test(url)) {
    url += "index.html";
  }
  url += "?action=course-schedule";
  // Debug: Local development
  // url = "/vrtx/__vrtx/static-resources/js/tp-test.json";
  
  // GET JSON
  $.ajax({
    type: "GET",
    url: url + (!useCache ? "&t=" + (+new Date()) : ""),
    dataType: "json",
    cache: useCache,
    success: function(data, xhr, textStatus) {
      retrievedScheduleData = data;
    }
  }).always(function() {
    retrievedScheduleDeferred.resolve();
    endAjaxTime = +new Date() - scheduleStartTime;
  });
  
  $.when(scheduleDocumentReady).done(function() {
    $("#disabled-js").hide();
    loadingUpdate(scheduleI18n.loadingRetrievingData);
  });
  
  $.when(retrievedScheduleDeferred).done(function() {
    if(retrievedScheduleData == null) {
      $.when(scheduleDocumentReady).done(function() {
        $("#activities").attr("aria-busy", "error").html("<p>" + scheduleI18n.noData + "</p>");
      });
      scheduleDeferred.resolve();
      return;
    }
    
    loadingUpdate(scheduleI18n.loadingGenerating);
    var startMakingThreadsTime = +new Date();
    
    var thread1Finished = $.Deferred(),
        thread2Finished = $.Deferred(),
        htmlPlenary = { tocHtml: "", tablesHtml: "", time: 0 },
        htmlGroup = { tocHtml: "", tablesHtml: "", time: 0 },
        plenaryData = retrievedScheduleData["plenary"],
        groupData = retrievedScheduleData["group"];
        
    if(plenaryData) {
      startThreadGenerateHTMLForType(JSON.stringify({
        data: plenaryData,
        type: "plenary",
        i18n: scheduleI18n,
        canEdit: schedulePermissions.hasReadWriteNotLocked
      }), htmlPlenary, thread1Finished);
    } else {
      thread1Finished.resolve();
    }
    if(plenaryData) {
      startThreadGenerateHTMLForType(JSON.stringify({
        data: groupData,
        type: "group",
        i18n: scheduleI18n,
        canEdit: schedulePermissions.hasReadWriteNotLocked
      }), htmlGroup, thread2Finished);
    } else {
      thread2Finished.resolve();
    }

    var endMakingThreadsTime = +new Date() - startMakingThreadsTime;
    
    $.when(thread1Finished, thread2Finished, scheduleDocumentReady).done(function() {
      var html = htmlPlenary.tocHtml + htmlGroup.tocHtml + htmlPlenary.tablesHtml + htmlGroup.tablesHtml;
      if(html === "") {
        $("#activities").attr("aria-busy", "error").html(scheduleI18n.noData);
      } else {
        $("#activities").attr("aria-busy", "false").html(/*"<p>Total: " + (+new Date() - scheduleStartTime) + "ms <= ((DocReady: " + scheduleDocReadyEndTime +
                            "ms) || (AJAX-complete: " + endAjaxTime + "ms + Threads invoking/serializing: " + (endMakingThreadsTime + htmlPlenary.parseRetrievedJSONTime + htmlGroup.parseRetrievedJSONTime) +
                            "ms + (Plenary: " + htmlPlenary.time + "ms || Group: " + htmlGroup.time + "ms)))</p>" + */ html);
      }
      
      // Toggle passed sessions
      $(document).on("click", ".course-schedule-table-toggle-passed", function(e) {
        var link = $(this);
        var table = link.prev();
        table.toggleClass("hiding-passed"); 
        link.text(table.hasClass("hiding-passed") ? scheduleI18n.tableShowPassed : scheduleI18n.tableHidePassed);
        e.stopPropagation();
        e.preventDefault();
      });
      
      // Edit session
      if(schedulePermissions.hasReadWriteNotLocked) {
        $(document).on("mouseover mouseout focusin focusout", "tbody tr", function(e) {
          var fn = (e.type === "mouseover" || e.type === "focusin") ? "addClass" : "removeClass";
          $(this).find(".course-schedule-table-edit-wrapper")[fn]("visible");
        });
        $(document).on("click", "a.course-schedule-table-edit-link", function(e) {
          var row = $(this).closest("tr");
          var editUrl = window.location.pathname;
          if(/\/$/.test(editUrl)) {
            editUrl += "index.html";
          }
          var openedEditWindow = popupEditWindow(850, 680, editUrl + "?vrtx=admin&mode=editor&action=edit&embed&sessionid=" + row[0].id, "editActivity");
          refreshWhenRefocused();
          e.stopPropagation();
          e.preventDefault();
        });
      }
      
      scheduleDeferred.resolve();
    });
  });
}

function loadingUpdate(msg) {
  var loader = $("#loading-message");
  if(!loader.length) {
    var loaderHtml = "<p id='loading-message'>" + msg + "...</p>";
    $("#activities").attr("aria-busy", "true").append(loaderHtml);
  } else {
    loader.text(msg + "...");
  }
}

function popupEditWindow(w, h, url, name) {
  var screenWidth = window.screen.width;
  var screenHeight = window.screen.height;
  var left = (screenWidth - w) / 2;
  var top = (screenHeight - h) / 2;
  var openedWindow = window.open(url, name, "height=" + h + ", width=" + w + ", left=" + left + ", top=" + top + ", status=no, resizable=no, toolbar=no, menubar=no, scrollbars=yes, location=no, directories=no");
  openedWindow.focus();
  return openedWindow;
}

function refreshWhenRefocused() {
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
      if(window.localStorage) {
        window.localStorage.setItem(hasEditedKey, "true");
      }
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
  var self = this;

  /** Private */

  /* toISOString() shim: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toISOString
     - Without doing things to Date-object */
  var dateToISO = function(date) {
    if(!Date.prototype.toISOString) {
      var pad = function(number) {
        if (number < 10) {
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
    } else {
      return date.toISOString();
    }
  },
  parseDate = function(dateString) {
    var m = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2})(?::([0-9]*)(\.[0-9]*)?)?(?:([+-])([0-9]{2}):([0-9]{2}))?/.exec(dateString);
    return { year: m[1], month: m[2], date: m[3], hh: m[4], mm: m[5], tzhh: m[9], tzmm: m[10] };
  },
  getDateUTC = function(year, month, date, hh, mm) {
    var utcDateString = dateToISO(new Date(year, month, date, hh, mm, 0, 0));
    var utcDateParsed = parseDate(utcDateString);
    return new Date(utcDateParsed.year, utcDateParsed.month - 1, utcDateParsed.date, utcDateParsed.hh, utcDateParsed.mm, 0, 0);
  },
  getNowUTC = function() {
    var localNow = new Date();
    var utcNow = getDateUTC(localNow.getFullYear(), localNow.getMonth(), localNow.getDate(), localNow.getHours(), localNow.getMinutes());
    return utcNow;
  },
  formatName = function(name) {
    var arr = name.replace(/ +(?= )/g,'').split(" ");
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
  };

  /** Public */
  this.getNowUTC = getNowUTC(); // Cache
  this.getDateTime = function(s, e) {
    var startDateTime = parseDate(s);
    var endDateTime = parseDate(e);
    return { start: startDateTime, end: endDateTime };
  };
  this.getDateFormatted = function(dateStart, dateEnd) {
    return dateStart.date + "." + dateStart.month + "." + dateStart.year.substring(2,4);
  },
  this.getEndUTCDayFormatted = function(dateStart, dateEnd, i18n) {
    var utcDateEnd = getDateUTC(dateEnd.year, dateEnd.month - 1, dateEnd.date, dateEnd.hh, dateEnd.mm);
    var dateEnd = new Date(+utcDateEnd + dateEnd.tzhh * 60000); // ms + server timezone
    return { endUTC: utcDateEnd, day: i18n["d" + dateEnd.getDay()] };
  };
  this.getTimeFormatted = function(dateStart, dateEnd) {
    return dateStart.hh + ":" + dateStart.mm + "&ndash;" + dateEnd.hh + ":" + dateEnd.mm;
  };
  this.getPostFixId = function(dateStart, dateEnd) {
    return dateStart.date + "-" + dateStart.month + "-" + dateStart.year + "-" + dateStart.hh + "-" + dateStart.mm + "-" + dateEnd.hh + "-" + dateEnd.mm;
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
    var html = "<div tabindex='0' class='course-schedule-table-wrapper'>";
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
  this.getTableEndHtml = function(i18n) {
    return "</tbody></table><a class='course-schedule-table-toggle-passed' href='javascript:void(0);'>" + i18n.tableShowPassed + "</a></div>";
  };
  this.splitThirds = function(arr, title) {
    var html = "<p>" + title + "</p>",
        len = arr.length,
        split1 = Math.ceil(len / 3),
        split2 = split1 + Math.ceil((len - split1) / 2);
    html += "<div class='course-schedule-toc-thirds'><ul class='thirds-left'>";
    for(var i = 0; i < len; i++) {
      if(i === split1) html += "</ul><ul class='thirds-middle'>";
      if(i === split2) html += "</ul><ul class='thirds-right'>";
      html += arr[i];
    }
    html += "</ul></div>";
    return html;
  };
  this.editLink = function(clazz, html, displayEditLink, canEdit, i18n) {
    var startHtml = "<td class='" + clazz + ((displayEditLink && canEdit) ? " course-schedule-table-edit-cell" : "") + "'>";
    var endHtml = "</td>"
    if(!displayEditLink || !canEdit) return startHtml + html + endHtml;

    return startHtml + "<div class='course-schedule-table-edit-wrapper'>" + html + "<a class='button course-schedule-table-edit-link' href='javascript:void'><span>" + i18n.tableEdit + "</span></a></div>" + endHtml;
  };
}

function generateHTMLForType(d) {
  var dta = JSON.parse(d),
      data = dta.data["activities"],
      tocHtml = "",
      tablesHtml = "";
  
  if(!data) return { tocHtml: tocHtml, tablesHtml: tablesHtml };
  var dataLen = data.length;
  if(!dataLen) return { tocHtml: tocHtml, tablesHtml: tablesHtml };
  
  var type = dta.type,
      scheduleI18n = dta.i18n,
      canEdit = dta.canEdit,
      skipTier = type === "plenary",
      startGenHtmlForTypeTime = new Date(),
      utils = new scheduleUtils(),
      lastDtShort = "",
      forCode = "for",
      tocTimeMax = 3,
      tocHtmlArr = [];
  
  tocHtml += "<h2 class='course-schedule-toc-title accordion'>" + scheduleI18n["header-" + type] + "</h2>";
  tocHtml += "<div class='course-schedule-toc-content'>";
  if(skipTier) tocHtml += "<ul>";
  
  for(var i = 0; i < dataLen; i++) {
    var dt = data[i];
    
    var id = dt.id;
    var dtShort = dt.teachingMethod.toLowerCase();
    var dtLong = dt.teachingMethodName;
    var isFor = dtShort === forCode;
    
    if(!isFor || i == 0) {
      var activityId = isFor ? dtShort : dtShort + "-" + dt.id;
      var sessionsHtml = "";
      var resourcesCount = 0;
      var staffCount = 0;
      var passedCount = 0;
      var sessionsCount = 0;
      var sessions = [];
      var sessionsPreprocessed = [];
      if(skipTier) {
        var caption = dtLong;
      } else {
        groupCount = id.split("-")[1];
        var caption = dtLong + " - " + scheduleI18n.groupTitle.toLowerCase() + " " + groupCount;
      }
      var tocTime = "";
      var tocTimeCount = 0;
    }
    
    // Add together sessions from sequences
    for(var j = 0, len = dt.sequences.length; j < len; j++) {
      sessions = sessions.concat(dt.sequences[j].sessions);
    }

    if(!isFor || (isFor && (!data[i+1] || data[i+1].teachingMethod.toLowerCase() !== forCode))) {
      // Sort sessions
      sessions.sort(function(a,b) { // XXX: avoid parsing datetime for sessions twice
        var dateTimeA = utils.getDateTime(a.dtStart, a.dtEnd);
        var startA = dateTimeA.start;
        var endA = dateTimeA.end;
        var a = startA.year + "" + startA.month + "" + startA.date + "" + startA.hh + "" + startA.mm + "" + endA.hh + "" + endA.mm;
        
        var dateTimeB = utils.getDateTime(b.dtStart, b.dtEnd);
        var startB = dateTimeB.start;
        var endB = dateTimeB.end;
        var b = startB.year + "" + startB.month + "" + startB.date + "" + startB.hh + "" + startB.mm + "" + endB.hh + "" + endB.mm;
        
        return parseInt(a, 10) - parseInt(b, 10);
      });
      
      // Preprocess sessions and store what has been checked
      for(j = 0, len = sessions.length; j < len; j++) {
        var session = sessions[j];
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
        
        var dateTime = utils.getDateTime(session.dtStart, session.dtEnd);
        var date = utils.getDateFormatted(dateTime.start, dateTime.end);
        var endUTCDay = utils.getEndUTCDayFormatted(dateTime.start, dateTime.end, scheduleI18n);
        var day = endUTCDay.day;
        var endUTC = endUTCDay.endUTC;
        var time = utils.getTimeFormatted(dateTime.start, dateTime.end);
        
        var sessionId = (skipTier ? type : dtShort + "-" + id) + "-" + session.id.replace(/\//g, "-") + "-" + utils.getPostFixId(dateTime.start, dateTime.end);
        var isCancelled = (session.status && session.status === "cancelled") ||
                          (session.vrtxStatus && session.vrtxStatus === "cancelled");

        var classes = (j & 1) ? "even" : "odd";     
        if(isCancelled) {
          if(classes !== "") classes += " ";
          classes += "cancelled";
        }
        if(endUTC < utils.getNowUTC) {
          if(classes !== "") classes += " ";
          classes += "passed";
          passedCount++;
        }
        sessionsCount++;
        
        sessionsHtml += classes !== "" ? "<tr tabindex='0' id='" + sessionId + "' class='" + classes + "'>" : "<tr>";
          sessionsHtml += "<td class='course-schedule-table-date'>" + date + "</td>";
          sessionsHtml += "<td class='course-schedule-table-day'>" + day + "</td>";
          sessionsHtml += "<td class='course-schedule-table-time'>" + time + "</td>";
          sessionsHtml += "<td class='course-schedule-table-title'>" + utils.getTitle(session, isCancelled, scheduleI18n) + "</td>";
          if(resourcesCount) sessionsHtml += "<td class='course-schedule-table-resources'>" + sessionsPreprocessed[j].resources + "</td>";
          sessionsHtml += utils.editLink("course-schedule-table-place", utils.getPlace(session), !staffCount, canEdit, scheduleI18n);
          if(staffCount)     sessionsHtml += utils.editLink("course-schedule-table-staff", sessionsPreprocessed[j].staff, staffCount, canEdit, scheduleI18n);
        sessionsHtml += "</tr>";
      
        if(tocTimeCount < tocTimeMax) {
          var newTocTime = day.toLowerCase().substring(0,3) + " " + time;
          if(tocTime.indexOf(newTocTime) === -1) {
            if(tocTimeCount > 0) tocTime += ", ";
            tocTime += newTocTime;
            tocTimeCount++;
          }
        }
      }
      tablesHtml += utils.getTableStartHtml(activityId, caption, (passedCount === sessionsCount), resourcesCount, staffCount, scheduleI18n) + sessionsHtml + utils.getTableEndHtml(scheduleI18n);
      
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
  tocHtml += "</div>";
  
  return { tocHtml: tocHtml, tablesHtml: tablesHtml, time: (+new Date() - startGenHtmlForTypeTime) };
}