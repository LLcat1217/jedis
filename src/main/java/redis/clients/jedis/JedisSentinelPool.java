package redis.clients.jedis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class JedisSentinelPool extends JedisPoolAbstract {

  /**
   * @deprecated This will be private in future.
   */
  @Deprecated
  protected static Logger log = LoggerFactory.getLogger(JedisSentinelPool.class);

  protected final GenericObjectPoolConfig<Jedis> poolConfig;

  protected final int connectionTimeout;
  protected final int soTimeout;
  protected final int infiniteSoTimeout;

  protected final String user;
  protected final String password;
  protected final int database;
  protected final String clientName;

  protected int sentinelConnectionTimeout;
  protected int sentinelSoTimeout;
  protected String sentinelUser;
  protected String sentinelPassword;
  protected String sentinelClientName;

  protected final Set<MasterListener> masterListeners = new HashSet<>();

  private volatile JedisFactory factory;
  private volatile HostAndPort currentHostMaster;
  
  private final Object initPoolLock = new Object();

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels) {
    this(masterName, sentinels, new GenericObjectPoolConfig<Jedis>(), Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels, String password) {
    this(masterName, sentinels, new GenericObjectPoolConfig<Jedis>(), Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels, String password, String sentinelPassword) {
    this(masterName, sentinels, new GenericObjectPoolConfig<Jedis>(), Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT,
        password, Protocol.DEFAULT_DATABASE, null, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, sentinelPassword, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String password) {
    this(masterName, sentinels, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int timeout) {
    this(masterName, sentinels, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final String password) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String password,
      final int database) {
    this(masterName, sentinels, poolConfig, timeout, timeout, null, password, database);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String user,
      final String password, final int database) {
    this(masterName, sentinels, poolConfig, timeout, timeout, user, password, database);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String password,
      final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, timeout, timeout, password, database, clientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, int timeout, final String user,
      final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, timeout, timeout, user, password, database, clientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String password, final int database) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, null, password, database, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String user, final String password, final int database) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, user, password, database, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, null, password, database, clientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String user, final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, user, password, database, clientName,
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, null, null, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String user, final String password, final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, infiniteSoTimeout, user, password, database, clientName,
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, null, null, null);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String password, final int database, final String clientName,
      final int sentinelConnectionTimeout, final int sentinelSoTimeout, final String sentinelPassword,
      final String sentinelClientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, null, password, database, clientName,
        sentinelConnectionTimeout, sentinelSoTimeout, null, sentinelPassword, sentinelClientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig, final int connectionTimeout, final int soTimeout,
      final String user, final String password, final int database, final String clientName,
      final int sentinelConnectionTimeout, final int sentinelSoTimeout, final String sentinelUser,
      final String sentinelPassword, final String sentinelClientName) {
    this(masterName, sentinels, poolConfig, connectionTimeout, soTimeout, 0, user, password, database, clientName,
        sentinelConnectionTimeout, sentinelSoTimeout, sentinelUser, sentinelPassword, sentinelClientName);
  }

  public JedisSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig<Jedis> poolConfig,
      final int connectionTimeout, final int soTimeout, final int infiniteSoTimeout,
      final String user, final String password, final int database, final String clientName,
      final int sentinelConnectionTimeout, final int sentinelSoTimeout, final String sentinelUser,
      final String sentinelPassword, final String sentinelClientName) {

    this.poolConfig = poolConfig;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.infiniteSoTimeout = infiniteSoTimeout;
    this.user = user;
    this.password = password;
    this.database = database;
    this.clientName = clientName;
    this.sentinelConnectionTimeout = sentinelConnectionTimeout;
    this.sentinelSoTimeout = sentinelSoTimeout;
    this.sentinelUser = sentinelUser;
    this.sentinelPassword = sentinelPassword;
    this.sentinelClientName = sentinelClientName;

    HostAndPort master = initSentinels(sentinels, masterName);
    initPool(master);
  }

  @Override
  public void destroy() {
    for (MasterListener m : masterListeners) {
      m.shutdown();
    }

    super.destroy();
  }

  public HostAndPort getCurrentHostMaster() {
    return currentHostMaster;
  }

  private void initPool(HostAndPort master) {
    synchronized(initPoolLock){
      if (!master.equals(currentHostMaster)) {
        currentHostMaster = master;
        if (factory == null) {
          factory = new JedisFactory(master.getHost(), master.getPort(), connectionTimeout,
              soTimeout, infiniteSoTimeout, user, password, database, clientName);
          initPool(poolConfig, factory);
        } else {
          factory.setHostAndPort(currentHostMaster);
          // although we clear the pool, we still have to check the returned object in getResource,
          // this call only clears idle instances, not borrowed instances
          clearInternalPool();
        }

        log.info("Created JedisPool to master at {}", master);
      }
    }
  }

  private HostAndPort initSentinels(Set<String> sentinels, final String masterName) {

    HostAndPort master = null;
    boolean sentinelAvailable = false;

    log.info("Trying to find master from available Sentinels...");

    for (String sentinel : sentinels) {
      final HostAndPort hap = HostAndPort.parseString(sentinel);

      log.debug("Connecting to Sentinel {}", hap);

      
      try (Jedis jedis = new Jedis(hap.getHost(), hap.getPort(), sentinelConnectionTimeout, sentinelSoTimeout)){
        if (sentinelUser != null) {
          jedis.auth(sentinelUser, sentinelPassword);
        } else if (sentinelPassword != null) {
          jedis.auth(sentinelPassword);
        }
        if (sentinelClientName != null) {
          jedis.clientSetname(sentinelClientName);
        }

        List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName);

        // connected to sentinel...
        sentinelAvailable = true;

        if (masterAddr == null || masterAddr.size() != 2) {
          log.warn("Can not get master addr, master name: {}. Sentinel: {}", masterName, hap);
          continue;
        }

        master = toHostAndPort(masterAddr);
        log.debug("Found Redis master at {}", master);
        break;
      } catch (JedisException e) {
        // resolves #1036, it should handle JedisException there's another chance
        // of raising JedisDataException
        log.warn(
          "Cannot get master address from sentinel running @ {}. Reason: {}. Trying next one.", hap, e);
      }
    }

    if (master == null) {
      if (sentinelAvailable) {
        // can connect to sentinel, but master name seems to not
        // monitored
        throw new JedisException("Can connect to sentinel, but " + masterName
            + " seems to be not monitored...");
      } else {
        throw new JedisConnectionException("All sentinels down, cannot determine where is "
            + masterName + " master is running...");
      }
    }

    log.info("Redis master running at {}, starting Sentinel listeners...", master);

    for (String sentinel : sentinels) {
      final HostAndPort hap = HostAndPort.parseString(sentinel);
      MasterListener masterListener = new MasterListener(masterName, hap.getHost(), hap.getPort());
      // whether MasterListener threads are alive or not, process can be stopped
      masterListener.setDaemon(true);
      masterListeners.add(masterListener);
      masterListener.start();
    }

    return master;
  }

  private HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
    String host = getMasterAddrByNameResult.get(0);
    int port = Integer.parseInt(getMasterAddrByNameResult.get(1));

    return new HostAndPort(host, port);
  }

  @Override
  public Jedis getResource() {
    while (true) {
      Jedis jedis = super.getResource();
      jedis.setDataSource(this);

      // get a reference because it can change concurrently
      final HostAndPort master = currentHostMaster;
      final HostAndPort connection = new HostAndPort(jedis.getClient().getHost(), jedis.getClient()
          .getPort());

      if (master.equals(connection)) {
        // connected to the correct master
        return jedis;
      } else {
        returnBrokenResource(jedis);
      }
    }
  }

  @Override
  public void returnResource(final Jedis resource) {
    if (resource != null) {
      try {
        resource.resetState();
        returnResourceObject(resource);
      } catch (Exception e) {
        returnBrokenResource(resource);
        log.debug("Resource is returned to the pool as broken", e);
      }
    }
  }

  protected class MasterListener extends Thread {

    protected String masterName;
    protected String host;
    protected int port;
    protected long subscribeRetryWaitTimeMillis = 5000;
    protected volatile Jedis j;
    protected AtomicBoolean running = new AtomicBoolean(false);

    protected MasterListener() {
    }

    public MasterListener(String masterName, String host, int port) {
      super(String.format("MasterListener-%s-[%s:%d]", masterName, host, port));
      this.masterName = masterName;
      this.host = host;
      this.port = port;
    }

    public MasterListener(String masterName, String host, int port,
        long subscribeRetryWaitTimeMillis) {
      this(masterName, host, port);
      this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
    }

    @Override
    public void run() {

      running.set(true);

      while (running.get()) {

        try {
          // double check that it is not being shutdown
          if (!running.get()) {
            break;
          }
          
          j = new Jedis(host, port, sentinelConnectionTimeout, sentinelSoTimeout);
          if (sentinelUser != null) {
            j.auth(sentinelUser, sentinelPassword);
          } else if (sentinelPassword != null) {
            j.auth(sentinelPassword);
          }
          if (sentinelClientName != null) {
            j.clientSetname(sentinelClientName);
          }

          // code for active refresh
          List<String> masterAddr = j.sentinelGetMasterAddrByName(masterName);
          if (masterAddr == null || masterAddr.size() != 2) {
            log.warn("Can not get master addr, master name: {}. Sentinel: {}:{}.", masterName, host, port);
          } else {
            initPool(toHostAndPort(masterAddr));
          }

          j.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
              log.debug("Sentinel {}:{} published: {}.", host, port, message);

              String[] switchMasterMsg = message.split(" ");

              if (switchMasterMsg.length > 3) {

                if (masterName.equals(switchMasterMsg[0])) {
                  initPool(toHostAndPort(Arrays.asList(switchMasterMsg[3], switchMasterMsg[4])));
                } else {
                  log.debug(
                    "Ignoring message on +switch-master for master name {}, our master name is {}",
                    switchMasterMsg[0], masterName);
                }

              } else {
                log.error(
                  "Invalid message received on Sentinel {}:{} on channel +switch-master: {}", host,
                  port, message);
              }
            }
          }, "+switch-master");

        } catch (JedisException e) {

          if (running.get()) {
            log.error("Lost connection to Sentinel at {}:{}. Sleeping 5000ms and retrying.", host,
              port, e);
            try {
              Thread.sleep(subscribeRetryWaitTimeMillis);
            } catch (InterruptedException e1) {
              log.error("Sleep interrupted: ", e1);
            }
          } else {
            log.debug("Unsubscribing from Sentinel at {}:{}", host, port);
          }
        } finally {
          if (j != null) {
            j.close();
          }
        }
      }
    }

    public void shutdown() {
      try {
        log.debug("Shutting down listener on {}:{}", host, port);
        running.set(false);
        // This isn't good, the Jedis object is not thread safe
        if (j != null) {
          j.disconnect();
        }
      } catch (Exception e) {
        log.error("Caught exception while shutting down: ", e);
      }
    }
  }
}
