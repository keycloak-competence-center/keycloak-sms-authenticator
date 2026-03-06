<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('code') displayInfo=true; section>
    <#if section = "header">
        ${msg("smsAuthTitle",realm.displayName)}
    <#elseif section = "form">
        <#if smsResent?? && smsResent>
            <div class="alert alert-info">
                <span class="pficon pficon-info"></span>
                <span>${msg("smsAuthCodeResent")}</span>
            </div>
        </#if>
        <form id="kc-sms-code-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="code" class="${properties.kcLabelClass!}">${msg("smsAuthLabel")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="code" name="code" class="${properties.kcInputClass!}" autofocus
                           aria-invalid="<#if messagesPerField.existsError('code')>true</#if>"
                    />

                    <#if messagesPerField.existsError('code')>
                        <span id="input-error-code" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('code'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>
            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <#if resetPhoneNumberUri??>
                            <span><a href="${resetPhoneNumberUri}">${kcSanitize(msg("smsPhoneNumberReset"))?no_esc}</a></span>
                        </#if>
                        <span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}"/>
                </div>
                <div class="${properties.kcFormButtonsClass!}" style="margin-top: 0.5rem;">
                    <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" name="resend-sms" value="true">${msg("doResend")}</button>
                </div>
            </div>
        </form>
    <#elseif section = "info" >
        <#if showPhoneNumber?? && showPhoneNumber>
            ${msg("smsAuthInstructionWithNumber", mobileNumber)}
        <#else>
            ${msg("smsAuthInstruction")}
        </#if>
    </#if>
</@layout.registrationLayout>