package net.lz1998.pbbot.common.enums;

import java.util.Arrays;

/**
 * 成都思致科技有限公司
 *
 * @author Changge Zhang
 * @date 2022/5/9 13:58
 * =========================================================================
 * 变更履历：
 * -------------------------------------------------------------------------
 * 变更编号     变更时间    变更人   变更原因    变更内容
 * <p>
 * -------------------------------------------------------------------------
 */
public enum CCBErrorCode {
    
    OK("00000","交易成功"),
    DATA_ERR("0001","数据处理异常"),
    NO_RECORD("0005","无符合条件的记录"),
    BAD_REQ("0006","请求参数错误"),
    INVALID_TRADE("0012","无效交易"),
    NO_AVAILABLE_SERVICE("0016","联机服务暂未开通"),
    TIMEOUT("0080","超时"),
    BUSINESS_MAX_THREADS("0090","系统忙，超过最大并发数"),
    REPEAT_SEQ_NO("0094","交易流水号重复"),
    INVALID_INSTITUTE("0096","无效机构"),
    FUNC_ERR("0097","功能BIT配置错误"),
    STATE_ERR("0098","机构状态异常"),
    FAILED_TRADE("0099","交易处理失败"),
    SECURE_ERR("00A0","秘钥不同步"),
    SIGN_ERR("00A1","验签失败"),
    UNKNOWN_ERR("9999","其他错误"),
    CORE_INSUFFICIENT_LOAN_AMOUNT("0020","核心企业所剩贷款额度不足"),
    GROUP_INSUFFICIENT_LOAN_AMOUNT("0021","核心企业所属集团所剩额度不足"),
    AUTH_EXPIRED("0022","征信结果已过期"),
    PRE_CREDIT_EXPIRED("0023","预授信编号已过期"),
    TER_OVER_LIMITED("0024","参考贷款期限超过预授信贷款期限"),
    LOAN_OVER_PRE_CREDIT("0025","申请金额大于预授信额度"),
    FILE_NOT_FOUND("0026","文件不存在"),
    ;


    private String code;
    
    private String msg;

    CCBErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static final boolean isSuccess(String code){
        return OK.getCode().equals(code);
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public static final String getMsg(String code){
        return Arrays.stream(CCBErrorCode.values()).filter(c -> c.getCode().equals(code)).findFirst().map(CCBErrorCode::getMsg).orElse(UNKNOWN_ERR.getMsg());
    }

    public static final CCBErrorCode getErr(String code){
        return Arrays.stream(CCBErrorCode.values()).filter(c -> c.getCode().equals(code)).findFirst().orElse(UNKNOWN_ERR);
    }
}
