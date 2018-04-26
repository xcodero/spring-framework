/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.view;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Simple implementation of the {@link org.springframework.web.servlet.ViewResolver}
 * interface, allowing for direct resolution of symbolic view names to URLs,
 * without explicit mapping definition. This is useful if your symbolic names
 * match the names of your view resources in a straightforward manner
 * (i.e. the symbolic name is the unique part of the resource's filename),
 * without the need for a dedicated mapping to be defined for each view.
 *
 * <p>Supports {@link AbstractUrlBasedView} subclasses like {@link InternalResourceView}
 * and {@link org.springframework.web.servlet.view.freemarker.FreeMarkerView}.
 * The view class for all views generated by this resolver can be specified
 * via the "viewClass" property.
 *
 * <p>View names can either be resource URLs themselves, or get augmented by a
 * specified prefix and/or suffix. Exporting an attribute that holds the
 * RequestContext to all views is explicitly supported.
 *
 * <p>Example: prefix="/WEB-INF/jsp/", suffix=".jsp", viewname="test" ->
 * "/WEB-INF/jsp/test.jsp"
 *
 * <p>As a special feature, redirect URLs can be specified via the "redirect:"
 * prefix. E.g.: "redirect:myAction.do" will trigger a redirect to the given
 * URL, rather than resolution as standard view name. This is typically used
 * for redirecting to a controller URL after finishing a form workflow.
 *
 * <p>Furthermore, forward URLs can be specified via the "forward:" prefix. E.g.:
 * "forward:myAction.do" will trigger a forward to the given URL, rather than
 * resolution as standard view name. This is typically used for controller URLs;
 * it is not supposed to be used for JSP URLs - use logical view names there.
 *
 * <p>Note: This class does not support localized resolution, i.e. resolving
 * a symbolic view name to different resources depending on the current locale.
 *
 * <p><b>Note:</b> When chaining ViewResolvers, a UrlBasedViewResolver will check whether
 * the {@link AbstractUrlBasedView#checkResource specified resource actually exists}.
 * However, with {@link InternalResourceView}, it is not generally possible to
 * determine the existence of the target resource upfront. In such a scenario,
 * a UrlBasedViewResolver will always return View for any given view name;
 * as a consequence, it should be configured as the last ViewResolver in the chain.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 13.12.2003
 * @see #setViewClass
 * @see #setPrefix
 * @see #setSuffix
 * @see #setRequestContextAttribute
 * @see #REDIRECT_URL_PREFIX
 * @see AbstractUrlBasedView
 * @see InternalResourceView
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerView
 */
public class UrlBasedViewResolver extends AbstractCachingViewResolver implements Ordered {

	/**
	 * Prefix for special view names that specify a redirect URL (usually
	 * to a controller after a form has been submitted and processed).
	 * Such view names will not be resolved in the configured default
	 * way but rather be treated as special shortcut.
	 */
	public static final String REDIRECT_URL_PREFIX = "redirect:";

	/**
	 * Prefix for special view names that specify a forward URL (usually
	 * to a controller after a form has been submitted and processed).
	 * Such view names will not be resolved in the configured default
	 * way but rather be treated as special shortcut.
	 */
	public static final String FORWARD_URL_PREFIX = "forward:";


	@Nullable
	private Class<?> viewClass;

	private String prefix = "";

	private String suffix = "";

	@Nullable
	private String contentType;

	private boolean redirectContextRelative = true;

	private boolean redirectHttp10Compatible = true;

	@Nullable
	private String[] redirectHosts;

	@Nullable
	private String requestContextAttribute;

	/** Map of static attributes, keyed by attribute name (String) */
	private final Map<String, Object> staticAttributes = new HashMap<>();

	@Nullable
	private Boolean exposePathVariables;

	@Nullable
	private Boolean exposeContextBeansAsAttributes;

	@Nullable
	private String[] exposedContextBeanNames;

	@Nullable
	private String[] viewNames;

	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * Set the view class that should be used to create views.
	 * @param viewClass class that is assignable to the required view class
	 * (by default, AbstractUrlBasedView)
	 * @see AbstractUrlBasedView
	 */
	public void setViewClass(@Nullable Class<?> viewClass) {
		if (viewClass != null && !requiredViewClass().isAssignableFrom(viewClass)) {
			throw new IllegalArgumentException("Given view class [" + viewClass.getName() +
					"] is not of type [" + requiredViewClass().getName() + "]");
		}
		this.viewClass = viewClass;
	}

	/**
	 * Return the view class to be used to create views.
	 */
	@Nullable
	protected Class<?> getViewClass() {
		return this.viewClass;
	}

	/**
	 * Return the required type of view for this resolver.
	 * This implementation returns AbstractUrlBasedView.
	 * @see AbstractUrlBasedView
	 */
	protected Class<?> requiredViewClass() {
		return AbstractUrlBasedView.class;
	}

	/**
	 * Set the prefix that gets prepended to view names when building a URL.
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * Return the prefix that gets prepended to view names when building a URL.
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * Set the suffix that gets appended to view names when building a URL.
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * Return the suffix that gets appended to view names when building a URL.
	 */
	protected String getSuffix() {
		return this.suffix;
	}

	/**
	 * Set the content type for all views.
	 * <p>May be ignored by view classes if the view itself is assumed
	 * to set the content type, e.g. in case of JSPs.
	 */
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Return the content type for all views, if any.
	 */
	@Nullable
	protected String getContentType() {
		return this.contentType;
	}

	/**
	 * Set whether to interpret a given redirect URL that starts with a
	 * slash ("/") as relative to the current ServletContext, i.e. as
	 * relative to the web application root.
	 * <p>Default is "true": A redirect URL that starts with a slash will be
	 * interpreted as relative to the web application root, i.e. the context
	 * path will be prepended to the URL.
	 * <p><b>Redirect URLs can be specified via the "redirect:" prefix.</b>
	 * E.g.: "redirect:myAction.do"
	 * @see RedirectView#setContextRelative
	 * @see #REDIRECT_URL_PREFIX
	 */
	public void setRedirectContextRelative(boolean redirectContextRelative) {
		this.redirectContextRelative = redirectContextRelative;
	}

	/**
	 * Return whether to interpret a given redirect URL that starts with a
	 * slash ("/") as relative to the current ServletContext, i.e. as
	 * relative to the web application root.
	 * <p>是否将以斜杠开头的重定向URL解释为"相对于当前ServletContext"，即相对于web应用的根</p>
	 */
	protected boolean isRedirectContextRelative() {
		return this.redirectContextRelative;
	}

	/**
	 * Set whether redirects should stay compatible with HTTP 1.0 clients.
	 * <p>In the default implementation, this will enforce HTTP status code 302
	 * in any case, i.e. delegate to {@code HttpServletResponse.sendRedirect}.
	 * Turning this off will send HTTP status code 303, which is the correct
	 * code for HTTP 1.1 clients, but not understood by HTTP 1.0 clients.
	 * <p>Many HTTP 1.1 clients treat 302 just like 303, not making any
	 * difference. However, some clients depend on 303 when redirecting
	 * after a POST request; turn this flag off in such a scenario.
	 * <p><b>Redirect URLs can be specified via the "redirect:" prefix.</b>
	 * E.g.: "redirect:myAction.do"
	 * @see RedirectView#setHttp10Compatible
	 * @see #REDIRECT_URL_PREFIX
	 */
	public void setRedirectHttp10Compatible(boolean redirectHttp10Compatible) {
		this.redirectHttp10Compatible = redirectHttp10Compatible;
	}

	/**
	 * Return whether redirects should stay compatible with HTTP 1.0 clients.
	 */
	protected boolean isRedirectHttp10Compatible() {
		return this.redirectHttp10Compatible;
	}

	/**
	 * Configure one or more hosts associated with the application.
	 * All other hosts will be considered external hosts.
	 * <p>In effect, this property provides a way turn off encoding on redirect
	 * via {@link HttpServletResponse#encodeRedirectURL} for URLs that have a
	 * host and that host is not listed as a known host.
	 * <p>If not set (the default) all URLs are encoded through the response.
	 * @param redirectHosts one or more application hosts
	 * @since 4.3
	 */
	public void setRedirectHosts(@Nullable String... redirectHosts) {
		this.redirectHosts = redirectHosts;
	}

	/**
	 * Return the configured application hosts for redirect purposes.
	 * @since 4.3
	 */
	@Nullable
	public String[] getRedirectHosts() {
		return this.redirectHosts;
	}

	/**
	 * Set the name of the RequestContext attribute for all views.
	 * @param requestContextAttribute name of the RequestContext attribute
	 * @see AbstractView#setRequestContextAttribute
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * Return the name of the RequestContext attribute for all views, if any.
	 */
	@Nullable
	protected String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * Set static attributes from a {@code java.util.Properties} object,
	 * for all views returned by this resolver.
	 * <p>This is the most convenient way to set static attributes. Note that
	 * static attributes can be overridden by dynamic attributes, if a value
	 * with the same name is included in the model.
	 * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
	 * or a "props" element in XML bean definitions.
	 * @see org.springframework.beans.propertyeditors.PropertiesEditor
	 * @see AbstractView#setAttributes
	 */
	public void setAttributes(Properties props) {
		CollectionUtils.mergePropertiesIntoMap(props, this.staticAttributes);
	}

	/**
	 * Set static attributes from a Map, for all views returned by this resolver.
	 * This allows to set any kind of attribute values, for example bean references.
	 * <p>Can be populated with a "map" or "props" element in XML bean definitions.
	 * @param attributes Map with name Strings as keys and attribute objects as values
	 * @see AbstractView#setAttributesMap
	 */
	public void setAttributesMap(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			this.staticAttributes.putAll(attributes);
		}
	}

	/**
	 * Allow Map access to the static attributes for views returned by
	 * this resolver, with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via
	 * "attributesMap[myKey]". This is particularly useful for
	 * adding or overriding entries in child view definitions.
	 */
	public Map<String, Object> getAttributesMap() {
		return this.staticAttributes;
	}

	/**
	 * Specify whether views resolved by this resolver should add path variables to the model or not.
	 * <p>>The default setting is to let each View decide (see {@link AbstractView#setExposePathVariables}.
	 * However, you can use this property to override that.
	 * @param exposePathVariables
	 * <ul>
	 * <li>{@code true} - all Views resolved by this resolver will expose path variables
	 * <li>{@code false} - no Views resolved by this resolver will expose path variables
	 * <li>{@code null} - individual Views can decide for themselves (this is used by the default)
	 * <ul>
	 * @see AbstractView#setExposePathVariables
	 */
	public void setExposePathVariables(@Nullable Boolean exposePathVariables) {
		this.exposePathVariables = exposePathVariables;
	}

	/**
	 * Return whether views resolved by this resolver should add path variables to the model or not.
	 */
	@Nullable
	protected Boolean getExposePathVariables() {
		return this.exposePathVariables;
	}

	/**
	 * Set whether to make all Spring beans in the application context accessible
	 * as request attributes, through lazy checking once an attribute gets accessed.
	 * <p>This will make all such beans accessible in plain {@code ${...}}
	 * expressions in a JSP 2.0 page, as well as in JSTL's {@code c:out}
	 * value expressions.
	 * <p>Default is "false".
	 * @see AbstractView#setExposeContextBeansAsAttributes
	 */
	public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
		this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
	}

	@Nullable
	protected Boolean getExposeContextBeansAsAttributes() {
		return this.exposeContextBeansAsAttributes;
	}

	/**
	 * Specify the names of beans in the context which are supposed to be exposed.
	 * If this is non-null, only the specified beans are eligible for exposure as
	 * attributes.
	 * @see AbstractView#setExposedContextBeanNames
	 */
	public void setExposedContextBeanNames(@Nullable String... exposedContextBeanNames) {
		this.exposedContextBeanNames = exposedContextBeanNames;
	}

	@Nullable
	protected String[] getExposedContextBeanNames() {
		return this.exposedContextBeanNames;
	}

	/**
	 * Set the view names (or name patterns) that can be handled by this
	 * {@link org.springframework.web.servlet.ViewResolver}. View names can contain
	 * simple wildcards such that 'my*', '*Report' and '*Repo*' will all match the
	 * view name 'myReport'.
	 * @see #canHandle
	 */
	public void setViewNames(@Nullable String... viewNames) {
		this.viewNames = viewNames;
	}

	/**
	 * Return the view names (or name patterns) that can be handled by this
	 * {@link org.springframework.web.servlet.ViewResolver}.
	 */
	@Nullable
	protected String[] getViewNames() {
		return this.viewNames;
	}

	/**
	 * Specify the order value for this ViewResolver bean.
	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	protected void initApplicationContext() {
		super.initApplicationContext();
		if (getViewClass() == null) {
			throw new IllegalArgumentException("Property 'viewClass' is required");
		}
	}


	/**
	 * This implementation returns just the view name,
	 * as this ViewResolver doesn't support localized resolution.
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName;
	}

	/**
	 * Overridden to implement check for "redirect:" prefix.
	 * <p>Not possible in {@code loadView}, since overridden
	 * {@code loadView} versions in subclasses might rely on the
	 * superclass always creating instances of the required view class.
	 * @see #loadView
	 * @see #requiredViewClass
	 */
	@Override
	protected View createView(String viewName, Locale locale) throws Exception {
		// If this resolver is not supposed to handle the given view,
		// return null to pass on to the next resolver in the chain.
		// 1.如果该解析器不是用来处理给定视图的，返回null
		if (!canHandle(viewName, locale)) {
			return null;
		}
		// Check for special "redirect:" prefix.
		// 2.处理前缀为"redirect:"的视图名
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			RedirectView view = new RedirectView(redirectUrl, isRedirectContextRelative(), isRedirectHttp10Compatible());
			String[] hosts = getRedirectHosts();
			if (hosts != null) {
				view.setHosts(hosts);
			}
			return applyLifecycleMethods(viewName, view);
		}
		// Check for special "forward:" prefix.
		// 3.处理前缀为"forward:"的视图名
		if (viewName.startsWith(FORWARD_URL_PREFIX)) {
			String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
			return new InternalResourceView(forwardUrl);
		}
		// Else fall back to superclass implementation: calling loadView.
		return super.createView(viewName, locale);
	}

	/**
	 * Indicates whether or not this {@link org.springframework.web.servlet.ViewResolver} can
	 * handle the supplied view name. If not, {@link #createView(String, java.util.Locale)} will
	 * return {@code null}. The default implementation checks against the configured
	 * {@link #setViewNames view names}.
	 * @param viewName the name of the view to retrieve
	 * @param locale the Locale to retrieve the view for
	 * @return whether this resolver applies to the specified view
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean canHandle(String viewName, Locale locale) {
		String[] viewNames = getViewNames();
		return (viewNames == null || PatternMatchUtils.simpleMatch(viewNames, viewName));
	}

	/**
	 * Delegates to {@code buildView} for creating a new instance of the
	 * specified view class, and applies the following Spring lifecycle methods
	 * (as supported by the generic Spring bean factory):
	 * <ul>
	 * <li>ApplicationContextAware's {@code setApplicationContext}
	 * <li>InitializingBean's {@code afterPropertiesSet}
	 * </ul>
	 * @param viewName the name of the view to retrieve
	 * @return the View instance
	 * @throws Exception if the view couldn't be resolved
	 * @see #buildView(String)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		AbstractUrlBasedView view = buildView(viewName);
		View result = applyLifecycleMethods(viewName, view);
		return (view.checkResource(locale) ? result : null);
	}

	/**
	 * Creates a new View instance of the specified view class and configures it.
	 * Does <i>not</i> perform any lookup for pre-defined View instances.
	 * <p>Spring lifecycle methods as defined by the bean container do not have to
	 * be called here; those will be applied by the {@code loadView} method
	 * after this method returns.
	 * <p>Subclasses will typically call {@code super.buildView(viewName)}
	 * first, before setting further properties themselves. {@code loadView}
	 * will then apply Spring lifecycle methods at the end of this process.
	 * @param viewName the name of the view to build
	 * @return the View instance
	 * @throws Exception if the view couldn't be resolved
	 * @see #loadView(String, java.util.Locale)
	 */
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		Class<?> viewClass = getViewClass();
		Assert.state(viewClass != null, "No view class");

		AbstractUrlBasedView view = (AbstractUrlBasedView) BeanUtils.instantiateClass(viewClass);
		// 添加前缀和后缀
		view.setUrl(getPrefix() + viewName + getSuffix());

		String contentType = getContentType();
		if (contentType != null) {
			view.setContentType(contentType);
		}

		view.setRequestContextAttribute(getRequestContextAttribute());
		view.setAttributesMap(getAttributesMap());

		Boolean exposePathVariables = getExposePathVariables();
		if (exposePathVariables != null) {
			view.setExposePathVariables(exposePathVariables);
		}
		Boolean exposeContextBeansAsAttributes = getExposeContextBeansAsAttributes();
		if (exposeContextBeansAsAttributes != null) {
			view.setExposeContextBeansAsAttributes(exposeContextBeansAsAttributes);
		}
		String[] exposedContextBeanNames = getExposedContextBeanNames();
		if (exposedContextBeanNames != null) {
			view.setExposedContextBeanNames(exposedContextBeanNames);
		}

		return view;
	}

	/**
	 * Apply the containing {@link ApplicationContext}'s lifecycle methods
	 * to the given {@link View} instance, if such a context is available.
	 * @param viewName the name of the view
	 * @param view the freshly created View instance, pre-configured with
	 * {@link AbstractUrlBasedView}'s properties
	 * @return the {@link View} instance to use (either the original one
	 * or a decorated variant)
	 * @since 5.0
	 * @see #getApplicationContext()
	 * @see ApplicationContext#getAutowireCapableBeanFactory()
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#initializeBean
	 */
	protected View applyLifecycleMethods(String viewName, AbstractUrlBasedView view) {
		ApplicationContext context = getApplicationContext();
		if (context != null) {
			Object initialized = context.getAutowireCapableBeanFactory().initializeBean(view, viewName);
			if (initialized instanceof View) {
				return (View) initialized;
			}
		}
		return view;
	}

}
