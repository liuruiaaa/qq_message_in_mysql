package net.lz1998.pbbot.model.pojo;

import lombok.Data;

/**
 * 成都思致科技有限公司
 *
 * <p> 查询贷款账户信息vo
 * @date 2021/8/31 13:47
 * @author shuichangrong
 */
@Data
public class GroupMessages extends BasePojo {

    private static final long serialVersionUID = 6402781107554908123L;


    /**
     * 来自于那个QQ
     */
    private String selfId;


    /**
     * 群号
     */
    private String groupNum;


    /**
     * 群里那个QQ号说的
     */
    private String groupQqSay;

    /**
     * 说的内容
     */
    private String content;




}
