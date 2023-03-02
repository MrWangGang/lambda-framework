# lamb-framework
## lamb-framework-sub-redis
在pom文件中引用下面代码块
```		
<dependency>
	<groupId>org.lamb.framework</groupId>
	<artifactId>lamb-framework-redis</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
````
可以针对多数据源配置,使用LambReactiveRedisOperation.build()方法来切换不同的数据源
例如:
```
    @Resource(name = "lambAuthRedisTemplate")
    private ReactiveRedisTemplate lambAuthRedisTemplate;
    
    LambReactiveRedisOperation.build(lambAuthRedisTemplate).hasKey(authToken);
```
##lamb-framework-sub-redis



