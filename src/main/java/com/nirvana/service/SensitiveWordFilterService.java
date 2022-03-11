package com.nirvana.service;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

@Path("")
public class SensitiveWordFilterService {

    public static RateLimiter rateLimiter;

    public interface Code {

        FilterResult SIGN_INVALID = new FilterResult(1000, 1, "");
        FilterResult APP_ID_INVALID = new FilterResult(1001, 1, "");
        FilterResult SERVICE_BUSY = new FilterResult(1002, 1, "");
        FilterResult TIMEOUT_REQ = new FilterResult(1003, 1, "");
        FilterResult EXCEPTION = new FilterResult(2000, 1, "");
    }

    @POST
    @Path("/word_filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FilterResult word_filter(@FormParam String content, @FormParam String app_id, @FormParam String timestamp,
        @FormParam String sign) {
        try {
            if (!rateLimiter.tryAcquire()) {
                return Code.SERVICE_BUSY;
            }

            if (TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Long.parseLong(timestamp)) > 30) {
                return Code.TIMEOUT_REQ;
            }

            Optional<String> keyOptional = getKey(app_id);
            if (keyOptional.isEmpty()) {
                return Code.APP_ID_INVALID;
            }

            Map<String, String> p = Maps.newHashMap();
            p.put("content", content);
            p.put("app_id", app_id);
            p.put("timestamp", timestamp);
            String mySign = Encode(HashCodeType.MD5, (sortMapValue(p) + keyOptional.get()).getBytes());
            if (!mySign.equals(sign)) {
                return Code.SIGN_INVALID;
            }

            String rawContent = new String(Base64.getDecoder().decode(URLDecoder.decode(content, "UTF-8")), "UTF-8");
            String filteredContent = SensitiveWordUtil.replaceSensitiveWord(rawContent);

            return new FilterResult(0, filteredContent.equals(rawContent) ? 0 : 1, filteredContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Code.EXCEPTION;
    }

    private Optional<String> getKey(String app_id) {
        if (app_id.isBlank()) {
            return Optional.empty();
        }
        if (app_id.equals("wymyr")) {
            return Optional.of("WangYuan2022");
        }
        return Optional.empty();
    }

    public static enum HashCodeType {
        MD5("MD5"), SHA("SHA"), SHA256("SHA-256"), SHA512("SHA-512");

        private final String _value;

        private HashCodeType(String value) {
            this._value = value;
        }

        public String getValue() {
            if (this._value == null) {
                return "";
            }
            return this._value;
        }
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if ((src == null) || (src.length <= 0)) {
            return "";
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String Encode(HashCodeType typeCode, byte[] message) {
        String encode = null;
        try {
            MessageDigest md = MessageDigest.getInstance(typeCode.getValue());
            encode = bytesToHexString(md.digest(message));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encode;
    }

    public static String sortMapValue(Map<String, String> keys) throws Exception {
        StringBuffer sb = new StringBuffer();
        Object[] keyArray = keys.keySet().toArray();
        Arrays.sort(keyArray);
        for (Object s : keyArray) {
            sb.append(keys.get(s));
        }
        return sb.toString();
    }
}