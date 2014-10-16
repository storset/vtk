diff --git a/src/main/ftl/structured-resources/editor.ftl b/src/main/ftl/structured-resources/editor.ftl
index ba5a309..d302dfb 100644
--- a/src/main/ftl/structured-resources/editor.ftl
+++ b/src/main/ftl/structured-resources/editor.ftl
@@ -51,7 +51,7 @@
       vrtxAdm.mapShortcut("#vrtx-send-to-approval-shortcut", "#vrtx-send-to-approval");
       
       // Cancel action
-      _$("#editor").on("click", "#cancelAction", function(e) {
+      _$("#editor").on("click", "#cancelAction, #saveWorkingCopyAction, #makePublicVersionAction, #deleteWorkingCopyAction", function(e) {
         vrtxEditor.needToConfirm = false;
       });
     });
