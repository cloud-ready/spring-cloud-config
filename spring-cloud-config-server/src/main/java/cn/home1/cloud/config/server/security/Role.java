package cn.home1.cloud.config.server.security;

public enum Role {
  /**
   * /actuator/* endpoints
   */
  ACTUATOR,
  /**
   * admin
   */
  ADMIN,
  /**
   * /monitor hook
   */
  HOOK,
  /**
   * config user
   */
  USER
}
