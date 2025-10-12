package com.rockthejvm.jobsboard.config

import pureconfig.ConfigReader

final case class AppConfig(
    postgresConfig: PostgresConfig,
    emberConfig: EmberConfig
) derives ConfigReader


