package com.oncall.ai.service;

import com.oncall.ai.model.IntentEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Semantic Fallback Router —— 意图路由的语义兜底通道（受控实验变量）。
 *
 * 设计背景（Before/After 实验，2026-09-08）：
 *   关键词快速通道（Keyword Fast Path）在自然语言故障描述上存在关键词依赖：
 *   v2 分层数据集中 65/120 诊断样本因不含 9 个诊断关键词被误送 GENERAL_CHAT/CODE_GENERATION。
 *   本组件只在关键词未命中时兜底判定，不改动关键词优先级，保证实验单一变量。
 *
 * 相似度口径：
 *   特征 = CJK unigram + CJK bigram + ASCII token(>=2)，三元特征
 *   权重 = IDF（按意图原型语料统计，公共字自动降权；不做词频加权，避免长原型语料主导打分）
 *   度量 = 加权包含度 containment(intent) = Σ_{f∈Q∩P} idf(f) / Σ_{f∈Q} idf(f)
 *   （unigram 语义噪声大，降权至 0.4；阈值 0.30 于独立 DEV 集冻结）
 *
 * 完全离线、确定性、无外部模型/网络依赖；纯 JDK，无第三方库。
 */
@Component
public class SemanticFallbackRouter {

    /** 冻结阈值：最高分意图须 ≥ 该值才生效，否则返回 null（交由调用方默认 GENERAL_CHAT） */
    static final double THRESHOLD = 0.30;

    /** unigram 降权系数（bigram/token 语义精度更高） */
    static final double UNIGRAM_WEIGHT = 0.40;

    /** 意图原型语料（措辞独立于 eval 数据集模板，防评测泄漏；DEV 集调参用） */
    private static final Map<String, List<String>> PROTOTYPES = buildPrototypes();

    /** intent -> 特征计数（该意图全部原型拼接后的词袋） */
    private final Map<String, Map<String, Integer>> profiles = new HashMap<>();

    /** 特征 -> IDF（df = 该特征出现在几个意图的原型语料中） */
    private final Map<String, Double> idf = new HashMap<>();

    public SemanticFallbackRouter() {
        int n = PROTOTYPES.size();
        // 1) 各意图词袋
        Map<String, Set<String>> intentFeatureSets = new HashMap<>();
        for (Map.Entry<String, List<String>> e : PROTOTYPES.entrySet()) {
            Map<String, Integer> bag = new HashMap<>();
            Set<String> set = new HashSet<>();
            for (String text : e.getValue()) {
                for (String f : featurize(text)) {
                    bag.merge(f, 1, Integer::sum);
                    set.add(f);
                }
            }
            profiles.put(e.getKey(), bag);
            intentFeatureSets.put(e.getKey(), set);
        }
        // 2) IDF：ln((1+N)/(1+df)) + 1
        for (Set<String> set : intentFeatureSets.values()) {
            for (String f : set) {
                idf.put(f, idf.getOrDefault(f, 0.0) + 1.0);
            }
        }
        for (Map.Entry<String, Double> e : idf.entrySet()) {
            double df = e.getValue();
            e.setValue(Math.log((1.0 + n) / (1.0 + df)) + 1.0);
        }
    }

    /**
     * 语义兜底判定：返回最佳意图常量（{@link IntentEvent}），低于阈值或无信号返回 null。
     */
    public String decide(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        List<String> qf = featurize(message.toLowerCase());
        if (qf.isEmpty()) {
            return null;
        }
        String best = null;
        double bestScore = -1.0;
        for (Map.Entry<String, Map<String, Integer>> e : profiles.entrySet()) {
            double s = containment(qf, e.getValue());
            if (s > bestScore) {
                bestScore = s;
                best = e.getKey();
            }
        }
        return bestScore >= THRESHOLD ? best : null;
    }

    /** 供单测/观测使用的带分版本 */
    public Map<String, Double> scores(String message) {
        List<String> qf = featurize(message == null ? "" : message.toLowerCase());
        Map<String, Double> out = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : profiles.entrySet()) {
            out.put(e.getKey(), containment(qf, e.getValue()));
        }
        return out;
    }

    private double containment(List<String> queryFeatures, Map<String, Integer> profileBag) {
        double num = 0.0, den = 0.0;
        for (String f : new HashSet<>(queryFeatures)) {
            double w = idf.getOrDefault(f, 0.0);
            if (f.charAt(0) == 'u') {
                w *= UNIGRAM_WEIGHT;
            }
            den += w;
            if (profileBag.containsKey(f)) {
                num += w;
            }
        }
        return den == 0.0 ? 0.0 : num / den;
    }

    /** 特征化：返回 "u|<char>" / "b|<bigram>" / "t|<token>" 三元键列表 */
    static List<String> featurize(String text) {
        String s = text.toLowerCase();
        List<String> feats = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 0x4e00 && c <= 0x9fff) {
                run.append(c);
                feats.add("u|" + c);
                if (run.length() >= 2) {
                    feats.add("b|" + run.substring(run.length() - 2));
                }
            } else {
                run.setLength(0);
            }
        }
        for (String tok : s.toLowerCase().split("[^a-z0-9]+")) {
            if (tok.length() >= 2) {
                feats.add("t|" + tok);
            }
        }
        return feats;
    }

    private static Map<String, List<String>> buildPrototypes() {
        Map<String, List<String>> m = new HashMap<>();
        m.put(IntentEvent.ERROR_DIAGNOSIS, List.of(
            // 连接 / 端口 / 网络
            "服务之间调用不通连接被拒绝需要排查",
            "端口连不上主机访问不了像是网络或者服务问题",
            "机房之间互相访问全部断掉端口探测被拒",
            "机器能通但端口打不开怀疑服务没监听",
            "新装用户连不上站点怀疑握手或证书环节",
            "升级证书之后访问不了怀疑加密通道问题",
            "HTTPS 打不开一直在转圈怀疑证书链有问题",
            "弱网用户请求总是断怀疑连接不稳定",
            // 服务 / 进程 / 日志
            "服务进程退出日志末尾有大段堆栈",
            "功能跑到一半就挂掉日志里有线索帮忙分析",
            "程序卡住不往下走怀疑哪里堵住没释放",
            "服务重启之后一直起不来需要看日志定位",
            "应用用一会儿就闪退退回桌面",
            "页面卡死不动只能强杀进程",
            "系统服务看起来正常但业务表现异常",
            // 响应 / 中断 / 流式
            "接口迟迟没有返回等不到响应怀疑依赖慢",
            "回答生成到一半就停住等不到后续内容",
            "长输出途中断掉前端拿不到完整结果",
            "请求经常卡到超时限制才返回怀疑下游慢",
            "输出内容突然中断怀疑链路被断开",
            // 界面 / 渲染
            "启动之后屏幕整块黑着画面出不来",
            "页面白屏或者黑屏怀疑渲染链路有问题",
            "模拟器能起来但画面全黑内容不显示",
            "界面打不开一直白屏或者转圈",
            // 数据 / 存储
            "数据写不进去提示主键重复或者约束冲突",
            "保存时记录冲突怀疑是存储层报错",
            "订单或者库存写入失败怀疑数据库异常",
            "定时任务跑到一半被数据库拦下",
            // 外部依赖 / 供应商
            "上游接口返回拒绝状态码怀疑外部服务问题",
            "大模型供应商接口拒绝请求功能集体不可用",
            "外部依赖返回错误码所有请求都失败",
            // 通用故障
            "某个功能突然表现不正常希望帮忙定位",
            "问题间歇性出现怀疑是偶发故障",
            "新版本上线后出现异常需要分析日志找原因",
            "某个环境行为不一致需要对比排查",
            "帮忙看看日志里有没有报错线索",
            "故障现象复现不了想先分析现有证据",
            "系统变慢怀疑某处资源没有被释放",
            "请求被服务端直接断开连接异常终止",
            "证书校验环节报错导致访问失败",
            "写入时提示唯一键重复无法继续",
            "查询结果为空怀疑过滤条件或者数据问题",
            "接口偶发拿不到结果需要看日志确认",
            "机器负载很高怀疑有进程异常",
            "日志显示握手过程被中断连接建立失败"));
        m.put(IntentEvent.CODE_GENERATION, List.of(
            "请帮我写一段程序实现业务逻辑",
            "需要一个类的完整实现代码",
            "帮我用某个框架搭建一个页面",
            "生成数据访问层的模板代码",
            "把接口调用封装成可复用组件",
            "写一个工具函数处理数据转换",
            "帮我实现列表加载的代码结构",
            "给我一段可运行的示例代码",
            "设计接口的数据模型类",
            "写个脚本批量处理文件",
            "帮我写个网络请求的封装工具",
            "给一段把列表数据渲染出来的示例",
            "想要一个分页拉取数据的组件",
            "写个把 json 转对象的工具类",
            "生成一个登录页面的代码",
            "实现下拉刷新和加载更多的逻辑",
            "写一个文件上传的封装方法",
            "帮我写单元测试的骨架代码",
            "给一个状态管理模块的示例",
            "写个定时任务的调度代码"));
        m.put(IntentEvent.PROTOCOL_QA, List.of(
            "接口协议里字段如何定义想查一下文档",
            "页面之间跳转传参的约定规则是什么",
            "清单文件里怎么声明组件和路由配置",
            "对外开放的能力调用规范在哪里说明",
            "权限校验的流程说明想看下协议文档",
            "模块之间的通信方式有什么约定",
            "注册表项的含义需要查资料确认",
            "功能开关的配置项作用是什么",
            "组件怎么在容器里声明才能被唤起",
            "对外暴露的接口调用规范看哪份说明",
            "页面导航的路由表在哪里维护",
            "数据同步的协议格式说明文档",
            "插件和主程序之间如何约定交互",
            "组件生命周期里哪些回调会被调用"));
        m.put(IntentEvent.GENERAL_CHAT, List.of(
            "你好很高兴认识你",
            "随便聊聊今天过得怎么样",
            "你是谁你能帮我做什么",
            "感谢你耐心回答我的问题",
            "你觉得我接下来学什么方向好",
            "今天天气不错心情很好",
            "周末有什么放松的好建议",
            "随便说点什么吧",
            "哈哈这个笑话很有意思",
            "我只是来打个招呼的",
            "没什么想问的谢谢",
            "随便聊聊日常话题",
            "你觉得今天适合做什么",
            "我在学习想聊聊方法",
            "谢谢你的帮助"));
        return Map.copyOf(m);
    }
}
