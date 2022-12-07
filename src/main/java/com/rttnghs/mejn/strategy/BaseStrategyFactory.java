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
package com.rttnghs.mejn.strategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rttnghs.mejn.configuration.Config;

/**
 * A Base StrategyFactory implementation that can read strategies from
 * configuration and build strategies.
 */
public class BaseStrategyFactory implements StrategyFactory {

	private static final Logger logger = LogManager.getLogger(BaseStrategyFactory.class);

	private final List<String> strategyNames;
	private final List<String> strategyClasses;
	private final List<String> strategyParametersList;

	/**
	 * Create a strategy factory that retrieves strategies from the strategy congig
	 * file.
	 */
	public BaseStrategyFactory() {
		strategyNames = Config.configuration.getList(String.class, "strategies.strategy.name");
		strategyClasses = Config.configuration.getList(String.class, "strategies.strategy.class");
		strategyParametersList = Config.configuration.getList(String.class, "strategies.strategy.parameters");

	}

	@Override
	public Strategy getStrategy(String strategyName) {
		int index = strategyNames.indexOf(strategyName);
		if (index == -1) {
			throw new IllegalArgumentException("Invalid strategyName " + strategyName);
		}
		return getStrategy(strategyName, strategyClasses.get(index), strategyParametersList.get(index));

	}

	/**
	 * Create a strategy by reading the strategy parameters from configuration. If
	 * parameters are specified, it will invoke a constructor with String name and
	 * List<Integer> arguments, otherwise it will invoke a constructor with only a
	 * String name parameter.
	 * 
	 * @param strategyName      To pass to the strategy constructor as the first
	 *                          argument
	 * @param strategyClassName Class name for the strategy to be created through
	 *                          reflection.
	 * @param strategyParamters if
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Strategy getStrategy(String strategyName, String strategyClassName, String strategyParamters) {
		try {
			Class<?> strategyClass = Class.forName(strategyClassName);
			Constructor<? extends Strategy> cons;
			Strategy strategy;
			if ((strategyParamters == null) || (strategyParamters.equals(""))) {
				cons = (Constructor<? extends Strategy>) strategyClass.getConstructor(String.class);
				strategy = cons.newInstance(strategyName);
			} else {
				cons = (Constructor<? extends Strategy>) strategyClass.getConstructor(String.class, List.class);
				List<Integer> parameterList = Stream.of(strategyParamters.split(",", -1)).map(String::trim)
						.map(Integer::parseInt).collect(Collectors.toList());
				strategy = cons.newInstance(strategyName, parameterList);
			}
			return strategy;
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.fatal("Unable to properly construct Strategies from configuration", e);
			// Let it rip
			throw new RuntimeException("Unable to properly construct Strategies from configuration.", e);
		}
	}

}
