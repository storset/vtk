1. jquery.autocomplete.js (v1.0.2) is modified for use in Vortex (see file)

2. jquery.treeview.async.js is modified for use in Vortex with new JSON-data (see file)

3. jquery.vortexTips.js is a modified version of jquery.tinyTips.js for use in Vortex (see file)

4. jquery.forms.js has this added to it in $.fn.ajaxSubmit():
   ------------------------------------------------------------------------------------
   // USIT added name of clicked button
   if(typeof vrtxAdmin !== "undefined" && vrtxAdmin.editorSaveButtonName != "") {
     options.data += "&" + vrtxAdmin.editorSaveButtonName;
   }
   ------------------------------------------------------------------------------------
   
   and in $.fn.formToArray() in loop that adds files to formdata:
   ------------------------------------------------------------------------------------
   // USIT added: possible to skip uploading files
   if(typeof vrtxAdmin !== "undefined" && vrtxAdmin.uploadCopyMoveSkippedFiles[files[j].name]) {
   } else {
     a.push({name: n, value: files[j], type: el.type});
   }
   ------------------------------------------------------------------------------------
   
   and in $.fieldValue() to filter out CSS added by Chrome:
   ------------------------------------------------------------------------------------
   var val = $(el).val();
   try {
     val = val.replace(/<span style="font-size: 14px;">([^<]*)<\/span>/g, "$1", "");
     val = val.replace(/<(p|em|strong|s|ul|ol) style="font-size: 14px;">/g, "<$1>", "");
   } catch(err) {
     vrtxAdmin.log({msg: err});
   }
   return val;
   ------------------------------------------------------------------------------------
   
   API: http://malsup.com/jquery/form/#api