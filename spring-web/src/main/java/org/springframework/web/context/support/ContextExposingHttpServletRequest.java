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

package org.springframework.web.context.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashSet;
import java.util.Set;

/**
 * HttpServletRequest decorator that makes all Spring beans in a
 * given WebApplicationContext accessible as request attributes,
 * through lazy checking once an attribute gets accessed.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
// HttpServletRequest的装饰器，增加以请求属性的方式访问给定WebApplicationContext中所有bean的功能。
// 该增强功能是在访问请求中的属性时实现的。
public class ContextExposingHttpServletRequest extends HttpServletRequestWrapper {

	private final WebApplicationContext webApplicationContext;

	// 应该暴露为属性的bean名称集合。null表示所有bean都应该暴露为属性。
	@Nullable
	private final Set<String> exposedContextBeanNames;

	// 明确表示是请求中的属性而非bean名称的属性名集合。
	@Nullable
	private Set<String> explicitAttributes;


	/**
	 * Create a new ContextExposingHttpServletRequest for the given request.
	 * @param originalRequest the original HttpServletRequest
	 * @param context the WebApplicationContext that this request runs in
	 */
	public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context) {
		this(originalRequest, context, null);
	}

	/**
	 * Create a new ContextExposingHttpServletRequest for the given request.
	 * @param originalRequest the original HttpServletRequest
	 * @param context the WebApplicationContext that this request runs in
	 * @param exposedContextBeanNames the names of beans in the context which
	 * are supposed to be exposed (if this is non-null, only the beans in this
	 * Set are eligible for exposure as attributes)
	 * <p>上下文中需要暴露为属性的bean的名称（如果该参数不为null，则只有该集合中指定的bean才暴露为属性；否则，全部暴露为属性——与explicitAttributes中重名的除外）</p>
	 */
	public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context,
			@Nullable Set<String> exposedContextBeanNames) {

		super(originalRequest);
		Assert.notNull(context, "WebApplicationContext must not be null");
		this.webApplicationContext = context;
		this.exposedContextBeanNames = exposedContextBeanNames;
	}


	/**
	 * Return the WebApplicationContext that this request runs in.
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	@Override
	@Nullable
	public Object getAttribute(String name) {
		if ((this.explicitAttributes == null || !this.explicitAttributes.contains(name)) &&
				(this.exposedContextBeanNames == null || this.exposedContextBeanNames.contains(name)) &&
				this.webApplicationContext.containsBean(name)) {
			return this.webApplicationContext.getBean(name);
		}
		else {
			return super.getAttribute(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		super.setAttribute(name, value);
		if (this.explicitAttributes == null) {
			this.explicitAttributes = new HashSet<>(8);
		}
		this.explicitAttributes.add(name);
	}

}
