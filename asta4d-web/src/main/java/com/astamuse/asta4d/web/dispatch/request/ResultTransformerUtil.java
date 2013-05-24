/*
 * Copyright 2012 astamuse company,Ltd.
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
 * 
 */

package com.astamuse.asta4d.web.dispatch.request;

import java.util.List;

import com.astamuse.asta4d.web.dispatch.response.provider.ContentProvider;

public class ResultTransformerUtil {

    public final static ContentProvider<?> transform(Object result, List<ResultTransformer> transformerList) {

        if (result instanceof ContentProvider) {
            return (ContentProvider<?>) result;
        }

        ContentProvider<?> cp = null;
        Object before, after;
        before = result;
        for (ResultTransformer resultTransformer : transformerList) {
            after = resultTransformer.transformToContentProvider(before);
            if (after == null) {
                continue;
            } else if (after instanceof ContentProvider) {
                cp = (ContentProvider<?>) after;
                break;
            } else {
                before = after;
                continue;
            }
        }

        if (cp == null) {
            String msg = "Cannot recognize the result type of:%s. Maybe a ResultTransformer is neccessory.";
            String.format(msg, result.getClass().getName());
            throw new UnsupportedOperationException(msg);
        } else {

            return cp;
        }
    }
}