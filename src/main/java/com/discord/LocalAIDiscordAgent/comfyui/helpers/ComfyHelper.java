package com.discord.LocalAIDiscordAgent.comfyui.helpers;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ComfyHelper {

    @NotNull
    public static Map<String, Object> getStringObjectMap(Map<String, Object> workflow, String nodeId) {
        Object nodeObj = workflow.get(nodeId);
        if (!(nodeObj instanceof Map<?, ?> nodeMapRaw)) {
            throw new IllegalStateException("Node not found: " + nodeId);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> nodeMap = (Map<String, Object>) nodeMapRaw;

        Object inputsObj = nodeMap.get("inputs");
        if (!(inputsObj instanceof Map<?, ?> inputsRaw)) {
            throw new IllegalStateException("Inputs not found for node: " + nodeId);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> inputs = (Map<String, Object>) inputsRaw;
        return inputs;

    }
}
