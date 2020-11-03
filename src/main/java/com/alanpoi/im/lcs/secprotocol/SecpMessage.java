package com.alanpoi.im.lcs.secprotocol;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

//安全协议消息类型
public class SecpMessage {

	public static final byte VERRSION = 1;
	public static final int PKG_BEGIN = 0xFAED7439;
	public static final int COMPRESS_THRESHOLD = 1024 * 8; //压缩阀值

	public static final short FLAG_COMPRESS = 0x0001; //压缩标准位
	public static final short FLAG_NEEDRES = 0x0002; //返回标志位

	public static final int C2S_HEADER_LENGTH = 21; //客户端到服务器的数据包头长度
	public static final int S2C_HEADER_LENGTH = 22; //服务器到客户端的数据包头长度
	public static final int MAX_PKG_LENGTH = 65535; //数据包最大长度


	private static AtomicLong msgIdSeed = new AtomicLong(0);

	private volatile long msgId; //消息id, 用来跟踪消息的处理
	//消息处理跟踪信息
	private ConcurrentLinkedQueue<String> trace = new ConcurrentLinkedQueue<String>();
	private volatile long startTime; //消息开始处理的时间

	private short version; //协议版本号
	private short cmd;   //命令字 在Cmd中定义
	private short flags; //标志位
	private long lcId; //长连接会话ID
	private int seqId; //消息的序列号
	private short code; //错误码, 仅在客户端向服务器端的消息中使用

	private int bodyLength; //body长度
	private byte[] body; //消息体


	public SecpMessage(){
		msgId = 0;
		code = Errors.SUCCESS;
	}

	static public long newMsgId(){
		long id =  msgIdSeed.incrementAndGet();
		if(0 == id){
			id = msgIdSeed.incrementAndGet();
		}

		return id;
	}

	public void setMsgId(long id){
		this.msgId = id;
	}

	public long getMsgId(){
		return msgId;
	}

	public void setTrace(String[] msgs){
		trace.clear();
		trace.addAll(Arrays.asList(msgs));
	}

	public void addTrace(String msg){
		trace.add(msg);
	}
	public String[] getTrace(){
		return trace.toArray(new String[0]);
	}

	public void setStartTime(long t){
		startTime = t;
	}
	public long getStartTime(){
		return startTime;
	}

	public short getVersion() {
		return version;
	}
	public void setVersion(short version) {
		this.version = version;
	}
	public short getCmd() {
		return cmd;
	}
	public void setCmd(short cmd) {
		this.cmd = cmd;
	}
	public long getLcId() { return lcId;}
	public void setLcId(long lcId){this.lcId = lcId;};
	public int getSeqId() {
		return seqId;
	}
	public void setSeqId(int seqId) {
		this.seqId = seqId;
	}

	public short getFlags() {
		return flags;
	}
	public void setFlags(short flags) {
		this.flags = flags;
	}

	public short getCode() {
		return code;
	}

	public void setCode(short code) {
		this.code = code;
	}

	public int getBodyLength() {
		return bodyLength;
	}
	public void setBodyLength(int bodyLength) {
		this.bodyLength = bodyLength;
	}
	public byte[] getBody() {
		return body;
	}
	
	public void setBody(byte[] body) {
		this.body = body;
	}
	
	public boolean isCompress(){
		return ((this.flags & 0xffff) & FLAG_COMPRESS) == FLAG_COMPRESS;
	}
	public void setCompress(boolean b){
		if(b){
			this.flags = (short)(this.flags | FLAG_COMPRESS);
		}else{
			this.flags =(short)(this.flags & ~FLAG_COMPRESS);
		}
	}
	public boolean needResponse(){
		return ((this.flags & 0xfff) & FLAG_NEEDRES) == FLAG_NEEDRES;
	}
	public void needResponse(boolean b){
		if(b){
			flags = (short)(flags | FLAG_NEEDRES);
		}else{
			flags = (short)(flags & ~FLAG_NEEDRES);
		}
	}
	
}
