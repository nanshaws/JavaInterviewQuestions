# Redis分布式锁使用及问题

但是有四个错误的用法

## **场景一问题**（**死锁**）

​		**错误用法**：先通过setnx上锁，再通过expire设置过期时间，最后执行完任务后手动del释放锁；

### 问题分析

​		通过setnx上锁后**出现异常**，导致**无法**去expire**设置锁的过期时间**，更**无法**最后去**手动释放锁**，造成**死锁**！

```java
/**
     * redis分布式事务锁使用及注意事项（死锁：没有保证上锁和设置锁过期时间的原子性）
     * @throws Exception
     */
    @Test
    public void testLockDemo1() {
        try {
            //上锁
            jedis.setnx("lock","lwd");
            //系统故障(这个锁永远不会释放了)

            //设置上锁过期时间
            jedis.expire("lock",10);

            //业务功能代码....
        } finally {
            jedis.del("lock");
        }
    }

```

### 问题解决

使用上锁最新写法，**保证上锁、设置过期时间**一步完成的**原子性**：**set(lockKey,value,nx,ex,exporeTime);**

```java
/**
     * redis分布式事务锁使用及注意事项（死锁解决）
     * @throws Exception
     */
    @Test
    public void testLockDemo2() {
        try {
            //上锁、设置上锁过期时间保证原子性
            jedis.set("lock","lwd","nx","ex",10);

            //业务功能代码....
        } finally {
            jedis.del("lock");
        }
    }
```





## **场景二问题**（**误删锁**）

### 问题分析

**A线程**中**上锁并设置过期时间**完成以后，系统出现了**故障**，导致**锁到了过期时间并自动删除**了，这时**还没有**执行**手动释放锁**的操作，这个时候**B机器上锁成功**，并去执行任务，**任务还未执行完**，**A机器**反应过来了，继续**执行了手动释放锁**的操作，**把B线程**上的**锁给误删了**。

```java
    /**
     * redis分布式事务锁使用及注意事项（误删锁场景一：没有给锁上ID）
     * @throws Exception
     */
    @Test
    public void testLockDemo3() {
        try {
            //上锁、设置上锁过期时间保证原子性
            jedis.set("lock","lwd","nx","ex",10);

            //业务功能代码....
            //系统故障了
            Thread.sleep(20);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            jedis.del("lock");
        }
    }
```

### 问题解决

上锁同时**加上一个锁id**，如当前线程ID，将**锁id**存入**value**值并记录在变量中，**手动释放锁**的时候比较一下value中的锁id跟变量中id是否一致，也就是**判断一 下是否自己还在持有锁**，如果不是，就不执行删除操作了。

```java
/**
     * redis分布式事务锁使用及注意事项（误删锁场景一 解决）
     * @throws Exception
     */
    @Test
    public void testLockDemo4() {
        //锁ID
        String lockId = String.valueOf(Thread.currentThread().getId());  
        try {
            //上锁、设置上锁过期时间保证原子性
            jedis.set("lock",lockId,"nx","ex",10); 

            //业务功能代码....
            //系统故障了
            Thread.sleep(20);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            String nowLockId = jedis.get("lock"); 
            if(lockId.equals(nowLockId)) {
                jedis.del("lock");
            }
        }
    }
```



## **场景三问题**（**误删锁**）

### 问题分析

这种误删锁是基于场景2**判断锁id**和**释放锁操作**这两步没有保证原子性所导致的。

 具体为：A机器**带锁id方式取锁、设置过期时间并执行完任务后**，希望通过判断比较锁id之后去释放锁，**判断通过后系统出现阻塞**，阻塞到锁也到了过期时间自动释放了锁，这时还未进行手动释放锁操作，这个时候B机器上锁成功，并去执行任务，**任务还未执行完**，**A机器**反应过来了，继续**执行了手动释放锁**的操作，**把B机器**上的**锁给误删了**。

```java
    /**
     * redis分布式事务锁使用及注意事项（误删锁场景二：没有保证判断锁ID和删除锁的原子性）
     * @throws Exception
     */
    @Test
    public void testLockDemo5() throws InterruptedException {
        String lockId = String.valueOf(Thread.currentThread().getId()); AID  BID
        try {
            //上锁、设置上锁过期时间保证原子性
            jedis.set("lock",lockId,"nx","ex",10);     lock   BID
			//执行业务代码
        } finally {
            String nowLockId = jedis.get("lock");
            if(lockId.equals(nowLockId)) {
                //系统故障了  
                Thread.sleep(15);
                jedis.del("lock");
            }
        }
    }
```

​	

### 问题解决

​	保证**判断锁和释放锁的原子性**：使用redis执行**LUA脚本**，保证**一步执行判断锁和释放锁**。

		String luaScript = 'if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end';

​		redisClient.eval(luaScript , Collections.singletonList(key), Collections.singletonList(threadId));

```java
    /**
     * redis分布式事务锁使用及注意事项（误删锁场景二解决）
     * @throws Exception
     */
    @Test
    public void testLockDemo6() throws InterruptedException {
        String lockId = String.valueOf(Thread.currentThread().getId());  1
        try {
            //上锁、设置上锁过期时间保证原子性
            jedis.set("lock",lockId,"nx","ex",10);
			
        } finally {
            String luaScript = "\'if redis.call(\'get\', KEYS[1]) == ARGV[1] then return redis.call(\'del\', KEYS[1]) else return 0 end\'";
        
            jedis.eval(luaScript , Collections.singletonList("lock"), 		Collections.singletonList(lockId));
        }
    }
```



## **场景四问题（锁续命）**

### 问题分析

这种场景是指线程中任务还没执行完，锁就已经到过期时间

### 问题解决

这种情况可以给任务执行线程添加守护线程，守护线程负责对锁的expire时间进行监控，每当到过期前一秒就对过期进行判断，如果任务还在进行且锁马上过期，则对过期时间重新进行设置。

## Redis 锁中间件使用

   	补充：其实关于上述四个场景的问题，使用redis客户端Redisson都能够得到很好的解决，Redisson内部已经实现了上述几点问题的解决机制，原理同上。	



