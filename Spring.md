## Spring

### DefaultListableBeanFactory

Spring 的ConfigurableListableBeanFactory和BeanDefinitionRegistry接口的默认实现：一个基于 Bean 定义元数据的成熟 Bean 工厂，可通过后处理器进行扩展。BeanFactory的实现有很多，这个可以存放很多bean的一些元数据。

![image-20250426150024766](images/image-20250426150024766.png)

```java
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

/** Map from bean name to merged BeanDefinitionHolder. */
private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);

/** Map of singleton and non-singleton bean names, keyed by dependency type. */
private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

/** Map of singleton-only bean names, keyed by dependency type. */
private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

/** List of bean definition names, in registration order. */
private volatile List<String> beanDefinitionNames = new ArrayList<>(256);
```

Spring启动流程的逻辑都是将不同配置的方式去解析成统一的对象，然后统一解析完成后，交给BeanFactory去创建对象。但是注解，XML等不同的方式，是通过策略模式将他们统一解析成**BeanDefinition**，里面存储的就是一个个对象的"设计图"，**DefaultListableBeanFactory**就是用来去存储这些BeanDefinition对象的，以Map方式进行存储，所以不管是ClassPathXmlApplicationContext（基于Xml方式的加载方式）还是基于AnnotationConfigApplicationContext（基于注解的方式）都会通过**组合**的方式去去组合DefaultListableBeanFactory 。

XML方式（ClassPathXmlApplicationContext）

```java
@Override
protected final void refreshBeanFactory() throws BeansException {
    if (hasBeanFactory()) {
        destroyBeans();
        closeBeanFactory();
    }
    try {
        // 创建档案馆
        DefaultListableBeanFactory beanFactory = createBeanFactory();
        beanFactory.setSerializationId(getId());
        customizeBeanFactory(beanFactory);
        // 加载beanDefinition信息，里面会获取配置文件位置，去解析Xml文件
        loadBeanDefinitions(beanFactory);
        this.beanFactory = beanFactory;
    }
    catch (IOException ex) {
        throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
    }
}
// xml方式是耦合在refresh()中12大步里面的obtainFreshBeanFactory()中不是很好，扩展性降低，注解的方式使用了扩展节点来解析，扩展性更强。
```

**loadBeanDefinitions(beanFactory);**里面无非是创建ResourceLoader资源加载器去循环解析xml文件里面的每一项，最后封装成BeanDefinitionHolder，最后会被**BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());**注册到BeanDefinitionMap里面

```java
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
    BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
    if (bdHolder != null) {
        bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
        try {
            // Register the final decorated instance.
            BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
        }
        catch (BeanDefinitionStoreException ex) {
            getReaderContext().error("Failed to register bean definition with name '" +
                                     bdHolder.getBeanName() + "'", ele, ex);
        }
        // Send registration event. 发布一个通知事件
        getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
    }
}
```

registerBeanDefinition方法会是DefaultListableBeanFactory里面的方法，会先进行校验BeanDefinition是否合法，并且检查BeanDefinitionMap中是否已经还有该BeanDefinition信息，如果没有的话就会被添加到BeanDefinitionMap中。而且还会将BeanDefinition中的别名一并注册；虽然操作的是BeanDefinitionRegistry，但最后信息都是会被put到DefaultListableBeanfactory中；

```java
public static void registerBeanDefinition(
    BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
    throws BeanDefinitionStoreException {

    // Register bean definition under primary name.
    String beanName = definitionHolder.getBeanName();
    registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

    // Register aliases for bean name, if any.
    String[] aliases = definitionHolder.getAliases();
    if (aliases != null) {
        for (String alias : aliases) {
            registry.registerAlias(beanName, alias);
        }
    }
}
```

### Aware接口以及调用逻辑

Spring给我们暴露了很多Aware接口，这些接口通常以Aware结尾，表示Bean能够感知某些特定的Spring容器功能。

```java
/**
 * @author tchstart
 * @data 2025-04-26
 */
@Component
public class Person implements ApplicationContextAware {

    ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        // 利用回调机制，把ioc容器传入
        this.applicationContext = context;
    }
}
```

![image-20250426165620128](images/image-20250426165620128.png)

比如在组件中实现了**ApplicationContextAware**接口，就可以获取上下文对象；在组件中实现了**BeanFactoryAware**接口，就可以获取到BeanFactory信息。



**Aware的调用流程**

当我们将所有的配置解析成BeanDefinition之后，以为这就可以交给BeanFactory去创建对象。也就是refresh()中12大步中 **finishBeanFactoryInitialization(beanFactory);**来创建Bean，在创建Bean的过程中，会调用各种Aware接口，将需要的信息回调到Bean当中去。

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
    // ...
    // Instantiate all remaining (non-lazy-init) singletons.
    finishBeanFactoryInitialization(beanFactory);
    // ...
}
```

按照正常的对象创建的流程可以找到doCreateBean方法中的

```java
// 属性赋值
populateBean(beanName, mbd, instanceWrapper);
// 初始化Bean，这一步里面会执行Aware接口的回调，进行功能的增强
exposedObject = initializeBean(beanName, exposedObject, mbd);
```

**initializeBean**的第一步就是调用**invokeAwareMethods(beanName,bean);**来对其中三个Aware接口进行回调，剩余的会通过**BeanProcessor**的扩展点方式进行回调；

**initializeBean**方法中会依次走到**applyBeanPostProcessorsBeforeInitialization**调用BeanProcessor接口的**postProcessBeforeInitialization**会调用**ApplicationContextAwareProcessor**里面会给Bean实例回调信息，前提是实现对应的Aware。

```java
@SuppressWarnings("deprecation")
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
    invokeAwareMethods(beanName, bean);

    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    try {
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
            (mbd != null ? mbd.getResourceDescription() : null), beanName, ex.getMessage(), ex);
    }
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}

```

其中**BeanNameAware**，**BeanClassLoaderAware**，**BeanFactoryAware**三个接口时首先回调的。写死在代码中的，**initializeBean**的第一步。

```java
private void invokeAwareMethods(String beanName, Object bean) {
    if (bean instanceof Aware) {
        if (bean instanceof BeanNameAware beanNameAware) {
            beanNameAware.setBeanName(beanName);
        }
        if (bean instanceof BeanClassLoaderAware beanClassLoaderAware) {
            ClassLoader bcl = getBeanClassLoader();
            if (bcl != null) {
                beanClassLoaderAware.setBeanClassLoader(bcl);
            }
        }
        if (bean instanceof BeanFactoryAware beanFactoryAware) {
            beanFactoryAware.setBeanFactory(AbstractAutowireCapableBeanFactory.this);
        }
    }
}
```

下面这几个Aware接口是通过扩展点的方式**ApplicationContextAwareProcessor**进行回调

- EnvironmentAware
- EmbeddedValueResolverAware
- ResourceLoaderAware
- ApplicationEventPublisherAware
- MessageSourceAware
- ApplicationStartupAware
- ApplicationContextAware

```java
// ApplicationContextAwareProcessor  通过BeanProcessor扩展点的方式进行回调
@Override
@Nullable
public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (!(bean instanceof EnvironmentAware || bean instanceof EmbeddedValueResolverAware ||
          bean instanceof ResourceLoaderAware || bean instanceof ApplicationEventPublisherAware ||
          bean instanceof MessageSourceAware || bean instanceof ApplicationContextAware ||
          bean instanceof ApplicationStartupAware)) {
        return bean;
    }

    invokeAwareInterfaces(bean);
    return bean;
}

private void invokeAwareInterfaces(Object bean) {
    if (bean instanceof Aware) {
        // 如果这个bean是EnvironmentAware，则就给这个bean设置环境信息，下面的逻辑依旧
        if (bean instanceof EnvironmentAware environmentAware) {
            environmentAware.setEnvironment(this.applicationContext.getEnvironment());
        }
        if (bean instanceof EmbeddedValueResolverAware embeddedValueResolverAware) {
            embeddedValueResolverAware.setEmbeddedValueResolver(this.embeddedValueResolver);
        }
        if (bean instanceof ResourceLoaderAware resourceLoaderAware) {
            resourceLoaderAware.setResourceLoader(this.applicationContext);
        }
        if (bean instanceof ApplicationEventPublisherAware applicationEventPublisherAware) {
            applicationEventPublisherAware.setApplicationEventPublisher(this.applicationContext);
        }
        if (bean instanceof MessageSourceAware messageSourceAware) {
            messageSourceAware.setMessageSource(this.applicationContext);
        }
        if (bean instanceof ApplicationStartupAware applicationStartupAware) {
            applicationStartupAware.setApplicationStartup(this.applicationContext.getApplicationStartup());
        }
        if (bean instanceof ApplicationContextAware applicationContextAware) {
            applicationContextAware.setApplicationContext(this.applicationContext);
        }
    }
}

```

### populateBean (XML/注解)

在实例化对象中**finishBeanFactoryInitialization(beanFactory);**中里面会会对对象进行通过反射创建对象，实例化对象之后，此时对象只是创建了，但里面的值都还是为空，此时会调用**populateBean**进行属性赋值（XML、注解）两种方式都是在这个方法继续赋值。另外这个方法跟**refresh**方法一样。XML形式的属性赋值时耦合在方法中的，但是注解的方式也是通过**BeanPostProcessor**扩展点的方式进行属性赋值的。

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
    // ...

    // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
    // state of the bean before properties are set. This can be used, for example,
    // to support styles of field injection.
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        // 基于注解完成自动装配 AutowiredAnnotationBeanPostProcessor
        for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
            if (!bp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                return;
            }
        }
    }
	// ...
   
    if (pvs != null) {
   		// 通过xml方式进行属性赋值
        applyPropertyValues(beanName, mbd, bw, pvs);
    }
}
```

![image-20250426175439525](images/image-20250426175439525.png)

**AutowiredAnnotationBeanPostProcessor**是**InstantiationAwareBeanPostProcessor**的实现类，运行到此处会调用到**AutowiredAnnotationBeanPostProcessor**的**postProcessProperties**进行属性赋值完成基于注解的自动装配功能。通过**buildAutowiringMetadata**来解析属性或方法上是否含有@Autowired或者@Value注解，返回包装对象后，再通过**element.inject(target, beanName, pvs);**反射进行赋值。

```java
@Override
public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
    InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
    try {
        metadata.inject(bean, beanName, pvs);
    }
    catch (BeanCreationException ex) {
        throw ex;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
    }
    return pvs;
}
```

### BeanFactoryPostProcessor

对BeanFactory进行后置增强。两个方法都是在**invokeBeanFactoryPostProcessors**中一起执行的

#### BeanDefinitionRegistryPostProcessor

>基于注解的方式，AnnotationConfigApplicationContext中会有这个实现类ConfigurationClassPostProcessor，这个方法会解析配置类@ComponentScan,@Bean..等注解，构建BeanDefition信息

对象没创建之前，refresh中12大步中的**invokeBeanFactoryPostProcessors(beanFactory);**会被调用，获取到所有的**BeanDefinitionRegistryPostProcessor**并创建对象，这个接口相对于普通的Bean会提前初始化。这个接口在**invokeBeanFactoryPostProcessors(beanFactory);**中调用的是按照顺序的@PriorityOrderd > @Ordered > 没有实现任何接口的，按照这个顺序依次调用。当所有顺序的**postProcessBeanDefinitionRegistry**接口都执行玩了之后，会在统一调用**BeanFactoryPostProcessor.postProcessBeanFactory()**方法的实现，也是按照@PriorityOrderd > @Ordered > 没有实现任何接口的顺序执行**postProcessBeanFactory**方法的

```java
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
    // invokeBeanFactoryPostProcessors(beanFactory);中执行，会优先普通对象创建对象
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;
    
    // 当postProcessBeanDefinitionRegistry都执行完了之后，就会调用postProcessBeanFactory方法。
	@Override
	default void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}
}
```

### BeanPostProcessor

对Bean进行后置增强（在于改变）

```java
public interface BeanPostProcessor {
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
```

**registerBeanPostProcessors(beanFactory);**会注册**BeanPostProcessor**，此时Bean没有被创建，如果想让Bean的后置处理器对Bean进行增强，BeanPostProcessor要优先被创建，就是在此出创建，放入到一个**List<BeanPostProcessor>**集合中，提前创建对象。

- **MergedBeanDefinitionPostProcessor**
- **SmartInstantiationAwareBeanPostProcessor**
- **InstantiationAwareBeanPostProcessor**
- ...

```java
// 注册Bean的后置处理器，也就是提前实例化
protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
}

public static void registerBeanPostProcessors(
    ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

    // 获得容器中的所有的BeanPostProcessor
    String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

    int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
    beanFactory.addBeanPostProcessor(
        new BeanPostProcessorChecker(beanFactory, postProcessorNames, beanProcessorTargetCount));

    List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
    List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
    List<String> orderedPostProcessorNames = new ArrayList<>();
    List<String> nonOrderedPostProcessorNames = new ArrayList<>();
    for (String ppName : postProcessorNames) {
        if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
            priorityOrderedPostProcessors.add(pp);
            if (pp instanceof MergedBeanDefinitionPostProcessor) {
                internalPostProcessors.add(pp);
            }
        }
        else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
            orderedPostProcessorNames.add(ppName);
        }
        else {
            nonOrderedPostProcessorNames.add(ppName);
        }
    }

    // First, register the BeanPostProcessors that implement PriorityOrdered.
    sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

    // Next, register the BeanPostProcessors that implement Ordered.
    List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
    for (String ppName : orderedPostProcessorNames) {
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        orderedPostProcessors.add(pp);
        if (pp instanceof MergedBeanDefinitionPostProcessor) {
            internalPostProcessors.add(pp);
        }
    }
    sortPostProcessors(orderedPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, orderedPostProcessors);

    // Now, register all regular BeanPostProcessors.
    List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
    for (String ppName : nonOrderedPostProcessorNames) {
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        nonOrderedPostProcessors.add(pp);
        if (pp instanceof MergedBeanDefinitionPostProcessor) {
            internalPostProcessors.add(pp);
        }
    }
    registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

    // Finally, re-register all internal BeanPostProcessors.
    sortPostProcessors(internalPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, internalPostProcessors);

    // Re-register post-processor for detecting inner beans as ApplicationListeners,
    // moving it to the end of the processor chain (for picking up proxies etc).
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
}

```

#### SmartInstantiationAwareBeanPostProcessor

 预测Bean的类型，最后一次改变组件类型，在**registerListeners();**中会被调用

```java
protected void registerListeners() {
    //...

    // Do not initialize FactoryBeans here: We need to leave all regular beans
    // uninitialized to let post-processors apply to them!
    String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
   // ...
}
```

根据类型获取BeanNames里面会调用**SmartInstantiationAwareBeanPostProcessor**的后置处理器**predictBeanType**，将**beanDefinitionNames**遍历一边，调用**predictBeanType**最后一次确定Bean的类型。（此时beanDefinitionNames是所有的，包含BeanFactoryPostProcessor的对象，他们已经实例化了，虽然在beanDefinitionNames里面，但是不回在经过**predictBeanType**了，普通对象【还没有实例化的对象】会被次方法调用最后一次确定Bean的类型。

### InitializingBean

Bean组件初始化以后对组件进行后续设置（在于额外处理）因为他不回给你传入任何参数

```java
public interface InitializingBean {
	void afterPropertiesSet() throws Exception;
}
```

上面的**AutowiredAnnotationBeanPostProcessor**就是如此，注解版属性注入就是通过这个接口。