package net.lz1998.pbbot.model.pojo;

import lombok.Data;

@Data
public class UserMessages extends BasePojo {
    private static final long serialVersionUID = 6402781107554908123L;


    /**
     * 来自于那个QQ
     */
    private String selfId;

    /**
     * 说的内容
     */
    private String content;

    /**
     * 发消息的人
     */
    private String getUserId;
}
