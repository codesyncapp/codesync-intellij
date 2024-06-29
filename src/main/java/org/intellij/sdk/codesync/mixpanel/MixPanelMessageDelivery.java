package org.intellij.sdk.codesync.mixpanel;

import org.json.JSONObject;

import com.mixpanel.mixpanelapi.MixpanelAPI;
import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MixPanelMessageDelivery {
    public static String PROJECT_TOKEN = "be519a94c192acd76a504cb6be80e7e5"; // "YOUR TOKEN";
    String distinctId = "12345";
    private final MixpanelAPI mMixpanel;
    public MixPanelMessageDelivery () {
        mMixpanel = new MixpanelAPI();
    }

    public void sendMessage(String message) {
        MessageBuilder messageBuilder = new MessageBuilder(PROJECT_TOKEN);
        Map<String, String> namePropsMap = new HashMap<String, String>();
        namePropsMap.put("$first_name", distinctId);
        namePropsMap.put("message", message);
        JSONObject nameProps = new JSONObject(namePropsMap);
        JSONObject nameMessage = messageBuilder.set(distinctId, nameProps);
        ClientDelivery delivery = new ClientDelivery();
        delivery.addMessage(nameMessage);
        try {
            mMixpanel.deliver(delivery);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
