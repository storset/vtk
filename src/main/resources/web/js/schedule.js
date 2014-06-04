/*
 * Course schedule
 *
 */

var scheduleDeferred = $.Deferred();
var scheduleDocumentReady = $.Deferred();
var scheduleStartTime = +new Date();
var scheduleDocReadyEndTime = 0;
var scheduleSupportsThreads = false && typeof Worker === "function";
$(document).ready(function() {
  scheduleDocumentReady.resolve();
  scheduleDocReadyEndTime = +new Date() - scheduleStartTime;
});

function initSchedule() {
  var retrievedScheduleDeferred = $.Deferred();
  var retrievedScheduleData = null;
  var activitiesElm = null;
  var endAjaxTime = 0;
  
  // Don't cache if has returned from editing a session
  var useCache = true;
  var hasEditedKey = "hasEditedSession";
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
  // url = "/vrtx/__vrtx/static-resources/js/tp-test2.json";
  
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
    activitiesElm = $("#activities");
    $("#disabled-js").hide();
    loadingUpdate(scheduleI18n.loadingRetrievingData);
  });
  
  $.when(retrievedScheduleDeferred).done(function() {
    if(retrievedScheduleData == null) {
      $.when(scheduleDocumentReady).done(function() {
        activitiesElm.attr("aria-busy", "error").html("<p>" + scheduleI18n.noData + "</p>");
      });
      scheduleDeferred.resolve();
      return;
    }
    
    loadingUpdate(scheduleI18n.loadingGenerating);
    
    var startMakingThreadsTime = +new Date();
    
    var thread1Finished = $.Deferred(),
        thread2Finished = $.Deferred(),
        htmlPlenary = { tocHtml: "", tablesHtml: "", time: 0 },
        htmlGroup = { tocHtml: "", tablesHtml: "", time: 0 };
        
    // TODO: simplify
    if(retrievedScheduleData["plenary"]) {
      if(scheduleSupportsThreads) {
        startThreadGenerateHTMLForType(JSON.stringify({
          data: retrievedScheduleData["plenary"],
          type: "plenary",
          i18n: scheduleI18n,
          canEdit: schedulePermissions.hasReadWriteNotLocked
        }), htmlPlenary, thread1Finished);
      } else {
        startThreadGenerateHTMLForType(retrievedScheduleData, htmlPlenary, thread1Finished,
          "plenary",
          scheduleI18n,
          schedulePermissions.hasReadWriteNotLocked);
      }
    } else {
      thread1Finished.resolve();
    }
    if(retrievedScheduleData["group"]) {
      if(scheduleSupportsThreads) {
        startThreadGenerateHTMLForType(JSON.stringify({
          data: retrievedScheduleData["group"],
          type: "group",
          i18n: scheduleI18n,
          canEdit: schedulePermissions.hasReadWriteNotLocked
        }), htmlGroup, thread2Finished);
      } else {
        startThreadGenerateHTMLForType(retrievedScheduleData, htmlGroup, thread2Finished,
          "group",
          scheduleI18n,
          schedulePermissions.hasReadWriteNotLocked);
      }
    } else {
      thread2Finished.resolve();
    }

    var endMakingThreadsTime = +new Date() - startMakingThreadsTime;
    
    $.when(thread1Finished, thread2Finished, scheduleDocumentReady).done(function() {
      var html = htmlPlenary.tocHtml + htmlGroup.tocHtml + htmlPlenary.tablesHtml + htmlGroup.tablesHtml;
      
      if(html === "") {
        activitiesElm.attr("aria-busy", "error").html(scheduleI18n.noData);
        scheduleDeferred.resolve();
      } else {
        activitiesElm.attr("aria-busy", "false");
        asyncInnerHTML(/* "<p>Total: " + (+new Date() - scheduleStartTime) + "ms <= ((DocReady: " + scheduleDocReadyEndTime +
                       "ms) || (AJAX-complete: " + endAjaxTime + "ms + Threads invoking/serializing: " + ((endMakingThreadsTime || 0) + (htmlPlenary.parseRetrievedJSONTime || 0) + (htmlGroup.parseRetrievedJSONTime || 0)) +
                       "ms + (Plenary: " + htmlPlenary.time + "ms || Group: " + htmlGroup.time + "ms)))" + (scheduleSupportsThreads ? " [Uses Threads/Web Worker's]</p>" : "</p>") + */ html, function(fragment) {
          activitiesElm[0].appendChild(fragment);
          loadingUpdate("");
          scheduleDeferred.resolve();
        });
      }
      
      // Just in case GC is not sweeping garbage..
      html = "";
      htmlPlenary = { tocHtml: "", tablesHtml: "", time: 0 };
      htmlGroup = { tocHtml: "", tablesHtml: "", time: 0 };
      retrievedScheduleData = null;
      plenaryData = null;
      groupData = null;
      
      // Toggle passed sessions
      $(document).on("click", ".course-schedule-table-toggle-passed", function(e) {
        var link = $(this);
        var table = link.prev();
        table.toggleClass("hiding-passed"); 
        var isHidingPassed = table.hasClass("hiding-passed");
        link.text(isHidingPassed ? scheduleI18n.tableShowPassed : scheduleI18n.tableHidePassed);
        if(!isHidingPassed) {
          $("html, body").finish().animate({ scrollTop: (table.offset().top - 20) }, 100);
        }
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
          var openedEditWindow = popupEditWindow(820, 680, editUrl + "?vrtx=admin&mode=editor&action=edit&embed&sessionid=" + row[0].id, "editActivity");
          refreshWhenRefocused(hasEditedKey);
          e.stopPropagation();
          e.preventDefault();
        });
      }
    });
  });
}

function asyncInnerHTML(HTML, callback) { // http://james.padolsey.com/javascript/asynchronous-innerhtml/
  var temp = document.createElement('div'),
      frag = document.createDocumentFragment();
 temp.innerHTML = HTML;
 (function(){
   if(temp.firstChild){
     frag.appendChild(temp.firstChild);
     setTimeout(arguments.callee, 0);
   } else {
     callback(frag);
   }
 })();
}

function loadingUpdate(msg) {
  var loader = $("#loading-message");
  if(!loader.length) {
    var loaderHtml = "<p id='loading-message'>" + msg + "...</p>";
    $("#activities").attr("aria-busy", "true").append(loaderHtml);
  } else {
    if(msg.length) {
      loader.text(msg + "...");
    } else {
      loader.remove();
    }
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

function refreshWhenRefocused(hasEditedKey) {
  var isVisible = false;
  var visibilityEvent = "visibilitychange";
  var delayCheckVisibility = 450;
  var waitForVisibility = setTimeout(function() {
    if(document.addEventListener) {
      var detectVisibilityChange = function() {
        isVisible = !document.hidden;
        if(isVisible && document.removeEventListener) {
          document.removeEventListener(visibilityEvent, detectVisibilityChange);
        }
      }
      document.addEventListener(visibilityEvent, detectVisibilityChange, false);
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

function startThreadGenerateHTMLForType(data, htmlRef, threadRef, type, scheduleI18n, canEdit) {
  if(scheduleSupportsThreads) { // Use own thread
    var worker = new Worker("/vrtx/__vrtx/static-resources/js/schedule-worker.js");
    worker.onmessage = function(e) {
      finishedThreadGenerateHTMLForType(e.data, htmlRef, threadRef);
    };
    worker.onerror = function(err) {
      finishedThreadGenerateHTMLForType({ tocHtml: "", tablesHtml: "<p>" + err.message + "</p>", time: 0 }, htmlRef, threadRef);
    };
    worker.postMessage(data);
  } else { // Use main thread
    finishedThreadGenerateHTMLForType(generateHTMLForType(data, false, type, scheduleI18n, canEdit), htmlRef, threadRef);
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