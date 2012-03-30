<#macro genOkCancelButtons nameOk nameCancel msgOk msgCancel>
  <div id="submitButtons">
    <div class="vrtx-focus-button">
      <input type="submit" name="${nameOk}" value="<@vrtx.msg code="${msgOk}" default="Ok"/>" />
    </div>
    <div class="vrtx-button">
      <input type="submit" name="${nameCancel}" value="<@vrtx.msg code="${msgCancel}" default="Cancel"/>" />
    </div>
  </div>
</#macro>