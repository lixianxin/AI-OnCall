package com.oncall.ai.model;

/**
 * 消息类型
 *
 * 支持后续 Agent 编排时区分文本/工具调用/系统/错误消息。
 */
public enum MessageType {
    TEXT,
    TOOL,
    SYSTEM,
    ERROR
}
