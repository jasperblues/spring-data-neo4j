/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repositories;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repositories.domain.Movie;
import org.springframework.data.neo4j.repositories.domain.User;
import org.springframework.data.neo4j.repositories.repo.MovieRepository;
import org.springframework.data.neo4j.repositories.repo.UserRepository;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactory;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;
import static org.neo4j.ogm.testutil.GraphTestUtils.assertSameGraph;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
public class ProgrammaticRepositoryIT extends MultiDriverTestClass {

    private static GraphDatabaseService graphDatabaseService;

    private MovieRepository movieRepository;
    private SessionFactory sessionFactory = new SessionFactory("org.springframework.data.neo4j.repositories.domain");
    private PlatformTransactionManager platformTransactionManager = new Neo4jTransactionManager(sessionFactory);
    private Session session;
    private TransactionTemplate transactionTemplate;

    @BeforeClass
    public static void beforeClass(){
        graphDatabaseService = getGraphDatabaseService();
    }

    @Before
    public void init() {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);
        session = sessionFactory.openSession();
        session.purgeDatabase();
    }

    @Test
    public void canInstantiateRepositoryProgrammatically() {

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

                movieRepository = factory.getRepository(MovieRepository.class);

                Movie movie = new Movie("PF");
                movieRepository.save(movie);
            }
        });

        assertSameGraph(graphDatabaseService, "CREATE (m:Movie {title:'PF'})");

        assertEquals(1, IterableUtils.count(movieRepository.findAll()));
    }

	/**
     * @see DATAGRAPH-847
     */
    @Test
    @Transactional
    public void shouldBeAbleToDeleteAllViaRepository() {

        RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

        UserRepository userRepository = factory.getRepository(UserRepository.class);

        User userA = new User("A");
        User userB = new User("B");
        userRepository.save(userA);
        userRepository.save(userB);

        assertEquals(2, userRepository.count());

        userRepository.deleteAll();
        assertEquals(0, userRepository.count());
    }

}
