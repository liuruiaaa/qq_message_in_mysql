package net.lz1998.pbbot.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lz1998.pbbot.common.event.GenTables;
import net.lz1998.pbbot.service.SendMsgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api(value = "消息回复", tags = "消息回复")
@RequestMapping("/send")
public class SendMsgController {
    private final SendMsgService sendMsgService;
    private final GenTables genTables;
    @ApiOperation(value = "sendGroupMsg")
    @GetMapping("/sendGroupMsg")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupCode" , value = "qqCode  如：16641755654146361", dataType = "String", defaultValue = "", paramType = "query"),
            @ApiImplicitParam(name = "msg" , value = "Msg", dataType = "String", defaultValue = "", paramType = "query")
    })
    public String sendGroupMsg(
            @RequestParam(value = "groupCode")  String  groupCode,
            @RequestParam(value = "msg")  String  msg

    ){
        return sendMsgService.sendGroupMsg(groupCode,msg);
    }
}
