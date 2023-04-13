package net.lz1998.pbbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lz1998.pbbot.bot.Bot;
import net.lz1998.pbbot.bot.BotContainer;
import net.lz1998.pbbot.utils.Msg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SendMsgService {
    @Autowired
    private BotContainer botContainer;

    public String sendGroupMsg(String groupCode,String msg)  {
        Bot bot = botContainer.getBots().get(240874973L);
        // 第二种方式，最后直接 .sendToGroup()
        Msg.builder().text(msg).sendToGroup(bot, Long.valueOf(groupCode));
        log.info("接口发送：群号："+groupCode+" ====》QQ："+bot.getSelfId()+":"+msg);
        return groupCode+":"+msg;
    }





}
