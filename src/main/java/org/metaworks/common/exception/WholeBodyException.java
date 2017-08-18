/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Oce Iam Project
 *
 * Copyright (C) 2011-2013 uEngine.
 *
 * This file is part of IAM Project.
 */
package org.metaworks.common.exception;

import org.apache.commons.lang.math.RandomUtils;
import org.metaworks.common.util.DateUtils;

/**
 * HTTP Body를 전체적으로 통제하는 서비스에서 발생하는 예외.
 *
 * @author Seungpil, Park
 * @since 0.1
 */
public class WholeBodyException extends RuntimeException {

    /**
     * Serialization UID
     */
    private static final long serialVersionUID = 1;

    private String exceptionId = DateUtils.getCurrentDateTime() + "_" + RandomUtils.nextLong();

    private String message = null;

    private int exitCode;

    private int code;

    private String recentLog;

    /**
     * 기본 생성자.
     */
    public WholeBodyException() {
        super();
    }

    /**
     * 기본 생성자.
     *
     * @param message 예외 메시지
     */
    public WholeBodyException(String message) {
        super(message);
        this.message = message;
    }

    /**
     * 기본 생성자.
     *
     * @param message 예외 메시지
     * @param cause   예외 원인
     */
    public WholeBodyException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    /**
     * 기본 생성자.
     *
     * @param message 예외 메시지
     * @param code    에러 코드
     * @param cause   예외 원인
     */
    public WholeBodyException(String message, int code, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 기본 생성자.
     *
     * @param cause 예외 원인
     */
    public WholeBodyException(Throwable cause) {
        super(cause);
    }

    /**
     * 기본 생성자.
     *
     * @param exitCode  프로세스 종료 코드
     * @param recentLog 최긴 실행 로그
     */
    public WholeBodyException(int exitCode, String recentLog) {
        this.exitCode = exitCode;
        this.message = String.valueOf(exitCode);
        this.recentLog = recentLog;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getRecentLog() {
        return recentLog;
    }

    /**
     * Root Cause를 반환한다.
     *
     * @return Root Cause
     */
    public Throwable getRootCause() {
        return super.getCause();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getExceptionId() {
        return exceptionId;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setRecentLog(String recentLog) {
        this.recentLog = recentLog;
    }

    @Override
    public String toString() {
        return message;
    }
}
