<#ftl strip_whitespace=true>

<#--
  - File: preview-iframe.ftl
  - 
  - Description: A HTML page with a <iframe> tag to the previewed resource
  - 
  - Dynamic resizing of iframe only works in IE and Firefox. 
  - 
  - Required model data:
  -   resourceReference
  -   resourceContext
  -  
  - Optional model data:
  -   title
  -
  -->

<#import "/lib/vortikal.ftl" as vrtx />

<#if !resourceReference?exists>
  <#stop "Unable to render model: required submodel
  'resourceReference' missing">
</#if>
<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${(title.title)?default(resourceContext.currentResource.name)}</title>

  <script language="javascript" >
  $(document).ready(function(){
	$('iframe').load(function() {
		$("iframe").contents().find("a").each(function(i, e){
			this.target = "_parent";	
		});
		<#if visualizeBrokenLinks?exists && visualizeBrokenLinks = 'true'>
		  $("iframe").contents().find("body")
		    .filter(function() {
              return this.id.match(/^(?!vrtx-[\S]+-listing|vrtx-collection)[\S]+/);
            })
            .find("#main")
            .not("#left-main")
	        .find("a").each(function(i, e){
	          alert('hei');
		      visualizeDeadLink(this, e);
          });
       </#if>
	});	
  });	
  </script>
  
  </head>
  <body>

    <#if !previewRefreshParameter?exists>
      <#assign previewRefreshParameter = 'force-refresh' />
    </#if>

    <#assign constructor = "freemarker.template.utility.ObjectConstructor"?new() />
    <#assign dateStr = constructor("java.util.Date")?string("yyyymmddhhmmss") />

    <#assign url = resourceReference />
    <#if url?contains("?")>
      <#assign url = url + "&amp;" + previewRefreshParameter + "=" + dateStr />
    <#else>
      <#assign url = url + "?" + previewRefreshParameter + "=" + dateStr />
    </#if>

    <iframe class="preview" name="previewIframe" id="previewIframe" src="${url}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" vspace="0" hspace="0" style="overflow:visible; width:100%; ">
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <a href="${resourceReference}">the related document.</a>]
    </iframe>

    <#-- iframe name="previewIframe" id="previewIframe" class="preview" src="${url}">
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <a href="${resourceReference}">the related document.</a>]
    </iframe -->

    <#--
    <noframes>
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <a href="${resourceReference}">the related document.</a>]
    -->


    <#-- We could also use the 'object' tag: -->
    <#-- 
    <object id="previewObject" class="preview" data="${resourceReference}"
            type="${resourceContext.currentResource.*contentType*}">
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <a href="${resourceReference}">the related document.</a>]
    </object>
    -->


  </body>
</html>


