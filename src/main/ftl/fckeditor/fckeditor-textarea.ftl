<#import "/lib/vortikal.ftl" as vrtx />

<#if !fckeditorBase?exists>
  <#stop "fckeditorBase attribute must exist in model" />
</#if>


<#--
 * editorInTextArea
 * 
 * Writes a <script> reference to the main fckeditor.js file
 *
 -->
<#macro declareEditor>
  <#if !__editorDeclared?exists>
    <script type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
    <script type="text/javascript"><!--
      function editorAvailable() {
        return FCKeditor_IsCompatibleBrowser();
      }
      // -->
    </script>
    <#assign __editorDeclared = true />
  </#if>
</#macro>


<#--
 * editorInTextArea
 *
 * Display a minimal FCKeditor in a div.
 *
 * @param textarea - the id of the textarea to replace with FCKeditor
 * @param fckeditorBase - the FCKeditor config (required to contain a 'url' entry)
 * @param runOnLoad - whether to run the editor immediately or wait
 *        until the JavaScript function loadEditor() is
 *        invoked. Defaults to 'false'.
 * @validElements - a list of [name, attribute-list] maps that
 *        describe the valid HTML elements
 * @toolbarElements - a list of strings that describe the set of
 *        editor toolbar elements to use
 *
-->

<#macro editorInTextarea
        textarea
        toolbar='Complete'
        fontFormats='p;h1;h2;h3;h4;h5;h6;pre'
        fullpage=false
        collapseToolbar=false
        runOnLoad=true
        enableFileBrowsers=true
        fckSkin='editor/skins/silver/'>

    <#if !__editorDeclared?exists>
      <@declareEditor />
    </#if>

    <script type="text/javascript">
      var initialized = false;

      function loadEditor() {
          if (initialized) return;
          var editor = new CKEDITOR;
          editor.BasePath = '${fckeditorBase.url?html}/';
          editor.config['DefaultLanguage'] = '<@vrtx.requestLanguage />';

          editor.config['CustomConfigurationsPath'] = '${fckeditorBase.url?html}/custom-fckconfig.js';
          editor.ToolbarSet = '${toolbar}';
          <#if enableFileBrowsers && !fckBrowse?exists>
           <#stop "parameter 'enableFileBrowsers' requires that model attribute 'fckBrowse' exists">
          <#elseif enableFileBrowsers>
          var baseFolder = "${resourceContext.parentURI?html}";
          
          editor.config.filebrowserLinkBrowseURL  = '${fckeditorBase.url?html}/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=${fckBrowse.url.pathRepresentation}';
          editor.config.filebrowserImageBrowseURL = '${fckeditorBase.url?html}/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}';
          editor.config.filebrowserFlashBrowseURL = '${fckeditorBase.url?html}/plugins/filemanager/browser/cddefault/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=${fckBrowse.url.pathRepresentation}';
          
          </#if>
          editor.config['FontFormats'] = '${fontFormats}';
          editor.config['FullPage'] = ${fullpage?string};
          editor.config['ToolbarCanCollapse'] = ${collapseToolbar?string};
          editor.config.LinkBrowser = ${enableFileBrowsers?string};
          editor.config.LinkUpload = false;

	  	  editor.config.EMailProtection = 'none';
          editor.config.ImageBrowser = ${enableFileBrowsers?string};
          editor.config.ImageUpload = ${enableFileBrowsers?string};
          editor.config.FlashBrowser = ${enableFileBrowsers?string};
          editor.config.FlashUpload = ${enableFileBrowsers?string};
          <#-- editor.config.BaseHref = '${fckeditorBase.documentURL?html}'; -->
          editor.config['SkinPath'] = editor.BasePath + '${fckSkin}';
	  	  editor.config['EditorAreaCSS'] = "<#list fckEditorAreaCSSURL as url>${url?html}<#if url_has_next>,</#if></#list>";

          editor.replaceAll();

          initialized = true;
      }
      <#if runOnLoad>
        loadEditor();
      </#if>
    </script>
</#macro>
