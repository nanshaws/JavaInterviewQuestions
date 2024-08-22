# Redis实现限流的三种方式

## 一、固定窗口

所谓固定窗口限流即时间窗口的起始和结束时间是固定的，在固定时间段内允许要求的请求数量访问，超过则拒绝；当固定时间段结束后，再重新开始下一个时间段进行计数。

可以根据当前的时间，以分钟为时间段，每分钟都生成一个key，用来inc，当达到请求数量就返回一些友好信息。

```java
  /**
   * @author: AngJie
   * @create: 2022-07-26 14:41
   **/
   @RestController
   @RequestMapping("/redisTest")
   public class RedisTestController {
       @Autowired
       private RedisTemplate redisTemplate;
       private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
       
       @GetMapping("/Fixed")
       public String testFixedWindow() {
           String now = formatter.format(LocalDateTime.now());
           Long count = redisTemplate.opsForValue().increment(now + ":fixed");
           if (count > 5) {
               return "不好意思，服务器正忙，请一分钟后再试......";
           } else {
               return "服务端正在处理";
           }
       }
   }
```




​       该方式优点是比较简单粗暴，缺点是不够灵活，对于边界问题不能够处理，如设置的时间段刚开始时流量占满了设置的最大次数，后面一段时间则不能够再进行访问，必须等该时间段过了后才可以再次访问。

## 二、滑动窗口

针对固定窗口限流的问题，可以采用滑动窗口来优化改善。所谓滑动窗口即设置的时间窗口的起始和结束时间是不断变化的，时间差值不变，允许的请求数量不变。

我们可以将请求打造成一个zset数组，当每一次请求进来的时候，value保持唯一，可以用UUID生成，而score可以用当前时间戳表示，因为score我们可以用来计算当前时间戳之内有多少的请求数量。而zset数据结构也提供了zrange方法让我们可以很轻易的获取到2个时间戳内有多少请求。

```java
/**

 * @author: AngJie

 * @create: 2022-07-26 14:41
   **/
   @RestController
   @RequestMapping("/redisTest")
   public class RedisTestController {

   @Autowired
   private RedisTemplate redisTemplate;

   @GetMapping("/Sliding")
   public String testSlidingWindow() {
       Long currentTime = System.currentTimeMillis();

       System.out.println(currentTime);
       if (redisTemplate.hasKey("limit")) {
           // intervalTime是限流的时间
           Long intervalTime = 60000L;
           Integer count = redisTemplate.opsForZSet().rangeByScore("limit", currentTime - intervalTime, currentTime).size();
           System.out.println(count);
           if (count != null && count > 5) {
               return "每分钟最多只能访问5次";
           }
       }
       redisTemplate.opsForZSet().add("limit", UUID.randomUUID().toString(), currentTime);
       return "访问成功";
   }
}
```


通过上述代码可以做到滑动窗口的效果，并且能保证每N秒内至多M个请求，缺点就是zset的数据结构会越来越大。实现方式相对也是比较简单的。

## 三、令牌桶

Redisson可以实现很多东西,在Redis的基础上,Redisson做了超多的封装，不仅可以用来实现分布式锁，还可以帮助我们实现令牌桶限流。

RateLimter主要作用就是可以限制调用接口的次数。主要原理就是调用接口之前，需要拥有指定个令牌。限流器每秒会产生X个令牌放入令牌桶，调用接口需要去令牌桶里面拿令牌。如果令牌被其它请求拿完了，那么自然而然，当前请求就调用不到指定的接口。

```java
RateLimter实现限流
   /**
	* @author: AngJie
 	* @create: 2022-07-26 14:41
   **/
   @RestController
   @RequestMapping("/redisTest")
   public class RedisTestController {

   @Autowired
   private RedisTemplate redisTemplate;

   @Autowired
   private Redisson redisson;

   @GetMapping("/Token")
   public String testTokenBucket() {
       RRateLimiter rateLimiter = redisson.getRateLimiter("myRateLimiter");

       // 最大流速 = 每10秒钟产生1个令牌
       rateLimiter.trySetRate(RateType.OVERALL, 1, 10, RateIntervalUnit.SECONDS);
       //需要1个令牌
       if (rateLimiter.tryAcquire(1)) {
           return "令牌桶里面有可使用的令牌";
       }
       return "不好意思，请过十秒钟再来~~~~~~~";

   }
```

