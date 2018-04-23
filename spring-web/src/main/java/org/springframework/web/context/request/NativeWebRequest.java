/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.request;

import org.springframework.lang.Nullable;

/**
 * Extension of the {@link WebRequest} interface, exposing the
 * native request and response objects in a generic fashion.
 *
 * <p>扩展自WebRequest接口，以一种通用风格来暴露原生请求、响应对象。
 *
 * <p>Mainly intended for framework-internal usage,
 * in particular for generic argument resolution code.
 *
 * <p>主要在框架内部使用。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public interface NativeWebRequest extends WebRequest {

	/**
	 * Return the underlying native request object.
	 * @see javax.servlet.http.HttpServletRequest
	 */
	Object getNativeRequest();

	/**
	 * Return the underlying native response object, if any.
	 * @see javax.servlet.http.HttpServletResponse
	 */
	@Nullable
	Object getNativeResponse();

	/**
	 * Return the underlying native request object, if available.
	 * @param requiredType the desired type of request object
	 * @return the matching request object, or {@code null} if none
	 * of that type is available
	 * @see javax.servlet.http.HttpServletRequest
	 */
	@Nullable
	<T> T getNativeRequest(@Nullable Class<T> requiredType);

	/**
	 * Return the underlying native response object, if available.
	 * @param requiredType the desired type of response object
	 * @return the matching response object, or {@code null} if none
	 * of that type is available
	 * @see javax.servlet.http.HttpServletResponse
	 */
	@Nullable
	<T> T getNativeResponse(@Nullable Class<T> requiredType);

}
