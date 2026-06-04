# SSLException

## 错误特征

SSLException、SSLHandshakeException、SSLPeerUnverifiedException、
证书、certificate、cert、handshake failure、
javax.net.ssl.SSLHandshakeException、trust anchor、
CertPathValidatorException、unable to find valid certification path

## 根因

客户端与服务端的 SSL/TLS 握手失败。

常见原因：
- 服务端使用了自签名证书，客户端不信任
- 客户端 CA 证书库未包含服务端证书链
- 证书已过期
- 域名与证书 CN/SAN 不匹配
- Android 低版本不支持 TLS 1.2/1.3

## 排查步骤

1. 确认服务端证书是否由可信 CA 签发
2. 检查证书是否过期：openssl x509 -in cert.pem -text -noout
3. 确认访问域名与证书 CN/SAN 匹配
4. Android 端检查 SSLContext 配置

## 解决方案

```java
// 方案1：信任所有证书（仅开发环境）
TrustManager[] trustAll = new TrustManager[]{
    new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] c, String a) {}
        public void checkServerTrusted(X509Certificate[] c, String a) {}
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }
};
SSLContext sc = SSLContext.getInstance("TLS");
sc.init(null, trustAll, new SecureRandom());

// 方案2：导入服务端证书到信任库
// keytool -import -alias server -file server.crt -keystore cacerts

// 方案3：OkHttp 配置
val client = OkHttpClient.Builder()
    .sslSocketFactory(sc.socketFactory, trustAll[0] as X509TrustManager)
    .hostnameVerifier { _, _ -> true }  // 仅开发环境
    .build()
```

生产环境请务必使用正规 CA 证书。

## 相关来源

faq > ssl-error-debug.md