package net.lz1998.pbbot;

import cn.hutool.extra.spring.EnableSpringUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import net.lz1998.pbbot.common.event.GenTables;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

@EnableAspectJAutoProxy
@SpringBootApplication(scanBasePackages = {"net.lz1998.pbbot.*"})
@MapperScan(basePackages = {"net.lz1998.pbbot.*"})
@EnableAsync
@EnableSpringUtil
@EnableScheduling
@EnableConfigurationProperties
public class SpringMiraiServerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =SpringApplication.run(SpringMiraiServerApplication.class, args);

        GenTables genTables = context.getBean(GenTables.class);
        genTables.genTable();
    }

}
