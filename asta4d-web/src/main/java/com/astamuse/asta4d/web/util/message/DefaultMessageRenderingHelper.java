/*
 * Copyright 2014 astamuse company,Ltd.
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
package com.astamuse.asta4d.web.util.message;

import static com.astamuse.asta4d.render.SpecialRenderer.Clear;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.astamuse.asta4d.Context;
import com.astamuse.asta4d.data.ContextBindData;
import com.astamuse.asta4d.render.ElementNotFoundHandler;
import com.astamuse.asta4d.render.ElementSetter;
import com.astamuse.asta4d.render.Renderable;
import com.astamuse.asta4d.render.Renderer;
import com.astamuse.asta4d.template.Template;
import com.astamuse.asta4d.template.TemplateException;
import com.astamuse.asta4d.template.TemplateNotFoundException;
import com.astamuse.asta4d.util.SelectorUtil;
import com.astamuse.asta4d.util.collection.RowRenderer;
import com.astamuse.asta4d.web.WebApplicationConfiguration;
import com.astamuse.asta4d.web.WebApplicationContext;
import com.astamuse.asta4d.web.dispatch.RedirectInterceptor;
import com.astamuse.asta4d.web.dispatch.RedirectUtil;

public class DefaultMessageRenderingHelper implements MessageRenderingHelper {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageRenderingHelper.class);

    private final static String FLASH_MSG_LIST_KEY = "FLASH_MSG_LIST_KEY#" + DefaultMessageRenderingHelper.class;

    protected final static class MessageHolder {
        MessageRenderingSelector selector;
        MessageRenderingSelector alternativeSelector;
        String message;

        public MessageHolder(MessageRenderingSelector selector, MessageRenderingSelector alternativeSelector, String message) {
            super();
            this.selector = selector;
            this.alternativeSelector = alternativeSelector;
            this.message = message;
        }

    }

    public final static class MessageRenderingSelector {

        private String duplicator;

        private String valueTarget;

        public MessageRenderingSelector() {

        }

        public MessageRenderingSelector(String duplicator, String valueTarget) {
            super();
            this.duplicator = duplicator;
            this.valueTarget = valueTarget;
        }

        public String getDuplicator() {
            return duplicator;
        }

        public void setDuplicator(String duplicator) {
            this.duplicator = duplicator;
        }

        public String getValueTarget() {
            return valueTarget;
        }

        public void setValueTarget(String valueTarget) {
            this.valueTarget = valueTarget;
        }

        @Override
        public int hashCode() {
            return ((duplicator == null) ? 0 : duplicator.hashCode()) ^ ((valueTarget == null) ? 0 : valueTarget.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MessageRenderingSelector other = (MessageRenderingSelector) obj;
            if (duplicator == null) {
                if (other.duplicator != null)
                    return false;
            } else if (!duplicator.equals(other.duplicator))
                return false;
            if (valueTarget == null) {
                if (other.valueTarget != null)
                    return false;
            } else if (!valueTarget.equals(other.valueTarget))
                return false;
            return true;
        }

    }

    private String messageGlobalContainerParentSelector = "body";

    private String messageGlobalContainerSelector = "#global-msg-container";

    private String messageGlobalContainerSnippetFilePath = "/com/astamuse/asta4d/web/util/message/DefaultMessageContainerSnippet.html";

    private MessageRenderingSelector messageGlobalInfoSelector = new MessageRenderingSelector("#info-msg li", ":root");

    private MessageRenderingSelector messageGlobalWarnSelector = new MessageRenderingSelector("#warn-msg li", ":root");

    private MessageRenderingSelector messageGlobalErrSelector = new MessageRenderingSelector("#err-msg li", ":root");

    private String messageDuplicatorIndicatorAttrName = "afd:message-duplicator";

    private Elements cachedSnippet = null;

    private ContextBindData<List<MessageHolder>> messageList = new ContextBindData<List<MessageHolder>>(true) {
        @Override
        protected List<MessageHolder> buildData() {
            return new LinkedList<>();
        }
    };

    public DefaultMessageRenderingHelper() {
        super();
    }

    public static DefaultMessageRenderingHelper getConfiguredInstance() {
        return (DefaultMessageRenderingHelper) WebApplicationConfiguration.getWebApplicationConfiguration().getMessageRenderingHelper();
    }

    public String getMessageGlobalContainerParentSelector() {
        return messageGlobalContainerParentSelector;
    }

    public void setMessageGlobalContainerParentSelector(String messageGlobalContainerParentSelector) {
        this.messageGlobalContainerParentSelector = messageGlobalContainerParentSelector;
    }

    public String getMessageGlobalContainerSelector() {
        return messageGlobalContainerSelector;
    }

    public void setMessageGlobalContainerSelector(String messageGlobalContainerSelector) {
        this.messageGlobalContainerSelector = messageGlobalContainerSelector;
    }

    public String getMessageGlobalContainerSnippetFilePath() {
        return messageGlobalContainerSnippetFilePath;
    }

    public void setMessageGlobalContainerSnippetFilePath(String messageGlobalContainerSnippetFilePath) {
        this.messageGlobalContainerSnippetFilePath = messageGlobalContainerSnippetFilePath;
    }

    public MessageRenderingSelector getMessageGlobalInfoSelector() {
        return messageGlobalInfoSelector;
    }

    public void setMessageGlobalInfoSelector(MessageRenderingSelector messageGlobalInfoSelector) {
        this.messageGlobalInfoSelector = messageGlobalInfoSelector;
    }

    public MessageRenderingSelector getMessageGlobalWarnSelector() {
        return messageGlobalWarnSelector;
    }

    public void setMessageGlobalWarnSelector(MessageRenderingSelector messageGlobalWarnSelector) {
        this.messageGlobalWarnSelector = messageGlobalWarnSelector;
    }

    public MessageRenderingSelector getMessageGlobalErrSelector() {
        return messageGlobalErrSelector;
    }

    public void setMessageGlobalErrSelector(MessageRenderingSelector messageGlobalErrSelector) {
        this.messageGlobalErrSelector = messageGlobalErrSelector;
    }

    public String getMessageDuplicatorIndicatorAttrName() {
        return messageDuplicatorIndicatorAttrName;
    }

    public void setMessageDuplicatorIndicatorAttrName(String messageDuplicatorIndicatorAttrName) {
        this.messageDuplicatorIndicatorAttrName = messageDuplicatorIndicatorAttrName;
    }

    public Renderer createMessageRenderer() {
        Renderer renderer = Renderer.create();

        Renderer message = renderMesssages();
        if (message != null) {
            renderer.add(message);
        }
        renderer.add(postMessageRendering());
        return renderer;
    }

    protected Renderer postMessageRendering() {
        // remove all the remaining message duplicators which may not be referenced in message outputting, which is why they are remaining
        Renderer render = Renderer.create();
        render.disableMissingSelectorWarning();
        render.add(SelectorUtil.attr(messageDuplicatorIndicatorAttrName), Clear);
        render.enableMissingSelectorWarning();
        return render;
    }

    /**
     * 
     * @return Pair.left: whether the alternative message container is necessary <br>
     *         Pair.right: the actual renderer
     */
    protected Renderer renderMesssages() {
        List<MessageHolder> allMsgList = new LinkedList<>();

        allMsgList.addAll(messageList.get());

        if (allMsgList.isEmpty()) {
            return null;
        }
        Renderer renderer = Renderer.create();

        final Map<MessageRenderingSelector, List<MessageHolder>> msgMap = new HashMap<>();
        final Map<MessageRenderingSelector, List<String>> alternativeMsgMap = new HashMap<>();
        List<MessageHolder> tmpList;
        for (MessageHolder mh : allMsgList) {
            tmpList = msgMap.get(mh.selector);
            if (tmpList == null) {
                tmpList = new LinkedList<>();
                msgMap.put(mh.selector, tmpList);
            }
            tmpList.add(mh);
        }

        renderer.disableMissingSelectorWarning();

        for (final Entry<MessageRenderingSelector, List<MessageHolder>> item : msgMap.entrySet()) {
            if (item.getKey() == null) {
                List<String> list;
                for (MessageHolder mh : item.getValue()) {
                    list = alternativeMsgMap.get(mh.alternativeSelector);
                    if (list == null) {
                        list = new LinkedList<>();
                        alternativeMsgMap.put(mh.alternativeSelector, list);
                    }
                    list.add(mh.message);
                }
            } else {
                final MessageRenderingSelector selector = item.getKey();
                renderer.add(selector.duplicator, item.getValue(), new RowRenderer<MessageHolder>() {
                    @Override
                    public Renderer convert(int rowIndex, MessageHolder obj) {
                        Renderer render = Renderer.create(selector.valueTarget, obj.message);
                        render.add(":root", messageDuplicatorIndicatorAttrName, Clear);
                        return render;
                    }
                });
                renderer.add(new ElementNotFoundHandler(selector.duplicator) {
                    @Override
                    public Renderer alternativeRenderer() {
                        List<String> list;
                        for (MessageHolder mh : item.getValue()) {
                            list = alternativeMsgMap.get(mh.alternativeSelector);
                            if (list == null) {
                                list = new LinkedList<>();
                                alternativeMsgMap.put(mh.alternativeSelector, list);
                            }
                            list.add(mh.message);
                        }
                        return Renderer.create();
                    }
                });
            }
        }// end for loop

        renderer.enableMissingSelectorWarning();

        renderer.add(messageGlobalContainerParentSelector, new Renderable() {
            @Override
            public Renderer render() {
                Renderer renderer = Renderer.create();
                if (!alternativeMsgMap.isEmpty()) {
                    renderer.add(new ElementNotFoundHandler(messageGlobalContainerSelector) {
                        @Override
                        public Renderer alternativeRenderer() {
                            // add global message container if not exists
                            return Renderer.create(":root", new ElementSetter() {
                                @Override
                                public void set(Element elem) {
                                    List<Element> elems = new ArrayList<>(retrieveCachedContainerSnippet());
                                    Collections.reverse(elems);
                                    for (Element child : elems) {
                                        elem.prependChild(child.clone());
                                    }
                                }
                            });
                        }// alternativeRenderer
                    });// ElementNotFoundHandler

                    renderer.add(messageGlobalContainerSelector, new Renderable() {
                        @Override
                        public Renderer render() {
                            Renderer alternativeMsgRenderer = Renderer.create();
                            for (final Entry<MessageRenderingSelector, List<String>> item : alternativeMsgMap.entrySet()) {
                                final MessageRenderingSelector selector = item.getKey();
                                alternativeMsgRenderer.add(selector.duplicator, item.getValue(), new RowRenderer<String>() {
                                    @Override
                                    public Renderer convert(int rowIndex, String msg) {
                                        Renderer render = Renderer.create(selector.valueTarget, msg);
                                        render.add(":root", messageDuplicatorIndicatorAttrName, Clear);
                                        return render;
                                    }
                                });
                            }
                            return alternativeMsgRenderer;
                        }
                    });// messageGlobalContainerSelector
                }
                return renderer;
            }
        });

        return renderer;

    }

    protected Elements retrieveCachedContainerSnippet() {
        if (WebApplicationConfiguration.getWebApplicationConfiguration().isCacheEnable()) {
            if (cachedSnippet == null) {
                cachedSnippet = retrieveContainerSnippet();
            }
            return cachedSnippet;
        } else {
            return retrieveContainerSnippet();
        }

    }

    protected Elements retrieveContainerSnippet() {
        WebApplicationConfiguration conf = WebApplicationConfiguration.getWebApplicationConfiguration();
        Template template;
        try {
            // at first, we treat the configured snippet file as a template file
            try {
                template = conf.getTemplateResolver().findTemplate(messageGlobalContainerSnippetFilePath);
            } catch (TemplateNotFoundException e) {
                // then treat it as classpath resource
                // TODO use MultiSearchPathResourceLoader instead for i18n
                InputStream input = this.getClass().getClassLoader().getResourceAsStream(messageGlobalContainerSnippetFilePath);
                if (input == null) {
                    throw new NullPointerException("Configured message container snippet file[" + messageGlobalContainerSnippetFilePath +
                            "] was not found");
                }
                template = new Template(messageGlobalContainerSnippetFilePath, input);
            }
            return template.getDocumentClone().body().children();
        } catch (TemplateException | TemplateNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void outputMessage(final MessageRenderingSelector selector, final MessageRenderingSelector alternativeSelector, final String msg) {
        messageList.get().add(new MessageHolder(selector, alternativeSelector, msg));
        RedirectUtil.registerRedirectInterceptor(this.getClass().getName() + "#outputMessage", new RedirectInterceptor() {
            @Override
            public void beforeRedirect() {
                List<MessageHolder> list = new ArrayList<>(messageList.get());
                if (!list.isEmpty()) {
                    RedirectUtil.addFlashScopeData(FLASH_MSG_LIST_KEY, list);
                }
            }

            @Override
            public void afterRedirectDataRestore() {
                List<MessageHolder> flashedList = Context.getCurrentThreadContext().getData(WebApplicationContext.SCOPE_FLASH,
                        FLASH_MSG_LIST_KEY);
                if (flashedList != null) {
                    messageList.get().addAll(flashedList);
                }
            }
        });
    }

    public void info(String msg) {
        info(null, msg);
    }

    public void info(String selector, String msg) {
        outputMessage(selector == null ? null : new MessageRenderingSelector(selector, ":root"), messageGlobalInfoSelector, msg);
    }

    public void warn(String msg) {
        warn(null, msg);
    }

    public void warn(String selector, String msg) {
        outputMessage(selector == null ? null : new MessageRenderingSelector(selector, ":root"), messageGlobalWarnSelector, msg);
    }

    public void err(String msg) {
        err(null, msg);
    }

    public void err(String selector, String msg) {
        outputMessage(selector == null ? null : new MessageRenderingSelector(selector, ":root"), messageGlobalErrSelector, msg);
    }

}
