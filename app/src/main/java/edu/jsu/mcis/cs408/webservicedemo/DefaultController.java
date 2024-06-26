package edu.jsu.mcis.cs408.webservicedemo;

import org.json.JSONObject;

public class DefaultController extends AbstractController {

    public static final String ELEMENT_OUTPUT_PROPERTY = "Output";

    public void changeOutputText(String newText) {
        setModelProperty(ELEMENT_OUTPUT_PROPERTY, newText);
    }

    public void sendGetRequest() {
        invokeModelMethod("sendGetRequest", null);
    }

    public void sendPostRequest() {
        invokeModelMethod("sendPostRequest", null);
    }

    public void sendDeleteRequest() {
        invokeModelMethod("sendDeleteRequest", null);
    }

}