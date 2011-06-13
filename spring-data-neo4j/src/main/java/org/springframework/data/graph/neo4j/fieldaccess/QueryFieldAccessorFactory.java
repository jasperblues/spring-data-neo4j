/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.annotation.GraphQuery;
import org.springframework.data.graph.annotation.GraphTraversal;
import org.springframework.data.graph.core.FieldTraversalDescriptionBuilder;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.node.Neo4jNodeBacking;
import org.springframework.data.graph.neo4j.support.query.QueryExecutor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.springframework.data.graph.neo4j.support.DoReturn.doReturn;

public class QueryFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
	@Override
    public boolean accept(final Field f) {
        final GraphQuery graphQuery = f.getAnnotation(GraphQuery.class);
        return graphQuery != null
                && !graphQuery.value().isEmpty();
    }


    @Override
    public FieldAccessor<NodeBacked> forField(final Field field) {
        return new QueryFieldAccessor(field);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class QueryFieldAccessor implements FieldAccessor<NodeBacked> {
	    protected final Field field;
	    private final String query;
	    private Class<?> target;
        protected String[] params;
        private boolean iterableResult;

        public QueryFieldAccessor(final Field field) {
	        this.field = field;
            final GraphQuery graphQuery = field.getAnnotation(GraphQuery.class);
	        this.target = graphQuery.elementClass();
            this.params = graphQuery.params();
            this.query = graphQuery.value();
            this.iterableResult = Iterable.class.isAssignableFrom(field.getType());
	    }

	    @Override
	    public boolean isWriteable(NodeBacked nodeBacked) {
	        return false;
	    }

	    @Override
	    public Object setValue(final NodeBacked nodeBacked, final Object newVal) {
	        throw new InvalidDataAccessApiUsageException("Cannot set readonly query field " + field);
	    }

	    @Override
	    public Object getValue(final NodeBacked nodeBacked) {
            final String queryString = String.format(this.query, (Object[])createPlaceholderParams(nodeBacked));
            return doReturn(executeQuery(nodeBacked, queryString));
	    }

        private Object executeQuery(NodeBacked nodeBacked, String queryString) {
            if (iterableResult) {
                if (target.equals(Object.class)) return nodeBacked.findAllByQuery(queryString);
                return nodeBacked.findAllByQuery(queryString, this.target);
            }
            return nodeBacked.findByQuery(queryString,this.target);
        }

        private Object[] createPlaceholderParams(NodeBacked nodeBacked) {
            if (params.length==0) return new Object[] {nodeBacked.getNodeId()};

            final Object[] parameters = new Object[1 + this.params.length];
            parameters[0]=nodeBacked.getNodeId();
            System.arraycopy(this.params,0,parameters,1,this.params.length);
            return parameters;
        }
    }
}
