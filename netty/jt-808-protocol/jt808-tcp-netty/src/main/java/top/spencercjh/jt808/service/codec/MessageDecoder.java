package top.spencercjh.jt808.service.codec;

import top.spencercjh.jt808.common.TPMSConsts;
import top.spencercjh.jt808.util.Bcd8421CodeOperater;
import top.spencercjh.jt808.util.BitOperator;
import top.spencercjh.jt808.vo.PackageData;
import top.spencercjh.jt808.vo.request.LocationInfoUploadMsg;
import top.spencercjh.jt808.vo.request.TerminalRegisterMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(MessageDecoder.class);

    private BitOperator bitOperator;
    private Bcd8421CodeOperater bcd8421Operater;

    public MessageDecoder() {
        this.bitOperator = new BitOperator();
        this.bcd8421Operater = new Bcd8421CodeOperater();
    }

    /**
     * 参考JT808 4.4 消息的组成
     *
     * @param data 标识位分割出来的消息
     * @return PackageData
     */
    public PackageData bytes2PackageData(byte[] data) {
        PackageData packageData = new PackageData();
        // 1. 16byte 或 12byte 消息头
        PackageData.MsgHeader msgHeader = this.parseMsgHeaderFromBytes(data);
        packageData.setMsgHeader(msgHeader);
        int msgBodyByteStartIndex = 12;
        // 2. 消息体
        // 有子包信息,消息体起始字节后移四个字节:消息包总数(word(16))+包序号(word(16))
        if (msgHeader.isHasSubPackage()) {
            msgBodyByteStartIndex = 16;
        }
        byte[] tmp = new byte[msgHeader.getMsgBodyLength()];
        System.arraycopy(data, msgBodyByteStartIndex, tmp, 0, tmp.length);
        packageData.setMsgBodyBytes(tmp);
        // 3. 去掉分隔符之后，最后一个字节就是校验码
        int checkSumInPackage = data[data.length - 1];
        int calculatedCheckSum = this.bitOperator.getCheckSum4JT808(data, 0, data.length - 1);
        packageData.setCheckSum(checkSumInPackage);
        if (checkSumInPackage != calculatedCheckSum) {
            log.warn("检验码不一致,msgid:{},pkg:{},calculated:{}", msgHeader.getMsgId(), checkSumInPackage, calculatedCheckSum);
        }
        return packageData;
    }

    private PackageData.MsgHeader parseMsgHeaderFromBytes(byte[] data) {
        PackageData.MsgHeader msgHeader = new PackageData.MsgHeader();
        // 1. 消息ID word(16)
        // 2个字节的消息ID
        msgHeader.setMsgId(this.parseIntFromBytes(data, 0, 2));
        // 2. 消息体属性 word(16)=================>
        // 2个字节的消息体属性
        int msgBodyProps = this.parseIntFromBytes(data, 2, 2);
        msgHeader.setMsgBodyPropsField(msgBodyProps);
        // 消息体属性的2个字节16位又分为下列属性
        // [ 0-9 ] 0000,0011,1111,1111(3FF)(消息体长度)
        msgHeader.setMsgBodyLength(msgBodyProps & 0x3ff);
        // [10-12] 0001,1100,0000,0000(1C00)(加密类型)
        msgHeader.setEncryptionType((msgBodyProps & 0x1c00) >> 10);
        // [ 13 ] 0010,0000,0000,0000(2000)(是否有子包)
        msgHeader.setHasSubPackage(((msgBodyProps & 0x2000) >> 13) == 1);
        // [14-15] 1100,0000,0000,0000(C000)(保留位)
        msgHeader.setReservedBit(((msgBodyProps & 0xc000) >> 14) + "");
        // 消息体属性 word(16)<=================
        // 3. 终端手机号 bcd[6]
        // 6个字节共48位的终端手机号，它们由12个8421码（4位）构成，即表示了12位十进制数字
        msgHeader.setTerminalPhone(this.parseBcdStringFromBytes(data, 4, 6));
        // 4. 消息流水号 word(16) 按发送顺序从 0 开始循环累加
        // 2个字节的消息流水号
        msgHeader.setFlowId(this.parseIntFromBytes(data, 10, 2));
        // 5. 消息包封装项
        // 有子包信息
        if (msgHeader.isHasSubPackage()) {
            // 消息包封装项字段
            msgHeader.setPackageInfoField(this.parseIntFromBytes(data, 12, 4));
            // byte[0-1] 消息包总数(word(16))
            // 2个字节的消息包总数
            msgHeader.setTotalSubPackage(this.parseIntFromBytes(data, 12, 2));
            // byte[2-3] 包序号(word(16)) 从 1 开始
            // 2个字节的包序号
            msgHeader.setSubPackageSeq(this.parseIntFromBytes(data, 14, 2));
        }
        return msgHeader;
    }

    private String parseStringFromBytes(byte[] data, int startIndex, int length) {
        return this.parseStringFromBytes(data, startIndex, length, null);
    }

    private String parseStringFromBytes(byte[] data, int startIndex, int length, String defaultVal) {
        try {
            byte[] tmp = new byte[length];
            System.arraycopy(data, startIndex, tmp, 0, length);
            return new String(tmp, TPMSConsts.STRING_CHARSET);
        } catch (Exception e) {
            log.error("解析字符串出错:{}", e.getMessage());
            e.printStackTrace();
            return defaultVal;
        }
    }

    private String parseBcdStringFromBytes(byte[] data, int startIndex, int lenth) {
        return this.parseBcdStringFromBytes(data, startIndex, lenth, null);
    }

    private String parseBcdStringFromBytes(byte[] data, int startIndex, int lenth, String defaultVal) {
        try {
            byte[] tmp = new byte[lenth];
            System.arraycopy(data, startIndex, tmp, 0, lenth);
            return this.bcd8421Operater.bcdCodeToString(tmp);
        } catch (Exception e) {
            log.error("解析BCD(8421码)出错:{}", e.getMessage());
            e.printStackTrace();
            return defaultVal;
        }
    }

    private int parseIntFromBytes(byte[] data, int startIndex, int length) {
        return this.parseIntFromBytes(data, startIndex, length, 0);
    }

    private int parseIntFromBytes(byte[] data, int startIndex, int length, int defaultVal) {
        try {
            // 字节数大于4,从起始索引开始向后处理4个字节,其余超出部分丢弃
            final int len = Math.min(length, 4);
            byte[] tmp = new byte[len];
            System.arraycopy(data, startIndex, tmp, 0, len);
            return bitOperator.byteToInteger(tmp);
        } catch (Exception e) {
            log.error("解析整数出错:{}", e.getMessage());
            e.printStackTrace();
            return defaultVal;
        }
    }

    public TerminalRegisterMsg toTerminalRegisterMsg(PackageData packageData) {
        TerminalRegisterMsg ret = new TerminalRegisterMsg(packageData);
        byte[] data = ret.getMsgBodyBytes();
        TerminalRegisterMsg.TerminalRegInfo body = new TerminalRegisterMsg.TerminalRegInfo();
        // 1. byte[0-1] 省域ID(WORD)
        // 设备安装车辆所在的省域，省域ID采用GB/T2260中规定的行政区划代码6位中前两位
        // 0保留，由平台取默认值
        body.setProvinceId(this.parseIntFromBytes(data, 0, 2));
        // 2. byte[2-3] 设备安装车辆所在的市域或县域,市县域ID采用GB/T2260中规定的行 政区划代码6位中后四位
        // 0保留，由平台取默认值
        body.setCityId(this.parseIntFromBytes(data, 2, 2));
        // 3. byte[4-8] 制造商ID(BYTE[5]) 5 个字节，终端制造商编码
        body.setManufacturerId(this.parseStringFromBytes(data, 4, 5));
        // 4. byte[9-28] 终端型号(BYTE[20]) 八个字节， 此终端型号 由制造商自行定义 位数不足八位的，补空格。
        body.setTerminalType(this.parseStringFromBytes(data, 9, 20));
        // 5. byte[29-35] 终端ID(BYTE[7]) 七个字节， 由大写字母 和数字组成， 此终端 ID由制造 商自行定义
        body.setTerminalId(this.parseStringFromBytes(data, 29, 7));
        // 6. byte[36] 车牌颜色(BYTE) 车牌颜 色按照JT/T415-2006 中5.4.12 的规定
        body.setLicensePlateColor(this.parseIntFromBytes(data, 36, 1));
        // 7. byte[37-x] 车牌(STRING) 公安交 通管理部门颁 发的机动车号牌
        body.setLicensePlate(this.parseStringFromBytes(data, 37, data.length - 37));
        ret.setTerminalRegInfo(body);
        return ret;
    }


    public LocationInfoUploadMsg toLocationInfoUploadMsg(PackageData packageData) {
        LocationInfoUploadMsg ret = new LocationInfoUploadMsg(packageData);
        final byte[] data = ret.getMsgBodyBytes();
        // 1. byte[0-3] 报警标志(DWORD(32))
        ret.setWarningFlagField(this.parseIntFromBytes(data, 0, 3));
        // 2. byte[4-7] 状态(DWORD(32))
        ret.setStatusField(this.parseIntFromBytes(data, 4, 4));
        // 3. byte[8-11] 纬度(DWORD(32)) 以度为单位的纬度值乘以10^6，精确到百万分之一度
        ret.setLatitude(this.parseIntFromBytes(data, 8, 4) * 1.0F / 100_0000);
        // 4. byte[12-15] 经度(DWORD(32)) 以度为单位的经度值乘以10^6，精确到百万分之一度
        ret.setLongitude(this.parseIntFromBytes(data, 12, 4) * 1.0F / 100_0000);
        // 5. byte[16-17] 高程(WORD(16)) 海拔高度，单位为米（ m）
        ret.setElevation(this.parseIntFromBytes(data, 16, 2));
        // byte[18-19] 速度(WORD) 1/10km/h
        ret.setSpeed(this.parseIntFromBytes(data, 18, 2));
        // byte[20] 方向(WORD) 0-359，正北为 0，顺时针
        ret.setDirection(this.parseIntFromBytes(data, 20, 1));
        // byte[21-x] 时间(BCD[6]) YY-MM-DD-hh-mm-ss GMT+8 时间，本标准中之后涉及的时间均采用此时区
        String dateStr = this.parseBcdStringFromBytes(data, 21, 6);
        Date date = null;
        try {
            date = new SimpleDateFormat("yyMMddHHmmss").parse(dateStr);
        } catch (ParseException e) {
            log.error("", e);
        }
        ret.setTime(date);
        return ret;
    }
}
