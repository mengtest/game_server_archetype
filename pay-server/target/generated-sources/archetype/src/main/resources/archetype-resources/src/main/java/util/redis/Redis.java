#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util.redis;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Tuple;

import ${package}.core.GameInit;

public class Redis {
	private static Redis instance;
	public static Logger log = LoggerFactory.getLogger(Redis.class);
	public static final int GLOBAL_DB = 0;// 全局
	public static String password = null;

	public static Redis getInstance() {
		if (instance == null) {
			instance = new Redis();
		}
		return instance;
	}

	// private JedisPool pool;
	private JedisSentinelPool sentinelPool;
	public String host;
	public int port;

	// public int getDB(long userid){
	//
	// }

	public Jedis getJedis() {
		return this.sentinelPool.getResource();
	}

	public void returnResource(Jedis jedis) {
		this.sentinelPool.returnResource(jedis);
	}

	public void init() {
		String redisServer = null;
		if (GameInit.cfg != null) {
			redisServer = GameInit.cfg.get("redisServer");
			password = GameInit.cfg.get("redisPwd");
		}
		if (redisServer == null) {
			redisServer = "123.59.110.201:6440";
		}
		redisServer = redisServer.trim();
		String[] tmp = redisServer.split(":");
		host = tmp[0];
		port = Integer.parseInt(tmp[1]);
		if (tmp.length == 2) {
			port = Integer.parseInt(tmp[1].trim());
		}
		log.info("Redis sentinel at {}:{}", host, port);
		// sentinelPool = new JedisPool(host, port);
		Set sentinels = new HashSet();
		sentinels.add(new HostAndPort(host, port).toString());
		sentinelPool = new JedisSentinelPool("master1", sentinels);
	}

	// private void init() {
	// String redisServer = null;
	// if (GameInit.cfg != null) {
	// redisServer = GameInit.cfg.get("redisServer");
	// password = GameInit.cfg.get("redisPwd");
	// }
	// if (redisServer == null) {
	// redisServer = "123.59.110.201:6379";
	// }
	// redisServer = redisServer.trim();
	// String[] tmp = redisServer.split(":");
	// host = tmp[0];
	// port = Integer.parseInt(tmp[1]);
	// if (tmp.length == 2) {
	// port = Integer.parseInt(tmp[1].trim());
	// }
	// log.info("Redis at {}:{}", host, port);
	// // sentinelPool = new JedisPool(host, port);
	// JedisPoolConfig config = new JedisPoolConfig();
	// sentinelPool = new JedisPool(config, host, port, 100000, password);
	// }

	public void test() {
		Jedis j = getJedis();
		j.auth(password);
		returnResource(j);
	}

	public void select(int index) {
		Jedis j = getJedis();
		j.auth(password);
		j.select(index);
		returnResource(j);
	}

	public static Map<String, String> objectToHash(Object obj)
			throws IntrospectionException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Map<String, String> map = new HashMap<String, String>();
		BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
		PropertyDescriptor[] propertyDescriptors = beanInfo
				.getPropertyDescriptors();
		for (PropertyDescriptor property : propertyDescriptors) {
			if (!property.getName().equals("class")) {
				map.put(property.getName(), ""
						+ property.getReadMethod().invoke(obj));
			}
		}
		return map;
	}

	public static void hashToObject(Map<?, ?> map, Object obj)
			throws IllegalAccessException, InvocationTargetException {
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (entry.getValue().equals("null")) {
				entry.setValue(null);
			}
		}
		BeanUtils.populate(obj, (Map) map);
	}

	@SuppressWarnings("unchecked")
	public static <T> T hashToObject(Map<?, ?> map, Class c)
			throws IllegalAccessException, InvocationTargetException,
			InstantiationException {
		Object obj = c.newInstance();
		hashToObject(map, obj);
		return (T) obj;
	}

	public List<String> hmget(int db, String key, String... fields) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		List<String> ret = redis.hmget(key, fields);
		redis.select(db);
		returnResource(redis);
		return ret;
	}

	public String hmset(int db, String key, Object object)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, IntrospectionException {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		String ret = redis.hmset(key, objectToHash(object));
		returnResource(redis);
		return ret;
	}

	public String hmset(int db, String key, Map<String, String> fields)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, IntrospectionException {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		String ret = redis.hmset(key, fields);
		returnResource(redis);
		return ret;
	}

	public boolean hexist(int db, String key, String field) {
		if (key == null) {
			return false;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		boolean ret = redis.hexists(key, field);
		returnResource(redis);
		return ret;
	}

	public Long hdel(int db, String key, String... fields) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Long cnt = redis.hdel(key, fields);
		returnResource(redis);
		return cnt;
	}

	public String hget(int db, String key, String field) {
		if (key == null) {
			return null;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		String ret = redis.hget(key, field);
		returnResource(redis);
		return ret;
	}

	public void hset(int db, String key, String field, String value) {
		if (field == null || field.length() == 0) {
			return;
		}
		if (value == null || value.length() == 0) {
			return;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		redis.hset(key, field, value);
		returnResource(redis);
	}

	/**
	 * Map 的存放和获取
	 */
	public void add(int db, String group, Map<String, String> values) {
		if (values == null || values.size() == 0) {
			return;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		redis.hmset(group, values);
		returnResource(redis);
	}

	public void add(int db, String group, String key, String value) {
		if (value == null || key == null) {
			return;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		redis.hset(group, key, value);
		returnResource(redis);
	}

	public void set(int db, String key, String value) {
		if (value == null || key == null) {
			return;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		redis.set(key, value);
		returnResource(redis);
	}

	public Long hDelBuilder(int db, String group, String... keys) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		byte[][] fields = new byte[keys.length][];
		for (int i = 0; i < keys.length; i++) {
			fields[i] = keys[i].getBytes();
		}
		Long cnt = redis.hdel(group.getBytes(), fields);
		returnResource(redis);
		return cnt;
	}

	public Map<String, String> getMap(int db, String group) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Map<String, String> ret = redis.hgetAll(group);
		returnResource(redis);
		return ret;
	}

	public String get(int db, String key) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		String ret = redis.get(key);
		returnResource(redis);
		return ret;
	}

	/**
	 * 添加元素到集合中
	 * 
	 * @param key
	 * @param element
	 */
	public boolean sadd(int db, String key, String... element) {
		if (element == null || element.length == 0) {
			return false;
		}

		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		boolean success = redis.sadd(key, element) == 1;
		returnResource(redis);
		return success;
	}

	public boolean smove(int db, String oldKey, String newKey, String element) {
		if (element == null) {
			return false;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		boolean success = (redis.smove(oldKey, newKey, element) == 1);
		returnResource(redis);
		return success;
	}

	/**
	 * 删除指定set内的元素
	 * */
	public boolean sremove(int db, String key, String... element) {
		if (element == null) {
			return false;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		boolean success = (redis.srem(key, element) == 1);
		returnResource(redis);
		return success;
	}

	public Set<String> sget(int db, String key) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Set<String> m = redis.smembers(key);
		returnResource(redis);
		return m;
	}

	/**
	 * 返回set的的元素个数
	 * 
	 * @Title: zcard_
	 * @Description:
	 * @param key
	 * @return
	 */
	public long scard_(int db, String key) {
		Jedis redis = getJedis();
		redis.auth(password);
		long size = redis.scard(key);
		returnResource(redis);
		return size;
	}

	public void laddList(int db, String key, String... elements) {
		if (elements == null || elements.length == 0) {
			return;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		redis.lpush(key, elements);
		returnResource(redis);
	}

	/**
	 * 
	 * @Title: lpush_
	 * @Description:
	 * @param key
	 * @param id
	 */
	public void lpush_(int db, String key, String id) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		redis.lpush(key, id);
		returnResource(redis);
	}

	public void rpush_(int db, String key, String id) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		redis.rpush(key, id);
		returnResource(redis);
	}

	/**
	 * add by wangzhuan
	 * 
	 * @Title: lrange
	 * @Description:
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public List<String> lrange(int db, String key, int start, int end) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		List<String> list = redis.lrange(key, start, end);
		returnResource(redis);
		return list;
	}

	public List<String> lgetList(int db, String key) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		long len = redis.llen(key);
		List<String> ret = redis.lrange(key, 0, len - 1);
		returnResource(redis);
		return ret;
	}

	/**
	 * 列表list中是否包含value
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean lexist(int db, String key, String value) {
		List<String> list = lgetList(db, key);
		return list.contains(value);
	}

	public List<String> lgetList(int db, String key, long len) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		long max = redis.llen(key);
		long l = max > len ? len : max;
		List<String> ret = redis.lrange(key, 0, l - 1);
		returnResource(redis);
		return ret;
	}

	public Long del(int db, String key) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Long cnt = redis.del(key);
		returnResource(redis);
		return cnt;
	}

	/**
	 * 模糊删除
	 * 
	 * @param key
	 * @return
	 */
	public Long delKeyLikes(int db, String key) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Set<String> keys = redis.keys(key);
		Long cnt = redis.del(keys.toArray(new String[keys.size()]));
		returnResource(redis);
		return cnt;
	}

	/**
	 * 测试元素是否存在
	 * 
	 * @param key
	 * @param element
	 * @return
	 */
	public boolean sexist(int db, String key, String element) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		boolean ret = redis.sismember(key, element);
		returnResource(redis);
		return ret;
	}

	/**
	 * 判断某一个key值得存储结构是否存在
	 * 
	 * @Title: exist_
	 * @Description:
	 * @param key
	 * @return
	 */
	public boolean exist_(int db, String key) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		boolean yes = redis.exists(key);
		returnResource(redis);
		return yes;
	}

	/**********************************************************************
	 * 排行用到的SortedSet
	 **********************************************************************/
	public long zadd(int db, String key, int score, String member) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		long ret = 0;
		try {
			ret = redis.zadd(key, score, member);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			returnResource(redis);
		}
		return ret;
	}

	/**
	 * 添加 分数，并返回修改后的值
	 * 
	 * @param key
	 * @param update
	 * @param member
	 * @return
	 */
	public double zincrby(int db, String key, int update, String member) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		double ret = redis.zincrby(key, update, member);
		returnResource(redis);
		return ret;
	}

	/**
	 * 返回有序集 key 中，成员 member 的 score 值,存在返回score，不存在返回-1
	 * 
	 * @param key
	 * @param member
	 * @return
	 */
	public double zscore(int db, String key, String member) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Double ret = redis.zscore(key, member);
		returnResource(redis);
		if (ret == null) {
			return -1;
		}
		return ret;
	}

	/**
	 * 按 从大到小的排名，获取 member的 排名
	 * 
	 * @param key
	 * @param member
	 * @return
	 */
	public long zrevrank(int db, String key, String member) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		long ret = redis.zrevrank(key, member);
		returnResource(redis);
		return ret;
	}

	/**
	 * 按照score的值从小到大排序，返回member的排名 排序是从0开始
	 * 
	 * @Title: zrank
	 * @Description:
	 * @param key
	 * @param member
	 * @return 设置为名次从1开始。返回为-1，表示member无记录
	 */
	public long zrank(int db, String key, String member) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		long ret = -1;
		Long vv = redis.zrank(key, member);
		if (vv != null) {
			ret = vv.longValue();
		}
		returnResource(redis);
		if (ret != -1) {
			ret += 1;
		}
		return ret;
	}

	/**
	 * 返回的是score的值
	 * 
	 * @Title: zscore_
	 * @Description:
	 * @param key
	 * @param member
	 * @return 返回有序集 key 中，成员 member 的 score 值 如果 member 元素不是有序集 key 的成员，或 key
	 *         不存在，返回 null 。
	 */
	public int zscore_(int db, String key, String member) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		int ret = -1;
		Double vv = redis.zscore(key, member);
		if (vv != null) {
			ret = (int) vv.doubleValue();
		}
		returnResource(redis);
		if (ret != -1) {
			ret += 1;
		}
		return ret;
	}

	/**
	 * min 和max 都是score的值
	 * 
	 * @Title: zrangebyscore_
	 * @Description:
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	// add 20141216
	public Set<String> zrangebyscore_(int db, String key, long min, long max) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Set<String> ss = redis.zrangeByScore(key, min, max);
		returnResource(redis);
		return ss;
	}

	public Set<String> zrange(int db, String key, long min, long max) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Set<String> ss = redis.zrange(key, min, max);
		returnResource(redis);
		return ss;
	}

	/**
	 * min 和max 都是score的值 获得一个包含了score的元组集合. 元组（Tuple）
	 * 笛卡尔积中每一个元素（d1，d2，…，dn）叫作一个n元组（n-tuple）或简称元组
	 * 
	 * @Title: zrangebyscorewithscores_
	 * @Description:
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public Set<Tuple> zrangebyscorewithscores_(int db, String key, long min,
			long max) {
		Jedis redis = getJedis();
		redis.auth(password);
		if (redis == null) {
			return null;
		}
		redis.select(db);
		Set<Tuple> result = null;
		try {
			result = redis.zrangeByScoreWithScores(key, min, max);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			returnResource(redis);
		}
		return result;
	}

	/**
	 * zrevrangeWithScores ： 从大到小排序 zrangeWithScores ： 从小到大排序
	 * 
	 * @Title: zrangeWithScores
	 * @Description:
	 * @param key
	 * @param start
	 *            ： （排名）0表示第一个元素，-x：表示倒数第x个元素
	 * @param end
	 *            ： （排名）-1表示最后一个元素（最大值）
	 * @return 返回 排名在start 、end之间带score元素
	 */
	public Map<String, Double> zrevrangeWithScores(int db, String key,
			long start, long end) {
		Jedis redis = getJedis();
		redis.auth(password);
		if (redis == null) {
			return null;
		}
		redis.select(db);
		Set<Tuple> result = null;
		try {
			result = redis.zrevrangeWithScores(key, start, end);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			returnResource(redis);
		}
		return tupleToMap(db, result);
	}

	/**
	 * @Title: tupleToMap
	 * @Description:
	 * @param tupleSet
	 * @return Map<String, Double> ： 返回的是 有序<element, score>
	 */
	public Map<String, Double> tupleToMap(int db, Set<Tuple> tupleSet) {
		if (tupleSet == null)
			return null;
		Map<String, Double> map = new LinkedHashMap<String, Double>();
		for (Tuple tup : tupleSet) {
			map.put(tup.getElement(), tup.getScore());
		}
		return map;
	}

	/**
	 * 删除key中的member
	 * 
	 * @Title: zrem
	 * @Description:
	 * @param key
	 * @param mem
	 * @return
	 */
	public long zrem(int db, String key, String member) {
		Jedis redis = getJedis();
		redis.auth(password);
		if (redis == null) {
			return -1;
		}
		redis.select(db);
		long result = -1;
		try {
			result = redis.zrem(key, member);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			returnResource(redis);
		}
		return result;
	}

	/**
	 * 从高到低排名，返回前 num 个score和member
	 * 
	 * @param key
	 * @param num
	 * @return
	 */
	public Set<Tuple> ztopWithScore(int db, String key, int num) {
		if (num <= 0) {
			return null;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Set<Tuple> ret = redis.zrevrangeWithScores(key, 0, num - 1);
		returnResource(redis);
		return ret;
	}

	/**
	 * 返回score区间的member
	 * 
	 * @param key
	 * @param max
	 * @param min
	 * @return
	 */
	public Set<String> zrankByScore(int db, String key, int max, int min) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Set<String> ret = redis.zrevrangeByScore(key, max, min);
		returnResource(redis);
		return ret;
	}

	/**
	 * 从高到低排名，返回前 num 个
	 * 
	 * @param key
	 * @param num
	 * @return
	 */
	public Set<String> ztop(int db, String key, int num) {
		if (num <= 0) {
			return null;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Set<String> ret = redis.zrevrange(key, 0, num - 1);
		returnResource(redis);
		return ret;
	}

	/**
	 * 从高到低排名，返回start到end的前 num 个
	 * 
	 * @param key
	 * @param num
	 * @return
	 */
	public Set<String> ztop(int db, String key, int start, int end) {
		if (end <= start) {
			return null;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		Set<String> ret = redis.zrevrange(key, start, end - 1);
		returnResource(redis);
		return ret;
	}

	/**
	 * 返回zset的的元素个数
	 * 
	 * @Title: zcard_
	 * @Description:
	 * @param key
	 * @return
	 */
	public long zcard_(int db, String key) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		long size = redis.zcard(key);
		returnResource(redis);
		return size;
	}

	public static void destroy() {
		getInstance().sentinelPool.destroy();
	}

	public void publish(int db, String channel, String message) {
		if (channel == null || message == null) {
			return;
		}
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		redis.publish(channel, message);
		returnResource(redis);
	}

	public String lpop(int db, String key) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		String value = redis.lpop(key);
		returnResource(redis);
		return value;
	}

	public void lrem(int db, String key, int count, String value) {
		Jedis redis = getJedis();
		redis.auth(password);
		redis.select(db);
		redis.lrem(key, count, value);
		returnResource(redis);
	}
}
