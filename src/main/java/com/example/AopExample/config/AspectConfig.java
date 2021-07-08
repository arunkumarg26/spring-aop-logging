package com.example.AopExample.config;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Aspect
@Component
public class AspectConfig {


    private ObjectWriter objectWriter;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Autowired
    public AspectConfig(ObjectWriter objectWriter) {
        this.objectWriter = objectWriter;
    }

    @Before("@annotation(entryExitLoggingAnnotation)")
    public void beforeController(JoinPoint joinPoint, EntryExitLogging entryExitLoggingAnnotation) {
        boolean entryExitLogging = isEntryExitLogging(entryExitLoggingAnnotation);
        appendLoggingAttributes(joinPoint);
        writeLog(joinPoint, entryExitLogging, joinPoint.getArgs(), "Entering method", "Request");
    }

    @Around("@annotation(entryExitLoggingAnnotation)")
    public Object aroundController(ProceedingJoinPoint joinPoint, EntryExitLogging entryExitLoggingAnnotation) throws Throwable {
        return executeAround(joinPoint);
    }

    private Object executeAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        // executes before method

        // run actual method
        Object output = joinPoint.proceed();

        // executes upon method finishing
        long executionTime = System.currentTimeMillis() - start;
        MDC.put("timeTaken", String.valueOf(executionTime));
        return output;
    }

    @AfterReturning(pointcut = "@annotation(entryExitLoggingAnnotation)", returning = "result")
    public void afterController(JoinPoint joinPoint, Object result, EntryExitLogging entryExitLoggingAnnotation) {
        boolean entryExitLogging = isEntryExitLogging(entryExitLoggingAnnotation);
    }



    private void writeLog(JoinPoint joinPoint, boolean logArg, Object arg, String noArgMessage, String info){
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String noArgMsg = new StringBuilder()
                .append(noArgMessage)
                .append(" [").append(joinPoint.getSignature().getName()).append("]")
                .toString();
        String message = logArg? transformToJson(arg) : noArgMsg;
        log.info("{} | {}" , info, message);
    }


    private boolean isEntryExitLogging(EntryExitLogging entryExitLoggingAnnotation) {
        boolean profileExists = Objects.nonNull(entryExitLoggingAnnotation.envProfiles());
        boolean isActive = profileExists && Arrays.asList(entryExitLoggingAnnotation.envProfiles()).contains(activeProfile);
        return (entryExitLoggingAnnotation.logParams() && (profileExists && entryExitLoggingAnnotation.envProfiles().length == 0))
                || ( entryExitLoggingAnnotation.logParams() && isActive);
    }

    private String transformToJson(Object object) {
        try {
            String transFormString = objectWriter.writeValueAsString(object);
            return transFormString == null ? null : transFormString.replaceAll("[^\\x00-\\x7F]", "");
        } catch (IOException e) {
            return "JSON can't be processed";
        }
    }

    private void appendLoggingAttributes(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        try{
            final HttpServletRequest servletRequest = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            final Map<String, String> pathVariables = (Map<String, String>) servletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            MDC.put("methodType", servletRequest.getMethod());
        } catch (NullPointerException | ClassCastException ignored) {
            // ignored, not all requests have path variables
        }

        MDC.put("method", methodName);
    }




}
