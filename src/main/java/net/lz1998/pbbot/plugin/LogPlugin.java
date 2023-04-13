package net.lz1998.pbbot.plugin;

import lombok.extern.slf4j.Slf4j;
import net.lz1998.pbbot.bot.Bot;
import net.lz1998.pbbot.bot.BotPlugin;
import net.lz1998.pbbot.common.event.GenTables;
import net.lz1998.pbbot.model.pojo.GroupMessages;
import net.lz1998.pbbot.model.pojo.UserMessages;
import onebot.OnebotEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class LogPlugin extends BotPlugin {

    private final GenTables genTables;
    private String b = "=";

    public LogPlugin(GenTables genTables) {
        this.genTables = genTables;
    }

    @Override
    public int onPrivateMessage(@NotNull Bot bot, @NotNull OnebotEvent.PrivateMessageEvent event) {
        log.info("收到私聊消息 MY：{}  QQ：{} 内容：{}", event.getSelfId(), event.getUserId(), event.getRawMessage());
        //入库
        UserMessages userMessages = new UserMessages();
        userMessages.setSelfId(event.getSelfId()+"");
        userMessages.setGetUserId(event.getUserId()+"");
        userMessages.setContent(event.getRawMessage()+"");
        genTables.save(userMessages);
        return MESSAGE_IGNORE;
    }


    @Override
    public int onGroupMessage(@NotNull Bot bot, @NotNull OnebotEvent.GroupMessageEvent event) {

        String str = String.valueOf(event.getGroupId());
        int lastDigit = Integer.parseInt(str.substring(str.length() - 2));
        String  strstr = Stream.generate(() -> b).limit(lastDigit).collect(Collectors.joining());

        log.info("收到群消息 MY：{} 群号：{}{}> QQ：{} 内容：{}", event.getSelfId(),event.getGroupId(),strstr, event.getUserId(), event.getRawMessage());
       // log.info("=======>{}",JSONObject.toJSON(event));
        GroupMessages groupMessages = new GroupMessages();
        groupMessages.setGroupNum(event.getGroupId()+"");
        groupMessages.setGroupQqSay(event.getUserId()+"");
        groupMessages.setContent(event.getRawMessage()+"");
        groupMessages.setSelfId(event.getSelfId()+"");
        genTables.save(groupMessages);
        //log.info("=======>{}",JSONObject.toJSON())
        return MESSAGE_IGNORE;
    }

}
