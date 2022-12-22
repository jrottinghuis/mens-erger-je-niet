/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rttnghs.mejn.configuration;

import java.io.File;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.combined.CombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public record Config(int dieFaces, int pawnsPerPlayer, int dotsPerPlayer) {

	private static final Logger logger = LogManager.getLogger(Config.class);

	private static final String CONFIG_FILENAME = "mejn-config.xml";
	public static Configuration configuration;

	static {
		Parameters params = new Parameters();
		CombinedConfigurationBuilder builder = new CombinedConfigurationBuilder()
				.configure(params.fileBased().setFile(new File(CONFIG_FILENAME)));
		try {
			configuration = builder.getConfiguration();
		} catch (ConfigurationException e) {
			logger.fatal("Unable to load configuraiton. Due to " + e);
			throw new RuntimeException("Unable to load configuraiton.", e);
		}
	}

	public static Config value = new Config(configuration.getInt("dieFaces"), configuration.getInt("pawnsPerPlayer"),
			configuration.getInt("dotsPerPlayer"));

	/**
	 * @param dieFaces       for example 6 for cube dice.
	 * @param pawnsPerPlayer For example, 4. Must be less than dotsPerPlayer
	 *                       otherwise home areas will overlap.
	 * @param dotsPerPlayer  How many spots there are on the field between players'
	 *                       start spot. For example, 4 players of 10 spots each, or
	 *                       6 players and 8 spots each.
	 */
	public Config {
		if (dieFaces < 1) {
			throw new IllegalArgumentException("Cannot have a die with <1 faces");
		}
		if (pawnsPerPlayer >= dotsPerPlayer) {
			throw new IllegalArgumentException("pawnsPerPlayermust be smaller than dotsPerPlayer");
		}
		if (pawnsPerPlayer >= dieFaces) {
			throw new IllegalArgumentException("pawnsPerPlayermust be smaller than dieFaces");
		}
	}

}
