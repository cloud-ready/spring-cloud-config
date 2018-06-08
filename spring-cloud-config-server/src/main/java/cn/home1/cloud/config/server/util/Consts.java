package cn.home1.cloud.config.server.util;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public abstract class Consts {

  public static final String DATA_DIRECTORY = System.getProperty("user.home") + "/.data/config-server";

  public static final String DOT_ENV = ".env";

  public static final String PRIVILEGE_ENV_PROFILE_ = "PRIVILEGE_ENV_PROFILE_";
  public static final String PRIVILEGE_ENV_PROFILE_WILDCARD = PRIVILEGE_ENV_PROFILE_ + "*";
}
