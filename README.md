# 🚀 lamb-framework

[![License: apache2.0](https://img.shields.io/github/license/tensorflow/tensorflow.svg)](https://www.apache.org/licenses/LICENSE-2.0)
## 📖 Introduction

Welcome to lamb-framework,I hope more practitioners can join me in improving the lamb-framework, making the framework simpler and more user-friendly. This will allow programmers to focus on their business code, without having to worry about the complex configuration of each component.

欢迎,我希望更多的从业者能加入我，共同完善lamb-framework，使框架变得更加简单和易用。这将使程序员能够专注于业务代码，而无需担心每个组件的复杂数
***每个组件用lamb-framework-sub-xxxx命名形式***
***每个properties 使用 lamb.xxx.xxx_xxx命名形式***
| 组件名称               | 说明         |
| ----------        	| ----------- |
| lamb-framework-common      		| 公共方法模块       |
| lamb-framework-sub-guid   		| 唯一序列号GUID生成组件        |
| lamb-framework-sub-openai   		| openAi调用组件        |
| lamb-framework-sub-redis   		| 抽象redis组件        |
| lamb-framework-web-core   		| reactor web核心 基于reactive webflux        |
| lamb-framework-web-security   	| 权限框架        |



## lamb-framework-sub-openai
在pom文件中引用下面代码块
```		
  <dependency>
    <groupId>org.lamb.framework</groupId>
    <artifactId>lamb-framework-sub-openai</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </dependency>
```
使用下面的示例来调用
```
 LambOpenAiUniqueParam lambOpenAiUniqueParam = LambOpenAiUniqueParam.builder().uniqueId(req.getUniqueId()).uniqueTime(req.getUniqueTime()).build();
        LambOpenAiPaintParam param =  LambOpenAiPaintParam.builder()
                .prompt(req.getPrompt())
                .lambOpenAiUniqueParam(lambOpenAiUniqueParam)
                .userId(userId)
                .openAiApiKey(openAiApiKey)
                .n(4)
                .size(LambOpenAiContract.image_size_512)
                .responseFormat(LambOpenAiContract.responseFormat)
                .timeOut(LambOpenAiContract.clientTimeOut)
                .quota(quato)
                .maxTokens(imageTokens(LambOpenAiContract.image_size_512,4) + encoding(req.getPrompt()))
                .build();
        return returning(lambOpenAiPaintService.execute(param).flatMap(e->{
            //模拟扣减配额
            quato = quato-e.getCurrentTotalTokens();
            return Mono.just(e);
        }));
```

## lamb-framework-sub-redis
在pom文件中引用下面代码块
```		
<dependency>
	<groupId>org.lamb.framework</groupId>
	<artifactId>lamb-framework-redis</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```
可以针对多数据源配置,使用LambReactiveRedisOperation.build()方法来切换不同的数据源
例如:
```
    @Resource(name = "lambAuthRedisTemplate")
    private ReactiveRedisTemplate lambAuthRedisTemplate;
    
    LambReactiveRedisOperation.build(lambAuthRedisTemplate).hasKey(authToken);
```
## lamb-framework-sub-guid
在pom文件中引用下面代码块

```		
<dependency>
	<groupId>org.lamb.framework</groupId>
	<artifactId>lamb-framework-guid</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```
引用guid之后你必须配置
```
lamb.guid.datacenter_id=xxx
lamb.guid.machine_id=xxx
```
并注入
```
@Resource
private LambGUIDFactory lambGUIDFactory;
```
才可使用GUID()方法
```
lambGUIDFactory.GUID();
```
## lamb-framework-web-security
在pom文件中引用下面代码块
```
<dependency>
  <groupId>org.lamb.framework</groupId>
  <artifactId>lamb-framework-web-security</artifactId>
   <version>0.0.1-SNAPSHOT</version>
</dependency>
```
###统一认证
在请求的headers中添加Auth-Token：xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
可以使用如下的例子来设置token
```
@Resource
private LambPrincipalUtil lambPrincipalUtil;

@GetMapping("/login")
public Mono login(){  //1
    User user =  User.builder().age(30).name("王刚").school("兰州理工大学").build();
    String userJson = JsonUtil.objToString(user);
    String authToken = lambPrincipalUtil.setPrincipalToToken(userJson);
    return returning(authToken);
}
```

并配置auth和autz的redis-database信息
```
lamb.security.redis.host=47.98.122.4
lamb.security.redis.password=XXXXXXXXXXXX
#default
lamb.security.redis.port=6379
lamb.security.redis.lettuce.pool.max_active=8
lamb.security.redis.lettuce.pool.max_wait_seconds=50
lamb.security.redis.lettuce.pool.max_idle=8
lamb.security.redis.lettuce.pool.min_idle=0
```
lamb-security已经写好了统一身份认证,无论是多服务和单机应用程序都都可以使用auth-token的形式登陆
在引用lamb-security后你需要在properties配置文件中配置你的auth认证的redis数据库和autz授权redis数据库
lamb-security默认使用redis来存储用户的auth-token和request path权限信息。
如果需要更改auth-token和request path权限信息的存储位置,可以重新配置bean
例如：
```
    lambCustomAuthManager extends LambAuthManager
    
    lambCustomAutzManager extends LambAutzManager
    
```
重写父类的方法覆盖掉逻辑并注入

```
    
    @Bean
    public LambAutzManager lambCustomAutzManager(LambCustomAuthManager lambCustomAuthManager){
        return new LambCustomAutzManager(lambCustomAuthManager) {
            @Override
            public boolean verify(String currentPathAutzTree, String principal) {
	    	//your code 
                return true;
            }
        };
    }

    @Bean
    public LambAuthManager lambCustomAuthManager(){
        return new LambCustomAuthManager(){
            @Override
            public boolean verify(String principal) {
	    	//your code 
                return true;
            }
        };
    }
```
如果你不希望更改身份认证校验逻辑，但是需要添加一些个性化的账号校验逻辑，你可以只重写方法来实现.
```
LambAuthManager.verify
```

授权校验的逻辑需要自己去写逻辑,框架提供了verify接口
```
LambAutzManager.verify(String currentPathAutzTree,String principal)
```
currentPathAutzTree代表当前路径的权限树
principal代表当前用户信息
你可以在verify接口中去校验currentPathAutzTree中的角色和权限是否存在于principal

## lamb-framework-web-core
web框架的核心 lamb框架采用了springboot2.7.5并使用了spring boot web-flux响应式web框架,详情见lamb-framework-demo
因为采用的web-flux，请不要提前订阅你的mono or flux 框架会帮你自动处理 否则无法response给api调用者
以下代码块是错误示范:
```
error code

public class DemoApplication extends LambResponseHandler

@GetMapping("/testSecurity1")
public Mono testSecurity1(){  
    userService.getUser().subscribe()
    return returning();
}    
```
在启动类中添加
```
@SpringBootApplication(scanBasePackages = {"org.lamb.framework","your packages"} )
@EnableWebFlux
@RestController

```
来开启lamb-framework-web-core功能
框架中已经写好了统一异常，LambGlobalExceptionHandler
使用这样的形式去抛出自己的异常，否则都为ES000000000
```
throw new LambEventException(ES00000099);
```
```
{
    "serviceCode": "E000000000",
    "serviceMessage": "操作成功",
    "data": "lamb.auth.token.c495e2e9e4a32c94d3791f5602d20a97"
}
```
```
{
    "serviceCode": "EA00000003",
    "serviceMessage": "无效令牌"
}
```
你的controller类需要继承LambResponseHandler
```
public class DemoApplication extends LambResponseHandler

@GetMapping("/testSecurity1")
public Mono testSecurity1(){  
    return returning();
}    
```

你可以使用下面的方法返回你的信息保持返回信息的格式一致性

```
protected Mono<ServerResponse> routing(Collection data)

    protected Mono<ServerResponse> routing(Object data)

    protected Mono<ServerResponse> routing(Mono data) 

    protected Mono<ServerResponse> routing(Flux data)

    protected Mono<ServerResponse> routing() 

    protected Mono<LambResponseTemplete> returning(Collection data) {
        return this.handlerFlux(Flux.just(data));
    }

    protected Mono<LambResponseTemplete> returning(Object data)

    protected Mono<LambResponseTemplete> returning(Mono data)

    protected Mono<LambResponseTemplete> returning(Flux data)

    protected Mono<LambResponseTemplete> returning()
```
routing是针对路由形式的返回
returning是针对标准形式的返回
下面是路由形式的写法
```
   @Bean
    RouterFunction<ServerResponse> userRouterFunction(UserHandler userHandler) {
        return RouterFunctions.nest(RequestPredicates.path("/test")
                ,RouterFunctions.route(RequestPredicates.GET("/testSecurity2"), userHandler::testSecurity2)
        );
    }

    //路由写法
    @Component
    class UserHandler{
        public Mono<ServerResponse> testSecurity2(ServerRequest serverRequest) {
            return routing();
        }
    }
```
