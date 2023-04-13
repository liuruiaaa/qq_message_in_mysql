package net.lz1998.pbbot.common.enums;

/**
 * @Desc 是否
 * @Author hujieyun
 * @Date 2021/10/20 15:13
 * =========================================================================
 * 变更履历：
 * -------------------------------------------------------------------------
 * 变更编号     变更时间    变更人   变更原因    变更内容
 * -------------------------------------------------------------------------
 */
public enum YesOrNo {

    NO((byte)0, "false"),
    YES((byte)1, "true");

    private byte code;
    private String desc;

    YesOrNo(Byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getDesc(Byte code) {
        for (YesOrNo c : YesOrNo.values()) {
            if (code.byteValue() == c.getCode()) {
                return c.desc;
            }
        }
        return null;
    }

    public byte getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
