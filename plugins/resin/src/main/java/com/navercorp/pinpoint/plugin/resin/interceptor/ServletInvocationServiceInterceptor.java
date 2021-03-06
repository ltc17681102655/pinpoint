/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.resin.interceptor;

import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.http.HttpServletResponseImpl;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.http.HttpStatusCodeRecorder;

/**
 * @author jaehong.kim
 */
public class ServletInvocationServiceInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private MethodDescriptor methodDescriptor;
    private TraceContext traceContext;
    private HttpStatusCodeRecorder httpStatusCodeRecorder;

    public ServletInvocationServiceInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        this.traceContext = traceContext;
        this.methodDescriptor = methodDescriptor;
        this.httpStatusCodeRecorder = new HttpStatusCodeRecorder(traceContext.getProfilerConfig().getHttpStatusCodeErrors());
    }

    @Override
    public void before(Object target, Object[] args) {
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final Trace trace = this.traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }

        try {
            if (trace.canSampled() && validate(args)) {
                final HttpServletResponseImpl response = (HttpServletResponseImpl) args[1];
                this.httpStatusCodeRecorder.record(trace.getSpanRecorder(), response.getStatus());
            }
        } finally {
            // Close, Defense code(important)
            this.traceContext.removeTraceObject();
            trace.close();
        }
    }

    private boolean validate(final Object[] args) {
        if (args == null || args.length < 2) {
            return false;
        }

        if (!(args[0] instanceof HttpServletRequestImpl)) {
            if (isDebug) {
                logger.debug("Invalid args[0] object, Not implemented of com.caucho.server.http.HttpServletRequestImpl. args[0]={}", args[0]);
            }
            return false;
        }

        if (!(args[1] instanceof HttpServletResponseImpl)) {
            if (isDebug) {
                logger.debug("Invalid args[1] object, Not implemented of com.caucho.server.http.HttpServletResponseImpl. args[1]={}.", args[1]);
            }
            return false;
        }
        return true;
    }
}