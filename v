diff --git a/src/main/resources/web/themes/default/editor-common.css b/src/main/resources/web/themes/default/editor-common.css
index 72c52eb..2ad5d07 100644
--- a/src/main/resources/web/themes/default/editor-common.css
+++ b/src/main/resources/web/themes/default/editor-common.css
@@ -612,11 +612,11 @@ div#resource\.studentExchangeUniversityAgreementListing\.picture\.preview.no-pre
 }
 
 .cke_chrome .cke_top {
-  box-shadow: none !important;
   background: #ededed !important;
   padding: 0 !important;
-  filter: none !important;
   border-bottom: none !important;
+  box-shadow: none !important;
+  -ms-filter: "none" !important;
 }
 
 /* Contains float alternative fix to make help-menu visible on slide-down (overflowing) */
@@ -627,11 +627,11 @@ div#resource\.studentExchangeUniversityAgreementListing\.picture\.preview.no-pre
 
 .cke_chrome .cke_bottom {
   background: transparent !important;
-  filter: none !important;
   padding: 10px 0 0 0 !important;
   margin: 0 0 -5px 0 !important;
   border-top: none !important;
   box-shadow: none !important;
+  -ms-filter: "none" !important;
 }
 
 .cke_chrome .cke_resizer {
@@ -649,16 +649,16 @@ div#resource\.studentExchangeUniversityAgreementListing\.picture\.preview.no-pre
 }
 
 .cke_chrome .cke_toolgroup {
-  box-shadow: none !important;
   background: #fff !important;
-  filter: none !important;
   margin-bottom: 10px !important;
   border: 1px solid #bbb !important;
+  box-shadow: none !important;
+  -ms-filter: "none" !important;
 }
 
 .cke_chrome .cke_combo_button {
   background: #fff !important;
-  filter: none !important;
+  -ms-filter: "none" !important;
 }
 
 .cke_chrome .cke_combo_text {
