# ss-java
shadowsocks的Java实现版本。

暂时只写了Client端，使用BIO(不支持SSL)及原生NIO两种方式实现，之后加入Netty实现。

加密部分参考[blakey22/shadowsocks-java](https://github.com/blakey22/shadowsocks-java).(仅测试AES encryption)

协议暂时只支持TCP协议


