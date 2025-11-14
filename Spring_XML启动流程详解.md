# Spring 基于XML的启动流程详解

## 一、启动入口

### 1.1 代码入口

```12:13:Beanmeaet_spring_xml/src/main/java/com/beanmeat/App.java
// 使用ClassPathXmlApplicationContext加载XML配置
ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
```

当执行 `new ClassPathXmlApplicationContext("beans.xml")` 时，Spring的启动流程正式开始。

### 1.2 构造函数调用链

**ClassPathXmlApplicationContext** 的构造函数会：
1. 调用父类构造函数
2. 设置配置文件路径
3. **关键：调用 `refresh()` 方法** - 这是Spring容器初始化的核心方法

```java
// ClassPathXmlApplicationContext 构造函数
public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
    this(new String[] {configLocation}, true, null);
}

public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, 
                                      ApplicationContext parent) throws BeansException {
    super(parent);
    setConfigLocations(configLocations);
    if (refresh) {
        refresh(); // 核心方法：刷新容器
    }
}
```

## 二、核心方法：refresh() - 12个关键步骤

`refresh()` 方法是Spring容器初始化的核心，位于 `AbstractApplicationContext` 类中，包含12个关键步骤：

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // 1. 准备刷新上下文
        prepareRefresh();
        
        // 2. 获取BeanFactory（对于XML方式，这里会创建并加载BeanDefinition）
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
        
        // 3. 准备BeanFactory（配置标准上下文特性）
        prepareBeanFactory(beanFactory);
        
        // 4. 允许子类对BeanFactory进行后处理
        postProcessBeanFactory(beanFactory);
        
        // 5. 调用BeanFactoryPostProcessor（在Bean实例化之前）
        invokeBeanFactoryPostProcessors(beanFactory);
        
        // 6. 注册BeanPostProcessor（Bean的后置处理器）
        registerBeanPostProcessors(beanFactory);
        
        // 7. 初始化消息源（国际化）
        initMessageSource();
        
        // 8. 初始化事件广播器
        initApplicationEventMulticaster();
        
        // 9. 初始化其他特殊的Bean（子类实现）
        onRefresh();
        
        // 10. 注册监听器
        registerListeners();
        
        // 11. 完成BeanFactory初始化，实例化所有非懒加载的单例Bean
        finishBeanFactoryInitialization(beanFactory);
        
        // 12. 完成刷新，发布容器刷新事件
        finishRefresh();
    }
}
```

## 三、详细流程分析

### 步骤1：prepareRefresh() - 准备刷新上下文

**作用**：为刷新操作做准备

**主要工作**：
- 记录启动时间
- 设置容器状态为激活
- 初始化属性源（PropertySource）
- 验证必需的属性

```java
protected void prepareRefresh() {
    this.startupDate = System.currentTimeMillis();
    this.closed.set(false);
    this.active.set(true);
    // ... 其他初始化工作
}
```

### 步骤2：obtainFreshBeanFactory() - 获取BeanFactory

**作用**：创建或刷新BeanFactory，并加载BeanDefinition

**对于XML方式，这是关键步骤**：

#### 2.1 refreshBeanFactory()

```java
@Override
protected final void refreshBeanFactory() throws BeansException {
    if (hasBeanFactory()) {
        destroyBeans();
        closeBeanFactory();
    }
    try {
        // 创建DefaultListableBeanFactory（Bean的"档案馆"）
        DefaultListableBeanFactory beanFactory = createBeanFactory();
        beanFactory.setSerializationId(getId());
        customizeBeanFactory(beanFactory);
        
        // 关键：加载BeanDefinition（解析XML文件）
        loadBeanDefinitions(beanFactory);
        
        this.beanFactory = beanFactory;
    }
    catch (IOException ex) {
        throw new ApplicationContextException("I/O error parsing bean definition source", ex);
    }
}
```

#### 2.2 loadBeanDefinitions() - 加载BeanDefinition

**作用**：解析XML配置文件，将 `<bean>` 标签转换为 `BeanDefinition` 对象

**流程**：
1. 创建 `XmlBeanDefinitionReader`（XML读取器）
2. 将XML文件转换为 `Resource` 资源对象
3. 解析XML文件，遍历每个 `<bean>` 标签
4. 将每个Bean配置转换为 `BeanDefinition` 对象
5. 注册到 `DefaultListableBeanFactory` 的 `beanDefinitionMap` 中

**关键代码流程**：

```java
// XmlBeanDefinitionReader.loadBeanDefinitions()
public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
    // 1. 将Resource转换为EncodedResource
    EncodedResource encodedResource = new EncodedResource(resource);
    
    // 2. 获取输入流
    InputStream inputStream = encodedResource.getResource().getInputStream();
    
    // 3. 使用SAX解析器解析XML
    InputSource inputSource = new InputSource(inputStream);
    
    // 4. 调用doLoadBeanDefinitions()进行实际解析
    return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
}
```

**解析过程**：

```java
// DefaultBeanDefinitionDocumentReader.processBeanDefinition()
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
    // 1. 解析Bean元素，创建BeanDefinitionHolder
    BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
    
    if (bdHolder != null) {
        // 2. 装饰BeanDefinition（处理自定义标签）
        bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
        
        try {
            // 3. 注册BeanDefinition到BeanFactory
            BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, 
                getReaderContext().getRegistry());
        }
        catch (BeanDefinitionStoreException ex) {
            // 异常处理
        }
        
        // 4. 发送注册事件
        getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
    }
}
```

**注册到BeanFactory**：

```java
// BeanDefinitionReaderUtils.registerBeanDefinition()
public static void registerBeanDefinition(
        BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
    
    // 1. 使用主名称注册
    String beanName = definitionHolder.getBeanName();
    registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());
    
    // 2. 注册别名（如果有）
    String[] aliases = definitionHolder.getAliases();
    if (aliases != null) {
        for (String alias : aliases) {
            registry.registerAlias(beanName, alias);
        }
    }
}
```

**最终存储位置**：

```java
// DefaultListableBeanFactory.registerBeanDefinition()
@Override
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
    // 验证BeanDefinition
    // ...
    
    // 存储到Map中
    this.beanDefinitionMap.put(beanName, beanDefinition);
    
    // 添加到名称列表（保持注册顺序）
    this.beanDefinitionNames.add(beanName);
    
    // 移除旧的BeanDefinition（如果存在）
    // ...
}
```

**此时的状态**：
- XML文件已解析完成
- 所有 `<bean>` 标签已转换为 `BeanDefinition` 对象
- `BeanDefinition` 已存储在 `DefaultListableBeanFactory.beanDefinitionMap` 中
- **但Bean实例还未创建**（只是"设计图"）

### 步骤3：prepareBeanFactory() - 准备BeanFactory

**作用**：配置BeanFactory的标准特性

**主要工作**：
- 设置类加载器
- 设置表达式解析器（SpEL）
- 添加 `ApplicationContextAwareProcessor`（处理Aware接口）
- 注册一些特殊的Bean（如Environment、ResourceLoader等）

```java
protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    // 设置类加载器
    beanFactory.setBeanClassLoader(getClassLoader());
    
    // 设置表达式解析器
    beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver());
    
    // 添加属性编辑器注册器
    beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this));
    
    // 添加ApplicationContextAwareProcessor（处理Aware接口）
    beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
    
    // 忽略某些依赖注入的接口
    beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
    // ...
}
```

### 步骤4：postProcessBeanFactory() - 后处理BeanFactory

**作用**：允许子类对BeanFactory进行后处理（模板方法模式）

**默认实现**：空方法，子类可以重写

### 步骤5：invokeBeanFactoryPostProcessors() - 调用BeanFactoryPostProcessor

**作用**：在Bean实例化之前，对BeanFactory进行后置处理

**主要工作**：
- 获取所有 `BeanFactoryPostProcessor` 类型的Bean名称
- 按照优先级排序（PriorityOrdered > Ordered > 普通）
- 创建这些Bean的实例
- 调用 `postProcessBeanFactory()` 方法

**重要接口**：

#### BeanDefinitionRegistryPostProcessor

```java
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
    // 在BeanDefinition注册后，可以修改或添加新的BeanDefinition
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry);
    
    // 在BeanFactory准备完成后调用
    @Override
    default void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }
}
```

**执行顺序**：
1. 先执行所有 `BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry()`
2. 再执行所有 `BeanFactoryPostProcessor.postProcessBeanFactory()`

**注意**：这些PostProcessor会**提前实例化**（在普通Bean之前）

### 步骤6：registerBeanPostProcessors() - 注册BeanPostProcessor

**作用**：注册Bean的后置处理器，这些处理器会在Bean创建过程中被调用

**主要工作**：
- 获取所有 `BeanPostProcessor` 类型的Bean名称
- 按照优先级排序
- **提前创建这些BeanPostProcessor的实例**
- 注册到BeanFactory的 `beanPostProcessors` 列表中

**重要接口**：

```java
public interface BeanPostProcessor {
    // Bean初始化之前调用
    @Nullable
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
    
    // Bean初始化之后调用
    @Nullable
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

**常见的BeanPostProcessor**：
- `ApplicationContextAwareProcessor`：处理Aware接口
- `AutowiredAnnotationBeanPostProcessor`：处理@Autowired注解
- `CommonAnnotationBeanPostProcessor`：处理@Resource等注解

**注意**：BeanPostProcessor也会**提前实例化**

### 步骤7：initMessageSource() - 初始化消息源

**作用**：初始化国际化消息源（MessageSource）

**默认行为**：如果没有配置，会创建一个 `DelegatingMessageSource`

### 步骤8：initApplicationEventMulticaster() - 初始化事件广播器

**作用**：初始化事件广播器，用于发布和监听Spring事件

**默认行为**：创建 `SimpleApplicationEventMulticaster`

### 步骤9：onRefresh() - 子类扩展点

**作用**：允许子类在刷新时进行特殊处理

**默认实现**：空方法

### 步骤10：registerListeners() - 注册监听器

**作用**：注册应用监听器（ApplicationListener）

**主要工作**：
- 注册早期事件监听器
- 查找所有 `ApplicationListener` 类型的Bean
- 注册到事件广播器

**注意**：此时会调用 `SmartInstantiationAwareBeanPostProcessor.predictBeanType()` 来预测Bean类型，但**不会实例化普通Bean**

### 步骤11：finishBeanFactoryInitialization() - 完成BeanFactory初始化

**作用**：**实例化所有非懒加载的单例Bean** - 这是创建Bean实例的关键步骤

**主要工作**：

```java
protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    // 初始化ConversionService（类型转换服务）
    if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME)) {
        beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class);
    }
    
    // 初始化LoadTimeWeaverAware（AOP相关）
    if (!beanFactory.containsTempClassLoader()) {
        beanFactory.setTempClassLoader(null);
    }
    
    // 冻结BeanDefinition，不允许再修改
    beanFactory.freezeConfiguration();
    
    // 关键：实例化所有非懒加载的单例Bean
    beanFactory.preInstantiateSingletons();
}
```

#### 11.1 preInstantiateSingletons() - 预实例化单例Bean

```java
@Override
public void preInstantiateSingletons() throws BeansException {
    // 获取所有BeanDefinition名称
    List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);
    
    // 遍历所有BeanDefinition
    for (String beanName : beanNames) {
        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        
        // 只实例化单例、非抽象、非懒加载的Bean
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            // 如果是FactoryBean
            if (isFactoryBean(beanName)) {
                // 处理FactoryBean
            }
            else {
                // 关键：获取Bean实例（如果不存在则创建）
                getBean(beanName);
            }
        }
    }
    
    // 触发所有单例Bean的初始化后回调
    for (String beanName : beanNames) {
        Object singletonInstance = getSingleton(beanName);
        if (singletonInstance instanceof SmartInitializingSingleton) {
            ((SmartInitializingSingleton) singletonInstance).afterSingletonsInstantiated();
        }
    }
}
```

#### 11.2 getBean() - 获取Bean实例

```java
@Override
public Object getBean(String name) throws BeansException {
    return doGetBean(name, null, null, false);
}
```

#### 11.3 doGetBean() - 实际获取Bean的逻辑

```java
protected <T> T doGetBean(String name, @Nullable Class<T> requiredType,
                          @Nullable Object[] args, boolean typeCheckOnly) {
    String beanName = transformedBeanName(name);
    Object bean;
    
    // 1. 尝试从缓存中获取单例Bean
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null && args == null) {
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }
    else {
        // 2. Bean不存在，需要创建
        // ...
        
        // 3. 标记Bean正在创建（解决循环依赖）
        if (mbd.isSingleton()) {
            sharedInstance = getSingleton(beanName, () -> {
                try {
                    // 创建Bean实例
                    return createBean(beanName, mbd, args);
                }
                catch (BeansException ex) {
                    // 异常处理
                }
            });
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
        }
    }
    
    return (T) bean;
}
```

#### 11.4 createBean() - 创建Bean实例

```java
@Override
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    // 1. 解析Bean的Class类型
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
    
    // 2. 准备方法覆盖
    mbd.prepareMethodOverrides();
    
    // 3. 给BeanPostProcessor一个机会返回代理对象（而不是目标Bean）
    Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
    if (bean != null) {
        return bean; // 如果返回了代理对象，直接返回
    }
    
    // 4. 实际创建Bean
    Object beanInstance = doCreateBean(beanName, mbdToUse, args);
    
    return beanInstance;
}
```

**resolveBeforeInstantiation()** - 实例化前的后处理：

```java
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
    Object bean = null;
    if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            Class<?> targetType = determineTargetType(beanName, mbd);
            if (targetType != null) {
                // 调用InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()
                bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                if (bean != null) {
                    // 如果返回了Bean，调用postProcessAfterInitialization()
                    bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                }
            }
        }
        mbd.beforeInstantiationResolved = (bean != null);
    }
    return bean;
}
```

#### 11.5 doCreateBean() - 实际创建Bean

**这是Bean创建的核心方法，包含三个关键步骤**：

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    BeanWrapper instanceWrapper = null;
    
    // ========== 第一步：实例化Bean ==========
    if (mbd.isSingleton()) {
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }
    if (instanceWrapper == null) {
        // 通过反射创建Bean实例
        instanceWrapper = createBeanInstance(beanName, mbd, args);
    }
    Object bean = instanceWrapper.getWrappedInstance();
    Class<?> beanType = instanceWrapper.getWrappedClass();
    
    // 应用MergedBeanDefinitionPostProcessor
    applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
    
    // ========== 解决循环依赖：提前暴露Bean ==========
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
                                      isSingletonCurrentlyInCreation(beanName));
    if (earlySingletonExposure) {
        // 将Bean工厂添加到三级缓存（解决循环依赖）
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }
    
    // ========== 第二步：属性注入（populateBean） ==========
    Object exposedObject = bean;
    try {
        // 填充Bean的属性（依赖注入）
        populateBean(beanName, mbd, instanceWrapper);
        
        // ========== 第三步：初始化Bean ==========
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    }
    catch (Throwable ex) {
        // 异常处理
    }
    
    // ========== 注册单例Bean ==========
    if (earlySingletonExposure) {
        Object earlySingletonReference = getSingleton(beanName, false);
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
        }
    }
    
    // 注册为单例
    if (mbd.isSingleton()) {
        registerSingleton(beanName, exposedObject);
    }
    
    return exposedObject;
}
```

#### 11.5.1 createBeanInstance() - 实例化Bean

**作用**：通过反射创建Bean实例

**主要工作**：
- 确定使用哪个构造器（可能通过 `SmartInstantiationAwareBeanPostProcessor.determineCandidateConstructors()`）
- 通过反射调用构造器创建实例
- 返回 `BeanWrapper` 包装对象

```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, 
                                         @Nullable Object[] args) {
    Class<?> beanClass = resolveBeanClass(mbd, beanName);
    
    // 1. 检查是否有Supplier（Spring 5.0+）
    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
        return obtainFromSupplier(instanceSupplier, beanName);
    }
    
    // 2. 检查是否有工厂方法
    if (mbd.getFactoryMethodName() != null) {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }
    
    // 3. 使用构造器实例化
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
        mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
        return autowireConstructor(beanName, mbd, ctors, args);
    }
    
    // 4. 使用默认构造器
    return instantiateBean(beanName, mbd);
}
```

**对于你的SimpleBean**：
- 使用默认无参构造器
- 调用 `SimpleBean()` 构造函数
- 打印 "SimpleBean 构造函数被调用"

#### 11.5.2 populateBean() - 属性注入

**作用**：将依赖注入到Bean中

**主要工作**：

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, 
                            @Nullable BeanWrapper bw) {
    PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
    
    // 1. 调用InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
            if (!bp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                return; // 如果返回false，停止属性注入
            }
        }
    }
    
    // 2. 获取属性值
    PropertyValues pvsToUse = pvs;
    if (hasInstantiationAwareBeanPostProcessors()) {
        // 调用InstantiationAwareBeanPostProcessor.postProcessProperties()
        // 这里会处理@Autowired注解（通过AutowiredAnnotationBeanPostProcessor）
        for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
            PropertyValues pvsToUse = bp.postProcessProperties(pvsToUse, bw.getWrappedInstance(), beanName);
        }
    }
    
    // 3. 应用XML配置的属性值
    if (pvsToUse != null) {
        applyPropertyValues(beanName, mbd, bw, pvsToUse);
    }
}
```

**对于XML方式**：
- `applyPropertyValues()` 会解析XML中的 `<property>` 标签
- 通过反射调用setter方法注入属性值

**对于注解方式**：
- `AutowiredAnnotationBeanPostProcessor.postProcessProperties()` 会：
  - 扫描Bean的所有字段和方法
  - 查找 `@Autowired`、`@Value` 等注解
  - 通过反射注入依赖

#### 11.5.3 initializeBean() - 初始化Bean

**作用**：初始化Bean，调用各种初始化回调

**主要工作**：

```java
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
    // ========== 1. 调用Aware接口 ==========
    invokeAwareMethods(beanName, bean);
    
    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        // ========== 2. BeanPostProcessor.postProcessBeforeInitialization() ==========
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }
    
    try {
        // ========== 3. 调用初始化方法 ==========
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName, 
                                       "Invocation of init method failed", ex);
    }
    
    if (mbd == null || !mbd.isSynthetic()) {
        // ========== 4. BeanPostProcessor.postProcessAfterInitialization() ==========
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }
    
    return wrappedBean;
}
```

**详细步骤**：

##### 1. invokeAwareMethods() - 调用Aware接口

```java
private void invokeAwareMethods(String beanName, Object bean) {
    if (bean instanceof Aware) {
        // BeanNameAware
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        // BeanClassLoaderAware
        if (bean instanceof BeanClassLoaderAware) {
            ClassLoader bcl = getBeanClassLoader();
            if (bcl != null) {
                ((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
            }
        }
        // BeanFactoryAware
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
        }
    }
}
```

**其他Aware接口**（通过 `ApplicationContextAwareProcessor` 处理）：
- `EnvironmentAware`
- `EmbeddedValueResolverAware`
- `ResourceLoaderAware`
- `ApplicationEventPublisherAware`
- `MessageSourceAware`
- `ApplicationStartupAware`
- `ApplicationContextAware`

##### 2. applyBeanPostProcessorsBeforeInitialization()

**作用**：调用所有 `BeanPostProcessor.postProcessBeforeInitialization()`

**重要处理器**：
- `ApplicationContextAwareProcessor`：处理ApplicationContext相关的Aware接口

##### 3. invokeInitMethods() - 调用初始化方法

```java
protected void invokeInitMethods(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
        // 调用InitializingBean.afterPropertiesSet()
        ((InitializingBean) bean).afterPropertiesSet();
    }
    
    if (mbd != null && bean.getClass() != NullBean.class) {
        String initMethodName = mbd.getInitMethodName();
        if (StringUtils.hasLength(initMethodName) &&
            !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
            !mbd.isExternallyManagedInitMethod(initMethodName)) {
            // 调用XML配置的init-method
            invokeCustomInitMethod(beanName, bean, mbd);
        }
    }
}
```

**初始化方法调用顺序**：
1. `InitializingBean.afterPropertiesSet()`（如果实现了该接口）
2. XML配置的 `init-method`（如果配置了）

##### 4. applyBeanPostProcessorsAfterInitialization()

**作用**：调用所有 `BeanPostProcessor.postProcessAfterInitialization()`

**常见用途**：
- AOP代理对象的创建（`AbstractAutoProxyCreator`）

### 步骤12：finishRefresh() - 完成刷新

**作用**：完成容器的刷新，发布容器刷新事件

**主要工作**：

```java
protected void finishRefresh() {
    // 1. 清除资源缓存
    clearResourceCaches();
    
    // 2. 初始化LifecycleProcessor
    initLifecycleProcessor();
    
    // 3. 启动所有实现了Lifecycle接口的Bean
    getLifecycleProcessor().onRefresh();
    
    // 4. 发布容器刷新事件
    publishEvent(new ContextRefreshedEvent(this));
    
    // 5. 注册到LiveBeansView（JMX相关）
    LiveBeansView.registerApplicationContext(this);
}
```

## 四、Bean创建流程图

```
new ClassPathXmlApplicationContext("beans.xml")
    ↓
refresh()
    ↓
obtainFreshBeanFactory()
    ↓
refreshBeanFactory()
    ↓
loadBeanDefinitions(beanFactory)
    ↓
解析XML → 创建BeanDefinition → 注册到beanDefinitionMap
    ↓
finishBeanFactoryInitialization(beanFactory)
    ↓
preInstantiateSingletons()
    ↓
getBean(beanName)
    ↓
createBean(beanName, mbd, args)
    ↓
doCreateBean(beanName, mbd, args)
    ├─ createBeanInstance()      [实例化：反射创建对象]
    ├─ populateBean()             [属性注入：依赖注入]
    └─ initializeBean()           [初始化：Aware、PostProcessor、init-method]
        ├─ invokeAwareMethods()
        ├─ postProcessBeforeInitialization()
        ├─ invokeInitMethods()
        └─ postProcessAfterInitialization()
    ↓
注册到singletonObjects（一级缓存）
    ↓
Bean创建完成，可以使用
```

## 五、关键数据结构

### 5.1 DefaultListableBeanFactory 的核心Map

```java
// 存储BeanDefinition（Bean的"设计图"）
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

// Bean名称列表（保持注册顺序）
private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

// 单例Bean缓存（一级缓存）
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

// 早期单例对象缓存（二级缓存，解决循环依赖）
private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

// 单例工厂缓存（三级缓存，解决循环依赖）
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

// BeanPostProcessor列表
private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
```

### 5.2 BeanDefinition 存储的信息

- Bean的类名（`beanClassName`）
- 作用域（`scope`：singleton/prototype）
- 是否懒加载（`lazyInit`）
- 属性值（`propertyValues`）
- 构造器参数（`constructorArgumentValues`）
- 初始化方法（`initMethodName`）
- 销毁方法（`destroyMethodName`）
- 等等...

## 六、循环依赖解决机制

### 6.1 三级缓存

1. **一级缓存（singletonObjects）**：完全初始化完成的单例Bean
2. **二级缓存（earlySingletonObjects）**：提前暴露的Bean（未完全初始化）
3. **三级缓存（singletonFactories）**：Bean工厂（用于创建早期Bean）

### 6.2 没有循环依赖时，三级缓存的使用情况

**重要问题**：如果没有循环依赖，还会用到三级缓存和二级缓存吗？

**答案**：
- **三级缓存**：**会被使用**（放入），但**不会被读取**
- **二级缓存**：**不会被使用**（既不会放入，也不会读取）
- **一级缓存**：**始终被使用**

#### 详细说明

**1. 三级缓存的放入（无条件）**

在 `doCreateBean()` 方法中，只要满足以下条件，就会将Bean的工厂放入三级缓存：

```java
boolean earlySingletonExposure = (mbd.isSingleton() && 
                                  this.allowCircularReferences && 
                                  isSingletonCurrentlyInCreation(beanName));
if (earlySingletonExposure) {
    // 无论是否有循环依赖，都会执行这一步
    addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
}
```

**关键点**：Spring在实例化Bean后，**不知道是否会有循环依赖**，所以会**无条件地**将Bean工厂放入三级缓存，这是一种**防御性编程**策略。

**2. 三级缓存的读取（有循环依赖时才读取）**

三级缓存的读取发生在 `getSingleton()` 方法中：

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // 1. 先从一级缓存获取
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        synchronized (this.singletonObjects) {
            // 2. 从二级缓存获取
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                // 3. 从三级缓存获取工厂，创建Bean并放入二级缓存
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    singletonObject = singletonFactory.getObject();
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}
```

**关键点**：
- 只有当 `isSingletonCurrentlyInCreation(beanName)` 为 `true` 时（即Bean正在创建中），才会去读取二级和三级缓存
- **如果没有循环依赖**，在属性注入时调用 `getBean()` 获取依赖Bean时，该Bean还未开始创建，`isSingletonCurrentlyInCreation()` 返回 `false`，**不会读取三级缓存**
- 三级缓存中的工厂会一直存在，直到Bean完成初始化后，在 `registerSingleton()` 时被清理

**3. 二级缓存的使用（只在有循环依赖时使用）**

二级缓存的使用场景：
- 只有在**从三级缓存获取Bean**时，才会将Bean放入二级缓存
- 如果没有循环依赖，就不会有从三级缓存获取Bean的操作，所以**二级缓存不会被使用**

**4. 没有循环依赖时的完整流程**

```
创建SimpleBean（没有循环依赖）

1. 实例化SimpleBean
   - createBeanInstance() → 调用SimpleBean()构造函数
   - 打印"SimpleBean 构造函数被调用"

2. 提前暴露（防御性操作）
   - 将SimpleBean的工厂放入三级缓存
   - 【注意】此时三级缓存中有SimpleBean的工厂，但不会被读取

3. 属性注入（populateBean）
   - SimpleBean没有需要注入的属性
   - 不会调用getBean()获取其他Bean
   - 三级缓存不会被读取

4. 初始化（initializeBean）
   - 调用Aware接口（如果有）
   - 调用BeanPostProcessor
   - 调用init-method（如果有）

5. 注册单例
   - registerSingleton(beanName, exposedObject)
   - 将SimpleBean放入一级缓存
   - 从三级缓存移除SimpleBean的工厂（清理）
   - 【注意】二级缓存从未被使用

结果：
- 一级缓存：SimpleBean实例 ✓
- 二级缓存：空（从未使用）
- 三级缓存：空（已清理）
```

**5. 有循环依赖时的完整流程**

```
A依赖B，B依赖A

1. 创建A
   - 实例化A → 将A的工厂放入三级缓存
   - 属性注入时发现需要B → 调用getBean(B)

2. 创建B
   - 实例化B → 将B的工厂放入三级缓存
   - 属性注入时发现需要A → 调用getBean(A)
   - 此时A正在创建中 → 从三级缓存获取A的工厂
   - 创建A的早期对象 → 放入二级缓存
   - 从三级缓存移除A的工厂
   - B属性注入完成 → B初始化完成 → B放入一级缓存

3. 回到A
   - 从一级缓存获取B（B已完成）
   - A属性注入完成 → A初始化完成
   - 检查二级缓存中的A（如果有代理，使用代理）
   - A放入一级缓存
   - 从二级缓存移除A

结果：
- 一级缓存：A实例、B实例 ✓
- 二级缓存：空（已清理）
- 三级缓存：空（已清理）
```

### 6.3 总结

| 缓存级别 | 没有循环依赖 | 有循环依赖 |
|---------|------------|----------|
| **一级缓存** | ✅ 使用（最终存储） | ✅ 使用（最终存储） |
| **二级缓存** | ❌ 不使用 | ✅ 使用（临时存储） |
| **三级缓存** | ⚠️ 放入但不读取 | ✅ 使用（获取早期Bean） |

**设计原因**：
1. Spring无法提前知道是否有循环依赖，所以采用**防御性策略**，总是提前暴露Bean
2. 三级缓存的设计主要是为了**支持AOP代理**：如果Bean需要AOP代理，通过工厂可以返回代理对象，而不是原始对象
3. 即使没有循环依赖，放入三级缓存的开销也很小，但可以保证代码的统一性和正确性

**性能影响**：
- 没有循环依赖时，三级缓存只是多了一次Map的put操作，性能影响可忽略
- 二级缓存完全不会被使用，没有额外开销

## 七、总结

### 7.1 启动流程的核心步骤

1. **解析阶段**：XML → BeanDefinition → 存储到Map
2. **准备阶段**：配置BeanFactory、注册PostProcessor
3. **创建阶段**：实例化 → 属性注入 → 初始化
4. **完成阶段**：发布事件、完成刷新

### 7.2 关键设计模式

- **模板方法模式**：`refresh()` 方法定义了流程框架
- **策略模式**：不同的配置方式（XML/注解）统一解析为BeanDefinition
- **工厂模式**：BeanFactory创建Bean实例
- **观察者模式**：事件发布和监听
- **责任链模式**：BeanPostProcessor链式调用

### 7.3 扩展点

- **BeanFactoryPostProcessor**：在Bean实例化前修改BeanDefinition
- **BeanPostProcessor**：在Bean创建过程中进行增强
- **Aware接口**：让Bean感知Spring容器
- **InitializingBean**：Bean初始化后的回调
- **FactoryBean**：自定义Bean创建逻辑

### 7.4 代码执行流程

```java
// 1. 创建容器
ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
// → 调用refresh()
// → 解析beans.xml
// → 创建SimpleBean的BeanDefinition
// → 注册到beanDefinitionMap
// → 创建SimpleBean实例（调用构造函数，打印"SimpleBean 构造函数被调用"）
// → 属性注入（你的SimpleBean没有需要注入的属性）
// → 初始化（没有Aware、没有init-method）
// → SimpleBean创建完成

// 2. 获取Bean
SimpleBean simpleBean = context.getBean("simpleBean", SimpleBean.class);
// → 从singletonObjects（一级缓存）获取
// → 返回已创建的SimpleBean实例

// 3. 使用Bean
simpleBean.sayHello();
// → 打印"Hello from SimpleBean"

// 4. 关闭容器
context.close();
// → 销毁所有单例Bean
// → 调用destroy-method（如果有）
```
