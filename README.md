# 🚀 lamb-framework

[![License: apache2.0](https://img.shields.io/github/license/tensorflow/tensorflow.svg)](https://www.apache.org/licenses/LICENSE-2.0)
## 📖 简介

Welcome to lambda-framework,I hope more practitioners can join me in improving the lambda-framework, making the framework simpler and more user-friendly. This will allow programmers to focus on their business code, without having to worry about the complex configuration of each component.

lambda-framework，使框架变得更加简单和易用。这将使程序员能够专注于业务代码，而无需担心每个组件的复杂配置

***每个组件用lambda-framework-xxxx命名形式***
***每个properties 使用 lambda.组件name.xxx  or  xxx-xxx命名形式***
***对外暴露bean 使用 模块名+功能命名形式 类似securityAuthRedisConfig***
| 组件名称               | 说明         |
| ----------        	| ----------- |
| lambda-framework-common      	| 公共方法模块       |
| lambda-framework-guid   		| 唯一序列号GUID生成组件        |
| lambda-framework-rpc   	| 远程调用框架        |
| lambda-framework-openai   	| openAi调用组件        |
| lambda-framework-redis   		| 抽象redis组件        |
| lambda-framework-repository   	| 持久层框架        |
| lambda-framework-web   		| reactor web核心 基于reactive webflux        |
| lambda-framework-security   	| 权限组件        |
| lambda-framework-compliance  	| 合规组件        |

***微服务建议:***
***每个微服务得有独特的异常命名，命名规范为 微服务名+ExceptionEnums***
***异常枚举必须实现ExceptionEnumFunction接口***
***ES_XXXXX_XXX ES开头代表这是框架底层抛出的异常***

新建一个工程的pom准备
```
    <parent>
        <groupId>org.lambda.framework</groupId>
        <artifactId>lambda-framework</artifactId>
        <version>1.0.0</version>
        <!--<relativePath/>--> <!-- lookup parent from repository -->
    </parent>
```

使用你想用的组件，不需要填写版本号，版本号与parent version同步 例如:
```
<dependency>
	<groupId>org.lambda.framework</groupId>
	<artifactId>lambda-framework-security</artifactId>
</dependency>
```
创建异常枚举类->例:
```
public enum SecurityExceptionEnum implements ExceptionEnumFunction {

    //系统异常-spring security 异常 - 200-299
    ES_SECURITY_000("ES_SECURITY_000","身份认证失败"), //AuthenticationException
    ES_SECURITY_001("ES_SECURITY_001","拒绝访问"), //AccessDeniedException
    
    @Getter
    @Setter
    private String code;
    
    @Getter
    @Setter
    private String message;
    // 构造方法
    private WebExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
    
}	
```

如果在reactor流中使用，可以使用
```
return Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_004));
```
当然那也可以直接
```
throw new EventException(SecurityExceptionEnum.ES_SECURITY_004);
```
## lambda-framework-compliance
合规组件，有2个核心的设计理念，统一规范 和 敏捷开发
我们从mvc去理解，
先看controller层

1.DefaultBasicController
2.DefaultTreeController
```
public  class DefaultBasicController<PO extends UnifyPO,ID,Service extends IDefaultBasicService<PO,ID>> {

public class DefaultTreeController<PO extends UnifyPO & IFlattenTreePO,ID,Service extends IDefaultTreeService<PO,ID>> extends DefaultBasicController<PO,ID,Service>{
```
其中 Basic是基础的 包含了增删改查的基础接口，类似于自动生产代码的controller，直接继承就能实现。
而Tree是树的构建，比如这张表的结构是 id parentId orgId 这是一颗树的类型。 继承了这个controller后就能实现对树的增删改查的操作接口

service层
1.DefaultBasicServiceImpl
2.DefaultTreeServiceImpl
```
public class DefaultBasicServiceImpl<PO extends UnifyPO,ID,Repository extends ReactiveMySqlCrudRepositoryOperation<PO,ID>>  implements IDefaultBasicService<PO,ID> {

public class DefaultTreeServiceImpl<PO extends UnifyPO & IFlattenTreePO,ID,Repository extends ReactiveMySqlCrudRepositoryOperation<PO,ID>>  extends DefaultBasicServiceImpl<PO,ID,Repository> implements IDefaultTreeService<PO,ID> {
```
其中UnifyPO 是每个数据库表的
private LocalDateTime createTime;
```
    private LocalDateTime updateTime;

    private Long creatorId;

    private Long updaterId;

    private String creatorName;

    private String updaterName;
```
都有的字段，IFlattenTreePO 是这棵树的扁平化展示 他是一个接口，代表了这颗树的核心结构
```
    public Long getId();

    public void setId(Long id);

    public Long getParentId();

    public void setParentId(Long parentId);

    public Long getOrganizationId();

    public void setOrganizationId(Long organizationId);

    public <PO extends IFlattenTreePO>List<PO> getChildrens();

    public <PO extends IFlattenTreePO>void setChildrens(List<PO> childrens);
```

登录用户模型，SecurityLoginUser和AbstractLoginUser
```
public interface SecurityLoginUser {

    public Long getId();
    public Long getOrganizationId();
    public String getName();
}
```
SecurityLoginUser是security模块里的，他是security抽象出来的一种用户模型。
AbstractLoginUser是compliance抽象出来的，实现了SecurityLoginUser的方法，他是用户模型的基础类也是父类。
通过他，我才能用范型去写抽象的方法。
## lambda-framework-redis
在pom文件中引用下面代码块
```		
<dependency>
	<groupId>org.lambda.framework</groupId>
	<artifactId>lambda-framework-redis</artifactId>
</dependency>
```
可以针对多数据源配置,针对不同的模块，你可以这样去配置
如果要使用相同的host地址不同的database可以先定义一个抽象超类，
```
    public abstract class AbstractSecurityRedisConfig extends AbstractReactiveRedisConfig {
    //##Redis服务器地址
    @Value("${lambda.security.redis.host:0}")
    protected String host;
    //## Redis服务器连接端口
    @Value("${lambda.security.redis.port:6379}")
    protected Integer port;
    //连接池密码
    @Value("${lambda.security.redis.password:}")
    protected String password;
    //# 连接池最大连接数
    @Value("${lambda.security.redis.lettuce.pool.max_active:8}")
    protected Integer maxActive;
    //# 连接池最大阻塞等待时间（使用负值表示没有限制）
    @Value("${lambda.security.redis.lettuce.pool.max_wait_seconds:50}")
    protected Integer maxWaitSeconds;

    //# 连接池中的最大空闲连接
    @Value("${lambda.security.redis.lettuce.pool.max_idle:8}")
    protected Integer maxIdle;

    //# 连接池中的最小空闲连接
    @Value("${lambda.security.redis.lettuce.pool.min_idle:0}")
    protected Integer minIdle;


    @Override
    protected String host() {
        return this.host;
    }

    @Override
    protected Integer port() {
        return this.port;
    }

    @Override
    protected String password() {
        return this.password;
    }

    @Override
    protected Integer maxActive() {
        return this.maxActive;
    }

    @Override
    protected Integer maxWaitSeconds() {
        return this.maxWaitSeconds;
    }

    @Override
    protected Integer maxIdle() {
        return this.maxIdle;
    }

    @Override
    protected Integer minIdle() {
        return this.minIdle;
    }
}
```
通过实现超类，将database自定义配置暴露出去
```
@Configuration
public class SecurityAuthRedisConfig extends AbstractSecurityRedisConfig {
    //##数据库序号
    @Value("${lambda.security.redis.auth.database:0}")
    private Integer database;
    @Bean("securityAuthRedisOperation")
    public ReactiveRedisOperation securityAuthRedisOperation(){
        return buildRedisOperation();
    }

    @Override
    protected Integer database() {
        return this.database;
    }
}
```
## lambda-framework-repository
在pom文件中引用下面代码块
```		
<dependency>
	<groupId>org.lambda.framework</groupId>
	<artifactId>lambda-framework-repository</artifactId>
</dependency>
```
可以针对多数据源配置,针对不同的模块，你可以这样去配置
如果要使用相同的host地址不同的database可以先定义一个抽象超类，
```
    public abstract class AbstractCustomerMysqlRepositoryConfig extends AbstractReactiveMysqlRepositoryConfig {
    //##mysql服务器地址
    protected abstract String host();
    //##mysql用户名
    protected abstract String user();
    //##mysql密码
    protected abstract String password();
    //##mysql库名
    protected abstract String database();
    //##mysql端口
    protected abstract Integer port();
    //##mysql最大超时时间
    protected abstract Integer connectTimeoutSeconds();
    //##mysql线程池-线程最大空闲时间
    protected abstract Integer maxIdleTimeSeconds();
    //##mysql线程池-线程最大数量
    protected abstract Integer maxSize();
    ...get/set
}
```
通过实现超类，将database自定义配置暴露出去
```
@Configuration
public class CustomerMysqlRepositoryConfig extends AbstractCustomerMysqlRepositoryConfig {
    //##数据库序号
    @Value("${xxx.xxx.xxx.xxx.database:store}")
    private Integer database;
    
    @Bean("customerMysqlConnectionFactory")
    public ConnectionFactory customerMysqlConnectionFactory(){
        return buildMysqlConnectionFactory();
    }
}
```
配置好了ConnectionFactory后需要通过继承ReactiveMySqlRepositoryOperation，并声明@Repository,如果使用了这种形式，只能使用单数据源
```
public class CustomerMysqlRepositoryOperation extends ReactiveMySqlRepositoryOperation{
  @Query("select o  from Employee o where id=(select max(id) from Employee t1)")
  Employee getEmployeeByMaxId();
}
```
如果要使用多数据源的形式，通过ConnectionFactory去使用
```
@Resource(name="customerMysqlConnectionFactory")
private ConnectionFactory customerMysqlConnectionFactory;

pulic Mono get(){
    Mono.from(connectionFactory.create())
            .flatMap(connection -> Mono.from(
                    connection.createStatement("SELECT id,name,phone,mail FROM user WHERE name = ?name")
                            .bind("name", "张三")
                            .execute())
    
            )})).subscribe(u -> System.out.println(u.toString()));
}
```
## lambda-framework-rpc
lambda-framework-rpc使用了spring6的声明式http interface 来作为rpc框架的支持。
将远程接口暴露的class文件必须放入 名为facade包的目录下，facade包可以在任意位置
class文件位置遵循以下原则:
```
classpath*:/**/facade/**/*.class
```
使用方式
```
@HttpExchange("https://mockend.com/Fall-Rain/mockend/posts")
public interface UserApi {
@GetExchange
    List<User> getUsers();
}
```
如果是注册中心，则将https://mockend.com/Fall-Rain/mockend/posts 改成 注册中心中的服务名
## lambda-framework-guid
在pom文件中引用下面代码块

```		
<dependency>
	<groupId>org.lambda.framework</groupId>
	<artifactId>lambda-framework-guid</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```
引用guid之后你必须配置
```
lambda.guid.datacenter_id=xxx
lambda.guid.machine_id=xxx
```
并注入
```
@Resource
private GuidFactory guidFactory;
```
才可使用GUID()方法
```
guidFactory.GUID();
```
## lambda-framework-security
在pom文件中引用下面代码块
```
<dependency>
  <groupId>org.lambda.framework</groupId>
  <artifactId>lambda-framework-security</artifactId>
</dependency>
```
###统一认证
在请求的headers中添加Auth-Token：xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
可以使用如下的例子来设置token
```
@Resource
private SecurityPrincipalUtil securityPprincipalUtil;

@GetMapping("/login")
public Mono login(){  //1
    User user =  User.builder().age(30).name("王刚").school("兰州理工大学").build();
    String userJson = JsonUtil.objToString(user);
    String authToken = principalUtil.setPrincipalToToken(userJson);
    return returning(authToken);
}
```

并配置auth和autz的redis-database信息
```
lambda.security.redis.host=47.98.122.4
lambda.security.redis.password=XXXXXXXXXXXX
#default
lambda.security.redis.port=6379
lambda.security.redis.lettuce.pool.max_active=8
lambda.security.redis.lettuce.pool.max_wait_seconds=50
lambda.security.redis.lettuce.pool.max_idle=8
lambda.security.redis.lettuce.pool.min_idle=0
```
lambda-security已经写好了统一身份认证,无论是多服务和单机应用程序都都可以使用auth-token的形式登陆
在引用lambda-security后你需要在properties配置文件中配置你的auth认证的redis数据库和autz授权redis数据库
lambda-security默认使用redis来存储用户的auth-token和request path权限信息。
如果需要更改auth-token和request path权限信息的存储位置,可以重新配置bean
例如：
```
    CustomAuthManager extends SecurityAuthManager
    
    CustomAutzManager extends SecurityAutzManager
    
```
重写父类的方法覆盖掉逻辑并注入

```
    
    @Bean
    public SecurityAutzManager customAutzManager(CustomAuthManager customAuthManager){
        return new CustomAutzManager(customAuthManager) {
            @Override
            public boolean verify(String currentPathAutzTree, String principal) {
	    	//your code 
                return true;
            }
        };
    }

    @Bean
    public SecurityAuthManager customAuthManager(){
        return new CustomAuthManager(){
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
SecurityAuthManager.verify
```

授权校验的逻辑需要自己去写逻辑,框架提供了verify接口
```
SecurityAutzManager.verify(String currentPathAutzTree,String principal)
```
currentPathAutzTree代表当前路径的权限树
principal代表当前用户信息
你可以在verify接口中去校验currentPathAutzTree中的角色和权限是否存在于principal


lambda.security.url-autz-model=MAPPING or ALL
当你没有配置路径树 ，当检查路径树为空的时候 
当值为ALL的时候，会拒绝访问 ，方便于生产环境，所有的路径都需要在路径树中

当你没有配置路径树，当检查路径树为空的时候,当值为mapping的时候，会放过请求，方便开发环境

当路径树不为空的时候，都会经过SecurityAutzManager.verify方法进行校验(此时，url-autz-model将会失效)

## lambda-framework-web
web框架的核心 lambda框架采用了springboot3.x并使用了spring boot web-flux响应式web框架,详情见lambda-framework-demo
因为采用的web-flux，请不要提前订阅你的mono or flux 框架会帮你自动处理 否则无法response给api调用者
以下代码块是错误示范:
```
error code

public class DemoApplication extends WebResponseHandler

@GetMapping("/testSecurity1")
public Mono testSecurity1(){  
    userService.getUser().subscribe()
    return returning();
}    
```
在启动类中添加
```
@SpringBootApplication(scanBasePackages = {"org.lambda.framework","your packages"} )
@EnableWebFlux
@RestController

```
来开启lambda-framework-web功能
框架中已经写好了统一异常，WebGlobalExceptionHandler
使用这样的形式去抛出自己的异常，否则都为ES_WEB_000
```
throw new EventException(ES_WEB_000);
```
```
{
    "serviceCode": "E00000000",
    "serviceMessage": "操作成功",
    "data": "lambda.auth.token.c495e2e9e4a32c94d3791f5602d20a97"
}
```
```
{
    "serviceCode": "ES_SECURITY_003",
    "serviceMessage": "无效令牌"
}
```
你的controller类需要继承WebResponseHandler
```
public class DemoApplication extends WebResponseHandler

@GetMapping("/testSecurity1")
public Mono testSecurity1(){  
    return returning();
}    
```
我用了全局返回信息的转换，所以不需要特定的模版了。只需要这样返回 便能得到统一的格式 


```

    @PostMapping("/login")
    public Mono<String> login(@RequestBody LoginDTO loginDTO){
        return service.login(loginDTO);
    }

```

如果是返回mono 便会返回统一的格式
```
{
"serviceCode": "E00000000",
"serviceMessage": "操作成功",
"data": "lambda.security.auth-token.57283d40bd38cf3936c2b79b4314433a"
}
```

如果是返回Stirng 便会返回统一的格式
```
    @PostMapping("/login")
    public String login(@RequestBody LoginDTO loginDTO){
        return service.login(loginDTO);
    }
```
那便直接返回值，不具备统一格式的返回
```
hello word
```

## lambda-framework-openai
在pom文件中引用下面代码块
```
  <dependency>
    <groupId>org.lambda.framework</groupId>
    <artifactId>lambda-framework-openai</artifactId>
  </dependency>
```
使用下面的示例来调用
```
UniqueParam openAiUniqueParam = UniqueParam.builder().uniqueId(req.getUniqueId()).uniqueTime(req.getUniqueTime()).build();
        ImageParam param =  ImageParam.builder()
                .prompt(req.getPrompt())
                .uniqueParam(uniqueParam)
                .userId(userId)
                .apiKey(apiKey)
                .n(4)
                .size(Contract.image_size_512)
                .responseFormat(Contract.responseFormat)
                .timeOut(Contract.clientTimeOut)
                .quota(quato)
                .maxTokens(imageTokens(Contract.image_size_512,4) + encoding(req.getPrompt()))
                .build();
        return returning(openAiImageService.execute(param).flatMap(e->{
            //模拟扣减配额
            quato = quato-e.getTotalTokens();
            return Mono.just(e);
        }));
```



