/*
 * Schedule editor
 *
 */

function courseSchedule() {

  var baseUrl = location.protocol + "//" + location.host + location.pathname;
  if(/\/$/.test(baseUrl)) {
    baseUrl += "index.html";
  }
  url = baseUrl + "?action=course-schedule&mode=edit&t=" + (+new Date());
  // Debug: Local development
  // url = "/vrtx/__vrtx/static-resources/js/tp-test.json";
  
  // Hide shortcut for saving working copy
  $("#vrtx-save-as-working-copy-shortcut, #saveWorkingCopyAction, #buttons-or-text").hide();
 
  // TODO: Should we move these to seperate files?
 
  // i18n messages
  var allI18n = {
    "no": {
      "m01": "jan",
      "m02": "feb",
      "m03": "mar",
      "m04": "apr",
      "m05": "mai",
      "m06": "jun",
      "m07": "jul",
      "m08": "aug",
      "m09": "sep",
      "m10": "okt",
      "m11": "nov",
      "m12": "des",

      "d0": "Søndag",
      "d1": "Mandag",
      "d2": "Tirsdag",
      "d3": "Onsdag",
      "d4": "Torsdag",
      "d5": "Fredag",
      "d6": "Lørdag",
      
      "titles": {
        "plenary": "Fellesundervisning",
        "group": "Partiundervisning",
        "groupNum": "Gruppe", 
        "activities": "Aktiviteter"
      },
      "editOnlySessionTitle": "Rediger aktivitet",
      "loadingRetrievingData": "Henter timeplandata",
      "loadingGenerating": "Lager timeplan",
      "noData": "Ingen data",
      "noSessionData": "Ingen aktivitetsdata",
      "cancelled": "AVLYST",
      "cancelledVortexTooltip": "Aktiviteten kan avlyses i Vortex",
      "cancelledTPTooltip": "Aktiviteten er avlyst i timeplanleggingssystemet",
      "orphan": "SLETTET",
      
      "prev": "Forrige",
      "next": "Neste",
      
      "saveView": "Lagre og vis",
      "save": "Lagre",
      "cancel": "Avbryt",

      "vrtxTitle": "Tittel",
      "vrtxStaff": "Forelesere",
      "vrtxStaffExternal": "Eksterne forelesere",
      "vrtxStaffExternal-name": "Navn",
      "vrtxStaffExternal-url": "Lenke",
      "vrtxResources": "Ressurser",
      "vrtxResources-title": "Tittel",
      "vrtxResources-url": "Lenke",
      "vrtxResourcesText": "Pensum",
      "vrtxResourcesFixed": "Faste ressurser for medisinstudiet",
      "vrtxResourcesFixedInfo": "Ressurser som følger med fra semester til semester. Lesetilgang for brukere tilknyttet medisinstudiet",
      "vrtxResourcesFixedUploadAdminFolder": "Last opp flere / administrer",
      "vrtxResourcesFixedCreateFolder": "Lag mappe",
      "vrtxStatus": "Avlys"
    },
    "nn": {
      "m01": "jan",
      "m02": "feb",
      "m03": "mar",
      "m04": "apr",
      "m05": "mai",
      "m06": "jun",
      "m07": "jul",
      "m08": "aug",
      "m09": "sep",
      "m10": "okt",
      "m11": "nov",
      "m12": "des",
      
      "d0": "Søndag",
      "d1": "Mandag",
      "d2": "Tirsdag",
      "d3": "Onsdag",
      "d4": "Torsdag",
      "d5": "Fredag",
      "d6": "Lørdag",
      
      "titles": {
        "plenary": "Fellesundervisning",
        "group": "Partiundervisning",
        "groupNum": "Gruppe",
        "activities": "Aktiviteter"
      },
      "editOnlySessionTitle": "Rediger aktivitet",
      "loadingRetrievingData": "Henter timeplandata",
      "loadingGenerating": "Lager timeplan",
      "noData": "Ingen data",
      "noSessionData": "Ingen aktivitetsdata",
      "cancelled": "AVLYST",
      "cancelledVortexTooltip": "Aktiviteten kan avlyses i Vortex",
      "cancelledTPTooltip": "Aktiviteten er avlyst i timeplanleggingssystemet",
      "orphan": "SLETTET",
      
      "prev": "Forrige",
      "next": "Neste",
      
      "saveView": "Lagre og vis",
      "save": "Lagre",
      "cancel": "Avbryt",
      
      "vrtxTitle": "Tittel",
      "vrtxStaff": "Forelesere",
      "vrtxStaffExternal": "Eksterne forelesere",
      "vrtxStaffExternal-name": "Namn",
      "vrtxStaffExternal-url": "Lenkje",
      "vrtxResources": "Ressursar",
      "vrtxResources-title": "Tittel",
      "vrtxResources-url": "Lenkje",
      "vrtxResourcesText": "Pensum",
      "vrtxResourcesFixed": "Faste ressursar for medisinstudiet",
      "vrtxResourcesFixedInfo": "Ressursar som følgjer med frå semester til semester. Lesetilgang for brukarar knytta til medisinstudiet",
      "vrtxResourcesFixedUploadAdminFolder": "Last opp fleire / administrer",
      "vrtxResourcesFixedCreateFolder": "Lag mappe",
      "vrtxStatus": "Avlys"
    },
    "en": {
      "m01": "Jan",
      "m02": "Feb",
      "m03": "Mar",
      "m04": "Apr",
      "m05": "May",
      "m06": "Jun",
      "m07": "Jul",
      "m08": "Aug",
      "m09": "Sep",
      "m10": "Oct",
      "m11": "Nov",
      "m12": "Dec",
      
      "d0": "Sunday",
      "d1": "Monday",
      "d2": "Tuesday",
      "d3": "Wednesday",
      "d4": "Thursday",
      "d5": "Friday",
      "d6": "Saturday",
      
      "titles": {
        "plenary": "Plenary teaching",
        "group": "Group teaching",
        "groupNum": "Group",
        "activities": "Activities"
      },
      "editOnlySessionTitle": "Edit activity",
      "loadingRetrievingData": "Retrieving schedule data",
      "loadingGenerating": "Generating schedule",
      "noData": "No data",
      "noSessionData": "No activity data",
      "cancelled": "CANCELLED",
      "cancelledVortexTooltip": "The activity can be cancelled in Vortex",
      "cancelledTPTooltip": "The activity is cancelled in the schedulling system",
      "orphan": "DELETED",
      
      "prev": "Previous",
      "next": "Next",
      
      "saveView": "Save and view",
      "save": "Save",
      "cancel": "Cancel",
      
      "vrtxTitle": "Title",
      "vrtxStaff": "Staff",
      "vrtxStaffExternal": "External staff",
      "vrtxStaffExternal-name": "Name",
      "vrtxStaffExternal-url": "Link",
      "vrtxResources": "Resources",
      "vrtxResources-title": "Title",
      "vrtxResources-url": "Link",
      "vrtxResourcesText": "Curriculum",
      "vrtxResourcesFixed": "Fixed resources for the professional studies in medicine",
      "vrtxResourcesFixedInfo": "Permanent resources linked to the same activity every semester",
      "vrtxResourcesFixedUploadAdminFolder": "Upload more / manage",
      "vrtxResourcesFixedCreateFolder": "Create folder",
      "vrtxStatus": "Cancel"
    }
  };
  
  this.sessionsLookup = {};
  
  this.lastId = "";
  this.lastSessionId = "";
  this.lastElm = null;
  
  this.retrievedScheduleData = null;
  this.i18n = allI18n[vrtxAdmin.lang];
  this.getSessionOnlyHtml = function(sessionId) {
    var sessionData = this.getSessionJSONFromId(sessionId);
    if(!sessionData) return null;
    
    var id = sessionData.id;
    var session = sessionData.session;
    var sessionDateTime = sessionData.sessionDateTime;
    var sequences = sessionData.sequences;
    var type = sessionData.type;
    var skipTier = sessionData.skipTier;
    var dtShort = sessionData.dtShort;
    var prevId = sessionData.prevId;
    var nextId = sessionData.nextId;
    
    var descs = this.retrievedScheduleData[type].vrtxEditableDescription;

    if(!this.sessionsLookup[skipTier ? (dtShort != "for" ? id : dtShort) : id]) {
      this.sessionsLookup[skipTier ? (dtShort != "for" ? id : dtShort) : id] = {};
    }
    var sessionDateTime = this.getDateTime(session.dtStart, session.dtEnd);
    var sessionHtml = this.getSessionHtml(id, prevId, nextId, session, dtShort, sessionDateTime, sequences, descs, skipTier, vrtxEditor);    
    
    this.lastElm = $(".properties"); 
    this.lastId = skipTier ? (dtShort != "for" ? id : dtShort) : id;
    this.lastSessionId = sessionId;
                                                    
    return { id: id, skipTier: skipTier, dtShort: dtShort, html: sessionHtml.html, title: sessionHtml.title };
  };
  this.getActivitiesForTypeHtml = function(type, skipTier) {
    if(!this.retrievedScheduleData[type]) return "";
    var descs = this.retrievedScheduleData[type].vrtxEditableDescription,
        data = this.retrievedScheduleData[type].activities;
    if(!descs || !data) return "";
        
    var dataLen = data.length;
    if(!dataLen) return "";
    
    // Store sessions HTML and multiple descriptions in lookup object
    var vrtxEdit = vrtxEditor,
        html = "",
        htmlArr = [],
        sessions = [],
        sequences = {}, // For fixed resources
        sessionsHtml = "",
        self = this;
    for(var i = 0; i < dataLen; i++) {
      var dt = data[i],
          dtShort = dt.teachingMethod.toLowerCase(),
          dtLong = dt.teachingMethodName,
          id = dtShort + "-" + dt.id,
          title = skipTier ? dtLong : (dt.title || dtLong),
          groupCode = dtShort,
          groupNumber = ((dt.party && dt.party.name) ? parseInt(dt.party.name, 10) : 0);

      this.sessionsLookup[skipTier ? (dtShort != "for" ? id : dtShort) : id] = {};
      
      // Add together sessions from sequences
      for(var j = 0, len = dt.sequences.length; j < len; j++) {
        var sequence = dt.sequences[j];
        var fixedResources = sequence.vrtxResourcesFixed;
        if(fixedResources) {
          sequences[sequence.id] = fixedResources;
        }
        sessions = sessions.concat(sequence.sessions);
      }
      
      if(!skipTier || (dtShort != "for" || (dtShort === "for" && (!data[i+1] || data[i+1].teachingMethod.toLowerCase() !== dtShort)))) {
        // Evaluate and cache dateTime
        var map = [], sessionsProcessed = [];
        for(j = 0, len = sessions.length; j < len; j++) {
          var session = sessions[j];
          
          var dateTime = self.getDateTime(session.dtStart, session.dtEnd);
          var start = dateTime.start;
          var end = dateTime.end;
          var startEndString = start.year + "" + start.month + "" + start.date + "" + start.hh + "" + start.mm + "" + end.hh + "" + end.mm;
          
          map.push({
            "index": j, // Save index
            "startEndString": startEndString,
            "isOrphan": session.vrtxOrphan
          });
          sessionsProcessed.push({
            "dateTime": dateTime
          });
        }
        // Sort
        map.sort(function(a, b) {
          var x = a.isOrphan, y = b.isOrphan;
          if(x === y) {
            return a.startEndString > b.startEndString ? 1 : -1;
          }
          return !x && y ? -1 : x && !y ? 1 : 0;
        });
        
        // Generate sessions HTML (get correctly sorted from map)
        for(j = 0, len = map.length; j < len; j++) {
          var session = sessions[map[j].index];
          var sessionProcessed = sessionsProcessed[map[j].index];
          var sessionHtml = this.getSessionHtml(id, null, null, session, dtShort, sessionProcessed.dateTime, sequences, descs, skipTier, vrtxEdit);
          sessionsHtml += vrtxEdit.htmlFacade.getAccordionInteraction(!skipTier ? "5" : "4", sessionHtml.sessionId, "session", sessionHtml.title, sessionHtml.html);
        }
        
        if(skipTier) {
          this.sessionsLookup[dtShort != "for" ? id : dtShort].html = "<span class='accordion-content-title'>" + this.i18n.titles.activities + "</span>" + sessionsHtml;
          html += vrtxEdit.htmlFacade.getAccordionInteraction("3", (dtShort != "for" ? id : dtShort), (type + " skip-tier"), dtLong, "");
        } else {
          this.sessionsLookup[id].html = "<span class='accordion-content-title'>" + this.i18n.titles.activities + "</span>" + sessionsHtml;
          htmlArr.push({ "groupCode": groupCode, "groupNr": groupNumber, "accHtml": vrtxEdit.htmlFacade.getAccordionInteraction("4", id, type, title, "") });
          
          if(!data[i+1] || data[i+1].teachingMethod.toLowerCase() !== dtShort) {
            // Sort group code and group number if equal
            htmlArr.sort(function(a, b) { // http://www.sitepoint.com/sophisticated-sorting-in-javascript/
              var x = a.groupCode, y = b.groupCode;
              if(x === y) {
                return a.groupNr - b.groupNr;
              }
              return x < y ? -1 : x > y ? 1 : 0;
            });
            var htmlMiddle = "";
            for(j = 0, len = htmlArr.length; j < len; j++) {
              htmlMiddle += htmlArr[j].accHtml;
            }
            if(htmlMiddle != "") {
              html += vrtxEdit.htmlFacade.getAccordionInteraction("3", dtShort, type, dtLong, "<div class='vrtx-grouped'>" + htmlMiddle + "</div>");
            }
            htmlArr = [];
          }
        }
        sessionsHtml = "";
        sessions = [];
      }
    }
     
    return html;
  };
  this.getSessionHtml = function(id, prevId, nextId, session, dtShort, sessionDateTime, sequences, descs, skipTier, vrtxEdit) {
    var sessionDatePostFixId = this.getDateAndPostFixId(sessionDateTime),
        sessionId = id + "-" + session.id.replace(/\//g, "-").replace(/#/g, "-") + "-" + sessionDatePostFixId.postFixId,
        sequenceIdSplit = session.id.split("/");
    if(sequenceIdSplit.length == 3) {
      var sequenceId = sequenceIdSplit[1];
    } else if(sequenceIdSplit == 2) {
      var sequenceId = sequenceIdSplit[0];
    } else {
      var sequenceId = sequenceIdSplit[0] || session.id;
    }
    var sessionOrphan = session.vrtxOrphan,
        sessionCancelled = !session.vrtxOrphan && (session.vrtxStatus && session.vrtxStatus === "cancelled") || (session.status && session.status === "cancelled"),
        rooms = session.rooms,
        sessionTitle = "<span class='session-date'>" + sessionDatePostFixId.date + "</span>" +
                       "<span class='session-title' data-orig='" + encodeURI(session.title || session.id) + "'>" + 
                       (sessionOrphan ? "<span class='header-status'>" + this.i18n.orphan + "</span> - " : "") +
                       (sessionCancelled ? "<span class='header-status'>" + this.i18n.cancelled + "</span> - " : "") +
                       "<span class='header-title'>" + (session.vrtxTitle || session.title || session.id) + "</span></span>" +
                       (rooms ? (" - <span class='session-room'>" + (rooms[0].buildingAcronym || rooms[0].buildingId) + " " + rooms[0].roomId) + "</span>" : "") +
                       ((prevId || nextId) ? "<div class='prev-next'>" : "") +
                       (prevId ? "<a class='prev' href='" + window.location.protocol + "//" + window.location.host + window.location.pathname + "?vrtx=admin&mode=editor&action=edit&embed&sessionid=" + prevId + "'>" + this.i18n.prev + "</a>" : "") +
                       (nextId ? "<a class='next' href='" + window.location.protocol + "//" + window.location.host + window.location.pathname + "?vrtx=admin&mode=editor&action=edit&embed&sessionid=" + nextId + "'>" + this.i18n.next + "</a>" : "") +
                       ((prevId || nextId) ? "</div>" : ""),
        sessionContent = vrtxEdit.htmlFacade.jsonToHtml(id, sessionId, (skipTier ? (dtShort != "for" ? id : dtShort) : id), session, this.retrievedScheduleData.vrtxResourcesFixedUrl, { "vrtxResourcesFixed": sequences[sequenceId] }, descs, this.i18n);

     this.sessionsLookup[skipTier ? (dtShort != "for" ? id : dtShort) : id][sessionId] = {
       rawPtr: session,
       rawOrig: jQuery.extend(true, {}, session), // Copy object
       descsPtr: descs,
       multiples: sessionContent.multiples,
       rtEditors: sessionContent.rtEditors,
       sequenceId: sequenceId,
       isCancelled: sessionCancelled,
       isOrphan: sessionOrphan,
       isEnhanced: false,
       hasChanges: false
     };
     
     return { sessionId: sessionId, html: sessionContent.html, title: sessionTitle };
  };
  this.loadingUpdate = function(msg) {
    var loader = $("#editor-loading-inner");
    if(!loader.length) {
      var loaderHtml = "<span id='editor-loading'><span id='editor-loading-inner'>" + msg + "...</span></span>";
      $(loaderHtml).insertAfter(".properties");
    } else {
      if(msg.length) {
        loader.text(msg + "...");
      } else {
        loader.parent().remove();
        if(onlySessionId) {
          $("html").removeClass("embedded-loading");
          $("#editor").css("height", "auto");
        }
      }
    }
  };
  this.parseDate = function(dateString) {
    // Old
    // var m = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2})(?::([0-9]*)(\.[0-9]*)?)?(?:([+-])([0-9]{2}):([0-9]{2}))?/.exec(dateString);
            // 2014     - 08       - 18       T12        : 15       :00         .000         +    02       :00
    var m = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})\.([0-9]{3})([+-])([0-9]{2}):([0-9]{2})$/.exec(dateString);
    return { year: m[1], month: m[2], date: m[3], hh: m[4], mm: m[5], tzhh: m[9], tzmm: m[10] };
  };
  this.getDateTime = function(s, e) {
    var startDateTime = this.parseDate(s);
    var endDateTime = this.parseDate(e);
    return { start: startDateTime, end: endDateTime };
  };
  this.getDate = function(year, month, date, hh, mm, tzpm, tzhh, tzmm) {
    var date = new Date(year, month, date, hh, mm, 0, 0);
    
    var clientTimeZoneOffset = date.getTimezoneOffset();
    var serverTimeZoneOffset = (tzhh * 60) + tzmm;
    if(tzpm === "+") serverTimeZoneOffset = -serverTimeZoneOffset;
    
    if(clientTimeZoneOffset === serverTimeZoneOffset) return date; // Same offset in same date
    
    // Timezone correction offset for local time
    var offset = clientTimeZoneOffset > serverTimeZoneOffset ? clientTimeZoneOffset - serverTimeZoneOffset 
                                                             : serverTimeZoneOffset - clientTimeZoneOffset;
    return new Date(date.getTime() + offset);
  };
  this.getDateAndPostFixId = function(dateTime) {
    var start = dateTime.start;
    var end = dateTime.end;
    var endDate = this.getDate(end.year, parseInt(end.month, 10) - 1, parseInt(end.date, 10), parseInt(end.hh, 10), parseInt(end.mm, 10), end.tzpm, parseInt(end.tzhh, 10), parseInt(end.tzmm, 10));
    var endDay = this.i18n["d" + endDate.getDay()];
    var strDate = endDay.substring(0,2) + ". " + parseInt(start.date, 10) + ". " + this.i18n["m" + start.month] + ". - " + start.hh + ":" + start.mm + "-" + end.hh + ":" + end.mm;
    var postFixId = start.date + "-" + start.month + "-" + start.year + "-" + start.hh + "-" + start.mm + "-" + end.hh + "-" + end.mm;
    return { date: strDate, postFixId: postFixId };
  };
  this.getSessionJSONFromId = function(findSessionId) { // XXX: Refactor with getActivitiesForTypeHtml
    var foundObj = null;
    var nextId = null;
    var prevId = null;
    var sessions = [];
    var sequences = {};
    for(var type in this.retrievedScheduleData) {
      if(!this.retrievedScheduleData[type]) continue;
      var data = this.retrievedScheduleData[type].activities;
      if(!data) continue;
      var dataLen = data.length;
      if(!dataLen) continue;
      
      var skipTier = type === "plenary";
      for(var i = 0; i < dataLen; i++) {
        var dt = data[i];
        var dtShort = dt.teachingMethod.toLowerCase();
        var id = dtShort + "-" + dt.id;
        for(var j = 0, len = dt.sequences.length; j < len; j++) {
          var sequence = dt.sequences[j];
          var fixedResources = sequence.vrtxResourcesFixed;
          if(fixedResources) {           
            sequences[sequence.id] = fixedResources;
          }
          sessions = sessions.concat(sequence.sessions);
        }
        if(!skipTier || (dtShort != "for" || (dtShort === "for" && (!data[i+1] || data[i+1].teachingMethod.toLowerCase() !== dtShort)))) {
          // Evaluate and cache dateTime
          var map = [], sessionsProcessed = [];
          for(j = 0, len = sessions.length; j < len; j++) {
            var session = sessions[j];
          
            var dateTime = this.getDateTime(session.dtStart, session.dtEnd);
            var start = dateTime.start;
            var end = dateTime.end;
            var startEndString = start.year + "" + start.month + "" + start.date + "" + start.hh + "" + start.mm + "" + end.hh + "" + end.mm;
          
            map.push({
              "index": j, // Save index
              "startEndString": startEndString,
              "isOrphan": session.vrtxOrphan
            });
            sessionsProcessed.push({
              "dateTime": dateTime
            });
          }
          // Sort
          map.sort(function(a, b) {
            var x = a.isOrphan, y = b.isOrphan;
            if(x === y) {
              return a.startEndString > b.startEndString ? 1 : -1;
            }
            return !x && y ? -1 : x && !y ? 1 : 0;
          });
          for(j = 0, len = map.length; j < len; j++) {
            var session = sessions[map[j].index];
            var sessionProcessed = sessionsProcessed[map[j].index];
            var sessionDateTime = sessionProcessed.dateTime;
            var sessionDatePostFixId = this.getDateAndPostFixId(sessionDateTime);
            var sessionId = id + "-" + session.id.replace(/\//g, "-").replace(/#/g, "-") + "-" + sessionDatePostFixId.postFixId;

            if(foundObj && !nextId) {
              nextId = sessionId;
              break;
            }
            if(findSessionId === sessionId) {
              foundObj = { id: id, prevId: prevId, session: session, sessionDateTime: sessionDateTime, sequences: sequences, type: type, skipTier: skipTier, dtShort: dtShort };
            } else {
              prevId = sessionId;
            }
          }
          sessions = [];
        }
        if(nextId) {
          break;
        }
      }
      if(nextId) {
        break;
      }
    }
    if(foundObj) {
      foundObj.nextId = nextId;
    }
    return foundObj;
  };
  this.enhanceSession = function(id, sessionId, contentElm) {
    var session = this.sessionsLookup[id][sessionId];
    if(session && !session.isEnhanced) { // If not already enhanced
      var multiples = session.multiples;
      var enhanceMultipleInputFieldsFunc = enhanceMultipleInputFields;
      for(var i = multiples.length; i--;) {
        var m = multiples[i];
        enhanceMultipleInputFieldsFunc(m.name + "-" + sessionId, m.movable, m.browsable, 50, m.json);
      }
      var rtEditors = session.rtEditors;
      for(i = rtEditors.length; i--;) {
        vrtxEditor.richtextEditorFacade.setup({
          name: rtEditors[i],
          isCompleteEditor: false,
          isWithoutSubSuper: false,
          defaultLanguage: vrtxAdmin.lang,
          cssFileList: cssFileList,
          simple: true
        });
      }
      session.isEnhanced = true;
    }
  };
  this.checkUnsavedChanges = function() {
    this.saveLastSession();
    for(var type in this.sessionsLookup) {
      for(var session in this.sessionsLookup[type]) {
        if(this.sessionsLookup[type][session].hasChanges) {
          return true;
        }
      }
    }
    return false;
  };
  this.saveLastSession = function() {
    if(this.lastElm) {
      this.saveSession(this.lastElm, this.lastId, this.lastSessionId);
    }
  };
  this.saveSession = function(sessionElms, id, sessionId) {
    saveMultipleInputFields(sessionElms, "$$$");

    var sessionLookup = this.sessionsLookup[id][sessionId];
    var rawOrig = sessionLookup.rawOrig;
    var rawPtr = sessionLookup.rawPtr;
    var descsPtr = sessionLookup.descsPtr;

    sessionLookup.hasChanges = vrtxEditor.htmlFacade.htmlToJson(sessionElms, sessionId, descsPtr, rawOrig, rawPtr);
  };
  this.saved = function(isSaveView) {
    for(var type in this.sessionsLookup) {
      for(var session in this.sessionsLookup[type]) {
        var sessionObj = this.sessionsLookup[type][session];
        if(sessionObj.hasChanges) {
          sessionObj.rawOrig = sessionObj.rawPtr;
          sessionObj.hasChanges = false;
        }
      }
    }
    this.sessionOnlyWindowClose(isSaveView);
  };
  this.sessionOnlyWindowClose = function(isSaveView) {
    if(onlySessionId && isSaveView) {
      location.href = $("#global-menu-leave-admin a").attr("href");
    }
  };

  var contents = $("#contents");
  if(onlySessionId) contents.find("#vrtx-editor-title-submit-buttons").hide();

  this.loadingUpdate(this.i18n.loadingRetrievingData);
  var cs = this;
  
  // GET JSON
  var retrievedScheduleDeferred = $.Deferred();
  vrtxAdmin.serverFacade.getJSON(url, {
    success: function(data, xhr, textStatus) {
      cs.retrievedScheduleData = data;
      cs.loadingUpdate(cs.i18n.loadingGenerating);
      retrievedScheduleDeferred.resolve();
    },
    error: function(xhr, textStatus) {
      if(textStatus === "parsererror") { // Running Vortikal or invalid JSON
        retrievedScheduleDeferred.resolve();
        vrtxAdmin.displayErrorMsg(textStatus);
      }
    }
  }, false);
  
  // Create/admin fixed resource folders
  contents.on("click", ".create-fixed-resources-folder", function(e) {
    var linkElm = $(this);
    var sessionId = linkElm[0].id.split("create-fixed-resources-folder-")[1];
    var id = sessionId.split("SID")[0];
    sessionId = sessionId.split("SID")[1];
    var session = cs.sessionsLookup[id][sessionId];
    
    var sessionDisciplines = session.rawPtr.disciplines;
    var sessionTitle = session.rawPtr.vrtxTitle || session.rawPtr.title;
    var sequenceId = session.sequenceId;
    
    var collectionTitle = (sessionDisciplines ? sessionDisciplines.join(", ") + " - " : "") + sessionTitle + " - " + sequenceId;
    var collectionName = replaceInvalidChar((sessionDisciplines ? sessionDisciplines.join("-") + "-" : "") + sessionTitle + "-" + sequenceId, false);
    
    var collectionBaseUrl = cs.retrievedScheduleData.vrtxResourcesFixedUrl;
    if(!/\/$/.test(collectionBaseUrl)) { // Add last '/' if missing
      collectionBaseUrl += "/";
    }
    var collectionUrl = collectionBaseUrl + collectionName;
    
    // Create fixed resources folder
    vrtxAdmin.serverFacade.getHtml(baseUrl + "?vrtx=admin&service=create-collection-with-properties", {
      success: function (results, status, resp) {
        var form = $($.parseHTML(results)).find("#create-collection-form");
        var csrf = form.find("input[name='csrf-prevention-token']").val();
        var dataString = "uri=" + encodeURIComponent(collectionUrl) +
                         "&type=fixed-resources-collection" +
                         "&propertyNamespace%5B%5D=" +
                         "&propertyName%5B%5D=userTitle" +
                         "&propertyValue%5B%5D=" + encodeURIComponent(collectionTitle) +
                         "&propertyNamespace%5B%5D=" + encodeURIComponent("http://www.uio.no/resource-types/fixed-resources-collection") +
                         "&propertyName%5B%5D=fixed-resources-codes" +
                         "&propertyValue%5B%5D=" + encodeURIComponent(sequenceId);
        if(sessionDisciplines) {
          for(var i = 0, len = sessionDisciplines.length; i < len; i++) {
            dataString += "&propertyNamespace%5B%5D=" +
                          "&propertyName%5B%5D=tags" +
                          "&propertyValue%5B%5D=" + encodeURIComponent(sessionDisciplines[i]);
          }
        }
        dataString += "&csrf-prevention-token=" + csrf;
        vrtxAdmin.serverFacade.postHtml(form.attr("action"), dataString, {
          success: function (results, status, resp) {
            linkElm.hide();
            $("<a class='vrtx-button admin-fixed-resources-folder' href='" + collectionUrl + "?vrtx=admin&displaymsg=yes'>" + cs.i18n["vrtxResourcesFixedUploadAdminFolder"] + "</a>").insertAfter(linkElm);
            var fixedResourcesWindow = openPopup(collectionUrl + "?vrtx=admin&displaymsg=yes", 1000, 600, "adminFixedResources");
          },
          error: function (xhr, textStatus, errMsg) {
            if(xhr.status === 500) { // XXX: assumption that already created, as it can take time before folder created is coming through
              linkElm.hide();
              $("<a class='vrtx-button admin-fixed-resources-folder' href='" + collectionUrl + "?vrtx=admin&displaymsg=yes'>" + cs.i18n["vrtxResourcesFixedUploadAdminFolder"] + "</a>").insertAfter(linkElm);
            }
            $("body").scrollTo(0, 200, { easing: 'swing', queue: true, axis: 'y' });
          }
        });
      }
    });
    e.preventDefault();
    e.stopPropagation();
  });
  contents.on("click", ".admin-fixed-resources-folder", function(e) {
    var fixedResourcesWindow = openPopup(this.href, 1000, 600, "adminFixedResources");
    e.preventDefault();
    e.stopPropagation();
  });
  
  // Instant feedback on title and status change
  contents.on("click", "input[name='vrtxStatus']", function(e) {
    var cancelledElm = $(this);
    var content = cancelledElm.closest(onlySessionId ? ".properties" : ".accordion-wrapper");
    var titleElm = content.find(onlySessionId ? ".property-label > .session-title" : "> .header > .session-title");
    var newTitle = content.find("input[name='vrtxTitle']");
    var hasNewTitle = newTitle.length && newTitle.val() != "";
    var origTitle = decodeURI(titleElm.attr("data-orig"));
    titleElm.html((cancelledElm[0].checked ? "<span class='header-status'>" + cs.i18n.cancelled + "</span> - " : "") +
                  "<span class='header-title'>" + (hasNewTitle ? newTitle.val() : origTitle) + "</span>");
    e.stopPropagation();
  });
  contents.on("keyup", "input[name='vrtxTitle']", $.debounce(50, true, function () {
    var content = $(this).closest(onlySessionId ? ".properties" : ".accordion-wrapper");
    var titleElm = content.find(onlySessionId ? ".property-label > .session-title" : "> .header > .session-title");
    var newTitle = content.find("input[name='vrtxTitle']");
    var hasNewTitle = newTitle.length && newTitle.val() != "";
    var origTitle = decodeURI(titleElm.attr("data-orig"));
    titleElm.find(".header-title").html(hasNewTitle ? newTitle.val() : origTitle);
  }));
  
  var editorProperties = vrtxEditor.editorForm.find(".properties");
  editorProperties.hide();
  
  initMultipleInputFields();

  $.when(retrievedScheduleDeferred, vrtxEditor.multipleFieldsBoxesDeferred).done(function() {
    var csRef = cs;
    
    if(csRef.retrievedScheduleData == null) {
      csRef.loadingUpdate("");
      editorProperties.prepend("<p>" + csRef.i18n.noData + "</p>");
      return;
    }
    
    // Remove - TODO: don't generate
    $(".vrtx-json").remove();

    if(onlySessionId) {
      onlySessionId = decodeURIComponent(onlySessionId);
      var sessionOnly = csRef.getSessionOnlyHtml(onlySessionId);
      var html = !sessionOnly ? "<p>" + csRef.i18n.noSessionData + "</p>" : sessionOnly.html;

      contents.find("#vrtx-editor-title-submit-buttons-inner-wrapper > h2")
              .html(csRef.i18n.editOnlySessionTitle + "<a href='javascript:void(0)' class='vrtx-close-dialog-editor'></a>");
      document.title = csRef.i18n.editOnlySessionTitle;
      
      var editorSubmitButtons = vrtxEditor.editorForm.find(".submitButtons");
      
      if(sessionOnly) {
        editorProperties.prepend("<h4 class='property-label'>" + sessionOnly.title + "</h4>" + html);
        csRef.enhanceSession((sessionOnly.skipTier ? (sessionOnly.dtShort != "for" ? sessionOnly.id : sessionOnly.dtShort) : sessionOnly.id), onlySessionId, editorProperties);
        var newButtonsHtml = "<input class='vrtx-button vrtx-embedded-button' id='vrtx-embedded-save-view-button' type='submit' value='" + csRef.i18n.saveView + "' />" +
                             "<input class='vrtx-focus-button vrtx-embedded-button' id='vrtx-embedded-save-button' type='submit' value='" + csRef.i18n.save + "' />" +
                             "<input class='vrtx-button vrtx-embedded-button' id='vrtx-embedded-cancel-button' type='submit' value='" + csRef.i18n.cancel + "' />";

        /* Save and unlock */
        editorSubmitButtons.on("click", "#vrtx-embedded-save-view-button", function(e) {
          editorSubmitButtons.find("#saveAndViewButton").trigger("click");
          e.stopPropagation();
          e.preventDefault();
        });
        
        /* Save */
        editorSubmitButtons.on("click", "#vrtx-embedded-save-button", function(e) {
          editorSubmitButtons.find("#updateAction").trigger("click");
          e.stopPropagation();
          e.preventDefault();
        });
      } else {
        editorProperties.prepend(html);
        var newButtonsHtml = "<input class='vrtx-button vrtx-embedded-button' id='vrtx-embedded-cancel-button' type='submit' value='Avbryt' />";
      }
      
      editorSubmitButtons.prepend(newButtonsHtml);
      contents.find("#vrtx-editor-title-submit-buttons").show();

      /* Cancel is unlock */
      editorSubmitButtons.on("click", "#vrtx-embedded-cancel-button", function(e) {
        var form = $("form[name='unlockForm']");
        var url = form.attr("action");
        var dataString = form.serialize();
        vrtxAdmin.serverFacade.postHtml(url, dataString, {
          success: function (results, status, resp) {
            vrtxEditor.needToConfirm = false;
            csRef.sessionOnlyWindowClose(true);
          }
        });
        e.stopPropagation();
        e.preventDefault();
      });
    } else {
      var html = "<div class='accordion-title'>" + csRef.i18n.titles.plenary + "</div>" +
                 csRef.getActivitiesForTypeHtml("plenary", true) +
                 "<div class='accordion-title'>" + csRef.i18n.titles.group + "</div>" +
                 csRef.getActivitiesForTypeHtml("group", false);
      
      // Add HTML to DOM
      editorProperties.prepend("<div class='vrtx-grouped'>" + html + "</div>"); 
       
      // Accordions - define at run-time
      var accordionOnActivateTier3 = function (id, e, ui, accordion) {
        if(ui.newHeader[0]) { // Enhance multiple fields in session on open
          var sessionId = ui.newHeader[0].id;
          var sessionElm = $(ui.newHeader).closest("div");
          var content = sessionElm.find("> .accordion-content");
        
          csRef.lastId = id;
          csRef.lastSessionId = sessionId;
          csRef.lastElm = content;
          
          csRef.enhanceSession(id, sessionId, content);
        }
        if(ui.oldHeader[0]) { // Update session and accordion title on close
          var sessionId = ui.oldHeader[0].id;
          var sessionElm = $(ui.oldHeader).closest("div");
          var content = sessionElm.find("> .accordion-content");
          
          csRef.saveSession(content, id, sessionId);
        }
      };  
    
      // Tier 2
      var accordionOnActivateTier2 = function (id, isTier1, e, ui, accordion) {
        if(isTier1) {
          accordionOnActivateTier3(id, e, ui, accordion);
        } else {
          if(ui.newHeader[0]) {
            var contentWrp = $(ui.newHeader[0]).parent().find(".accordion-content");
            var optsH5 = {
              elem: contentWrp.find(".vrtx-grouped"),
              headerSelector: "h5",
              onActivate: function (e, ui, accordion) {
                accordionOnActivateTier3(id, e, ui, accordion);
              },
              animationSpeed: 200
            };
            var accH5 = new VrtxAccordion(optsH5);
            accH5.create();
            optsH5.elem.addClass("fast");
          }
        }
      };
      
      // Tier 1
      var accordionOnActivateTier1 = function (isTier1, e, ui, accordion) {
        if(ui.newHeader[0]) {
          var id = ui.newHeader[0].id;
          var contentWrp = $("#" + id).parent().find(".accordion-content");
          if(isTier1) { // Lookup and add sessions HTML to DOM
            if(!contentWrp.children().length) { // If not already added
              contentWrp.html("<div class='vrtx-grouped'>" + csRef.sessionsLookup[id].html + "</div>");
            }
          }
          var optsH4 = {
            elem: contentWrp.find(".vrtx-grouped"),
            headerSelector: "h4",
            onActivate: function (e, ui, accordion) {
              if(!isTier1 && ui.newHeader[0]) { // Lookup and add sessions HTML to DOM
                id = ui.newHeader[0].id;
                var contentWrp = $("#" + id).parent().find(".accordion-content");
                if(!contentWrp.children().length) { // If not already added
                  contentWrp.html("<div class='vrtx-grouped'>" + csRef.sessionsLookup[id].html + "</div>");
                }
              }
              accordionOnActivateTier2(id, isTier1, e, ui, accordion);
            },
            animationSpeed: 200
          };
          var accH4 = new VrtxAccordion(optsH4);
          accH4.create();
          optsH4.elem.addClass("fast");
        }
      };
    
      // Tier 0
      var optsH3 = {
        elem: editorProperties.find("> .vrtx-grouped"),
        headerSelector: "h3",
        onActivate: function (e, ui, accordion) {
          if(ui.newHeader[0]) {
            var ident = $(ui.newHeader[0]).closest(".accordion-wrapper");
            accordionOnActivateTier1(ident.hasClass("skip-tier"), e, ui, accordion);
          }
        },
        animationSpeed: 200
      };
      var accH3 = new VrtxAccordion(optsH3);
      accH3.create();
      optsH3.elem.addClass("fast");
    }
    
    JSON_ELEMENTS_INITIALIZED.resolve();
    
    var waitALittle = setTimeout(function() {      
      csRef.loadingUpdate("");
      editorProperties.show();
    }, 50);
  });
}

function editorDetectChange(sessionId, o1, o2, isCK) { // TODO: use description to check for CK (if textarea)
  if(typeof o1 === "object" && typeof o2 === "object") {
    if(o1.length) { // Array
      if(o1.length !== o2.length) return true;
      for(var i = 0, len = o1.length; i < len; i++) {
        if(editorDetectChange(sessionId, o1[i], o2[i])) return true;
      }
    } else {
      var propCount2 = 0;
      for(prop2 in o2) {
        propCount2++;
      }
      var propCount1 = 0;
      for(prop1 in o1) {
        if(editorDetectChange(sessionId, o1[prop1], o2[prop1], prop1 === "vrtxResourcesText")) return true;
        propCount1++;
      }
      if(propCount1 !== propCount2) return true;
    }
  } else if(typeof o1 === "string" && typeof o2 === "string") {
    if(typeof isCK === "boolean" && isCK) {
      var rteFacade = vrtxEditor.richtextEditorFacade;
      var ckInstance = rteFacade.getInstance("vrtxResourcesText-" + sessionId);
      if (ckInstance && rteFacade.isChanged(ckInstance) && rteFacade.getValue(ckInstance) !== "") {
        return true;
      }
    } else {
      if(o1 !== o2) return true;
    }
  } else if(typeof o1 === "number" && typeof o2 === "number") {
    if(o1 !== o2) return true;
  } else if(typeof o1 !== typeof o2) {
    return true;
  }
  return false;
}