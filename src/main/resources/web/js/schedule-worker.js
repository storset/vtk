/*
 * Course schedule (Web Worker or runned in main thread)
 *
 * Performance credits:
 * http://stackoverflow.com/questions/5080028/what-is-the-most-efficient-way-to-concatenate-n-arrays-in-javascript
 * http://blog.rodneyrehm.de/archives/14-Sorting-Were-Doing-It-Wrong.html
 * (http://en.wikipedia.org/wiki/Schwartzian_transform)
 * http://jsperf.com/math-ceil-vs-bitwise/10
 */

if(typeof alert === "undefined") { // Listen for messages only in Worker
  this.onmessage = function(e) {
    postMessage(generateHTMLForType(e.data, true));
  };
}

function scheduleUtils() {
  /** Private */
  var self = this,
  parseDate = function(dateString) {
    // Old
    // var m = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2})(?::([0-9]*)(\.[0-9]*)?)?(?:([+-])([0-9]{2}):([0-9]{2}))?/.exec(dateString);
            // 2014     - 08       - 18       T12        : 15       :00         .000         +    02       :00
    var m = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})\.([0-9]{3})([+-])([0-9]{2}):([0-9]{2})$/.exec(dateString);
    return { year: m[1], month: m[2], date: m[3], hh: m[4], mm: m[5], tzpm: m[8], tzhh: m[9], tzmm: m[10] };
  },
  getNowDate = new Date(),
  getDate = function(year, month, date, hh, mm, tzpm, tzhh, tzmm) {
    var date = new Date(year, month, date, hh, mm, 0, 0);
    
    var clientTimeZoneOffset = date.getTimezoneOffset();
    var serverTimeZoneOffset = (tzhh * 60) + tzmm;
    if(tzpm === "+") serverTimeZoneOffset = -serverTimeZoneOffset;
    
    if(clientTimeZoneOffset === serverTimeZoneOffset) return date; // Same offset in same date
    
    // Timezone correction offset for local time
    var offset = clientTimeZoneOffset > serverTimeZoneOffset ? clientTimeZoneOffset - serverTimeZoneOffset 
                                                             : serverTimeZoneOffset - clientTimeZoneOffset;
    return new Date(date.getTime() + offset);
  },
  formatName = function(name) {
    var arr = $.trim(name).replace(/ +(?= )/g, "").split(" ");
    var arrLen = arr.length;
    if(!arrLen) return name;
    
    var val = "";
    for(var i = 0, len = arrLen-1; i < len; i++) {
       val += arr[i].substring(0,1) + ". ";
    }
    return val + arr[i];
  },
  linkAbbr = function(url, title, text) {
    var val = "";
    if(url && title) {
      val += "<a class='place-short' title='" + title + "' href='" + url + "'>";
    } else if(url) {
      val += "<a class='place-short' href='" + url + "'>";
    } else if(title) {
      val += "<abbr class='place-short' title='" + title + "'>";
    }
    val += text;
    if(url) {
      val += "</a>";
    } else if(title) {
      val += "</abbr>";
    }
    // Responsive view
    if(url) {
      val += "<a class='place-long' href='" + url + "'>";
    } else {
      val += "<span class='place-long'>";
    }
    val += (title || text);
    if(url) {
      val += "</a>";
    } else {
      val += "</span>";
    }
    return val;
  },
  jsonArrayToHtmlList = function(arr, split) {
    var val = "";
    var valAfter = "";
    var totTxtLen = 0;
    var arrLen = arr.length;
    if(!arrLen) return  { val: val, valAfter: valAfter, txtLen: totTxtLen };

    for(var i = 0; i < arrLen; i++) {
      var obj = arr[i];

      if(obj.name && obj.url) {
        var txt = formatName(obj.name);
        totTxtLen += txt.length;
        txt = "<a href='" + obj.url + "'>" + txt + "</a>";
      } else if(obj.title && obj.url) {
        var txt = obj.title;
        totTxtLen += txt.length;
        txt = "<a href='" + obj.url + "'>" + txt + "</a>";
      } else if(obj.url) {
        var txt = obj.url;
        totTxtLen += txt.length;
        txt = "<a href='" + obj.url + "'>" + txt + "</a>";
      } else if(obj.name) {
        var txt = formatName(obj.name);
        totTxtLen += txt.length;
      } else if(obj.title) {
        var txt = obj.title;
        totTxtLen += txt.length;
      } else if(obj.id) {
        var txt = obj.id;
        totTxtLen += txt.length;
      }
      
      var midVal = ((arrLen > 1) ? "<li>" : "<p>") +
                   txt +
                   ((arrLen > 1) ? "</li>" : "</p>");
                   
      if(split && totTxtLen > resourcesTxtLimit) {
        valAfter += midVal;
      } else {
        val += midVal;
      }
    }
    if(arrLen > 1) {
      val = "<ul>" + val + "</ul>";
      if(valAfter != "") {
        valAfter = "<ul>" + valAfter + "</ul>";
      }
    }
    return { val: val, valAfter: valAfter, txtLen: totTxtLen };
  },
  ceil = function(n) {
    var f = (n << 0),
    f = f == n ? f : f + 1;
    return f;
  },
  resourcesTxtLimit = 70;

  /** Public */
  this.nowDate = getNowDate; // Cache
  this.getDateTime = function(s, e) {
    var startDateTime = parseDate(s);
    var endDateTime = parseDate(e);
    return { start: startDateTime, end: endDateTime };
  };
  this.getDateFormatted = function(dateStart, dateEnd, i18n) {
    return parseInt(dateStart.date, 10) + ". " + i18n["m" + parseInt(dateStart.month, 10)].toLowerCase() + ".";
  },
  this.getEndDateDayFormatted = function(dateStart, dateEnd, i18n) {
    var endDate = getDate(dateEnd.year, parseInt(dateEnd.month, 10) - 1, parseInt(dateEnd.date, 10), parseInt(dateEnd.hh, 10), parseInt(dateEnd.mm, 10), dateEnd.tzpm, parseInt(dateEnd.tzhh, 10), parseInt(dateEnd.tzmm, 10));
    return { endDate: endDate, day: i18n["d" + endDate.getDay()] };
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
      var len = rooms.length;
      if(len > 1) val = "<ul>";
      for(var i = 0; i < len; i++) {
        if(len > 1) val += "<li>";
        var room = rooms[i]; 
        val += linkAbbr(room.buildingUrl, room.buildingName, (room.buildingAcronym || room.buildingId));
        val += " ";
        val += linkAbbr(room.roomUrl, room.roomName, room.roomId);
        if(len > 1) val += "</li>";
      }
      if(len > 1) val += "</ul>";
    }
    return val;
  };
  this.getStaff = function(session) {
    var val = "";
    var staff = session.vrtxStaff || session.staff || [];
    var externalStaff = session.vrtxStaffExternal;
    if(externalStaff && externalStaff.length) {
      staff.push.apply(staff, externalStaff);
    }
    return jsonArrayToHtmlList(staff, false).val;
  };
  this.getResources = function(session, fixedResources) {
    var resources = session.vrtxResources || [];
    if(fixedResources) {
      for(var i = 0, len = fixedResources.resources.length; i < len; i++) { 
        resources.push({ "url": fixedResources.folderUrl + "/" + fixedResources.resources[i].name, "title": fixedResources.resources[i].title });
      }
    }
    var v = jsonArrayToHtmlList(resources, true);
    var totTxtLen = v.txtLen;
    var val = v.val;
    var valAfter = v.valAfter;

    var resourcesText = session.vrtxResourcesText;
    if(resourcesText && resourcesText.length) {   
      if(totTxtLen > resourcesTxtLimit) {
        valAfter += resourcesText;
      } else {
        var htmlSplitted = resourcesText.match(/<\s*(\w+\b)(?:(?!<\s*\/\s*\1\b)[\s\S])*<\s*\/\s*\1\s*>|\S+/gi); // http://stackoverflow.com/questions/19244318/javascript-split-messes-up-my-html-tags
        if(htmlSplitted != null) { // Check length of resourcesText and split if exceeds limit
          var totExtraTxtLen = 0;
          for(var i = 0, len = htmlSplitted.length; i < len; i++) {
            var htmlSplittedSingle = htmlSplitted[i];
            totExtraTxtLen += htmlSplittedSingle.replace(/(<([^>]+)>)/ig,"").length; // http://css-tricks.com/snippets/javascript/strip-html-tags-in-javascript/
            if((totTxtLen + totExtraTxtLen) > resourcesTxtLimit) {
              valAfter += htmlSplittedSingle;
              i++;
              for(;i < len; i++) { // Add rest
                valAfter += htmlSplitted[i];
              }
              break;
            } else {
              val += htmlSplittedSingle;
            }
          }
        } else {
          val += resourcesText;
        }
      }
    }
    return val + (valAfter != "" ? "<a href='javascript:void(0);' class='course-schedule-table-resources-after-toggle'>...</a><div class='course-schedule-table-resources-after'>" + valAfter + "</div>" : "");
  };
  this.getTableStartHtml = function(activityId, caption, isAllPassed, hasResources, hasStaff, i18n) {
    var html = "<div tabindex='0' class='course-schedule-table-wrapper'>";
    html += "<table id='" + activityId + "' class='course-schedule-table uio-zebra hiding-passed" + (isAllPassed ? " all-passed" : "") + (hasResources ? " has-resources" : "")  + (hasStaff ? " has-staff" : "") + "'><caption>" + caption + "</caption><thead><tr>";
      html += "<th class='course-schedule-table-date'>" + i18n.tableDate + "</th>";
      html += "<th class='course-schedule-table-day'>" + i18n.tableDay + "</th>";
      html += "<th class='course-schedule-table-time'>" + i18n.tableTime + "</th>";
      html += "<th class='course-schedule-table-title'>" + i18n.tableTitle + "</th>";
      html += "<th class='course-schedule-table-place'>" + i18n.tablePlace + "</th>";
      if(hasStaff)     html += "<th class='course-schedule-table-staff'>" + i18n.tableStaff + "</th>";
      if(hasResources) html += "<th class='course-schedule-table-resources'>" + i18n.tableResources + "</th>";
    html += "</tr></thead><tbody>";
    return html;
  };
  this.getTableEndHtml = function(isNoPassed, i18n) {
    var html = "</tbody></table>";
    // if(!isNoPassed) html += "<a class='course-schedule-table-toggle-passed' href='javascript:void(0);'>" + i18n.tableShowPassed + "</a>";
    html += "</div>";
    return html;
  };
  this.splitThirds = function(arr, title, notTime) {
    var html = "<span class='display-as-h3'>" + title + "</span>",
        len = arr.length,
        split1 = ceil(len / 3),
        split2 = split1 + ceil((len - split1) / 2);
    html += "<div class='course-schedule-toc-thirds'><ul class='thirds-left'>";
    for(var i = 0; i < len; i++) {
      if(i === split1) html += "</ul><ul class='thirds-middle'>";
      if(i === split2) html += "</ul><ul class='thirds-right'>";
      html += notTime ? arr[i].tocHtml.replace(/^.*(<a[^>]+>[^<]+<\/a>).*$/, "<li>$1</li>") : arr[i].tocHtml;
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

function generateHTMLForType(d, supportThreads, type, scheduleI18n, canEdit) {
  var startGenHtmlForTypeTime = new Date(),
      dta = supportThreads ? JSON.parse(d) : null,
      data = supportThreads ? dta.data.activities : d[type].activities,
      tocHtml = "",
      tablesHtml = "";
  
  if(!data) return { tocHtml: tocHtml, tablesHtml: tablesHtml, time: (+new Date() - startGenHtmlForTypeTime) };
  var dataLen = data.length;
  if(!dataLen) return { tocHtml: tocHtml, tablesHtml: tablesHtml, time: (+new Date() - startGenHtmlForTypeTime) };
  
  if(supportThreads) {
    type = dta.type,
    scheduleI18n = dta.i18n,
    canEdit = dta.canEdit;
  } 
     
  var skipTier = type === "plenary";
      
  var utils = new scheduleUtils(), // Cache
      nowDate = utils.nowDate,
      getDateTime = utils.getDateTime,
      getDateFormatted = utils.getDateFormatted,
      getEndDateDayFormatted = utils.getEndDateDayFormatted,
      getTimeFormatted = utils.getTimeFormatted,
      getPostFixId = utils.getPostFixId,
      getTitle = utils.getTitle,
      getPlace = utils.getPlace,
      getStaff = utils.getStaff,
      getResources = utils.getResources,
      getTableStartHtml = utils.getTableStartHtml,
      getTableEndHtml = utils.getTableEndHtml,
      splitThirds = utils.splitThirds,
      editLink = utils.editLink;

  var forCode = "for",
      sequences = {}, // For fixed resources
      tablesHtmlArr = [],
      tocTimeMax = skipTier ? 3 : 2,
      tocTimeNo = false,
      htmlArr = [];
  
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
        var idSplit = id.split("-");
        var groupCode = idSplit[0];
        var groupNumber = parseInt(idSplit[1], 10);
        var specialGroup = scheduleI18n[groupCode];
        var caption = (specialGroup ? specialGroup : dtLong) + " - " + scheduleI18n.groupTitle.toLowerCase() + " " + groupNumber;
      }
      var tocTime = "";
      var tocTimeCount = 0;
    }
    
    // Add together sessions from sequences
    for(var j = 0, len = dt.sequences.length; j < len; j++) {
      var sequence = dt.sequences[j];
      var fixedResources = sequence.vrtxResourcesFixed;
      if(fixedResources) {
        sequences[sequence.id] = fixedResources;
        resourcesCount++;
      }
      sessions.push.apply(sessions, sequence.sessions);
    }
 
    var dtShortNextDifferent = !data[i+1] || data[i+1].teachingMethod.toLowerCase() !== dtShort;
    if(!isFor || (isFor && dtShortNextDifferent)) {

      // Evaluate and cache dateTime, staff and resources
      var map = [], sessionsProcessed = [];
      for(j = 0, len = sessions.length; j < len; j++) {
        var session = sessions[j];
        
        var dateTime = getDateTime(session.dtStart, session.dtEnd);
        var start = dateTime.start;
        var end = dateTime.end;
        var startEndString = start.year + "" + start.month + "" + start.date + "" + start.hh + "" + start.mm + "" + end.hh + "" + end.mm;
        
        var staff = getStaff(session);
        if(staff.length) staffCount++;
        var sequenceId = session.id.replace(/\/[^\/]*$/, "");
        var resources = getResources(session, (sequences[sequenceId] || null));
        if(resources.length) resourcesCount++;
        
        map.push({
          "index": j, // Save index
          "startEndString": startEndString,
        });
        sessionsProcessed.push({
          "dateTime": dateTime,
          "staff": staff,
          "resources": resources
        });
      }
      // Sort
      map.sort(function(a, b) {
        return a.startEndString > b.startEndString ? 1 : -1;
      });
      // Generate sessions HTML (get correctly sorted from map)
      var correctedCounter = 0;
      for(j = 0, len = map.length; j < len; j++) {
        var session = sessions[map[j].index];
        if(session.vrtxOrphan) {
          correctedCounter--;
          continue;
        }
        var sessionPreprocessed = sessionsProcessed[map[j].index];
        
        var dateTime = sessionPreprocessed.dateTime;
        var sessionId = (skipTier ? type : dtShort + "-" + id) + "-" + session.id.replace(/\//g, "-") + "-" + getPostFixId(dateTime.start, dateTime.end);
        var isCancelled = (session.status && session.status === "cancelled") ||
                          (session.vrtxStatus && session.vrtxStatus === "cancelled");
        var date = getDateFormatted(dateTime.start, dateTime.end, scheduleI18n);
        var endDateDay = getEndDateDayFormatted(dateTime.start, dateTime.end, scheduleI18n);
        var endDate = endDateDay.endDate;
        var day = endDateDay.day;
        var time = getTimeFormatted(dateTime.start, dateTime.end);
        var title = getTitle(session, isCancelled, scheduleI18n);
        var place = getPlace(session);

        var classes = ((j + correctedCounter) & 1) ? "even" : "odd";     
        if(isCancelled) {
          if(classes !== "") classes += " ";
          classes += "cancelled";
        }
        if(endDate <= nowDate) {
          if(classes !== "") classes += " ";
          classes += "passed";
          passedCount++;
        }
        sessionsCount++;

        sessionsHtml += classes !== "" ? "<tr tabindex='0' id='" + sessionId + "' class='" + classes + "'>" : "<tr>";
          sessionsHtml += "<td class='course-schedule-table-date'><span class='responsive-header'>" + scheduleI18n.tableDate + "</span>" + date + "</td>";
          sessionsHtml += "<td class='course-schedule-table-day'><span class='responsive-header'>" + scheduleI18n.tableDay + "</span>" + day + "</td>";
          sessionsHtml += "<td class='course-schedule-table-time'><span class='responsive-header'>" + scheduleI18n.tableTime + "</span>" + time + "</td>";
          sessionsHtml += "<td class='course-schedule-table-title'><span class='responsive-header'>" + scheduleI18n.tableTitle + "</span>" + title + "</td>";
          sessionsHtml += editLink("course-schedule-table-place", "<span class='responsive-header'>" + scheduleI18n.tablePlace + "</span>" + place, !staffCount && !resourcesCount, canEdit, scheduleI18n);
          if(staffCount)     sessionsHtml += editLink("course-schedule-table-staff", "<span class='responsive-header'>" + scheduleI18n.tableStaff + "</span>" + sessionPreprocessed.staff, staffCount && !resourcesCount, canEdit, scheduleI18n);
          if(resourcesCount) sessionsHtml += editLink("course-schedule-table-resources", "<span class='responsive-header'>" + scheduleI18n.tableResources + "</span>" + sessionPreprocessed.resources, resourcesCount, canEdit, scheduleI18n);
        sessionsHtml += "</tr>";
      
        if(!tocTimeNo) {
          var newTocTime = day.toLowerCase().substring(0,3) + " " + time;
          if(tocTime.indexOf(newTocTime) === -1) {
            if(tocTimeCount < tocTimeMax) {
              if(tocTimeCount > 0) {
                tocTime += ", ";
                tocTime += "<span>";
              }
              tocTime += newTocTime;
              tocTime += "</span>";
            }
            tocTimeCount++;
            if(tocTimeCount >= tocTimeMax && !skipTier) {
              tocTimeNo = true;
            }
          }
        }
      }
      
      // Generate table and ToC
      var section = { "groupCode": groupCode, "groupNr": groupNumber, "tableHtml": getTableStartHtml(activityId, caption, (passedCount === sessionsCount), resourcesCount, staffCount, scheduleI18n) + sessionsHtml + getTableEndHtml(passedCount === 0, scheduleI18n) };
      
      tocTime = tocTime.replace(/,([^,]+)$/, " " + scheduleI18n.and + "$1");
      if(!skipTier) {
        section.tocHtml = "<li><span><a href='#" + activityId + "'>" + scheduleI18n.groupTitle + " " + groupNumber + "</a>" + (tocTimeCount <= tocTimeMax && !tocTimeNo ? " - " + tocTime : "") + "</li>";
      } else {
        tocHtml += "<li><span><a href='#" + activityId + "'>" + dtLong + "</a> - " + tocTime + "</li>";
      }
      
      htmlArr.push(section);

      if(dtShortNextDifferent) {
        // Sort group code and then group number (if codes are equal)
        htmlArr.sort(function(a, b) { // http://www.sitepoint.com/sophisticated-sorting-in-javascript/
          var x = a.groupCode, y = b.groupCode;
          if(x === y) {
            return a.groupNr - b.groupNr;
          }
          return x < y ? -1 : x > y ? 1 : 0;
        });
        // Slice up ToC and concat tables html
        var startSlice = 0;
        for(j = 0, len = htmlArr.length; j < len; j++) {
          if(!skipTier) {
            var specialGroupCode = scheduleI18n[htmlArr[j].groupCode];
            if(specialGroupCode && (!htmlArr[j+1] || specialGroupCode != scheduleI18n[htmlArr[j+1].groupCode])) {
              var slicedHtmlArr = htmlArr.slice(startSlice, j + 1);
              tocHtml += splitThirds(slicedHtmlArr, specialGroupCode, tocTimeNo || slicedHtmlArr.length > 30);
              startSlice = j + 1;
            }
          }
          tablesHtml += htmlArr[j].tableHtml;
        }
        if(!skipTier) {
          if(startSlice === 0) {
            tocHtml += splitThirds(htmlArr, dtLong, len > 30);
          }
        }
        htmlArr = [];
      }
    }
  }
  
  if(skipTier) tocHtml += "</ul>";
  tocHtml += "</div>";
  
  return { tocHtml: tocHtml, tablesHtml: tablesHtml, time: (+new Date() - startGenHtmlForTypeTime) };
}