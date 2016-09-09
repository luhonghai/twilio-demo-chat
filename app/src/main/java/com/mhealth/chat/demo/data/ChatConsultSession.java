package com.mhealth.chat.demo.data;

import android.util.Base64;

import com.google.gson.Gson;

import java.util.UUID;

/**
 * Created by luhonghai on 9/5/16.
 */

public class ChatConsultSession {

    public static final String CHAT_CONSULT_PREFIX = "chat_consult_";

    private String patientId;

    private String patientName;

    private String patientAvatar;

    private String doctorId;

    private String sessionId;

    public ChatConsultSession() {

    }

    public ChatConsultSession(String patientId, String patientName, String patientAvatar, String doctorId) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.patientName = patientName;
        this.patientAvatar = patientAvatar;
        this.sessionId = UUID.randomUUID().toString();
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public static String encode(ChatConsultSession chatConsultSession) {
        return Base64.encodeToString(new Gson().toJson(chatConsultSession).getBytes(), Base64.NO_WRAP);
    }

    public static String encodeChannelName(ChatConsultSession chatConsultSession) {
        return CHAT_CONSULT_PREFIX + encode(chatConsultSession);
    }

    public static ChatConsultSession decode(String source) {
        return new Gson().fromJson(new String(Base64.decode(source, Base64.NO_WRAP)), ChatConsultSession.class);
    }

    public static ChatConsultSession decodeChannelName(String source) {
        if (source.length() > CHAT_CONSULT_PREFIX.length()) {
            return decode(source.substring(CHAT_CONSULT_PREFIX.length(), source.length()));
        } else {
            return null;
        }
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientAvatar() {
        return patientAvatar;
    }

    public void setPatientAvatar(String patientAvatar) {
        this.patientAvatar = patientAvatar;
    }
}
