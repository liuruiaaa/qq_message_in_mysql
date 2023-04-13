package net.lz1998.pbbot.common.log;

import org.slf4j.Logger;

/**
 * 成都思致科技有限公司
 *
 * @author Changge Zhang
 * @date 2022/5/12 11:16
 * =========================================================================
 * 变更履历：
 * -------------------------------------------------------------------------
 * 变更编号     变更时间    变更人   变更原因    变更内容
 * <p>
 * -------------------------------------------------------------------------
 */
public class Log {

    private Log(){
        throw new IllegalStateException("Utility class");
    }


    public static final void info(Logger log,String message,Object... args){
        if(log.isInfoEnabled()){
            log.info(message,args);
        }
    }


    public static final void error(Logger log,String message,Object... args){
        if(log.isErrorEnabled()){
            log.error(message,args);
        }
    }


    public static final void warn(Logger log,String message,Object... args){
        if(log.isWarnEnabled()){
            log.warn(message,args);
        }
    }


    public static final void trace(Logger log,String message,Object... args){
        if(log.isTraceEnabled()){
            log.trace(message,args);
        }
    }


    public static final void debug(Logger log,String message,Object... args){
        if(log.isDebugEnabled()){
            log.debug(message,args);
        }
    }
}
