package io.github.jvlealc.marketsphere.billing.translator;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MessageTranslator {

    private final MessageSource messageSource;

    public MessageTranslator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String translate(String messageCode, Object... args) {
        Locale currentLocale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(messageCode, args, currentLocale);
    }
}
