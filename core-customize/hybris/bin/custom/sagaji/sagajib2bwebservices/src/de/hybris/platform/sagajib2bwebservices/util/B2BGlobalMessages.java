/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * Displays "confirmation, information, error" messages
 */
public final class B2BGlobalMessages {
    public static final String CONF_MESSAGES_HOLDER = "accConfMsgs";
    public static final String INFO_MESSAGES_HOLDER = "accInfoMsgs";
    public static final String ERROR_MESSAGES_HOLDER = "accErrorMsgs";

    private B2BGlobalMessages() {

    }

    public static void addConfMessage(final Model model, final String messageKey)
    {
        addMessage(model, CONF_MESSAGES_HOLDER, messageKey, null);
    }

    public static void addInfoMessage(final Model model, final String messageKey)
    {
        addMessage(model, INFO_MESSAGES_HOLDER, messageKey, null);
    }

    public static void addErrorMessage(final Model model, final String messageKey)
    {
        addMessage(model, ERROR_MESSAGES_HOLDER, messageKey, null);
    }

    public static void addMessage(final Model model, final String messageHolder, final String messageKey,
                                  final Object[] attributes)
    {
        final B2BGlobalMessage message = buildMessage(messageKey, attributes);

        final Map<String, Object> modelMap = model.asMap();
        if (modelMap.containsKey(messageHolder))
        {
            final List<B2BGlobalMessage> messages = new ArrayList<>((List<B2BGlobalMessage>) modelMap.get(messageHolder));
            messages.add(message);
            model.addAttribute(messageHolder, messages);
        }
        else
        {
            model.addAttribute(messageHolder, Collections.singletonList(message));
        }
    }

    public static void addFlashMessage(final RedirectAttributes model, final String messageHolder, final String messageKey)
    {
        addFlashMessage(model, messageHolder, messageKey, null);
    }

    public static void addFlashMessage(final RedirectAttributes model, final String messageHolder, final String messageKey,
                                       final Object[] attributes)
    {
        final B2BGlobalMessage message = buildMessage(messageKey, attributes);

        final Map<String, ?> flashModelMap = model.getFlashAttributes();
        if (flashModelMap.containsKey(messageHolder))
        {
            final List<B2BGlobalMessage> messages = new ArrayList<>((List<B2BGlobalMessage>) flashModelMap.get(messageHolder));
            messages.add(message);
            model.addFlashAttribute(messageHolder, messages);
        }
        else
        {
            model.addFlashAttribute(messageHolder, Collections.singletonList(message));
        }
    }

    public static void addFlashMessage(final Map<String, Object> flashMap, final String messageHolder, final String messageKey,
                                       final Object[] attributes)
    {
        final B2BGlobalMessage message = buildMessage(messageKey, attributes);

        if (flashMap.containsKey(messageHolder))
        {
            final List<B2BGlobalMessage> messages = new ArrayList<>((List<B2BGlobalMessage>) flashMap.get(messageHolder));
            messages.add(message);
            flashMap.put(messageHolder, messages);
        }
        else
        {
            flashMap.put(messageHolder, Collections.singletonList(message));
        }
    }

    private static B2BGlobalMessage buildMessage(final String messageKey, final Object[] attributes)
    {
        final B2BGlobalMessage message = new B2BGlobalMessage();
        message.setCode(messageKey);
        message.setAttributes(attributes != null ? Arrays.asList(attributes) : Collections.emptyList());

        return message;
    }

    public static boolean containsMessage(final Model model, final String messageHolder, final String messageKey)
    {
        final Map<String, Object> modelMap = model.asMap();
        if (modelMap.containsKey(messageHolder))
        {
            final List<B2BGlobalMessage> messages = new ArrayList<>((List<B2BGlobalMessage>) modelMap.get(messageHolder));
            return messages.stream().anyMatch(globalMessage -> globalMessage.getCode().equals(messageKey));
        }
        else
        {
            return false;
        }
    }
}
