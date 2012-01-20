/*
 * Copyright (C) 2011 Benoit GUEROUT <bguerout at gmail dot com> and Yves AMSELLEM <amsellem dot yves at gmail dot com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jongo;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.jongo.jackson.EntityProcessor;
import com.jongo.model.Coordinate;
import com.jongo.model.Coordinate3D;
import com.jongo.model.Poi;
import com.jongo.model.User;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class MongoCollectionTest {

    private MongoCollection mongoCollection;
    private String address = "22 rue des murlins", id = "1";
    private int lat = 48, lng = 2, alt = 7;

    @Before
    public void setUp() throws UnknownHostException, MongoException {
        mongoCollection = connect("jongo", "poi", true);
    }

    private MongoCollection connect(String dbname, String colname, boolean drop) throws UnknownHostException {
        DBCollection collection = new Mongo().getDB(dbname).getCollection(colname);
        MongoCollection col = new MongoCollection(collection, new EntityProcessor());
        if (drop)
            col.drop();
        return col;
    }

    String addressExists = "{address:{$exists:true}}";

    @Test
    public void canFindOne() throws Exception {
        /* given */
        String id = mongoCollection.save(new Poi("999", address));

        /* when */
        String poiId = mongoCollection.findOne(addressExists).map(new IdDBObjectMapper());
        Poi poi = mongoCollection.findOne(addressExists).as(Poi.class);

        /* then */
        assertThat(poiId).isEqualTo(id);
        assertThat(poi.id).isEqualTo(id);
    }

    @Test
    public void canFindOneWithParameters() throws Exception {
        /* given */
        String id = mongoCollection.save(new Poi("999", address));

        /* when */
        String poiId = mongoCollection.findOne("{_id:#}", id).map(new IdDBObjectMapper());
        Poi poi = mongoCollection.findOne("{_id:#}", id).as(Poi.class);

        /* then */
        assertThat(poiId).isEqualTo(id);
        assertThat(poi.id).isEqualTo(id);
    }

    @Test
    public void canFindOneWithPartialFieldsLoading() throws Exception {
        /* given */
        String id = mongoCollection.save(new Poi("999", address));

        /* when */
        mongoCollection.findOne("{_id:999}").on(id).map(new DBObjectMapper<String>() {
            @Override
            public String map(DBObject result) {
                /* then */
                assertThat(result.containsField("address")).isFalse();
                return null;
            }
        });
    }

    @Test
    public void shouldHandleEmptyResult() throws Exception {
        assertThat(mongoCollection.findOne("{_id:'invalid-id'}").as(Poi.class)).isNull();
        assertThat(mongoCollection.findOne("{_id:'invalid-id'}").map(new IdDBObjectMapper())).isNull();

        assertThat(mongoCollection.find("{_id:'invalid-id'}").as(Poi.class)).hasSize(0);
    }

    @Test
    public void canFindEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(id, address));

        /* when */
        Iterator<String> strings = mongoCollection.find(addressExists).map(new IdDBObjectMapper());
        Iterator<Poi> pois = mongoCollection.find(addressExists).as(Poi.class);

        /* then */
        assertThat(strings.next()).isEqualTo(id);
        assertThat(pois.next().id).isEqualTo(id);

        assertThat(strings.hasNext()).isFalse();
        assertThat(pois.hasNext()).isFalse();
    }

    @Test
    public void canFindEntitiesWithParameters() throws Exception {
        /* given */
        mongoCollection.save(new Poi(id, address));

        /* when */
        Iterator<String> strings = mongoCollection.find("{_id:#}", "1").map(new IdDBObjectMapper());

        /* then */
        assertThat(strings.hasNext()).isTrue();
        assertThat(strings.next()).isEqualTo(id);
    }

    @Test
    public void canFindEntitiesWithPartialFieldsLoading() throws Exception {
        /* given */
        String id = mongoCollection.save(new Poi("999", address));

        /* when */
        mongoCollection.findOne("{_id:999}").on(id).map(new DBObjectMapper<String>() {
            @Override
            public String map(DBObject result) {
                /* then */
                assertThat(result.containsField("address")).isFalse();
                return null;
            }
        });
    }

    @Test
    public void canFindWithObjectId() throws Exception {
        /* given */
        String id = mongoCollection.save(new User("john"));
        User user = mongoCollection.findOne("{_id:{$oid:#}}", id).as(User.class);

        /* then */
        assertThat(user).isNotNull();
        assertThat(user.id).isEqualTo(id);
    }

    @Test
    public void canFindUsingSubProperty() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));

        /* when */
        Iterator<Poi> results = mongoCollection.find("{'coordinate.lat':48}").as(Poi.class);

        /* then */
        assertThat(results.next().coordinate.lat).isEqualTo(lat);
        assertThat(results.hasNext()).isFalse();
    }

    @Test
    public void canSortEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi("23 rue des murlins"));
        mongoCollection.save(new Poi("21 rue des murlins"));
        mongoCollection.save(new Poi("22 rue des murlins"));

        /* when */
        Iterator<Poi> results = mongoCollection.find("{'$query':{}, '$orderby':{'address':1}}").as(Poi.class);

        /* then */
        assertThat(results.next().address).isEqualTo("21 rue des murlins");
        assertThat(results.next().address).isEqualTo("22 rue des murlins");
        assertThat(results.next().address).isEqualTo("23 rue des murlins");
        assertThat(results.hasNext()).isFalse();
    }

    @Test
    public void canLimitEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address));
        mongoCollection.save(new Poi(address));
        mongoCollection.save(new Poi(address));

        /* when */
        Iterator<Poi> results = mongoCollection.find("{'$query':{}, '$maxScan':2}").as(Poi.class);

        /* then */
        assertThat(results).hasSize(2);
    }

    @Test
    public void canUseConditionnalOperator() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, 1, 1));
        mongoCollection.save(new Poi(address, 2, 1));
        mongoCollection.save(new Poi(address, 3, 1));

        /* then */
        assertThat(mongoCollection.find("{coordinate.lat: {$gt: 2}}").as(Poi.class)).hasSize(1);
        assertThat(mongoCollection.find("{coordinate.lat: {$lt: 2}}").as(Poi.class)).hasSize(1);
        assertThat(mongoCollection.find("{coordinate.lat: {$gte: 2}}").as(Poi.class)).hasSize(2);
        assertThat(mongoCollection.find("{coordinate.lat: {$lte: 2}}").as(Poi.class)).hasSize(2);
        assertThat(mongoCollection.find("{coordinate.lat: {$gt: 1, $lt: 3}}").as(Poi.class)).hasSize(1);

        assertThat(mongoCollection.find("{coordinate.lat: {$ne: 2}}").as(Poi.class)).hasSize(2);
        assertThat(mongoCollection.find("{coordinate.lat: {$in: [1,2,3]}}").as(Poi.class)).hasSize(3);
    }

    @Test
    public void canUseGeospacial() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, 1, 1));
        mongoCollection.save(new Poi(address, 4, 4));

        MongoCollection indexes = connect("jongo", "system.indexes", false);
        indexes.index("{'name': 'coordinate' , 'ns' : 'jongo.poi' , 'key' : { 'coordinate' : '2d'}}");

        /* then */
        assertThat(mongoCollection.find("{'coordinate': {'$near': [0,0], $maxDistance: 5}}").as(Poi.class)).hasSize(1);
        assertThat(mongoCollection.find("{'coordinate': {'$near': [2,2], $maxDistance: 5}}").as(Poi.class)).hasSize(2);
        assertThat(mongoCollection.find("{'coordinate': {'$within': {'$box': [[0,0],[2,2]]}}}").as(Poi.class)).hasSize(1);
        assertThat(mongoCollection.find("{'coordinate': {'$within': {'$center': [[0,0],5]}}}").as(Poi.class)).hasSize(1);
    }

    @Test
    public void canFilterDistinctStringEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address));
        mongoCollection.save(new Poi(address));
        mongoCollection.save(new Poi("23 rue des murlins"));

        /* when */
        Iterator<String> addresses = mongoCollection.distinct("address", "", String.class);

        /* then */
        assertThat(addresses.next()).isEqualTo(address);
        assertThat(addresses.next()).isEqualTo("23 rue des murlins");
        assertThat(addresses.hasNext()).isFalse();
    }

    @Test
    public void canFilterDistinctIntegerEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, 4, 1));

        /* when */
        Iterator<Integer> addresses = mongoCollection.distinct("coordinate.lat", "", Integer.class);

        /* then */
        assertThat(addresses.next()).isEqualTo(lat);
        assertThat(addresses.next()).isEqualTo(4);
        assertThat(addresses.hasNext()).isFalse();
    }

    @Test
    public void canFilterDistinctEntitiesOnTypedProperty() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, 4, 1));

        /* when */
        Iterator<Coordinate> coordinates = mongoCollection.distinct("coordinate", "", Coordinate.class);

        /* then */
        Coordinate first = coordinates.next();
        assertThat(first.lat).isEqualTo(lat);
        assertThat(first.lng).isEqualTo(lng);
        Coordinate second = coordinates.next();
        assertThat(second.lat).isEqualTo(4);
        assertThat(second.lng).isEqualTo(1);
        assertThat(coordinates.hasNext()).isFalse();
    }

    @Test
    public void canFilterDistinctEntitiesWithQuery() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(null, 4, 1));

        /* when */
        Iterator<Coordinate> coordinates = mongoCollection.distinct("coordinate", addressExists, Coordinate.class);

        /* then */
        Coordinate first = coordinates.next();
        assertThat(first.lat).isEqualTo(lat);
        assertThat(first.lng).isEqualTo(lng);
        assertThat(coordinates.hasNext()).isFalse();
    }

    @Test
    public void canCountEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(null, 4, 1));

        /* then */
        assertThat(mongoCollection.count(addressExists)).isEqualTo(1);
        assertThat(mongoCollection.count("{'coordinate.lat': {$exists:true}}")).isEqualTo(2);
    }

    @Test
    public void canUpdateEntity() throws Exception {
        /* given */
        mongoCollection.save(new Poi(id, address));
        Iterator<Poi> pois = mongoCollection.find("{_id: '1'}").as(Poi.class);
        Poi poi = pois.next();
        poi.address = null;
        mongoCollection.save(poi);

        /* when */
        pois = mongoCollection.find("{_id: '1'}").as(Poi.class);

        /* then */
        poi = pois.next();
        assertThat(poi.id).isEqualTo(id);
        assertThat(poi.address).isNull();
    }

    @Test
    @Ignore
    public void canFindInheritedEntity() throws IOException {
        mongoCollection.save(new Poi(id, lat, lng, alt));
        Poi poi = mongoCollection.findOne("{_id: #}", id).as(Poi.class);
        assertThat(poi.coordinate).isInstanceOf(Coordinate3D.class);
    }

    @Test
    public void canGetCollectionName() throws Exception {
        assertThat(mongoCollection.getName()).isEqualTo("poi");
    }

    private static class IdDBObjectMapper implements DBObjectMapper<String> {
        @Override
        public String map(DBObject result) {
            return result.get(MongoCollection.MONGO_ID).toString();
        }
    }
}