<#ftl strip_whitespace=true>

<#--
 * alternativeViewCheckbox
 *
 * Display a checkbox for switching between an alternative view in a report (JS-based url change)
 * 
 * @param alternativeName - alternative name from report
 * @param isAlternativeView - if alternative name exists as a parameter in request
 *
-->

<#macro alternativeViewCheckbox alternativeName alternativeLabelText isAlternativeView=false>
  <div id="is-${alternativeName?html}-view" class="vrtx-checkbox vrtx-report-alternative-view-switch">
    <input name="${alternativeName?html}" id="is-${alternativeName?html}" type="checkbox" <#if isAlternativeView>checked="checked"</#if> />
    <label for="is-${alternativeName?html}"><@vrtx.msg code=alternativeLabelText /></label>
  </div>
</#macro>