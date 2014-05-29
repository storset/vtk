/*
 * Course schedule
 *
 *
 * Performance credits:
 * http://stackoverflow.com/questions/5080028/what-is-the-most-efficient-way-to-concatenate-n-arrays-in-javascript
 * http://blog.rodneyrehm.de/archives/14-Sorting-Were-Doing-It-Wrong.html
 * (http://en.wikipedia.org/wiki/Schwartzian_transform)
 * http://jsperf.com/math-ceil-vs-bitwise/10
 */

var scheduleDeferred = $.Deferred();
var scheduleDocumentReady = $.Deferred();
var scheduleStartTime = +new Date();
var scheduleDocReadyEndTime = 0;
var scheduleSupportsThreads = typeof Worker === "function";
$(document).ready(function() {
  scheduleDocumentReady.resolve();
  scheduleDocReadyEndTime = +new Date() - scheduleStartTime;
});

function initSchedule() {
  var retrievedScheduleDeferred = $.Deferred();
  var retrievedScheduleData = null;
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
    if(groupData) {
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
        $("#activities").attr("aria-busy", "false").html("<p>Total: " + (+new Date() - scheduleStartTime) + "ms <= ((DocReady: " + scheduleDocReadyEndTime +
                            "ms) || (AJAX-complete: " + endAjaxTime + "ms + Threads invoking/serializing: " + (endMakingThreadsTime + htmlPlenary.parseRetrievedJSONTime + htmlGroup.parseRetrievedJSONTime) +
                            "ms + (Plenary: " + htmlPlenary.time + "ms || Group: " + htmlGroup.time + "ms)))" + (scheduleSupportsThreads ? " [Uses Threads/Web Worker's]</p>" : "</p>") + html);
      }
      
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
          var openedEditWindow = popupEditWindow(850, 680, editUrl + "?vrtx=admin&mode=editor&action=edit&embed&sessionid=" + row[0].id, "editActivity");
          refreshWhenRefocused(hasEditedKey);
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

function refreshWhenRefocused(hasEditedKey) {
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