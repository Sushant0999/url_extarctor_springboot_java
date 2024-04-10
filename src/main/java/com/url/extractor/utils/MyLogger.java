package com.url.extractor.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyLogger {

    public static Logger logger = LoggerFactory.getLogger(Logger.class);

    public static void info(String msg){
        logger.info(msg);
    }
    public static void warn(String msg){
        logger.warn(msg);
    }
    public static void err(String msg){
        logger.error(msg);
    }
    public static void trace(String msg){
        logger.trace(msg);
    }
}
