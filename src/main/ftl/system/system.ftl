<#ftl strip_whitespace=true />
<#import "/lib/vtk.ftl" as vrtx />

<!--noscript>
  <div class="message infomessage">${vrtx.getMsg("msg.browser.javascript-off")}</div>
</noscript-->

<#assign lang = vrtx.getMsg("eventListing.calendar.lang", "en") />

<script type="text/javascript"><!--
  if(vrtxAdmin.isIE7 || vrtxAdmin.isIETridentInComp) {
    if(vrtxAdmin.isIETridentInComp) {
      var outdatedBrowserText = '${vrtx.getMsg("msg.browser.msie.comp")}';   
    } else if(vrtxAdmin.isIE7) {   
      var outdatedBrowserText = '${vrtx.getMsg("msg.browser.msie.msie7")}';    
    } else if(vrtxAdmin.isIE6) {
      var outdatedBrowserText = '${vrtx.getMsg("msg.browser.msie.msie6")}';
    }
  }
  
  var cancelI18n = '${vrtx.getMsg("editor.cancel")}',
      datePickerLang = "${lang}",
      loadingSubfolders = '${vrtx.getMsg("manage.load-subfolders")}';
  
  vrtxAdmin.messages = {
    upload: {
      inprogress: '${vrtx.getMsg("uploading.in-progress")}',
      processes: '${vrtx.getMsg("uploading.processes")}',
      existing: {
        title: '${vrtx.getMsg("uploading.existing.title")}',
        skip: '${vrtx.getMsg("uploading.existing.skip")}',
        overwrite: '${vrtx.getMsg("uploading.existing.overwrite")}'
      }
    },
    deleting: {
      inprogress: '${vrtx.getMsg("deleting.in-progress")}'
    },
    move: {
      existing: {
        sameFolder: "${vrtx.getMsg("move.existing.same-folder")}"
      }
    },
    publish: {
      unpublishDateBefore: '${vrtx.getMsg("publishing.edit.invalid.unpublishDateBefore")}',
      unpublishDateNonExisting: '${vrtx.getMsg("publishing.edit.invalid.unpublishDateNonExisting")}'
    },
    courseSchedule: {
      updated: '${vrtx.getMsg("course-schedule.edit.updated")}',
      updatedTitle: '${vrtx.getMsg("course-schedule.edit.updated.title")}'
    },
    dropdowns: {
      createTitle: '${vrtx.getMsg("dropdowns.create.title")}', 
      resourceTitle: '${vrtx.getMsg("dropdowns.resource.title")}',
      editorTitle: '${vrtx.getMsg("dropdowns.editor.title")}',
      publishingTitle: '${vrtx.getMsg("dropdowns.publishing.title")}'
    }
  }
  
  vrtxAdmin.serverFacade.errorMessages = {
    title: "${vrtx.getMsg('ajaxError.title')}", 
    general: "${vrtx.getMsg('ajaxError.general')}",
    timeout: "${vrtx.getMsg('ajaxError.timeout')}",
    abort: "${vrtx.getMsg('ajaxError.abort')}",
    parsererror: "${vrtx.getMsg('ajaxError.parsererror')}", 
    offline: "${vrtx.getMsg('ajaxError.offline')}",
    lockStolen: "${vrtx.getMsg('ajaxError.lockStolen')}",
    lockStolenTitle: "${vrtx.getMsg('ajaxError.lockStolen.title')}",
    lockStolenOk: "${vrtx.getMsg('ajaxError.lockStolen.ok')}",
    outOfDate: "${vrtx.getMsg('ajaxError.out-of-date')}",
    outOfDateTitle: "${vrtx.getMsg('ajaxError.out-of-date.title')}",
    outOfDateOk: "${vrtx.getMsg('ajaxError.out-of-date.ok')}",
    cantBackupFolderTitle: "${vrtx.getMsg('ajaxError.cant-backup-folder.title')}",
    cantBackupFolder: "${vrtx.getMsg('ajaxError.cant-backup-folder')}",
    uploadingFilesFailedTitle: "${vrtx.getMsg('ajaxError.uploading-files.title')}",
    uploadingFilesFailed: "${vrtx.getMsg('ajaxError.uploading-files')}",
    sessionInvalidOk: "${vrtx.getMsg('ajaxError.sessionInvalid.ok')}",
    sessionInvalidOkInfo: "${vrtx.getMsg('ajaxError.sessionInvalid.ok.info')}",
    sessionWaitReauthenticate: "${vrtx.getMsg('ajaxError.sessionInvalid.waitReauthenticate')}",
    sessionValidatedTitle: "${vrtx.getMsg('ajaxError.sessionValidated.title')}",
    sessionInvalidSave: "${vrtx.getMsg('ajaxError.sessionInvalid.save')}",
    sessionInvalidTitleSave: "${vrtx.getMsg('ajaxError.sessionInvalid.save.title')}",
    sessionValidatedSave: "${vrtx.getMsg('ajaxError.sessionValidated.save')}",
    sessionValidatedOkSave: "${vrtx.getMsg('ajaxError.sessionValidated.save.ok')}",
    sessionInvalid: "${vrtx.getMsg('ajaxError.sessionInvalid')}",
    sessionInvalidTitle: "${vrtx.getMsg('ajaxError.sessionInvalid.title')}",
    sessionValidated: "${vrtx.getMsg('ajaxError.sessionValidated')}",
    sessionValidatedOk: "${vrtx.getMsg('ajaxError.sessionValidated.ok')}",
    down: "${vrtx.getMsg('ajaxError.down')}",
    s500: "${vrtx.getMsg('ajaxError.s500')}",
    s400: "${vrtx.getMsg('ajaxError.s400')}",
    s401: "${vrtx.getMsg('ajaxError.s401')}",
    s403: "${vrtx.getMsg('ajaxError.s403')}",
    s404: "${vrtx.getMsg('ajaxError.s404')}",
    s423: "${vrtx.getMsg('ajaxError.s423')}",
    s4233: "${vrtx.getMsg('ajaxError.s423.parent')}", 
    customTitle: {
      "0": "${vrtx.getMsg('ajaxError.offline.title')}",
      "4233": "${vrtx.getMsg('ajaxError.s423.parent.title')}"
    }
  };
  if(vrtxAdmin.hasFreeze) { // Make immutable
    Object.freeze(vrtxAdmin.messages);
    Object.freeze(vrtxAdmin.serverFacade.errorMessages);
  }
// -->
</script>