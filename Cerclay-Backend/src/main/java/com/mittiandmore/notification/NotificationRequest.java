package com.mittiandmore.notification;

import jakarta.validation.constraints.NotBlank;

public class NotificationRequest {
    private boolean enabled;
    @NotBlank private String subjectTemplate;
    @NotBlank private String bodyTemplate;
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
    public String getSubjectTemplate(){return subjectTemplate;} public void setSubjectTemplate(String v){subjectTemplate=v;}
    public String getBodyTemplate(){return bodyTemplate;} public void setBodyTemplate(String v){bodyTemplate=v;}
}
