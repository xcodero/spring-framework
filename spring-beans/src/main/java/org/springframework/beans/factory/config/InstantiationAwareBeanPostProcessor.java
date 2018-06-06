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

package org.springframework.beans.factory.config;

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.lang.Nullable;

/**
 * Subinterface of {@link BeanPostProcessor} that adds a before-instantiation callback,
 * and a callback after instantiation but before explicit properties are set or
 * autowiring occurs.
 *
 * <p>Typically used to suppress default instantiation for specific target beans,
 * for example to create proxies with special TargetSources (pooling targets,
 * lazily initializing targets, etc), or to implement additional injection strategies
 * such as field injection.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. It is recommended to implement the plain
 * {@link BeanPostProcessor} interface as far as possible, or to derive from
 * {@link InstantiationAwareBeanPostProcessorAdapter} in order to be shielded
 * from extensions to this interface.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 1.2
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 */
// 1.这是BeanPostProcessor的一个子接口，增加了一个实例化前回调、实例化后Spring属性填充（明确设置属性或自动连线）前回调；
// 2.典型地，用于抑制特定目标bean的默认实例化，如为特殊的TargetSource（池化目标、延迟初始化目标等）创建代理，或实现额外的注入策略（如域注入）；
// 3.该接口是一个特殊用途接口，主要用于框架内部使用；
// 4.推荐实现BeanPostProcessor接口或继承InstantiationAwareBeanPostProcessorAdapter类，以免受该接口日后扩展的影响。
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * Apply this BeanPostProcessor <i>before the target bean gets instantiated</i>.
	 * The returned bean object may be a proxy to use instead of the target bean,
	 * effectively suppressing default instantiation of the target bean.
	 * <p>If a non-null object is returned by this method, the bean creation process
	 * will be short-circuited. The only further processing applied is the
	 * {@link #postProcessAfterInitialization} callback from the configured
	 * {@link BeanPostProcessor BeanPostProcessors}.
	 * <p>This callback will only be applied to bean definitions with a bean class.
	 * In particular, it will not be applied to beans with a "factory-method".
	 * <p>Post-processors may implement the extended
	 * {@link SmartInstantiationAwareBeanPostProcessor} interface in order
	 * to predict the type of the bean object that they are going to return here.
	 * <p>The default implementation returns {@code null}.
	 * @param beanClass the class of the bean to be instantiated
	 * @param beanName the name of the bean
	 * @return the bean object to expose instead of a default instance of the target bean,
	 * or {@code null} to proceed with default instantiation
	 * 返回要暴露的bean对象而不是目标bean的默认实例，返回null则会进行默认实例化
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#hasBeanClass
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName
	 */
	/*
	 * 1.在实例化目标bean前应用该BeanPostProcessor；
	 * 2.返回的bean对象可能是一个代理，有效抑制目标bean的默认实例化；
	 * 3.如果该方法返回一个非null对象，会导致bean创建过程短路，接下来的唯一处理就剩下BeanPostProcessor的postProcessAfterInitialization回调；
	 * 4.该回调仅应用于带beanClass的bean定义，它不应用于带factory-method的bean；
	 * 5.后处理器可以实现SmartInstantiationAwareBeanPostProcessor子接口，能预测这里将要返回的bean对象类型；
	 * 6.默认实现返回null。
	 */
	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * Perform operations after the bean has been instantiated, via a constructor or factory method,
	 * but before Spring property population (from explicit properties or autowiring) occurs.
	 * <p>This is the ideal callback for performing custom field injection on the given bean
	 * instance, right before Spring's autowiring kicks in.
	 * <p>The default implementation returns {@code true}.
	 * @param bean the bean instance created, with properties not having been set yet
	 * @param beanName the name of the bean
	 * @return {@code true} if properties should be set on the bean; {@code false}
	 * if property population should be skipped. Normal implementations should return {@code true}.
	 * Returning {@code false} will also prevent any subsequent InstantiationAwareBeanPostProcessor
	 * instances being invoked on this bean instance.
	 * 1.返回true，表示应该设置bean的属性；返回false，表示应该跳过属性填充。
	 * 2.正常的实现应该返回true；返回false也会阻止该bean上任何后续的InstantiationAwareBeanPostProcessor实例被调用。
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	/*
	 * 1.在bean已经通过构造器或工厂方法实例化后、Spring属性填充（明确设置属性或自动连线）前进行操作；
	 * 2.这是对给定bena实例进行自定义域注入的最理想回调，因为就在Spring自动连线开始前；
	 * 3.默认实现返回true。
	 */
	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	/**
	 * Post-process the given property values before the factory applies them
	 * to the given bean. Allows for checking whether all dependencies have been
	 * satisfied, for example based on a "Required" annotation on bean property setters.
	 * <p>Also allows for replacing the property values to apply, typically through
	 * creating a new MutablePropertyValues instance based on the original PropertyValues,
	 * adding or removing specific values.
	 * <p>The default implementation returns the given {@code pvs} as-is.
	 * @param pvs the property values that the factory is about to apply (never {@code null})
	 * @param pds the relevant property descriptors for the target bean (with ignored
	 * dependency types - which the factory handles specifically - already filtered out)
	 * @param bean the bean instance created, but whose properties have not yet been set
	 * @param beanName the name of the bean
	 * @return the actual property values to apply to the given bean
	 * (can be the passed-in PropertyValues instance), or {@code null}
	 * to skip property population
	 * 返回待应用到给定bean的实际属性值（可能是一个传入的PropertyValues实例）；或返回null，则会跳过属性填充。
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.MutablePropertyValues
	 */
	/*
	 * 1.在bean工厂将给定的属性值应用到给定bean前后处理这些属性值；
	 * 2.可用于检测是否所有的依赖都满足了，如基于属性设置器上的"Required"注解；
	 * 3.也可用于替换要应用的属性值，典型地，如基于原来的PropertyValues创建一个新的MutablePropertyValues实例，增加或删除特定的值；
	 * 3.默认实现原封不动地返回给定PropertyValues实例。
	 */
	@Nullable
	default PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		return pvs;
	}

}
