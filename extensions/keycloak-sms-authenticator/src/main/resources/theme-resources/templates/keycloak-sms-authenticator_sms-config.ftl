<#import "template.ftl" as layout>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('phone-number') displayInfo=true; section>
    <#if section = "header">
        ${msg("updateMobileTitle")}
    <#elseif section = "form">
        <form id="kc-sms-config-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="phone-number" class="${properties.kcLabelClass!}">${msg("updateMobileFieldLabel")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="tel" id="phone-number" name="phone-number" class="${properties.kcInputClass!}"
                           <#if phoneNumberHint??>placeholder="${phoneNumberHint}"</#if>
                           <#if phoneNumber??>value="${phoneNumber}"</#if>
                           <#if !isAppInitiatedAction??>required</#if>
                           aria-invalid="<#if messagesPerField.existsError('phone-number')>true</#if>"
                    />

                    <#if messagesPerField.existsError('phone-number')>
                        <span id="input-error-phone-number" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${msg('updateMobileFieldInvalidFormat')}
                        </span>
                    <#else>
                        <span id="input-description-phone-number" class="${properties.kcInputHelperTextBeforeClass!}">
                            ${msg('updateMobileFieldDescription')}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}"/>
                    <#else>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("updateMobileSubmit")}"/>
                    </#if>
                </div>
            </div>
        </form>
    <#elseif section = "info">
        ${msg("updateMobileHello",(username!''))}. ${msg("updateMobileText")}
    </#if>
</@layout.registrationLayout>
