package com.alanpoi.im.lcs.secprotocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 摸清 {@link SecpMsgDecoder} 在各种输入下的真实行为。
 *
 * <p>起因是线上刷 {@code readableBytes 20 < 21}。这个测试要回答两件事：
 * 哪些输入会打出这条日志、以及打出来之后流还正不正常。</p>
 */
public class SecpMsgDecoderTest {

    private static final int HEADER = SecpMessage.C2S_HEADER_LENGTH; // 21

    private EmbeddedChannel ch() {
        return new EmbeddedChannel(new SecpMsgDecoder());
    }

    /** 造一个合法帧：4 魔数 + 2 长度 + 1 版本 + 1 cmd + 1 flags + 8 lcId + 4 seq + body */
    private static ByteBuf frame(int bodyLen) {
        ByteBuf b = Unpooled.buffer();
        b.writeInt(SecpMessage.PKG_BEGIN);
        b.writeShort(HEADER + bodyLen);
        b.writeByte(1);          // version
        b.writeByte(0x01);       // cmd
        b.writeByte(0);          // flags
        b.writeLong(123456789L); // lcId
        b.writeInt(7);           // seqId
        for (int i = 0; i < bodyLen; i++) b.writeByte(i & 0xff);
        return b;
    }

    /** 头部字段齐全但把 pkgLen 写成一个比头还小的值 */
    private static ByteBuf frameWithBogusLen(int pkgLen) {
        ByteBuf b = Unpooled.buffer();
        b.writeInt(SecpMessage.PKG_BEGIN);
        b.writeShort(pkgLen);
        b.writeByte(1);
        b.writeByte(0x01);
        b.writeByte(0);
        b.writeLong(1L);
        b.writeInt(1);
        return b;
    }

    @Test
    public void 完整帧能正常解出来() {
        EmbeddedChannel c = ch();
        assertTrue(c.writeInbound(frame(10)));
        SecpMessage msg = c.readInbound();
        assertNotNull(msg);
        assertEquals(10, msg.getBodyLength());
        assertEquals(0x01, msg.getCmd());
    }

    @Test
    public void 分两段到达的完整帧不会丢数据() {
        EmbeddedChannel c = ch();
        ByteBuf full = frame(4);
        // 第一段只给 20 字节 —— 这正是线上日志里那个 20
        ByteBuf part1 = full.copy(0, 20);
        ByteBuf part2 = full.copy(20, full.readableBytes() - 20);

        assertFalse("只有 20 字节时不该解出消息", c.writeInbound(part1));
        assertNull(c.readInbound());

        assertTrue("补齐之后应该解出来", c.writeInbound(part2));
        SecpMessage msg = c.readInbound();
        assertNotNull("分片不能丢数据", msg);
        assertEquals(4, msg.getBodyLength());
    }

    @Test
    public void 纯垃圾数据被逐字节丢弃且不解出消息() {
        EmbeddedChannel c = ch();
        // 模拟非 SECP 客户端：HTTP 请求行 / 健康检查探测
        byte[] junk = "GET / HTTP/1.1\r\nHost: lcs\r\nUser-Agent: probe\r\n\r\n".getBytes();
        assertFalse(c.writeInbound(Unpooled.wrappedBuffer(junk)));
        assertNull(c.readInbound());
    }

    @Test
    public void 垃圾在前合法帧在后仍能同步上() {
        EmbeddedChannel c = ch();
        ByteBuf b = Unpooled.buffer();
        b.writeBytes("junkjunkjunk".getBytes());
        b.writeBytes(frame(3));
        assertTrue("应该跳过垃圾找到魔数", c.writeInbound(b));
        SecpMessage msg = c.readInbound();
        assertNotNull(msg);
        assertEquals(3, msg.getBodyLength());
    }

    /*
     * ⚠️ 下面两个用例钉的是**当前的错误行为**，不是期望行为。
     *
     * pkgLen 字段没有下限校验：只要它小于头长度 21，`bodyLen = pkgLen - 21` 就是负数，
     * 而且 decoder 实际消费了 21 字节、帧却声称自己只有 pkgLen 字节 ——
     * 多吃掉的 (21 - pkgLen) 字节属于下一帧，流从此错位。
     * 修好之后这两个用例会失败，那时把它们改成「非法帧被丢弃且流不错位」。
     */

    @Test
    public void 当前行为_pkgLen为0会产生负数bodyLength() {
        EmbeddedChannel c = ch();
        ByteBuf b = Unpooled.buffer();
        b.writeBytes(frameWithBogusLen(0));
        b.writeBytes(frame(2));
        c.writeInbound(b);

        SecpMessage first = c.readInbound();
        assertNotNull("非法帧当前没有被丢弃，而是照样交给了下游", first);
        assertEquals("bodyLength 是负数，直接流进业务层", -HEADER, first.getBodyLength());
    }

    @Test
    public void 当前行为_pkgLen小于头长度会产生负数bodyLength() {
        EmbeddedChannel c = ch();
        ByteBuf b = Unpooled.buffer();
        b.writeBytes(frameWithBogusLen(10));
        b.writeBytes(frame(2));
        c.writeInbound(b);

        SecpMessage first = c.readInbound();
        assertNotNull(first);
        assertEquals(10 - HEADER, first.getBodyLength());
    }

    @Test
    public void 连续两个完整帧一次到达() {
        EmbeddedChannel c = ch();
        ByteBuf b = Unpooled.buffer();
        b.writeBytes(frame(1));
        b.writeBytes(frame(2));
        c.writeInbound(b);
        SecpMessage a = c.readInbound();
        SecpMessage d = c.readInbound();
        assertNotNull("第一帧", a);
        assertNotNull("第二帧 —— 一次 decode 调用要能连着出多帧", d);
        assertEquals(1, a.getBodyLength());
        assertEquals(2, d.getBodyLength());
    }

    @Test
    public void 声明长度超过实际到达长度时等待而不是丢弃() {
        EmbeddedChannel c = ch();
        ByteBuf full = frame(100);
        ByteBuf part = full.copy(0, 50);       // 只到一半
        assertFalse(c.writeInbound(part));
        assertNull(c.readInbound());
        assertTrue(c.writeInbound(full.copy(50, full.readableBytes() - 50)));
        SecpMessage msg = c.readInbound();
        assertNotNull("剩下的到了要能拼起来", msg);
        assertEquals(100, msg.getBodyLength());
    }

}
