package net.lz1998.pbbot.model.pojo;

import net.lz1998.pbbot.common.enums.YesOrNo;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 成都思致科技有限公司
 *
 * @author Changge Zhang
 * @date 2022/5/24 13:54
 * =========================================================================
 * 变更履历：
 * -------------------------------------------------------------------------
 * 变更编号     变更时间    变更人   变更原因    变更内容
 * <p>
 * -------------------------------------------------------------------------
 */
@Data
public class BasePojo implements Serializable {

    private static final long serialVersionUID = 12154211435432514L;

    private Long keyId;

    private Timestamp addTime;

    private Timestamp modifyTime;

    private Byte lived = YesOrNo.YES.getCode();



}
