<#-- Adds the default required scripts necessary to use show and hide functionality -->
<#macro addShowHideScripts srcBase="">  
	  <script language="Javascript" type="text/javascript">
			function setShowHide(name, parameters){
				  $("#" + name + "-true").each( 
				  	function(){
				  		if(this.checked){
					  		for(i = 0;i<parameters.length;i++){
					  			$("div." + parameters[i]).hide();
					  		}
				  		}
				  	}
				  );	 
				  $("#" + name + "-false").each( 
				  	function(){
				  		if(this.checked){
					  		for(i = 0;i<parameters.length;i++){
					  			$("div." + parameters[i]).show();
					  		}
				  		}
				  	}
				  );
			}
	  </script>
</#macro>

<#macro addShowHide script>
  <#local serviceId = script.name />
  <#local parameters = '' />
  <#list script.params?keys as param>
   	<#list script.params[param] as value >
   		<#if parameters == ''>
      	  	<#local parameters = "'" + value?string + "'"/>
      	<#else>
        	<#local parameters = parameters + ", '" + value?string + "'" />
      </#if>
   	</#list>
  </#list>
  var parameters = new Array(${parameters});
  var name = new String('${script.name}');  
  setShowHide(name,parameters); 
  $("[name=${script.name}]").click(
  	function(){
  		setShowHide(name,parameters);
  	}
  );
</#macro>