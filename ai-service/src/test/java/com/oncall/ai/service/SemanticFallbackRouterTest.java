package com.oncall.ai.service;

import com.oncall.ai.model.IntentEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Semantic Fallback Router 单测
 *
 * 覆盖：自然语言故障（无 9 关键词）→ ERROR_DIAGNOSIS / 闲聊不误升 /
 *       代码/协议意图 / 空输入 / 防跨意图串扰（故障文本 diag 分须压过 chat 分）。
 * 纯内存实例化，离线确定性，无 Spring 依赖。措辞独立于 eval 数据集。
 */
class SemanticFallbackRouterTest {

    private final SemanticFallbackRouter router = new SemanticFallbackRouter();

    @Test
    @DisplayName("关键词未命中的自然语言故障描述 → 语义兜底判为 ERROR_DIAGNOSIS")
    void keywordFreeFaultDescriptionRoutesToDiagnosis() {
        assertThat(router.decide("两个机房互相访问全断了，端口探测直接被拒")).isEqualTo(IntentEvent.ERROR_DIAGNOSIS);
        assertThat(router.decide("屏幕整个黑掉内容出不来")).isEqualTo(IntentEvent.ERROR_DIAGNOSIS);
        assertThat(router.decide("保存时提示主键重复写不进去")).isEqualTo(IntentEvent.ERROR_DIAGNOSIS);
        assertThat(router.decide("服务退出前日志里有一段堆栈")).isEqualTo(IntentEvent.ERROR_DIAGNOSIS);
        assertThat(router.decide("上游一直拒绝请求怀疑是外部服务问题")).isEqualTo(IntentEvent.ERROR_DIAGNOSIS);
    }

    @Test
    @DisplayName("闲聊/寒暄 → 不触发工具意图（null 或 GENERAL_CHAT，二者等价）")
    void chitChatDoesNotTriggerFallback() {
        assertThat(router.decide("你好呀")).isIn(null, IntentEvent.GENERAL_CHAT);
        assertThat(router.decide("谢谢")).isIn(null, IntentEvent.GENERAL_CHAT);
        assertThat(router.decide("随便聊聊今天的安排")).isIn(null, IntentEvent.GENERAL_CHAT);
        assertThat(router.decide("没什么想问的")).isIn(null, IntentEvent.GENERAL_CHAT);
        // 关键护栏：闲聊绝不升为任何工具意图
        for (String msg : new String[]{"你好呀", "谢谢", "随便聊聊今天的安排", "没什么想问的"}) {
            assertThat(router.decide(msg)).isNotEqualTo(IntentEvent.ERROR_DIAGNOSIS);
            assertThat(router.decide(msg)).isNotEqualTo(IntentEvent.CODE_GENERATION);
            assertThat(router.decide(msg)).isNotEqualTo(IntentEvent.PROTOCOL_QA);
        }
    }

    @Test
    @DisplayName("无关键词的代码生成/协议意图 → 判为对应意图")
    void codeAndProtocolParaphrases() {
        assertThat(router.decide("帮我写个上传文件的封装")).isEqualTo(IntentEvent.CODE_GENERATION);
        assertThat(router.decide("页面跳转时传参的约定想确认下")).isEqualTo(IntentEvent.PROTOCOL_QA);
    }

    @Test
    @DisplayName("空/空白输入 → null（不误判）")
    void blankInputReturnsNull() {
        assertThat(router.decide("")).isNull();
        assertThat(router.decide("   ")).isNull();
        assertThat(router.decide(null)).isNull();
    }

    @Test
    @DisplayName("防跨意图串扰：故障文本诊断分须显著高于闲聊分")
    void faultTextDiagnosisScoreBeatsChat() {
        Map<String, Double> s = router.scores("端口连不上服务起不来帮忙看看日志");
        assertThat(s.get(IntentEvent.ERROR_DIAGNOSIS))
                .isGreaterThan(s.get(IntentEvent.GENERAL_CHAT) + 0.3);
        assertThat(s.get(IntentEvent.ERROR_DIAGNOSIS)).isGreaterThanOrEqualTo(SemanticFallbackRouter.THRESHOLD);
    }

    @Test
    @DisplayName("确定性：同输入多次判定结果一致")
    void deterministic() {
        String msg = "机器能通但端口打不开怀疑服务没监听";
        String a = router.decide(msg);
        for (int i = 0; i < 5; i++) {
            assertThat(router.decide(msg)).isEqualTo(a);
        }
    }
}
