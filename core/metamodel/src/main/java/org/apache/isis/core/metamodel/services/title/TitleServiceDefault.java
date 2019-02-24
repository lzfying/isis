/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.metamodel.services.title;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.isis.applib.services.title.TitleService;
import org.apache.isis.applib.services.wrapper.WrapperFactory;
import org.apache.isis.core.metamodel.adapter.ObjectAdapterProvider;
import org.apache.isis.core.metamodel.services.persistsession.ObjectAdapterService;

import lombok.val;

@Singleton
public class TitleServiceDefault implements TitleService {

    @Override
    public String titleOf(final Object domainObject) {
        val objectAdapter = getObjectAdapterProvider().adapterFor(unwrapped(domainObject));
        val destroyed = objectAdapter.isDestroyed();
        if(!destroyed) {
            return objectAdapter.getSpecification().getTitle(null, objectAdapter);
        } else {
            return "[DELETED]";
        }
    }

    @Override
    public String iconNameOf(final Object domainObject) {
        val objectAdapter = getObjectAdapterProvider().adapterFor(unwrapped(domainObject));
        return objectAdapter.getSpecification().getIconName(objectAdapter);
    }

    // //////////////////////////////////////

    private Object unwrapped(Object domainObject) {
        return wrapperFactory != null ? wrapperFactory.unwrap(domainObject) : domainObject;
    }

    // //////////////////////////////////////

    private ObjectAdapterProvider getObjectAdapterProvider() {
        return objectAdapterProvider;
    }
    
    @Inject ObjectAdapterService objectAdapterProvider;
    @Inject WrapperFactory wrapperFactory;

}
